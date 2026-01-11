package world;

import org.joml.Vector2i;
import org.joml.Vector3f;
import render.ChunkRenderer;
import world.blocks.*;
import world.generator.NoiseGenerator;

import java.io.*;
import java.util.*;

public class WorldManager {
    private final Map<Vector2i, Chunk> chunks = new HashMap<>();
    private final Map<Vector3f, Block> blocks = new HashMap<>();
    private final int CHUNK_SIZE = 16;
    private final int RENDER_DISTANCE = ChunkRenderer.getRenderDistance();
    private final int PRELOAD_DISTANCE = 6;

    // Chunk loading queue
    private final Queue<Vector2i> chunksToGenerate = new LinkedList<>();
    private final Set<Vector2i> currentlyGenerating = new HashSet<>();
    private final Set<Vector2i> modifiedChunks = new HashSet<>();

    private final NoiseGenerator noise = new NoiseGenerator(System.currentTimeMillis());

    // === CHUNK SAVE/LOAD PATHS ===
    private static final String SAVE_DIR = "saves/world/";

    static {
        // Create save directory on startup
        new File(SAVE_DIR).mkdirs();
    }

    // === MODIFIED: generateChunkInternal with proper save/load ===
    private void generateChunkInternal(int chunkX, int chunkZ) {
        Vector2i chunkKey = new Vector2i(chunkX, chunkZ);

        // Already loaded?
        if (chunks.containsKey(chunkKey)) {
            return;
        }

        // 1. FIRST: Try to load from saved file
        Chunk loadedChunk = loadChunkFromDisk(chunkX, chunkZ);
        if (loadedChunk != null) {
            chunks.put(chunkKey, loadedChunk);
            // IMPORTANT: Update the blocks map with loaded chunk data
            updateBlocksMapFromChunk(loadedChunk);
            return;
        }

        // 2. SECOND: Generate new chunk (only if no saved data exists)
        Chunk chunk = new Chunk(chunkX, 0, chunkZ);
        chunks.put(chunkKey, chunk);

        int worldXOffset = chunkX * CHUNK_SIZE;
        int worldZOffset = chunkZ * CHUNK_SIZE;

        NoiseGenerator stoneNoise = new NoiseGenerator(System.currentTimeMillis() + 12345);

        for (int x = 0; x < CHUNK_SIZE; x++) {
            for (int z = 0; z < CHUNK_SIZE; z++) {
                int worldX = x + worldXOffset;
                int worldZ = z + worldZOffset;

                // Terrain height noise
                float n = noise.interpolatedNoise(worldX * .02f, worldZ * .04f);
                int baseHeight = (int)((n + 2f) * 4) + 5;

                // Stone layer thickness
                float stoneNoiseVal = stoneNoise.interpolatedNoise(worldX * 0.1f, worldZ * 0.2f);
                int stoneLayers = 10 + (int)((stoneNoiseVal + 1f) * 1.5f);

                int totalHeight = Math.max(baseHeight, stoneLayers + 1);

                // Biome/moisture noise to determine top block type
                // Using lower frequency for larger biome areas
                float moisture = noise.interpolatedMoistureNoise(worldX * 0.5f, worldZ * 0.5f);

                for (int y = 0; y < totalHeight; y++) {
                    Vector3f pos = new Vector3f(worldX, y, worldZ);
                    Block block;

                    // Top layer logic
                    if (y == totalHeight - 1) {
                        // Determine top block based on moisture noise
                        if (moisture > 0.2f) {
                            // Moist areas get grass (you'll need to create a GrassBlock class)
                            block = new GrassBlock();
                        } else if (moisture < -0.2f) {
                            // Dry areas get sand
                            block = new SandBlock();
                        } else {
                            // Transition areas get dirt (you'll need to create a DirtBlock class)
                            block = new GrassBlock();
                        }
                    }
                    // Just below top layer - convert grass to dirt or keep as sand
                    else if (y == totalHeight - 2) {
                        if (moisture > 0.2f) {
                            // Under grass should be dirt
                            block = new DirtBlock();
                        } else {
                            // Under sand should be more sand
                            block = new SandBlock();
                        }
                    }
                    // Upper stone layers (with some dirt transition)
                    else if (y >= totalHeight - 3 && y >= totalHeight - 1 - stoneLayers) {
                        // Add some dirt/gravel transition before pure stone
                        if (y >= totalHeight - 5 && y < totalHeight - 3 && moisture > 0.2f) {
                            block = new GrassBlock();
                        } else {
                            block = new StoneBlock();
                        }
                    }
                    // Pure stone layers
                    else if (y >= totalHeight - 1 - stoneLayers) {
                        block = new StoneBlock();
                    }
                    // Everything else (debug or air)
                    else {
                        block = new GrassBlock();
                    }

                    blocks.put(pos, block);
                    chunk.setBlock(x, y, z, block);
                }
            }
        }
    }

    // === NEW: Update blocks map from loaded chunk ===
    private void updateBlocksMapFromChunk(Chunk chunk) {
        int worldXOffset = chunk.chunkX * CHUNK_SIZE;
        int worldZOffset = chunk.chunkZ * CHUNK_SIZE;

        for (int x = 0; x < CHUNK_SIZE; x++) {
            for (int y = 0; y < Chunk.SIZE; y++) {
                for (int z = 0; z < CHUNK_SIZE; z++) {
                    Block block = chunk.getBlock(x, y, z);
                    if (block != null && block.isVisible()) {
                        Vector3f worldPos = new Vector3f(
                                worldXOffset + x,
                                y,
                                worldZOffset + z
                        );
                        blocks.put(worldPos, block);
                    } else {
                        // Remove if exists (for broken blocks)
                        Vector3f worldPos = new Vector3f(
                                worldXOffset + x,
                                y,
                                worldZOffset + z
                        );
                        blocks.remove(worldPos);
                    }
                }
            }
        }
    }

    // === MODIFIED: Save chunk to disk ===
    private void saveChunkToDisk(Chunk chunk) {
        if (chunk == null) return;

        String filename = SAVE_DIR + "chunk_" + chunk.chunkX + "_" + chunk.chunkZ + ".dat";

        try (DataOutputStream dos = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(filename)))) {

            // Write chunk coordinates
            dos.writeInt(chunk.chunkX);
            dos.writeInt(chunk.chunkZ);

            // Write block data
            int savedBlocks = 0;
            for (int x = 0; x < CHUNK_SIZE; x++) {
                for (int y = 0; y < Chunk.SIZE; y++) {
                    for (int z = 0; z < CHUNK_SIZE; z++) {
                        Block block = chunk.getBlock(x, y, z);
                        if (block != null && block.isVisible()) {
                            dos.writeByte(1); // Block exists
                            // Write block type
                            if (block instanceof GrassBlock) {
                                dos.writeUTF("grass");
                            } else if (block instanceof StoneBlock) {
                                dos.writeUTF("stone");
                            } else if (block instanceof SandBlock) {
                                dos.writeUTF("sand");
                            } else {
                                dos.writeUTF("null"); // Default
                            }
                            savedBlocks++;
                        } else {
                            dos.writeByte(0); // Air block
                        }
                    }
                }
            }

        } catch (IOException e) {
            System.err.println("❌ Failed to save chunk " + chunk.chunkX + "," + chunk.chunkZ + ": " + e.getMessage());
        }
    }

    // === MODIFIED: Load chunk from disk ===
    private Chunk loadChunkFromDisk(int chunkX, int chunkZ) {
        String filename = SAVE_DIR + "chunk_" + chunkX + "_" + chunkZ + ".dat";
        File file = new File(filename);

        if (!file.exists()) {
            return null; // No saved data
        }

        try (DataInputStream dis = new DataInputStream(
                new BufferedInputStream(new FileInputStream(filename)))) {

            // Verify coordinates
            int savedX = dis.readInt();
            int savedZ = dis.readInt();

            if (savedX != chunkX || savedZ != chunkZ) {
                System.err.println("❌ Chunk file corrupted: coordinates mismatch");
                return null;
            }

            Chunk chunk = new Chunk(chunkX, 0, chunkZ);

            // Read block data
            int loadedBlocks = 0;
            for (int x = 0; x < CHUNK_SIZE; x++) {
                for (int y = 0; y < Chunk.SIZE; y++) {
                    for (int z = 0; z < CHUNK_SIZE; z++) {
                        byte hasBlock = dis.readByte();
                        if (hasBlock == 1) {
                            String blockType = dis.readUTF();
                            Block block;

                            switch (blockType) {
                                case "grass":
                                    block = new GrassBlock();
                                    break;
                                case "stone":
                                    block = new StoneBlock();
                                    break;
                                case "sand":
                                    block = new SandBlock();
                                    break;
                                case "debug":
                                    block = new DebugBlock();
                                    break;
                                case null:
                                    block = new DebugBlock();
                                    break;
                                default:
                                    System.err.println("Unknown block type: " + blockType + ", using dirt");
                                    block = new DirtBlock();
                            }

                            chunk.setBlock(x, y, z, block);
                            loadedBlocks++;
                        }
                        // else: air (null block) - already null by default
                    }
                }
            }

            // Mark as NOT modified (since we just loaded it fresh)
            chunk.markClean();
            return chunk;

        } catch (IOException e) {
            System.err.println("❌ Failed to load chunk " + chunkX + "," + chunkZ + ": " + e.getMessage());
            return null;
        }
    }

    // === MODIFIED: Save all modified chunks ===
    public void saveModifiedChunks() {
        if (modifiedChunks.isEmpty()) {
            return;
        }

        int savedCount = 0;

        // Create a copy to avoid ConcurrentModificationException
        Set<Vector2i> chunksToSave = new HashSet<>(modifiedChunks);

        for (Vector2i chunkKey : chunksToSave) {
            Chunk chunk = chunks.get(chunkKey);
            if (chunk != null && chunk.isModified()) {
                saveChunkToDisk(chunk);
                chunk.markClean(); // Mark as clean after saving
                savedCount++;
            }
        }

        // Only clear the ones we saved
        modifiedChunks.removeAll(chunksToSave);
    }

    // === MODIFIED: Mark chunk as modified ===
    private void markChunkModified(int chunkX, int chunkZ) {
        Vector2i chunkKey = new Vector2i(chunkX, chunkZ);
        modifiedChunks.add(chunkKey);

        Chunk chunk = chunks.get(chunkKey);
        if (chunk != null) {
            // Force the chunk to be marked as modified
            // (Chunk.setBlock() should already do this, but just in case)
        }

    }

    // === MODIFIED: breakBlock ===
    public void breakBlock(Vector3f pos) {
        int x = (int)pos.x;
        int y = (int)pos.y;
        int z = (int)pos.z;


        // Remove from blocks map
        Vector3f blockPos = new Vector3f(x, y, z);
        Block removed = blocks.remove(blockPos);

        if (removed == null) {
            System.out.println("  No block found at that position");
            return;
        }

        // Update chunk
        int chunkX = (int)Math.floor(pos.x / CHUNK_SIZE);
        int chunkZ = (int)Math.floor(pos.z / CHUNK_SIZE);
        Vector2i chunkKey = new Vector2i(chunkX, chunkZ);

        Chunk chunk = chunks.get(chunkKey);
        if (chunk != null) {
            int localX = x - chunkX * CHUNK_SIZE;
            int localZ = z - chunkZ * CHUNK_SIZE;
            chunk.setBlock(localX, y, localZ, null);

            // Mark chunk as modified
            markChunkModified(chunkX, chunkZ);
        } else {
            System.out.println("  ERROR: Chunk not loaded!");
        }
    }

    // === MODIFIED: placeBlock ===
    public boolean placeBlock(Vector3f pos, Block block) {
        int x = (int)pos.x;
        int y = (int)pos.y;
        int z = (int)pos.z;

        System.out.println("Placing " + block.getName() + " at " + x + "," + y + "," + z);

        // Check if position is already occupied
        if (hasCube(x, y, z)) {
            System.out.println("  Position already occupied");
            return false;
        }

        // Check if position is adjacent to an existing block
        boolean adjacent = false;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) == 1) {
                        if (hasCube(x + dx, y + dy, z + dz)) {
                            adjacent = true;
                            break;
                        }
                    }
                }
                if (adjacent) break;
            }
            if (adjacent) break;
        }

        if (!adjacent) {
            System.out.println("  Not adjacent to any block (floating)");
            return false;
        }

        // Place the block
        Vector3f blockPos = new Vector3f(x, y, z);
        blocks.put(blockPos, block);

        // Update chunk
        int chunkX = (int)Math.floor(pos.x / CHUNK_SIZE);
        int chunkZ = (int)Math.floor(pos.z / CHUNK_SIZE);
        Vector2i chunkKey = new Vector2i(chunkX, chunkZ);

        Chunk chunk = chunks.get(chunkKey);
        if (chunk != null) {
            int localX = x - chunkX * CHUNK_SIZE;
            int localZ = z - chunkZ * CHUNK_SIZE;
            chunk.setBlock(localX, y, localZ, block);

            // Mark chunk as modified
            markChunkModified(chunkX, chunkZ);
        }

        return true;
    }

    // === MODIFIED: Generate chunks around player (save before unloading) ===
    public void generateChunksAround(Vector3f playerPos) {
        int playerChunkX = (int)Math.floor(playerPos.x / CHUNK_SIZE);
        int playerChunkZ = (int)Math.floor(playerPos.z / CHUNK_SIZE);

        Set<Vector2i> neededChunks = new HashSet<>();

        // Determine which chunks are needed
        for (int dx = -RENDER_DISTANCE; dx <= RENDER_DISTANCE; dx++) {
            for (int dz = -RENDER_DISTANCE; dz <= RENDER_DISTANCE; dz++) {
                int cx = playerChunkX + dx;
                int cz = playerChunkZ + dz;

                // Circular distance check
                if (dx * dx + dz * dz > RENDER_DISTANCE * RENDER_DISTANCE) {
                    continue;
                }

                neededChunks.add(new Vector2i(cx, cz));

                // Add to generation queue if not already loaded/generating
                Vector2i chunkKey = new Vector2i(cx, cz);
                if (!chunks.containsKey(chunkKey) && !currentlyGenerating.contains(chunkKey)) {
                    chunksToGenerate.add(chunkKey);
                    currentlyGenerating.add(chunkKey);
                }
            }
        }

        // Generate 1-2 chunks per frame (smooth loading)
        int chunksGenerated = 0;
        while (!chunksToGenerate.isEmpty() && chunksGenerated < 2) {
            Vector2i chunkKey = chunksToGenerate.poll();
            generateChunkInternal(chunkKey.x, chunkKey.y);
            currentlyGenerating.remove(chunkKey);
            chunksGenerated++;
        }

        // Unload distant chunks (SAVE THEM FIRST!)
        Iterator<Map.Entry<Vector2i, Chunk>> iterator = chunks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Vector2i, Chunk> entry = iterator.next();
            Vector2i chunkKey = entry.getKey();

            if (!neededChunks.contains(chunkKey)) {
                Chunk chunk = entry.getValue();

                // SAVE before unloading!
                if (chunk.isModified()) {
                    saveChunkToDisk(chunk);
                    chunk.markClean();
                    modifiedChunks.remove(chunkKey);
                }

                // Remove blocks from flat map
                int worldXOffset = chunkKey.x * CHUNK_SIZE;
                int worldZOffset = chunkKey.y * CHUNK_SIZE;
                for (int x = 0; x < CHUNK_SIZE; x++) {
                    for (int z = 0; z < CHUNK_SIZE; z++) {
                        for (int y = 0; y < 256; y++) {
                            blocks.remove(new Vector3f(worldXOffset + x, y, worldZOffset + z));
                        }
                    }
                }

                // Remove chunk
                iterator.remove();
            }
        }
    }

    // === MODIFIED: Cleanup - save everything ===
    public void cleanup() {

        // Save all modified chunks
        saveModifiedChunks();

        // Clean up all chunks
        for (Chunk chunk : chunks.values()) {
            chunk.cleanup();
        }
        chunks.clear();

        // Clear other collections
        blocks.clear();
        chunksToGenerate.clear();
        currentlyGenerating.clear();
        modifiedChunks.clear();
    }

    // === NEW: Debug method to check chunk state ===
    public void debugChunkState(int chunkX, int chunkZ) {
        Vector2i key = new Vector2i(chunkX, chunkZ);
        Chunk chunk = chunks.get(key);

        System.out.println("=== DEBUG Chunk " + chunkX + "," + chunkZ + " ===");
        System.out.println("Loaded: " + (chunk != null));
        System.out.println("Modified: " + (chunk != null && chunk.isModified()));
        System.out.println("Visible blocks: " + (chunk != null ? chunk.getVisibleBlockCount() : 0));
        System.out.println("Has saved file: " + new File(SAVE_DIR + "chunk_" + chunkX + "_" + chunkZ + ".dat").exists());
        System.out.println("In modified set: " + modifiedChunks.contains(key));
    }

    // === The rest of your existing methods (unchanged) ===
    public List<Chunk> getLoadedChunks() {
        return new ArrayList<>(chunks.values());
    }

    public Chunk getChunkAt(int chunkX, int chunkZ) {
        return chunks.get(new Vector2i(chunkX, chunkZ));
    }

    public Vector3f getSpawnPoint() {
        // ... (your existing spawn point code)
        return new Vector3f(8.5f, 65.0f, 8.5f); // Example
    }

    public void generateInitialChunks() {
        // Generate spawn chunk first
        generateChunkInternal(0, 0);

        // Generate immediate area around spawn
        for (int dx = -PRELOAD_DISTANCE; dx <= PRELOAD_DISTANCE; dx++) {
            for (int dz = -PRELOAD_DISTANCE; dz <= PRELOAD_DISTANCE; dz++) {
                Vector2i key = new Vector2i(dx, dz);
                if (!chunks.containsKey(key)) {
                    generateChunkInternal(dx, dz);
                }
            }
        }

    }

    public Set<Vector3f> getRenderList() {
        return blocks.keySet();
    }

    public Block getBlock(Vector3f pos) {
        return blocks.get(pos);
    }

    public boolean hasCube(int x, int y, int z) {
        return blocks.containsKey(new Vector3f(x, y, z));
    }

    public int getTotalBlockCount() {
        return blocks.size();
    }

    public int getTotalChunkCount() {
        return chunks.size();
    }

    public boolean isPositionLoaded(Vector3f pos) {
        int chunkX = (int)Math.floor(pos.x / CHUNK_SIZE);
        int chunkZ = (int)Math.floor(pos.z / CHUNK_SIZE);
        return chunks.containsKey(new Vector2i(chunkX, chunkZ));
    }

    public boolean isPositionSafe(Vector3f pos) {
        int x = (int)Math.floor(pos.x);
        int y = (int)Math.floor(pos.y);
        int z = (int)Math.floor(pos.z);

        for (int checkY = y; checkY >= y - 10; checkY--) {
            if (hasCube(x, checkY, z)) {
                return true;
            }
        }

        return false;
    }

    public void forceGenerateChunk(int chunkX, int chunkZ) {
        generateChunkInternal(chunkX, chunkZ);
    }
}