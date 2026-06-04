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
    private static final int MAX_TEX_BLOCKS = 1024;
    private double viewBlockX, viewBlockZ;
    private double scale;
    private int dragStartMouseX, dragStartMouseY;
    private double dragStartViewBlockX, dragStartViewBlockZ;
    private boolean dragging = false;

    private DynamicTexture terrainTex;
    private NativeImage terrainImage;
    private ResourceLocation terrainTexId;
    private int lastTexW = -1, lastTexH = -1;
    private int lastTexOriginX = Integer.MIN_VALUE;
    private int lastTexOriginZ = Integer.MIN_VALUE;

    protected ChunkVisScreen() {
        super(Component.literal("ChunkVis Map"));
    }

    @Override
    protected void init() {
        super.init();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            viewBlockX = mc.player.getX();
            viewBlockZ = mc.player.getZ();
        }
        scale = 4.0;
    }

    private void updateTerrain() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        double halfW = width / (2.0 * scale);
        double halfH = height / (2.0 * scale);
        int texOriginX = (int) (Math.floor((viewBlockX - halfW) / 16) * 16);
        int texOriginZ = (int) (Math.floor((viewBlockZ - halfH) / 16) * 16);
        int texW = (int) (Math.ceil((viewBlockX + halfW) / 16) * 16 - texOriginX);
        int texH = (int) (Math.ceil((viewBlockZ + halfH) / 16) * 16 - texOriginZ);
        if (texW < 1) texW = 16;
        if (texH < 1) texH = 16;
        if (texW > MAX_TEX_BLOCKS) texW = MAX_TEX_BLOCKS;
        if (texH > MAX_TEX_BLOCKS) texH = MAX_TEX_BLOCKS;

        if (texOriginX == lastTexOriginX && texOriginZ == lastTexOriginZ
            && texW == lastTexW && texH == lastTexH) return;

        ensureImage(texW, texH);

        for (int dx = 0; dx < texW; dx++) {
            for (int dz = 0; dz < texH; dz++) {
                int wx = texOriginX + dx;
                int wz = texOriginZ + dz;

                if (!mc.level.hasChunk(wx >> 4, wz >> 4)) {
                    terrainImage.setPixelRGBA(dx, dz, 0xFF111111);
                    continue;
                }

                int y = mc.level.getHeight(Heightmap.Types.MOTION_BLOCKING, wx, wz) - 1;
                if (y < mc.level.getMinBuildHeight()) {
                    terrainImage.setPixelRGBA(dx, dz, 0xFF222222);
                    continue;
                }

                BlockPos pos = new BlockPos(wx, y, wz);
                BlockState state = mc.level.getBlockState(pos);
                MapColor mcColor = state.getMapColor(mc.level, pos);
                if (mcColor == null) {
                    terrainImage.setPixelRGBA(dx, dz, 0xFF222222);
                    continue;
                }

                int col = mcColor.col;
                int r = (col >> 16) & 0xFF;
                int g = (col >> 8) & 0xFF;
                int b = col & 0xFF;
                terrainImage.setPixelRGBA(dx, dz, 0xFF000000 | (b << 16) | (g << 8) | r);
            }
        }
        terrainTex.upload();

        lastTexOriginX = texOriginX;
        lastTexOriginZ = texOriginZ;
        lastTexW = texW;
        lastTexH = texH;
    }

    private void ensureImage(int w, int h) {
        if (terrainTex != null && lastTexW == w && lastTexH == h) return;
        if (terrainImage != null) terrainImage.close();
        if (terrainTexId != null) {
            Minecraft.getInstance().getTextureManager().release(terrainTexId);
        }
        terrainImage = new NativeImage(w, h, false);
        terrainTex = new DynamicTexture(terrainImage);
        Minecraft mc = Minecraft.getInstance();
        terrainTexId = mc.getTextureManager().register("chunkvis/map", terrainTex);
        lastTexW = w;
        lastTexH = h;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderBackground(graphics, mouseX, mouseY, delta);

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        updateTerrain();

        if (terrainTexId == null) return;

        int scrCX = width / 2;
        int scrCY = height / 2;

        int texScrX = (int) (scrCX + (lastTexOriginX - viewBlockX) * scale);
        int texScrY = (int) (scrCY + (lastTexOriginZ - viewBlockZ) * scale);
        int texScrW = (int) (lastTexW * scale);
        int texScrH = (int) (lastTexH * scale);

        graphics.blit(terrainTexId, texScrX, texScrY, texScrW, texScrH,
            0, 0, lastTexW, lastTexH, lastTexW, lastTexH);

        int startChunkX = (int) Math.floor((viewBlockX - scrCX / scale) / 16);
        int startChunkZ = (int) Math.floor((viewBlockZ - scrCY / scale) / 16);
        int endChunkX = (int) Math.ceil((viewBlockX + scrCX / scale) / 16) + 1;
        int endChunkZ = (int) Math.ceil((viewBlockZ + scrCY / scale) / 16) + 1;

        for (int cx = startChunkX; cx <= endChunkX; cx++) {
            for (int cz = startChunkZ; cz <= endChunkZ; cz++) {
                int x = (int) (scrCX + (cx * 16 - viewBlockX) * scale);
                int z = (int) (scrCY + (cz * 16 - viewBlockZ) * scale);
                int w = (int) Math.ceil(16 * scale) + 1;

                if (x + w < 0 || x > width || z + w < 0 || z > height) continue;

                boolean visited = ChunkVisMod.chunkManager.isVisited(mc.level.dimension(), cx, cz);
                boolean isPlayer = cx == (int) Math.floor(mc.player.getX() / 16)
                    && cz == (int) Math.floor(mc.player.getZ() / 16);

                if (visited) {
                    graphics.fill(x, z, x + w, z + w, 0x444466DD);
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
        lastTexOriginX = Integer.MIN_VALUE;
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
