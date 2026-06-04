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
    private static final int PIXELS_PER_CHUNK = 4;
    private final int textureSize;
    private final int chunks;
    private DynamicTexture texture;
    private NativeImage image;
    private ResourceLocation textureId;
    private int lastChunkX = Integer.MIN_VALUE;
    private int lastChunkZ = Integer.MIN_VALUE;
    private long lastScanTime = 0;
    private static final long RESCAN_INTERVAL = 1000;
    private boolean initialized = false;

    public ChunkVisMinimap(int gridRadius) {
        this.chunks = gridRadius * 2 + 1;
        this.textureSize = chunks * PIXELS_PER_CHUNK;
    }

    private void ensureTexture() {
        if (initialized) return;
        image = new NativeImage(textureSize, textureSize, false);
        texture = new DynamicTexture(image);
        Minecraft mc = Minecraft.getInstance();
        TextureManager tm = mc.getTextureManager();
        textureId = tm.register("chunkvis/minimap", texture);
        initialized = true;
    }

    public void scanAndRender(GuiGraphics graphics, Level level,
                              double playerX, double playerZ,
                              int screenX, int screenY,
                              int displaySize) {
        ensureTexture();

        int pcx = (int) Math.floor(playerX / 16);
        int pcz = (int) Math.floor(playerZ / 16);
        long now = System.currentTimeMillis();

        if (pcx != lastChunkX || pcz != lastChunkZ || now - lastScanTime > RESCAN_INTERVAL) {
            scan(level, pcx, pcz);
            lastChunkX = pcx;
            lastChunkZ = pcz;
            lastScanTime = now;
        }

        graphics.blit(textureId, screenX, screenY, displaySize, displaySize,
            0, 0, textureSize, textureSize, textureSize, textureSize);
    }

    private void scan(Level level, int pcx, int pcz) {
        int radius = chunks / 2;

        for (int px = 0; px < textureSize; px++) {
            for (int pz = 0; pz < textureSize; pz++) {
                int cx = pcx - radius + px / PIXELS_PER_CHUNK;
                int cz = pcz - radius + pz / PIXELS_PER_CHUNK;

                if (!level.hasChunk(cx, cz)) {
                    image.setPixelRGBA(px, pz, 0xFF111111);
                    continue;
                }

                int blockInChunkX = (px % PIXELS_PER_CHUNK) * 4;
                int blockInChunkZ = (pz % PIXELS_PER_CHUNK) * 4;
                int wx = cx * 16 + blockInChunkX;
                int wz = cz * 16 + blockInChunkZ;

                int totalR = 0, totalG = 0, totalB = 0, count = 0;
                for (int bx = 0; bx < 4; bx++) {
                    for (int bz = 0; bz < 4; bz++) {
                        int bw = wx + bx;
                        int bz2 = wz + bz;

                        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, bw, bz2) - 1;
                        if (y < level.getMinBuildHeight()) continue;

                        BlockPos pos = new BlockPos(bw, y, bz2);
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
                    image.setPixelRGBA(px, pz, 0xFF222222);
                } else {
                    int r = totalR / count;
                    int g = totalG / count;
                    int b = totalB / count;
                    int abgr = 0xFF000000 | (b << 16) | (g << 8) | r;
                    image.setPixelRGBA(px, pz, abgr);
                }
            }
        }

        texture.upload();
    }

    public int getChunks() {
        return chunks;
    }

    public int getTextureSize() {
        return textureSize;
    }
}
