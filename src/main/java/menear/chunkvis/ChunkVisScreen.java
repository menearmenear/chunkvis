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
    private static final int TEX_SIZE = 256;
    private double viewBlockX, viewBlockZ;
    private double scale;
    private int dragStartMouseX, dragStartMouseY;
    private double dragStartViewBlockX, dragStartViewBlockZ;
    private boolean dragging = false;

    private DynamicTexture terrainTex;
    private NativeImage terrainImage;
    private ResourceLocation terrainTexId;
    private int lastTexBlockX = Integer.MIN_VALUE;
    private int lastTexBlockZ = Integer.MIN_VALUE;
    private boolean texInit = false;

    protected ChunkVisScreen() {
        super(Component.literal("ChunkVis Map"));
    }

    private void initTexture() {
        if (texInit) return;
        terrainImage = new NativeImage(TEX_SIZE, TEX_SIZE, false);
        terrainTex = new DynamicTexture(terrainImage);
        Minecraft mc = Minecraft.getInstance();
        TextureManager tm = mc.getTextureManager();
        terrainTexId = tm.register("chunkvis/map", terrainTex);
        texInit = true;
    }

    @Override
    protected void init() {
        super.init();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            viewBlockX = mc.player.getX();
            viewBlockZ = mc.player.getZ();
        }
        scale = 12.0;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderBackground(graphics, mouseX, mouseY, delta);
        initTexture();

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        double scaleInv = scale;
        int scrCX = width / 2;
        int scrCY = height / 2;
        int texBlocks = TEX_SIZE;
        int halfTexBlocks = texBlocks / 2;

        int texBlockX = (int) Math.floor(viewBlockX / 16) * 16 + 8 - halfTexBlocks;
        int texBlockZ = (int) Math.floor(viewBlockZ / 16) * 16 + 8 - halfTexBlocks;

        if (texBlockX != lastTexBlockX || texBlockZ != lastTexBlockZ) {
            updateTerrain(mc.level, texBlockX, texBlockZ);
            lastTexBlockX = texBlockX;
            lastTexBlockZ = texBlockZ;
        }

        int texScrX = (int) (scrCX + (texBlockX - viewBlockX) * scaleInv);
        int texScrY = (int) (scrCY + (texBlockZ - viewBlockZ) * scaleInv);
        int texScrSize = (int) (texBlocks * scaleInv);

        graphics.blit(terrainTexId, texScrX, texScrY, texScrSize, texScrSize,
            0, 0, TEX_SIZE, TEX_SIZE, TEX_SIZE, TEX_SIZE);

        int startChunkX = (int) Math.floor((viewBlockX - scrCX / scaleInv) / 16);
        int startChunkZ = (int) Math.floor((viewBlockZ - scrCY / scaleInv) / 16);
        int endChunkX = (int) Math.ceil((viewBlockX + scrCX / scaleInv) / 16) + 1;
        int endChunkZ = (int) Math.ceil((viewBlockZ + scrCY / scaleInv) / 16) + 1;

        for (int cx = startChunkX; cx <= endChunkX; cx++) {
            for (int cz = startChunkZ; cz <= endChunkZ; cz++) {
                int x = (int) (scrCX + (cx * 16 - viewBlockX) * scaleInv);
                int z = (int) (scrCY + (cz * 16 - viewBlockZ) * scaleInv);
                int w = (int) Math.ceil(16 * scaleInv) + 1;

                if (x + w < 0 || x > width || z + w < 0 || z > height) continue;

                boolean visited = ChunkVisMod.chunkManager.isVisited(mc.level.dimension(), cx, cz);
                boolean isPlayer = cx == (int) Math.floor(mc.player.getX() / 16)
                    && cz == (int) Math.floor(mc.player.getZ() / 16);

                if (visited) {
                    graphics.fill(x, z, x + w, z + w, 0x4422AA22);
                }

                if (isPlayer) {
                    int ps = Math.max(5, w / 2);
                    graphics.fill(x + w / 2 - ps / 2, z + w / 2 - ps / 2,
                        x + w / 2 + ps / 2, z + w / 2 + ps / 2, 0xFF00BFFF);
                }

                graphics.fill(x, z, x + w, z + 1, 0x66444444);
                graphics.fill(x, z + w - 1, x + w, z + w, 0x66444444);
                graphics.fill(x, z, x + 1, z + w, 0x66444444);
                graphics.fill(x + w - 1, z, x + w, z + w, 0x66444444);
            }
        }

        String info = "Visited: " + ChunkVisMod.chunkManager.getVisitedCount(mc.level.dimension())
            + " | Scroll: zoom | Drag: pan | M/Esc: close";
        graphics.drawString(mc.font, info, 10, 10, 0xFFFFFF);
    }

    private void updateTerrain(Level level, int blockX, int blockZ) {
        for (int dx = 0; dx < TEX_SIZE; dx++) {
            for (int dz = 0; dz < TEX_SIZE; dz++) {
                int wx = blockX + dx;
                int wz = blockZ + dz;

                if (!level.hasChunk(wx >> 4, wz >> 4)) {
                    terrainImage.setPixelRGBA(dx, dz, 0xFF111111);
                    continue;
                }

                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, wx, wz) - 1;
                if (y < level.getMinBuildHeight()) {
                    terrainImage.setPixelRGBA(dx, dz, 0xFF222222);
                    continue;
                }

                BlockPos pos = new BlockPos(wx, y, wz);
                BlockState state = level.getBlockState(pos);
                MapColor mc = state.getMapColor(level, pos);
                if (mc == null) {
                    terrainImage.setPixelRGBA(dx, dz, 0xFF222222);
                    continue;
                }

                int col = mc.col;
                int r = (col >> 16) & 0xFF;
                int g = (col >> 8) & 0xFF;
                int b = col & 0xFF;
                terrainImage.setPixelRGBA(dx, dz, 0xFF000000 | (b << 16) | (g << 8) | r);
            }
        }
        terrainTex.upload();
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0) {
            if (!dragging) {
                dragging = true;
                dragStartMouseX = (int) mouseX;
                dragStartMouseY = (int) mouseY;
                dragStartViewBlockX = viewBlockX;
                dragStartViewBlockZ = viewBlockZ;
            }
            viewBlockX = dragStartViewBlockX - (mouseX - dragStartMouseX) / scale;
            viewBlockZ = dragStartViewBlockZ - (mouseY - dragStartMouseY) / scale;
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
        double prev = scale;
        if (scrollY > 0) {
            scale = Math.min(scale * 1.2, 80.0);
        } else {
            scale = Math.max(scale / 1.2, 1.0);
        }
        viewBlockX += (mouseX - width / 2.0) * (1.0 / prev - 1.0 / scale);
        viewBlockZ += (mouseY - height / 2.0) * (1.0 / prev - 1.0 / scale);
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
