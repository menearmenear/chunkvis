package menear.chunkvis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ChunkVisScreen extends Screen {
    private static final int BASE_CELL_SIZE = 12;
    private int cellSize = BASE_CELL_SIZE;
    private double panX = 0, panY = 0;
    private double dragStartX, dragStartY;
    private double panStartX, panStartY;
    private boolean dragging = false;
    private int centerChunkX, centerChunkZ;

    protected ChunkVisScreen() {
        super(Component.literal("ChunkVis Map"));
    }

    @Override
    protected void init() {
        super.init();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            centerChunkX = (int) Math.floor(mc.player.getX() / 16);
            centerChunkZ = (int) Math.floor(mc.player.getZ() / 16);
        }
        panX = width / 2.0;
        panY = height / 2.0;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderBackground(graphics, mouseX, mouseY, delta);

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        int visibleChunksX = (int) (width / (double) cellSize) + 4;
        int visibleChunksZ = (int) (height / (double) cellSize) + 4;

        int offsetChunkX = (int) ((panX - width / 2.0) / cellSize);
        int offsetChunkZ = (int) ((panY - height / 2.0) / cellSize);

        int startCx = centerChunkX - offsetChunkX - visibleChunksX / 2;
        int startCz = centerChunkZ - offsetChunkZ - visibleChunksZ / 2;

        for (int dx = 0; dx < visibleChunksX; dx++) {
            for (int dz = 0; dz < visibleChunksZ; dz++) {
                int cx = startCx + dx;
                int cz = startCz + dz;

                int x = (int) ((cx - centerChunkX) * cellSize + panX);
                int z = (int) ((cz - centerChunkZ) * cellSize + panY);

                if (x < -cellSize || x > width + cellSize || z < -cellSize || z > height + cellSize)
                    continue;

                int color;
                int borderColor = 0xFF333333;
                boolean visited = ChunkVisMod.chunkManager.isVisited(mc.level.dimension(), cx, cz);

                if (cx == centerChunkX && cz == centerChunkZ) {
                    color = 0xFF0088CC;
                } else if (visited) {
                    color = 0xFF22AA22;
                } else {
                    color = 0xFF1A1A1A;
                }

                graphics.fill(x, z, x + cellSize, z + cellSize, color);

                graphics.fill(x, z, x + cellSize, z + 1, borderColor);
                graphics.fill(x, z + cellSize - 1, x + cellSize, z + cellSize, borderColor);
                graphics.fill(x, z, x + 1, z + cellSize, borderColor);
                graphics.fill(x + cellSize - 1, z, x + cellSize, z + cellSize, borderColor);

                if (cellSize >= 10 && (dx % 4 == 0 || dz % 4 == 0)) {
                    graphics.drawString(mc.font, cx + "," + cz, x + 2, z + 2, 0x66FFFFFF);
                }
            }
        }

        if (mc.player != null) {
            float fracX = (float) ((mc.player.getX() % 16) / 16.0);
            float fracZ = (float) ((mc.player.getZ() % 16) / 16.0);
            if (fracX < 0) fracX += 1;
            if (fracZ < 0) fracZ += 1;

            int playerPixelX = (int) (panX + fracX * cellSize);
            int playerPixelZ = (int) (panY + fracZ * cellSize);

            graphics.fill(playerPixelX - 3, playerPixelZ - 3, playerPixelX + 3, playerPixelZ + 3, 0xFF44DDFF);
            graphics.fill(playerPixelX - 2, playerPixelZ - 2, playerPixelX + 2, playerPixelZ + 2, 0xFFFFFFFF);

            float yawRad = (float) Math.toRadians(mc.player.getYRot());
            float dirX = -(float) Math.sin(yawRad) * 8;
            float dirZ = -(float) Math.cos(yawRad) * 8;
            graphics.fill(playerPixelX + (int) dirX - 1, playerPixelZ + (int) dirZ - 1,
                playerPixelX + (int) dirX + 1, playerPixelZ + (int) dirZ + 1, 0xFFFFEE88);
        }

        int visited = ChunkVisMod.chunkManager.getVisitedCount(mc.level.dimension());
        String info = "ChunkVis Map | Visited: " + visited + " | Zoom: " + String.format("%.1f", cellSize / (float) BASE_CELL_SIZE) + "x";
        graphics.drawString(mc.font, info, 10, 10, 0xFFFFFF);
        graphics.drawString(mc.font, "Scroll: zoom | Drag: pan | M/Esc: close", 10, 22, 0x999999);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0) {
            if (!dragging) {
                dragging = true;
                dragStartX = mouseX;
                dragStartY = mouseY;
                panStartX = panX;
                panStartY = panY;
            }
            panX = panStartX + (mouseX - dragStartX);
            panY = panStartY + (mouseY - dragStartY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
        centerChunkX = (int) Math.floor((mouseX - panX) / cellSize) + centerChunkX;
        centerChunkZ = (int) Math.floor((mouseY - panY) / cellSize) + centerChunkZ;
        panX = width / 2.0;
        panY = height / 2.0;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int prevCellSize = cellSize;
        if (scrollY > 0) {
            cellSize = Math.min(cellSize + 2, 40);
        } else {
            cellSize = Math.max(cellSize - 2, 4);
        }
        panX += (mouseX - panX) * (cellSize - prevCellSize) / (double) prevCellSize;
        panY += (mouseY - panY) * (cellSize - prevCellSize) / (double) prevCellSize;
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 77 || keyCode == 256) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
