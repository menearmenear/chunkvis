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
    private static final int PIXELS_PER_CHUNK = 16;
    private final int textureSize;
    private final int chunks;
    private DynamicTexture texture;
    private NativeImage image;
    private ResourceLocation textureId;
    private int lastChunkX = Integer.MIN_VALUE;
    private int lastChunkZ = Integer.MIN_VALUE;
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

        if (pcx != lastChunkX || pcz != lastChunkZ) {
            scan(level, pcx, pcz);
            lastChunkX = pcx;
            lastChunkZ = pcz;
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

                int wx = cx * 16 + (px % PIXELS_PER_CHUNK);
                int wz = cz * 16 + (pz % PIXELS_PER_CHUNK);

                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, wx, wz) - 1;
                if (y < level.getMinBuildHeight()) {
                    image.setPixelRGBA(px, pz, 0xFF222222);
                    continue;
                }

                BlockPos pos = new BlockPos(wx, y, wz);
                BlockState state = level.getBlockState(pos);
                MapColor mc = state.getMapColor(level, pos);
                if (mc == null) {
                    image.setPixelRGBA(px, pz, 0xFF222222);
                    continue;
                }

                int col = mc.col;
                int r = (col >> 16) & 0xFF;
                int g = (col >> 8) & 0xFF;
                int b = col & 0xFF;
                int abgr = 0xFF000000 | (b << 16) | (g << 8) | r;
                image.setPixelRGBA(px, pz, abgr);
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
