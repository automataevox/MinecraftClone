package world;

import org.joml.Vector2i;
import org.joml.Vector3f;
import world.blocks.GrassBlock;
import world.blocks.StoneBlock;
import world.generator.NoiseGenerator;

import java.util.*;

public class WorldManager {

    private final Map<Vector3f, Block> blocks = new HashMap<>();
    private final Map<Vector2i, Boolean> loadedChunks = new HashMap<>();

    private final int CHUNK_SIZE = 16;
    private final int RENDER_DISTANCE = 3; // chunks around player

    private final NoiseGenerator noise = new NoiseGenerator(System.currentTimeMillis());

    // --- Generate a single chunk at chunk coordinates ---
    public void generateChunk(int chunkX, int chunkZ) {
        Vector2i chunkKey = new Vector2i(chunkX, chunkZ);
        if (loadedChunks.containsKey(chunkKey)) return;
        loadedChunks.put(chunkKey, true);

        int worldXOffset = chunkX * CHUNK_SIZE;
        int worldZOffset = chunkZ * CHUNK_SIZE;

        for (int x = 0; x < CHUNK_SIZE; x++) {
            for (int z = 0; z < CHUNK_SIZE; z++) {
                int worldX = x + worldXOffset;
                int worldZ = z + worldZOffset;

                float n = noise.interpolatedNoise(worldX * 0.1f, worldZ * 0.1f);
                int height = (int)((n + 1f) * 4) + 1; // height 1–9

                for (int y = 0; y < height; y++) {
                    Vector3f pos = new Vector3f(worldX, y, worldZ);
                    Block block = (y == height - 1) ? new GrassBlock() : new StoneBlock();
                    blocks.put(pos, block);
                }
            }
        }
    }

    // --- Generate chunks dynamically around player ---
    public void generateChunksAround(Vector3f playerPos) {
        int playerChunkX = (int)Math.floor(playerPos.x / CHUNK_SIZE);
        int playerChunkZ = (int)Math.floor(playerPos.z / CHUNK_SIZE);

        for (int dx = -RENDER_DISTANCE; dx <= RENDER_DISTANCE; dx++) {
            for (int dz = -RENDER_DISTANCE; dz <= RENDER_DISTANCE; dz++) {
                generateChunk(playerChunkX + dx, playerChunkZ + dz);
            }
        }
    }

    // --- Getters / World access ---
    public Block getBlock(Vector3f pos) {
        return blocks.get(pos);
    }

    public boolean hasCube(int x, int y, int z) {
        return blocks.containsKey(new Vector3f(x, y, z));
    }

    public void breakBlock(Vector3f pos) {
        blocks.remove(pos);
    }

    public Collection<Block> getBlocks() {
        return blocks.values();
    }

    public Set<Vector3f> getRenderList() {
        return blocks.keySet();
    }

    // --- Spawn point at first chunk ---
    public Vector3f getSpawnPoint() {
        int spawnX = CHUNK_SIZE / 2;
        int spawnZ = CHUNK_SIZE / 2;

        int highestY = -1;
        for (Vector3f pos : blocks.keySet()) {
            if ((int)pos.x == spawnX && (int)pos.z == spawnZ) {
                if (pos.y > highestY) highestY = (int)pos.y;
            }
        }

        return new Vector3f(spawnX + 0.5f, highestY + 1.5f, spawnZ + 0.5f);
    }

    public void cleanup() {
        blocks.clear();
        loadedChunks.clear();
    }
}
