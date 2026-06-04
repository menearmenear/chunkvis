package menear.chunkvis;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;

public class ChunkVisScreen extends Screen {
    private static final int BASE_CELL_SIZE = 12;
    private int cellSize = BASE_CELL_SIZE;
    private double panX, panY;
    private double dragStartX, dragStartY;
    private double panStartX, panStartY;
    private boolean dragging = false;

    private DynamicTexture terrainTex;
    private NativeImage terrainImage;
    private ResourceLocation terrainTexId;
    private int lastViewCx = Integer.MAX_VALUE;
    private int lastViewCz = Integer.MAX_VALUE;
    private int lastCellSize = 0;
    private boolean texInit = false;

    protected ChunkVisScreen() {
        super(Component.literal("ChunkVis Map"));
    }

    private void initTexture() {
        if (texInit) return;
        terrainImage = new NativeImage(128, 128, false);
        terrainTex = new DynamicTexture(terrainImage);
        Minecraft mc = Minecraft.getInstance();
        TextureManager tm = mc.getTextureManager();
        terrainTexId = tm.register("chunkvis/map", terrainTex);
        texInit = true;
    }

    @Override
    protected void init() {
        super.init();
        panX = width / 2.0;
        panY = height / 2.0;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderBackground(graphics, mouseX, mouseY, delta);
        initTexture();

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        int visX = (int) (width / (double) cellSize) + 4;
        int visZ = (int) (height / (double) cellSize) + 4;
        int midX = visX / 2;
        int midZ = visZ / 2;

        int offsetCx = (int) ((panX - width / 2.0) / cellSize);
        int offsetCz = (int) ((panY - height / 2.0) / cellSize);

        int startCx = -(offsetCx) - midX;
        int startCz = -(offsetCz) - midZ;

        boolean viewChanged = cellSize != lastCellSize
            || Math.abs(startCx - lastViewCx) > 2
            || Math.abs(startCz - lastViewCz) > 2;
        lastCellSize = cellSize;
        lastViewCx = startCx;
        lastViewCz = startCz;

        if (viewChanged) {
            updateTerrain(mc.level, startCx, startCz, visX, visZ);
        }

        graphics.blit(terrainTexId, 0, 0, width, height, 0, 0, 128, 128, 128, 128);

        for (int dx = 0; dx < visX; dx++) {
            for (int dz = 0; dz < visZ; dz++) {
                int cx = startCx + dx;
                int cz = startCz + dz;

                int x = (int) ((cx) * cellSize + panX - startCx * cellSize);
                int z = (int) ((cz) * cellSize + panY - startCz * cellSize);

                if (x < -cellSize || x > width + cellSize || z < -cellSize || z > height + cellSize)
                    continue;

                boolean visited = ChunkVisMod.chunkManager.isVisited(mc.level.dimension(), cx, cz);
                boolean isPlayer = dx == midX + offsetCx && dz == midZ + offsetCz;

                if (visited) {
                    graphics.fill(x, z, x + cellSize, z + cellSize, 0x4422AA22);
                }

                if (isPlayer) {
                    int pSize = Math.max(4, cellSize / 2);
                    graphics.fill(x + cellSize / 2 - pSize / 2, z + cellSize / 2 - pSize / 2,
                        x + cellSize / 2 + pSize / 2, z + cellSize / 2 + pSize / 2, 0xFF00BFFF);
                }

                graphics.fill(x, z, x + cellSize, z + 1, 0x66444444);
                graphics.fill(x, z + cellSize - 1, x + cellSize, z + cellSize, 0x66444444);
                graphics.fill(x, z, x + 1, z + cellSize, 0x66444444);
                graphics.fill(x + cellSize - 1, z, x + cellSize, z + cellSize, 0x66444444);
            }
        }

        String info = "Visited: " + ChunkVisMod.chunkManager.getVisitedCount(mc.level.dimension())
            + " | Scroll: zoom | Drag: pan | M/Esc: close";
        graphics.drawString(mc.font, info, 10, 10, 0xFFFFFF);
    }

    private void updateTerrain(Level level, int startCx, int startCz, int visX, int visZ) {
        terrainImage.fillRect(0, 0, 128, 128, 0xFF111111);

        int chunksW = visX;
        int chunksH = visZ;

        for (int dx = 0; dx < 128; dx++) {
            for (int dz = 0; dz < 128; dz++) {
                int cx = startCx + dx * chunksW / 128;
                int cz = startCz + dz * chunksH / 128;

                if (!level.hasChunk(cx, cz)) continue;

                int wx = cx * 16 + 8;
                int wz = cz * 16 + 8;

                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, wx, wz) - 1;
                if (y < level.getMinBuildHeight()) continue;

                BlockPos pos = new BlockPos(wx, y, wz);
                BlockState state = level.getBlockState(pos);
                MapColor mc = state.getMapColor(level, pos);
                if (mc == null) continue;

                int col = mc.col;
                int r = (col >> 16) & 0xFF;
                int g = (col >> 8) & 0xFF;
                int b = col & 0xFF;
                int abgr = 0xFF000000 | (b << 16) | (g << 8) | r;
                terrainImage.setPixelRGBA(dx, dz, abgr);
            }
        }

        terrainTex.upload();
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
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int prev = cellSize;
        if (scrollY > 0) {
            cellSize = Math.min(cellSize + 2, 40);
        } else {
            cellSize = Math.max(cellSize - 2, 4);
        }
        if (cellSize != prev) {
            panX += (mouseX - panX) * (cellSize - prev) / (double) prev;
            panY += (mouseY - panY) * (cellSize - prev) / (double) prev;
        }
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
