# Minecraft Java Clone

A Java-based Minecraft-like voxel game engine built with modern graphics rendering. This project implements core game mechanics including procedurally generated world terrain, physics simulation, block placement/removal, and advanced rendering features.

## Features

### Core Gameplay
- **Procedurally Generated World**: Infinite terrain generation with chunk-based system
- **Block Placement & Removal**: Interactive block manipulation with raycasting
- **Physics System**: Player movement, gravity, and collision detection
- **First-Person Camera**: Smooth FPS-style camera controls
- **Player Model**: Configurable player with customizable appearance

### Rendering
- **OpenGL Rendering**: Modern graphics pipeline using OpenGL
- **Chunk Mesh Optimization**: Efficient mesh generation and batching
- **Texture Atlas**: Dynamic texture atlas generation for block textures
- **Shadow Mapping**: Real-time shadow rendering (optional)
- **Highlight System**: Visual feedback for selected blocks
- **Performance Optimized**: Frustum culling and LOD optimization

### World Management
- **Chunk System**: Efficient terrain management with chunk loading/unloading
- **Block Types**: Multiple block types with distinct properties
- **Terrain Generation**: Procedural world generation with biomes
- **Save/Load System**: Persistent world data with auto-save functionality
- **Infinite Terrain**: Seamless terrain extending in all directions

## Project Structure

```
src/main/java/
├── Main.java                 # Entry point
├── Game.java                 # Main game loop and logic
├── Window.java               # GLFW window management
├── camera/                   # Camera system
├── player/                   # Player movement and state
├── world/                    # World, chunk, and block systems
│   ├── WorldManager.java
│   ├── Chunk.java
│   ├── Block.java
│   ├── blocks/              # Block type definitions
│   └── generator/           # Terrain generation
├── mesh/                     # Mesh data structures
├── render/                   # Rendering systems
│   ├── RenderManager.java
│   ├── ChunkRenderer.java
│   ├── ChunkMeshBuilder.java
│   ├── ShadowManager.java
│   └── HighlightManager.java
├── shader/                   # GLSL shaders
├── texture/                  # Texture management
├── physics/                  # Physics and collision detection
├── face/                     # Mesh face generation
└── saves/world/              # Game save files (chunks)
```

## Requirements

- **Java 21** or higher
- **Gradle** (included via gradlew)
- **OpenGL 3.3+** compatible graphics card
- Windows, macOS (Intel/ARM), or Linux

## Dependencies

- **LWJGL 3.3.4**: OpenGL, GLFW, STB bindings
- **JOML 1.10.5**: Java OpenGL Math Library for vector/matrix operations

## Building & Running

### Build the project
```bash
./gradlew build
```

### Run the game
```bash
./gradlew run
```

### Create executable JAR
```bash
./gradlew jar
java -jar build/libs/untitled.jar
```

## Controls

| Control | Action |
|---------|--------|
| **W/A/S/D** | Move forward/left/backward/right |
| **Space** | Jump |
| **Left Click** | Remove block |
| **Mouse** | Look around |


## Game Features Explained

### Procedural Generation
The world uses a custom terrain generator to create realistic landscapes with varied terrain heights and block types. Chunks are generated on-demand as the player explores.

### Block System
Blocks have properties including:
- Type (dirt, grass, stone, wood, etc.)
- Texture mapping
- Solid/transparent state
- Custom collision handling

### Physics Engine
Features include:
- Gravity simulation
- Collision detection
- Player movement with acceleration/deceleration
- Block interaction through raycasting

### Rendering Pipeline
- Vertex/Fragment shaders for lighting
- Chunk mesh generation with face culling
- Texture atlas for efficient batch rendering
- Optional shadow mapping for enhanced visuals
- Real-time highlight for selected blocks

### Save System
- Auto-save every 30 seconds
- Chunk-based persistence (`.dat` files in `saves/world/`)
- Load previous sessions automatically

## Performance Notes

- **Chunk Updates**: Optimized to avoid frame rate drops
- **Shadow Rendering**: Optional toggle to balance quality vs. performance
- **Frustum Culling**: Only visible chunks are rendered
- **Mesh Optimization**: Removed faces and efficient vertex layouts

## Configuration

Modifiable settings in `Game.java`:
- `SAVE_INTERVAL`: Auto-save frequency (default: 30 seconds)
- `shadowsEnabled`: Enable/disable shadow rendering
- Render distance and chunk loading

## Known Limitations

- Single-player only
- No survival/creative mode distinction
- Limited block variety
- No advanced features (redstone, crafting, etc.)

## Future Enhancements

- [ ] Multiplayer support
- [ ] More block types and variants
- [ ] Weather system (rain, snow)
- [ ] Inventory and crafting
- [ ] Mobs and NPCs
- [ ] Advanced terrain features (caves, structures)
- [ ] Water and lava simulation

## License

This is a personal project created for learning purposes.

## Credits

- LWJGL: OpenGL bindings and window management
- JOML: Mathematics library
- Java 21: Language and runtime

