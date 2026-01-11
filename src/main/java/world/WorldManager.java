package world;

import org.joml.Vector2i;
import org.joml.Vector3f;
import world.blocks.GrassBlock;
import world.blocks.StoneBlock;
import world.generator.NoiseGenerator;

import java.util.*;

public class WorldManager {
    private final Map<Vector2i, Chunk> chunks = new HashMap<>();
    private final Map<Vector3f, Block> blocks = new HashMap<>();
    private final int CHUNK_SIZE = 16;
    private final int RENDER_DISTANCE = 1;
    private final int PRELOAD_DISTANCE = 1; // Generate immediate area first

    // Chunk loading queue
    private final Queue<Vector2i> chunksToGenerate = new LinkedList<>();
    private final Set<Vector2i> currentlyGenerating = new HashSet<>();
    private final Set<Vector2i> modifiedChunks = new HashSet<>();

    private final NoiseGenerator noise = new NoiseGenerator(System.currentTimeMillis());

    // === EXISTING METHODS ===

    public List<Chunk> getLoadedChunks() {
        return new ArrayList<>(chunks.values());
    }

    public Chunk getChunkAt(int chunkX, int chunkZ) {
        return chunks.get(new Vector2i(chunkX, chunkZ));
    }

    private void generateChunkInternal(int chunkX, int chunkZ) {
        Vector2i chunkKey = new Vector2i(chunkX, chunkZ);
        if (chunks.containsKey(chunkKey)) return;

        Chunk chunk = new Chunk(chunkX, 0, chunkZ);
        chunks.put(chunkKey, chunk);

        int worldXOffset = chunkX * CHUNK_SIZE;
        int worldZOffset = chunkZ * CHUNK_SIZE;

        NoiseGenerator stoneNoise = new NoiseGenerator(System.currentTimeMillis() + 12345);

        for (int x = 0; x < CHUNK_SIZE; x++) {
            for (int z = 0; z < CHUNK_SIZE; z++) {
                int worldX = x + worldXOffset;
                int worldZ = z + worldZOffset;

                float n = noise.interpolatedNoise(worldX * 0.1f, worldZ * 0.1f);
                int baseHeight = (int)((n + 2f) * 4) + 1;

                float stoneNoiseVal = stoneNoise.interpolatedNoise(worldX * 0.2f, worldZ * 0.2f);
                int stoneLayers = 10 + (int)((stoneNoiseVal + 1f) * 1.5f);

                int totalHeight = Math.max(baseHeight, stoneLayers + 1);

                for (int y = 0; y < totalHeight; y++) {
                    Vector3f pos = new Vector3f(worldX, y, worldZ);
                    Block block;

                    if (y == totalHeight - 1) {
                        block = new GrassBlock();
                    } else if (y >= totalHeight - 1 - stoneLayers) {
                        block = new StoneBlock();
                    } else {
                        block = new StoneBlock();
                    }

                    blocks.put(pos, block);
                    chunk.setBlock(x, y, z, block);
                }
            }
        }
    }

    // === CHUNK PERSISTENCE METHODS ===

    // Track when chunks are modified (call this when blocks are broken/placed)
    private void markChunkModified(int chunkX, int chunkZ) {
        modifiedChunks.add(new Vector2i(chunkX, chunkZ));
    }

    public void saveModifiedChunks() {
        if (modifiedChunks.isEmpty()) {
            return;
        }

        int savedCount = 0;
        for (Vector2i chunkKey : modifiedChunks) {
            Chunk chunk = chunks.get(chunkKey);
            if (chunk != null && chunk.isModified()) {
                saveChunkToDisk(chunk);
                chunk.markClean(); // Mark as clean after saving
                savedCount++;
            }
        }

        System.out.println("Saved " + savedCount + " modified chunks to disk");
        modifiedChunks.clear();
    }

    // Save a single chunk to disk
    private void saveChunkToDisk(Chunk chunk) {
        // Create saves directory if it doesn't exist
        java.io.File saveDir = new java.io.File("saves/world/");
        if (!saveDir.exists()) {
            saveDir.mkdirs();
        }

        String filename = "saves/world/chunk_" + chunk.chunkX + "_" + chunk.chunkZ + ".dat";

        try (java.io.DataOutputStream dos = new java.io.DataOutputStream(
                new java.io.FileOutputStream(filename))) {

            // Write chunk coordinates
            dos.writeInt(chunk.chunkX);
            dos.writeInt(chunk.chunkZ);

            // Write block data
            for (int x = 0; x < Chunk.SIZE; x++) {
                for (int y = 0; y < Chunk.SIZE; y++) {
                    for (int z = 0; z < Chunk.SIZE; z++) {
                        Block block = chunk.getBlock(x, y, z);
                        if (block != null) {
                            dos.writeByte(1); // Block exists
                            // Write block type
                            if (block instanceof world.blocks.GrassBlock) {
                                dos.writeUTF("grass");
                            } else if (block instanceof world.blocks.StoneBlock) {
                                dos.writeUTF("stone");
                            } else {
                                dos.writeUTF("stone"); // Default fallback
                            }
                        } else {
                            dos.writeByte(0); // Air block
                        }
                    }
                }
            }

            System.out.println("Saved chunk " + chunk.chunkX + "," + chunk.chunkZ + " to disk");

        } catch (java.io.IOException e) {
            System.err.println("Failed to save chunk " + chunk.chunkX + "," + chunk.chunkZ + ": " + e.getMessage());
        }
    }

    // Load a chunk from disk
    private Chunk loadChunkFromDisk(int chunkX, int chunkZ) {
        String filename = "saves/world/chunk_" + chunkX + "_" + chunkZ + ".dat";
        java.io.File file = new java.io.File(filename);

        if (!file.exists()) {
            return null; // No saved data
        }

        try (java.io.DataInputStream dis = new java.io.DataInputStream(
                new java.io.FileInputStream(filename))) {

            // Verify coordinates
            int savedX = dis.readInt();
            int savedZ = dis.readInt();

            if (savedX != chunkX || savedZ != chunkZ) {
                System.err.println("Chunk file corrupted: coordinates mismatch");
                return null;
            }

            Chunk chunk = new Chunk(chunkX, 0, chunkZ);

            // Read block data
            for (int x = 0; x < Chunk.SIZE; x++) {
                for (int y = 0; y < Chunk.SIZE; y++) {
                    for (int z = 0; z < Chunk.SIZE; z++) {
                        byte hasBlock = dis.readByte();
                        if (hasBlock == 1) {
                            String blockType = dis.readUTF();
                            Block block;

                            switch (blockType) {
                                case "grass":
                                    block = new world.blocks.GrassBlock();
                                    break;
                                case "stone":
                                    block = new world.blocks.StoneBlock();
                                    break;
                                default:
                                    block = new world.blocks.StoneBlock(); // Default
                            }

                            chunk.setBlock(x, y, z, block);
                            // Also add to blocks map
                            Vector3f worldPos = new Vector3f(
                                    chunkX * CHUNK_SIZE + x,
                                    y,
                                    chunkZ * CHUNK_SIZE + z
                            );
                            blocks.put(worldPos, block);
                        }
                    }
                }
            }

            System.out.println("Loaded chunk " + chunkX + "," + chunkZ + " from disk");
            return chunk;

        } catch (java.io.IOException e) {
            System.err.println("Failed to load chunk " + chunkX + "," + chunkZ + ": " + e.getMessage());
            return null;
        }
    }

    // Check if a chunk has saved data on disk
    public boolean hasSavedChunk(int chunkX, int chunkZ) {
        String filename = "saves/world/chunk_" + chunkX + "_" + chunkZ + ".dat";
        return new java.io.File(filename).exists();
    }

    // Save chunk immediately (for testing)
    public void saveChunkImmediately(int chunkX, int chunkZ) {
        Vector2i chunkKey = new Vector2i(chunkX, chunkZ);
        Chunk chunk = chunks.get(chunkKey);
        if (chunk != null) {
            saveChunkToDisk(chunk);
            chunk.markClean();
            modifiedChunks.remove(chunkKey);
        }
    }


    // === SPAWN POINT FIX ===
    public Vector3f getSpawnPoint() {
        // Start at world center (chunk 0,0)
        int spawnChunkX = 0;
        int spawnChunkZ = 0;

        // Ensure the spawn chunk exists
        if (!chunks.containsKey(new Vector2i(spawnChunkX, spawnChunkZ))) {
            generateChunkInternal(spawnChunkX, spawnChunkZ);
        }

        // Find the highest solid block in the center of chunk (0,0)
        int centerX = spawnChunkX * CHUNK_SIZE + CHUNK_SIZE / 2;
        int centerZ = spawnChunkZ * CHUNK_SIZE + CHUNK_SIZE / 2;

        int highestY = -1;
        for (int y = 255; y >= 0; y--) {
            Vector3f pos = new Vector3f(centerX, y, centerZ);
            if (blocks.containsKey(pos)) {
                Block block = blocks.get(pos);
                if (block != null && block.isVisible()) {
                    highestY = y;
                    break;
                }
            }
        }

        // If no solid block found, generate terrain at spawn
        if (highestY < 0) {
            // Generate terrain column at spawn point
            float n = noise.interpolatedNoise(centerX * 0.1f, centerZ * 0.1f);
            int baseHeight = (int)((n + 2f) * 4) + 1;
            highestY = baseHeight;

            // Generate blocks from bedrock to surface
            for (int y = 0; y <= baseHeight; y++) {
                Vector3f pos = new Vector3f(centerX, y, centerZ);
                Block block;

                if (y == baseHeight) {
                    block = new GrassBlock();
                } else if (y >= baseHeight - 3) {
                    block = new StoneBlock();
                } else {
                    block = new StoneBlock();
                }

                blocks.put(pos, block);
                Chunk chunk = getChunkAt(spawnChunkX, spawnChunkZ);
                if (chunk != null) {
                    int localX = centerX - spawnChunkX * CHUNK_SIZE;
                    int localZ = centerZ - spawnChunkZ * CHUNK_SIZE;
                    chunk.setBlock(localX, y, localZ, block);
                }
            }
        }

        // Return position on top of the highest block, plus player height
        return new Vector3f(
                centerX + 0.5f,  // Center of block
                highestY + 1.0f,  // On top of block
                centerZ + 0.5f
        );
    }

    // === PUBLIC API METHODS ===

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

        // Unload distant chunks
        Iterator<Map.Entry<Vector2i, Chunk>> iterator = chunks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Vector2i, Chunk> entry = iterator.next();
            if (!neededChunks.contains(entry.getKey())) {
                Chunk chunk = entry.getValue();

                // Remove blocks from flat map
                int worldXOffset = entry.getKey().x * CHUNK_SIZE;
                int worldZOffset = entry.getKey().y * CHUNK_SIZE;
                for (int x = 0; x < CHUNK_SIZE; x++) {
                    for (int z = 0; z < CHUNK_SIZE; z++) {
                        for (int y = 0; y < 256; y++) {
                            blocks.remove(new Vector3f(worldXOffset + x, y, worldZOffset + z));
                        }
                    }
                }

                // Queue chunk mesh for cleanup (renderer will handle)
                iterator.remove();
            }
        }
    }

    public void breakBlock(Vector3f pos) {
        // Remove from blocks map
        blocks.remove(pos);

        // Update chunk
        int chunkX = (int)Math.floor(pos.x / CHUNK_SIZE);
        int chunkZ = (int)Math.floor(pos.z / CHUNK_SIZE);
        Vector2i chunkKey = new Vector2i(chunkX, chunkZ);

        Chunk chunk = chunks.get(chunkKey);
        if (chunk != null) {
            int localX = (int)pos.x - chunkX * CHUNK_SIZE;
            int localZ = (int)pos.z - chunkZ * CHUNK_SIZE;
            chunk.setBlock(localX, (int)pos.y, localZ, null);
        }
    }

    // === INITIAL CHUNK GENERATION ===
    public void generateInitialChunks() {
        // Generate spawn chunk first
        generateChunkInternal(0, 0);

        // Generate immediate area around spawn
        for (int dx = -PRELOAD_DISTANCE; dx <= PRELOAD_DISTANCE; dx++) {
            for (int dz = -PRELOAD_DISTANCE; dz <= PRELOAD_DISTANCE; dz++) {
                // Skip already generated chunks
                Vector2i key = new Vector2i(dx, dz);
                if (!chunks.containsKey(key)) {
                    generateChunkInternal(dx, dz);
                }
            }
        }

        System.out.println("Initial chunks generated: " + chunks.size());
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

    public void cleanup() {
        // Save all modified chunks before cleaning up
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

    // === ADDITIONAL HELPER METHODS ===

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

        // Check if position has solid ground below
        for (int checkY = y; checkY >= y - 10; checkY--) {
            if (hasCube(x, checkY, z)) {
                return true;
            }
        }

        return false;
    }

    public boolean placeBlock(Vector3f pos, Block block) {
        // Check if position is already occupied
        if (hasCube((int)pos.x, (int)pos.y, (int)pos.z)) {
            return false;
        }

        // Check if position is adjacent to an existing block (optional)
        boolean adjacent = false;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) == 1) { // Only direct neighbors
                        if (hasCube((int)pos.x + dx, (int)pos.y + dy, (int)pos.z + dz)) {
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
            return false; // Can't place floating blocks
        }

        // Place the block
        blocks.put(pos, block);

        // Update chunk
        int chunkX = (int)Math.floor(pos.x / CHUNK_SIZE);
        int chunkZ = (int)Math.floor(pos.z / CHUNK_SIZE);
        Vector2i chunkKey = new Vector2i(chunkX, chunkZ);

        Chunk chunk = chunks.get(chunkKey);
        if (chunk != null) {
            int localX = (int)pos.x - chunkX * CHUNK_SIZE;
            int localZ = (int)pos.z - chunkZ * CHUNK_SIZE;
            chunk.setBlock(localX, (int)pos.y, localZ, block);

            // Mark chunk as modified
            modifiedChunks.add(chunkKey);
        }

        return true;
    }

    // Force generate a chunk (for testing)
    public void forceGenerateChunk(int chunkX, int chunkZ) {
        generateChunkInternal(chunkX, chunkZ);
    }
}