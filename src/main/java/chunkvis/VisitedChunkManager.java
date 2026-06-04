package chunkvis;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

public class VisitedChunkManager {
    private final Map<ResourceLocation, Set<Long>> visitedChunks = new HashMap<>();

    public void visitChunk(ResourceKey<Level> dimension, int x, int z) {
        ResourceLocation dimId = dimension.location();
        visitedChunks.computeIfAbsent(dimId, k -> new HashSet<>()).add(ChunkPos.asLong(x, z));
    }

    public boolean isVisited(ResourceKey<Level> dimension, int x, int z) {
        ResourceLocation dimId = dimension.location();
        Set<Long> chunks = visitedChunks.get(dimId);
        return chunks != null && chunks.contains(ChunkPos.asLong(x, z));
    }

    public int getVisitedCount(ResourceKey<Level> dimension) {
        Set<Long> chunks = visitedChunks.get(dimension.location());
        return chunks != null ? chunks.size() : 0;
    }

    public void reset() {
        visitedChunks.clear();
    }

    private Path getChunkvisDir() {
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.gameDirectory == null) return null;

        Path savesDir = mc.gameDirectory.toPath().resolve("saves");
        String saveName;

        if (mc.getSingleplayerServer() != null) {
            Path worldPath = mc.getSingleplayerServer()
                .getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT);
            saveName = worldPath.getParent().getFileName().toString();
        } else if (mc.getCurrentServer() != null) {
            saveName = mc.getCurrentServer().ip.replace(':', '_');
        } else {
            return null;
        }

        return savesDir.resolve(saveName).resolve("chunkvis");
    }

    public void save() {
        if (visitedChunks.isEmpty()) return;
        Path dir = getChunkvisDir();
        if (dir == null) return;

        try {
            Files.createDirectories(dir);
            for (Map.Entry<ResourceLocation, Set<Long>> entry : visitedChunks.entrySet()) {
                ResourceLocation dim = entry.getKey();
                Set<Long> chunks = entry.getValue();
                if (chunks.isEmpty()) continue;
                String filename = dim.getNamespace() + "." + dim.getPath().replace('/', '.') + ".txt";
                Path file = dir.resolve(filename);

                StringBuilder sb = new StringBuilder();
                for (Long packed : chunks) {
                    sb.append(ChunkPos.getX(packed)).append(',')
                      .append(ChunkPos.getZ(packed)).append('\n');
                }
                Files.writeString(file, sb.toString(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void load() {
        Path dir = getChunkvisDir();
        if (dir == null || !Files.exists(dir)) return;

        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.txt")) {
            for (Path file : stream) {
                String filename = file.getFileName().toString();
                String dimStr = filename.substring(0, filename.length() - 4);
                String namespace;
                String path;
                int dot = dimStr.indexOf('.');
                if (dot == -1) continue;
                namespace = dimStr.substring(0, dot);
                path = dimStr.substring(dot + 1).replace('.', '/');
                ResourceLocation dimId = ResourceLocation.tryParse(namespace + ":" + path);
                if (dimId == null) continue;

                Set<Long> chunks = new HashSet<>();
                List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
                for (String line : lines) {
                    if (line.isBlank()) continue;
                    String[] parts = line.split(",");
                    if (parts.length == 2) {
                        try {
                            int x = Integer.parseInt(parts[0].trim());
                            int z = Integer.parseInt(parts[1].trim());
                            chunks.add(ChunkPos.asLong(x, z));
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
                visitedChunks.put(dimId, chunks);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
