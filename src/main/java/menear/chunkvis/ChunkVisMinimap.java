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
    private final int pixelSize;
    private DynamicTexture texture;
    private NativeImage image;
    private ResourceLocation textureId;
    private int lastChunkX = Integer.MIN_VALUE;
    private int lastChunkZ = Integer.MIN_VALUE;
    private long lastScanTime = 0;
    private static final long RESCAN_INTERVAL = 500;
    private boolean initialized = false;

    public ChunkVisMinimap(int gridRadius) {
        this.pixelSize = gridRadius * 2 + 1;
    }

    private void ensureTexture() {
        if (initialized) return;
        image = new NativeImage(pixelSize, pixelSize, false);
        texture = new DynamicTexture(image);
        Minecraft mc = Minecraft.getInstance();
        TextureManager tm = mc.getTextureManager();
        textureId = tm.register("chunkvis/minimap", texture);
        initialized = true;
    }

    public void scanAndRender(GuiGraphics graphics, Level level,
                              double playerX, double playerZ,
                              int screenX, int screenY,
                              int displaySize, int cellPixels) {
        ensureTexture();

        int playerChunkX = (int) Math.floor(playerX / 16);
        int playerChunkZ = (int) Math.floor(playerZ / 16);
        long now = System.currentTimeMillis();

        boolean moved = playerChunkX != lastChunkX || playerChunkZ != lastChunkZ;
        boolean timedOut = now - lastScanTime > RESCAN_INTERVAL;

        if (moved || timedOut) {
            scan(level, playerChunkX, playerChunkZ);
            lastChunkX = playerChunkX;
            lastChunkZ = playerChunkZ;
            lastScanTime = now;
        }

        graphics.blit(textureId, screenX, screenY, displaySize, displaySize,
            0, 0, pixelSize, pixelSize, pixelSize, pixelSize);
    }

    private void scan(Level level, int playerChunkX, int playerChunkZ) {
        int radius = pixelSize / 2;

        for (int dx = 0; dx < pixelSize; dx++) {
            for (int dz = 0; dz < pixelSize; dz++) {
                int cx = playerChunkX - radius + dx;
                int cz = playerChunkZ - radius + dz;

                if (!level.hasChunk(cx, cz)) {
                    image.setPixelRGBA(dx, dz, 0xFF111111);
                    continue;
                }

                int totalR = 0, totalG = 0, totalB = 0, count = 0;

                for (int bx = 0; bx < 16; bx += 4) {
                    for (int bz = 0; bz < 16; bz += 4) {
                        int wx = cx * 16 + bx;
                        int wz = cz * 16 + bz;

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
                    image.setPixelRGBA(dx, dz, 0xFF222222);
                } else {
                    int r = totalR / count;
                    int g = totalG / count;
                    int b = totalB / count;
                    int abgr = 0xFF000000 | (b << 16) | (g << 8) | r;
                    image.setPixelRGBA(dx, dz, abgr);
                }
            }
        }

        texture.upload();
    }
}
