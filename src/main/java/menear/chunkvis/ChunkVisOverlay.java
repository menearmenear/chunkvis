package menear.chunkvis;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

public class ChunkVisOverlay implements HudRenderCallback {
    private static final int DISPLAY = 176;
    private static final int PADDING = 6;
    private static final int PLAYER_SIZE = 4;

    private final ChunkVisMinimap minimap;
    private double blocksPerPixel = 1.0;

    public ChunkVisOverlay() {
        this.minimap = new ChunkVisMinimap();
    }

    public void changeZoom(int dir) {
        if (dir > 0) {
            blocksPerPixel = Math.min(blocksPerPixel * 2, 8);
        } else {
            blocksPerPixel = Math.max(blocksPerPixel / 2, 0.25);
        }
    }

    @Override
    public void onHudRender(GuiGraphics graphics, DeltaTracker deltaTracker) {
        if (!ChunkVisMod.overlayVisible) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        Player player = mc.player;
        double exactX = player.getX();
        double exactZ = player.getZ();
        int playerChunkX = (int) Math.floor(exactX / 16);
        int playerChunkZ = (int) Math.floor(exactZ / 16);
        double chunkCenterX = playerChunkX * 16 + 8;
        double chunkCenterZ = playerChunkZ * 16 + 8;

        int mapSize = DISPLAY;
        int startX = PADDING;
        int startY = PADDING;
        int x0 = startX + mapSize / 2;
        int z0 = startY + mapSize / 2;

        minimap.scanAndRender(graphics, mc.level, exactX, exactZ, startX, startY, mapSize, blocksPerPixel);

        graphics.fill(startX - 1, startY - 1,
            startX + mapSize + 1, startY + 1, 0xFFFFFFFF);
        graphics.fill(startX - 1, startY + mapSize,
            startX + mapSize + 1, startY + mapSize + 1, 0xFFFFFFFF);
        graphics.fill(startX - 1, startY - 1,
            startX + 1, startY + mapSize + 1, 0xFFFFFFFF);
        graphics.fill(startX + mapSize, startY - 1,
            startX + mapSize + 1, startY + mapSize + 1, 0xFFFFFFFF);

        double halfBlocks = mapSize * blocksPerPixel / 2.0;
        int startCX = (int) Math.floor((chunkCenterX - halfBlocks) / 16);
        int endCX = (int) Math.floor((chunkCenterX + halfBlocks) / 16);
        int startCZ = (int) Math.floor((chunkCenterZ - halfBlocks) / 16);
        int endCZ = (int) Math.floor((chunkCenterZ + halfBlocks) / 16);

        for (int cx = startCX; cx <= endCX; cx++) {
            for (int cz = startCZ; cz <= endCZ; cz++) {
                int bx = cx * 16;
                int bz = cz * 16;

                int x1 = x0 + (int) Math.round((bx - chunkCenterX) / blocksPerPixel);
                int z1 = z0 + (int) Math.round((bz - chunkCenterZ) / blocksPerPixel);
                int cellW = (int) Math.round(16.0 / blocksPerPixel);
                if (cellW < 2) cellW = 2;

                if (x1 + cellW < startX || x1 > startX + mapSize
                    || z1 + cellW < startY || z1 > startY + mapSize) continue;

                boolean visited = ChunkVisMod.chunkManager.isVisited(mc.level.dimension(), cx, cz);

                if (visited) {
                    graphics.fill(x1, z1, x1 + cellW, z1 + cellW, 0x554466DD);
                    graphics.fill(x1, z1, x1 + cellW, z1 + 1, 0xCC6688FF);
                    graphics.fill(x1, z1 + cellW - 1, x1 + cellW, z1 + cellW, 0xCC6688FF);
                    graphics.fill(x1, z1, x1 + 1, z1 + cellW, 0xCC6688FF);
                    graphics.fill(x1 + cellW - 1, z1, x1 + cellW, z1 + cellW, 0xCC6688FF);
                } else {
                    graphics.fill(x1, z1, x1 + cellW, z1 + cellW, 0x33000000);
                    graphics.fill(x1, z1, x1 + cellW, z1 + 1, 0x88555555);
                    graphics.fill(x1, z1 + cellW - 1, x1 + cellW, z1 + cellW, 0x88555555);
                    graphics.fill(x1, z1, x1 + 1, z1 + cellW, 0x88555555);
                    graphics.fill(x1 + cellW - 1, z1, x1 + cellW, z1 + cellW, 0x88555555);
                }
            }
        }

        graphics.fill(x0 - PLAYER_SIZE, z0 - PLAYER_SIZE,
            x0 + PLAYER_SIZE, z0 + PLAYER_SIZE, 0xFF00BFFF);
        graphics.fill(x0 - PLAYER_SIZE + 1, z0 - PLAYER_SIZE + 1,
            x0 + PLAYER_SIZE - 1, z0 + PLAYER_SIZE - 1, 0xFF55DDFF);

        float yawRad = (float) Math.toRadians(player.getYRot());
        float dirX = -(float) Math.sin(yawRad) * 5;
        float dirZ = -(float) Math.cos(yawRad) * 5;
        graphics.fill(x0 + (int) dirX - 1, z0 + (int) dirZ - 1,
            x0 + (int) dirX + 1, z0 + (int) dirZ + 1, 0xFFFFFFAA);

        String info = String.format("Visited: %d | [U] %s | Zoom: %.1f",
            ChunkVisMod.chunkManager.getVisitedCount(mc.level.dimension()),
            ChunkVisMod.tracking ? "ON" : "OFF",
            1.0 / blocksPerPixel);
        graphics.drawString(mc.font, info, PADDING, startY + mapSize + PADDING, 0xFFCCCCCC);
    }
}
