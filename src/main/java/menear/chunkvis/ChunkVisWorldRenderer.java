package menear.chunkvis;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
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

            double yLevel = Math.max(-64, mc.player.getY() - 50);

            poseStack.pushPose();
            poseStack.translate(-camPos.x, -camPos.y, -camPos.z);
            Matrix4f matrix = poseStack.last().pose();

            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            RenderSystem.enableDepthTest();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableCull();

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
                        r = 0; g = 150; b = 255; a = 60;
                    } else if (ChunkVisMod.chunkManager.isVisited(mc.level.dimension(), cx, cz)) {
                        r = 100; g = 80; b = 220; a = 50;
                    } else {
                        r = 80; g = 40; b = 40; a = 30;
                    }

                    buffer.addVertex(matrix, x1, (float) yLevel, z1).setColor(r, g, b, a);
                    buffer.addVertex(matrix, x2, (float) yLevel, z1).setColor(r, g, b, a);
                    buffer.addVertex(matrix, x2, (float) yLevel, z2).setColor(r, g, b, a);
                    buffer.addVertex(matrix, x1, (float) yLevel, z2).setColor(r, g, b, a);
                }
            }

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

                    int r, g, b, a = 200;
                    if (dx == 0 && dz == 0) {
                        r = 0; g = 191; b = 255;
                    } else if (ChunkVisMod.chunkManager.isVisited(mc.level.dimension(), cx, cz)) {
                        r = 130; g = 110; b = 255;
                    } else {
                        r = 180; g = 180; b = 180;
                    }

                    float y = (float) yLevel + 0.25f;

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

            RenderSystem.enableCull();
            RenderSystem.disableBlend();

            poseStack.popPose();
        });
    }
}
