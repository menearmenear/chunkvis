package chunkvis;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public class ChunkVisMod implements ClientModInitializer {
    public static final VisitedChunkManager chunkManager = new VisitedChunkManager();
    public static boolean overlayVisible = false;
    public static boolean tracking = false;
    private static KeyMapping toggleKey;
    private static KeyMapping mapKey;

    @Override
    public void onInitializeClient() {
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.chunkvis.toggle",
            GLFW.GLFW_KEY_U,
            "category.chunkvis"
        ));

        mapKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.chunkvis.map",
            GLFW.GLFW_KEY_M,
            "category.chunkvis"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.level == null) return;

            while (toggleKey.consumeClick()) {
                overlayVisible = !overlayVisible;
                if (overlayVisible) {
                    chunkManager.reset();
                    tracking = true;
                } else {
                    tracking = false;
                }
            }

            while (mapKey.consumeClick()) {
                if (!(client.screen instanceof ChunkVisScreen)) {
                    client.setScreen(new ChunkVisScreen());
                }
            }

            if (tracking) {
                int cx = (int) Math.floor(client.player.getX() / 16);
                int cz = (int) Math.floor(client.player.getZ() / 16);
                chunkManager.visitChunk(client.level.dimension(), cx, cz);
            }
        });

        ClientChunkEvents.CHUNK_LOAD.register((world, chunk) -> {
            if (tracking) {
                int cx = chunk.getPos().x;
                int cz = chunk.getPos().z;
                chunkManager.visitChunk(world.dimension(), cx, cz);
            }
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            chunkManager.save();
            overlayVisible = false;
            tracking = false;
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            chunkManager.save();
        });

        HudRenderCallback.EVENT.register(new ChunkVisOverlay());
        ChunkVisWorldRenderer.register();
    }
}
