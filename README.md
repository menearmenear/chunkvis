# ChunkVis

A Fabric mod for Minecraft 1.21.1 that shows visited chunks in a minimap and full-screen map. Perfect for use with [Simple World Downloader](https://modrinth.com/mod/simple-world-downloader) to track which chunks you've already visited and downloaded.

## Features

- **Minimap overlay** — press `U` to toggle. Shows the actual terrain rendered from above at 1px per block (176×176 texture), with visited chunks highlighted in green
- **Full-screen map** — press `M` to open. Shows terrain background, scroll to zoom, drag to pan
- **In-world chunk grid** — colored chunk overlays rendered at your feet level with visited/unvisited colors
- **Tracker** — starts fresh each time you press `U`, automatically saves on disconnect
- **Zoom** — `+` / `-` keys to zoom the minimap in and out

## Controls

| Key | Action |
|-----|--------|
| `U` | Toggle minimap overlay + start/stop tracking (resets each time) |
| `M` | Open full-screen interactive map |
| `+` | Zoom minimap in |
| `-` | Zoom minimap out |

## Requirements

- Minecraft 1.21.1
- Fabric Loader >=0.15.0
- Fabric API

## Usage

1. Press `U` to enable tracking — the minimap appears showing terrain from above, starts counting visited chunks from zero
2. Walk or fly around — visited chunks turn green on the minimap, in-world overlay, and full-screen map
3. Press `U` again to stop and reset the tracker
4. Press `M` at any time to open the full-screen map
5. Use `+` / `-` to zoom the minimap, scroll to zoom in the full-screen map

## Building from source

```bash
./gradlew build
```

The built jar will be in `build/libs/`.
