package menear.chunkvis;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class ChunkVisWorldRenderer {
    public static void register() {
        WorldRenderEvents.LAST.register(context -> {
            if (!ChunkVisMod.overlayVisible) return;

            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null) return;

            Camera camera = context.camera();
            Vec3 camPos = camera.getPosition();
            PoseStack poseStack = context.matrixStack();

            int playerChunkX = (int) Math.floor(mc.player.getX() / 16);
            int playerChunkZ = (int) Math.floor(mc.player.getZ() / 16);
            int renderDist = Math.min(mc.options.getEffectiveRenderDistance(), 12);

            double yLevel = Math.floor(mc.player.getY() - 2);
            yLevel = Math.max(-64, Math.min(yLevel, 320));

            poseStack.pushPose();
            poseStack.translate(-camPos.x, -camPos.y, -camPos.z);
            Matrix4f matrix = poseStack.last().pose();

            Tesselator tesselator = Tesselator.getInstance();

            BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

            for (int dx = -renderDist; dx <= renderDist; dx++) {
                for (int dz = -renderDist; dz <= renderDist; dz++) {
                    int cx = playerChunkX + dx;
                    int cz = playerChunkZ + dz;

                    float x1 = cx * 16;
                    float z1 = cz * 16;
                    float x2 = x1 + 16;
                    float z2 = z1 + 16;

                    int r, g, b, a;
                    if (dx == 0 && dz == 0) {
                        r = 0; g = 150; b = 255; a = 50;
                    } else if (ChunkVisMod.chunkManager.isVisited(mc.level.dimension(), cx, cz)) {
                        r = 0; g = 200; b = 0; a = 35;
                    } else {
                        r = 60; g = 60; b = 60; a = 25;
                    }

                    buffer.addVertex(matrix, x1, (float) yLevel + 0.05f, z1).setColor(r, g, b, a);
                    buffer.addVertex(matrix, x2, (float) yLevel + 0.05f, z1).setColor(r, g, b, a);
                    buffer.addVertex(matrix, x2, (float) yLevel + 0.05f, z2).setColor(r, g, b, a);
                    buffer.addVertex(matrix, x1, (float) yLevel + 0.05f, z2).setColor(r, g, b, a);
                }
            }

            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableDepthTest();
            BufferUploader.drawWithShader(buffer.buildOrThrow());

            buffer = tesselator.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR);

            for (int dx = -renderDist; dx <= renderDist; dx++) {
                for (int dz = -renderDist; dz <= renderDist; dz++) {
                    int cx = playerChunkX + dx;
                    int cz = playerChunkZ + dz;

                    float x1 = cx * 16;
                    float z1 = cz * 16;
                    float x2 = x1 + 16;
                    float z2 = z1 + 16;

                    int r, g, b, a = 180;
                    if (dx == 0 && dz == 0) {
                        r = 0; g = 191; b = 255;
                    } else if (ChunkVisMod.chunkManager.isVisited(mc.level.dimension(), cx, cz)) {
                        r = 0; g = 255; b = 0;
                    } else {
                        r = 180; g = 0; b = 0;
                    }

                    float y = (float) yLevel + 0.1f;

                    buffer.addVertex(matrix, x1, y, z1).setColor(r, g, b, a);
                    buffer.addVertex(matrix, x2, y, z1).setColor(r, g, b, a);

                    buffer.addVertex(matrix, x2, y, z1).setColor(r, g, b, a);
                    buffer.addVertex(matrix, x2, y, z2).setColor(r, g, b, a);

                    buffer.addVertex(matrix, x2, y, z2).setColor(r, g, b, a);
                    buffer.addVertex(matrix, x1, y, z2).setColor(r, g, b, a);

                    buffer.addVertex(matrix, x1, y, z2).setColor(r, g, b, a);
                    buffer.addVertex(matrix, x1, y, z1).setColor(r, g, b, a);
                }
            }

            BufferUploader.drawWithShader(buffer.buildOrThrow());
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();

            poseStack.popPose();
        });
    }
}
