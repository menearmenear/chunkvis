package menear.chunkvis;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class ChunkVisMod implements ClientModInitializer {
    public static final VisitedChunkManager chunkManager = new VisitedChunkManager();
    static ChunkVisOverlay overlay;
    public static boolean overlayVisible = false;
    public static boolean tracking = false;
    private static KeyMapping toggleKey;
    private static KeyMapping mapKey;
    private static KeyMapping zoomInKey;
    private static KeyMapping zoomOutKey;

    @Override
    public void onInitializeClient() {
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.chunkvis.toggle", GLFW.GLFW_KEY_U, "category.chunkvis"
        ));
        mapKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.chunkvis.map", GLFW.GLFW_KEY_M, "category.chunkvis"
        ));
        zoomInKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.chunkvis.zoomin", GLFW.GLFW_KEY_EQUAL, "category.chunkvis"
        ));
        zoomOutKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.chunkvis.zoomout", GLFW.GLFW_KEY_MINUS, "category.chunkvis"
        ));

        overlay = new ChunkVisOverlay();

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

            while (zoomInKey.consumeClick()) {
                overlay.changeZoom(2);
            }
            while (zoomOutKey.consumeClick()) {
                overlay.changeZoom(-2);
            }

            if (tracking) {
                int cx = (int) Math.floor(client.player.getX() / 16);
                int cz = (int) Math.floor(client.player.getZ() / 16);
                chunkManager.visitChunk(client.level.dimension(), cx, cz);
            }
        });

        ClientChunkEvents.CHUNK_LOAD.register((world, chunk) -> {
            if (tracking) {
                chunkManager.visitChunk(world.dimension(), chunk.getPos().x, chunk.getPos().z);
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

        HudRenderCallback.EVENT.register(overlay);
        ChunkVisWorldRenderer.register();
    }
}
