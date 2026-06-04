package chunkvis;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

public class ChunkVisOverlay implements HudRenderCallback {
    private static final int GRID_RADIUS = 5;
    private static final int CELL_SIZE = 14;
    private static final int PADDING = 6;
    private static final int PLAYER_SIZE = 4;

    private final ChunkVisMinimap minimap;

    public ChunkVisOverlay() {
        this.minimap = new ChunkVisMinimap(GRID_RADIUS * 2 + 1);
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
        float offsetX = (float) ((exactX % 16) / 16.0);
        float offsetZ = (float) ((exactZ % 16) / 16.0);
        if (offsetX < 0) offsetX += 1;
        if (offsetZ < 0) offsetZ += 1;

        int gridSize = GRID_RADIUS * 2 + 1;
        int mapSize = gridSize * CELL_SIZE;
        int startX = PADDING;
        int startY = PADDING;

        minimap.scanAndRender(graphics, mc.level, exactX, exactZ, startX, startY, mapSize, CELL_SIZE);

        graphics.fill(startX - 1, startY - 1,
            startX + mapSize + 1, startY + 1, 0xFFFFFFFF);
        graphics.fill(startX - 1, startY + mapSize,
            startX + mapSize + 1, startY + mapSize + 1, 0xFFFFFFFF);
        graphics.fill(startX - 1, startY - 1,
            startX + 1, startY + mapSize + 1, 0xFFFFFFFF);
        graphics.fill(startX + mapSize, startY - 1,
            startX + mapSize + 1, startY + mapSize + 1, 0xFFFFFFFF);

        int center = GRID_RADIUS;

        for (int dx = -GRID_RADIUS; dx <= GRID_RADIUS; dx++) {
            for (int dz = -GRID_RADIUS; dz <= GRID_RADIUS; dz++) {
                int cx = playerChunkX + dx;
                int cz = playerChunkZ + dz;

                int x = startX + (dx + GRID_RADIUS) * CELL_SIZE;
                int y = startY + (dz + GRID_RADIUS) * CELL_SIZE;

                boolean visited = ChunkVisMod.chunkManager.isVisited(mc.level.dimension(), cx, cz);

                if (visited) {
                    graphics.fill(x + 1, y + 1, x + CELL_SIZE - 1, y + CELL_SIZE - 1, 0x551A6B1A);
                    graphics.fill(x + 1, y + 1, x + CELL_SIZE - 1, y + 2, 0xCC33AA33);
                    graphics.fill(x + 1, y + CELL_SIZE - 2, x + CELL_SIZE - 1, y + CELL_SIZE - 1, 0xCC33AA33);
                    graphics.fill(x + 1, y + 1, x + 2, y + CELL_SIZE - 1, 0xCC33AA33);
                    graphics.fill(x + CELL_SIZE - 2, y + 1, x + CELL_SIZE - 1, y + CELL_SIZE - 1, 0xCC33AA33);
                } else {
                    graphics.fill(x + 1, y + 1, x + CELL_SIZE - 1, y + CELL_SIZE - 1, 0x33000000);
                    graphics.fill(x + 1, y + 1, x + CELL_SIZE - 1, y + 2, 0x88555555);
                    graphics.fill(x + 1, y + CELL_SIZE - 2, x + CELL_SIZE - 1, y + CELL_SIZE - 1, 0x88555555);
                    graphics.fill(x + 1, y + 1, x + 2, y + CELL_SIZE - 1, 0x88555555);
                    graphics.fill(x + CELL_SIZE - 2, y + 1, x + CELL_SIZE - 1, y + CELL_SIZE - 1, 0x88555555);
                }
            }
        }

        int playerPixelX = startX + (int) (center * CELL_SIZE + offsetX * CELL_SIZE);
        int playerPixelZ = startY + (int) (center * CELL_SIZE + offsetZ * CELL_SIZE);

        graphics.fill(playerPixelX - PLAYER_SIZE, playerPixelZ - PLAYER_SIZE,
            playerPixelX + PLAYER_SIZE, playerPixelZ + PLAYER_SIZE, 0xFF00BFFF);
        graphics.fill(playerPixelX - PLAYER_SIZE + 1, playerPixelZ - PLAYER_SIZE + 1,
            playerPixelX + PLAYER_SIZE - 1, playerPixelZ + PLAYER_SIZE - 1, 0xFF55DDFF);

        float yawRad = (float) Math.toRadians(player.getYRot());
        float dirX = -(float) Math.sin(yawRad) * 5;
        float dirZ = -(float) Math.cos(yawRad) * 5;
        graphics.fill(playerPixelX + (int) dirX - 1, playerPixelZ + (int) dirZ - 1,
            playerPixelX + (int) dirX + 1, playerPixelZ + (int) dirZ + 1, 0xFFFFFFAA);

        String info = String.format("Visited: %d | [U] %s",
            ChunkVisMod.chunkManager.getVisitedCount(mc.level.dimension()),
            ChunkVisMod.tracking ? "ON" : "OFF");
        graphics.drawString(mc.font, info, PADDING, startY + mapSize + PADDING, 0xFFCCCCCC);
    }
}
