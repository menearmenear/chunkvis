package menear.chunkvis;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;

public class ChunkVisMinimap {
    private int texPixels = -1;
    private int lastBpTex = -1;
    private DynamicTexture texture;
    private NativeImage image;
    private ResourceLocation textureId;
    private int lastChunkX = Integer.MIN_VALUE;
    private int lastChunkZ = Integer.MIN_VALUE;
    private boolean initialized = false;

    private void ensureTexture(int newTexPixels) {
        if (initialized && texPixels == newTexPixels) return;
        if (image != null) image.close();
        texPixels = newTexPixels;
        image = new NativeImage(texPixels, texPixels, false);
        texture = new DynamicTexture(image);
        Minecraft mc = Minecraft.getInstance();
        TextureManager tm = mc.getTextureManager();
        if (textureId != null) tm.release(textureId);
        textureId = tm.register("chunkvis/minimap", texture);
        initialized = true;
    }

    public void scanAndRender(GuiGraphics graphics, Level level,
                              double playerX, double playerZ,
                              int screenX, int screenY,
                              int mapSize, double bpp) {
        int pcx = (int) Math.floor(playerX / 16);
        int pcz = (int) Math.floor(playerZ / 16);

        int nTex, bpTex;
        if (bpp >= 1) {
            nTex = mapSize;
            bpTex = (int) bpp;
        } else {
            nTex = (int) Math.ceil(mapSize * bpp);
            if (nTex < 1) nTex = 1;
            bpTex = 1;
        }

        ensureTexture(nTex);

        if (pcx != lastChunkX || pcz != lastChunkZ || bpTex != lastBpTex || nTex != texPixels) {
            scan(level, pcx, pcz, nTex, bpTex);
            lastChunkX = pcx;
            lastChunkZ = pcz;
            lastBpTex = bpTex;
        }

        graphics.blit(textureId, screenX, screenY, mapSize, mapSize,
            0, 0, nTex, nTex, nTex, nTex);
    }

    private void scan(Level level, int pcx, int pcz, int nTex, int bpTex) {
        for (int px = 0; px < nTex; px++) {
            for (int pz = 0; pz < nTex; pz++) {
                double cx = pcx * 16 + 8 + (px - nTex / 2.0) * bpTex;
                double cz = pcz * 16 + 8 + (pz - nTex / 2.0) * bpTex;

                int totalR = 0, totalG = 0, totalB = 0, count = 0;
                int startWX = (int) Math.floor(cx);
                int startWZ = (int) Math.floor(cz);
                for (int dx = 0; dx < bpTex; dx++) {
                    for (int dz = 0; dz < bpTex; dz++) {
                        int wx = startWX + dx;
                        int wz = startWZ + dz;
                        if (!level.hasChunk(wx >> 4, wz >> 4)) continue;
                        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, wx, wz) - 1;
                        if (y < level.getMinBuildHeight()) continue;
                        BlockPos pos = new BlockPos(wx, y, wz);
                        BlockState state = level.getBlockState(pos);
                        MapColor mc = state.getMapColor(level, pos);
                        if (mc == null) continue;
                        int col = mc.col;
                        totalR += (col >> 16) & 0xFF;
                        totalG += (col >> 8) & 0xFF;
                        totalB += col & 0xFF;
                        count++;
                    }
                }
                if (count == 0) {
                    image.setPixelRGBA(px, pz, 0xFF111111);
                } else {
                    image.setPixelRGBA(px, pz, 0xFF000000
                        | ((totalB / count) << 16)
                        | ((totalG / count) << 8)
                        | (totalR / count));
                }
            }
        }
        texture.upload();
    }
}
