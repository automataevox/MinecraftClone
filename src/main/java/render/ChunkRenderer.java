package render;

import mesh.ChunkMesh;
import mesh.ChunkMeshData;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL13;
import shader.Shader;
import camera.Camera;
import texture.TextureAtlas;
import world.Chunk;
import world.WorldManager;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Future;

public class ChunkRenderer {
    private final Shader shader;
    private final Camera camera;
    private final WorldManager worldManager;
    private final TextureAtlas textureAtlas;
    private final ShadowManager shadowManager;

    // Performance settings
    private static final int RENDER_DISTANCE = 4;
    private final int MAX_ASYNC_BUILDS_PER_FRAME = 12;
    private final int MAX_SYNC_BUILDS_PER_FRAME = 5;

    // Mesh cache
    private final Map<String, ChunkMesh> chunkMeshes = new HashMap<>();
    private final Map<String, Future<List<ChunkMeshData>>> pendingBuilds = new HashMap<>();

    // Track which chunks are loaded from disk (modified)
    private final Set<String> savedChunks = new HashSet<>();

    // Priority chunks (block breaking)
    private final List<String> priorityChunks = new ArrayList<>();

    // Statistics
    private int frameCount = 0;
    private int asyncBuildsCompleted = 0;
    private int syncBuildsCompleted = 0;

    public ChunkRenderer(WorldManager world, Camera cam, ShadowManager shadow) throws IOException {
        this.worldManager = world;
        this.camera = cam;
        this.shadowManager = shadow;

        this.shader = new Shader("shader/cube/cube.vert", "shader/cube/cube.frag");
        this.textureAtlas = new TextureAtlas();
        // TEMPORARY: Build initial meshes sync
        System.out.println("=== BUILDING INITIAL MESHES ===");
        List<Chunk> initialChunks = worldManager.getLoadedChunks();
        for (Chunk chunk : initialChunks) {
            if (chunk.hasVisibleBlocks()) {
                String key = getChunkKey(chunk.chunkX, chunk.chunkZ);
                ChunkMesh mesh = new ChunkMesh();
                mesh.buildSync(chunk, textureAtlas);
                chunkMeshes.put(key, mesh);
                System.out.println("Built mesh for " + key + ": " + mesh.getVertexCount() + " vertices");
            }
        }
        System.out.println("=== END ===");
    }

    public static int getRenderDistance() {
        return RENDER_DISTANCE;
    }

    public void setPriorityChunks(List<String> priorityChunks) {
        this.priorityChunks.clear();
        this.priorityChunks.addAll(priorityChunks);
    }

    private String getChunkKey(int x, int z) {
        return x + "_" + z;
    }

    private List<Chunk> getVisibleChunks() {
        List<Chunk> visible = new ArrayList<>();
        Vector3f playerPos = camera.getPosition();
        int playerChunkX = (int)Math.floor(playerPos.x / 16);
        int playerChunkZ = (int)Math.floor(playerPos.z / 16);

        // Circular render distance
        for (int dx = -RENDER_DISTANCE; dx <= RENDER_DISTANCE; dx++) {
            for (int dz = -RENDER_DISTANCE; dz <= RENDER_DISTANCE; dz++) {
                if (dx * dx + dz * dz > RENDER_DISTANCE * RENDER_DISTANCE) {
                    continue;
                }

                int chunkX = playerChunkX + dx;
                int chunkZ = playerChunkZ + dz;

                Chunk chunk = worldManager.getChunkAt(chunkX, chunkZ);
                if (chunk != null && chunk.hasVisibleBlocks()) {
                    visible.add(chunk);
                }
            }
        }

        // Sort by distance (closest first)
        visible.sort((c1, c2) -> {
            float dist1 = (float)Math.pow(c1.chunkX - playerChunkX, 2) +
                    (float)Math.pow(c1.chunkZ - playerChunkZ, 2);
            float dist2 = (float)Math.pow(c2.chunkX - playerChunkX, 2) +
                    (float)Math.pow(c2.chunkZ - playerChunkZ, 2);
            return Float.compare(dist1, dist2);
        });

        return visible;
    }

    public void render() {
        shader.bind();

        // Setup textures and uniforms
        setupRenderState();

        // Get visible chunks
        List<Chunk> visibleChunks = getVisibleChunks();

        // Process async builds that have completed
        processCompletedAsyncBuilds();

        // Start new async builds for needed chunks
        startNewAsyncBuilds(visibleChunks);

        // Process priority chunks synchronously
        processPriorityChunks();

        // Render all available meshes
        renderChunks(visibleChunks);

        // Cleanup
        cleanupRenderState();

        // Log statistics occasionally
        if (frameCount % 120 == 0) { // Every 2 seconds at 60 FPS
            logStatistics(visibleChunks.size());
        }

        frameCount++;
    }

    private void setupRenderState() {
        // Bind textures
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        textureAtlas.getTexture().bind();
        shader.setUniform1i("u_Texture", 0);

        // Bind shadow map
        if (shadowManager != null) {
            GL13.glActiveTexture(GL13.GL_TEXTURE1);
            shadowManager.bindShadowMap();
            shader.setUniform1i("u_ShadowMap", 1);
            shader.setUniformMat4f("u_LightSpaceMatrix", shadowManager.getLightSpaceMatrix());
        }

        // Set lighting uniforms
        setupLightingUniforms();
    }

    private void setupLightingUniforms() {
        // Slower day/night cycle (4 minutes)
        float dayCycle = 24000.0f; // 4 minutes in milliseconds
        float time = (System.currentTimeMillis() % (int)dayCycle) / dayCycle;
        float sunAngle = time * 2.0f * (float)Math.PI;

        // Sun direction with smooth animation
        Vector3f sunDir = new Vector3f(
                (float)Math.sin(sunAngle) * 0.8f,
                Math.max(0.0f, (float)Math.cos(sunAngle)) * 0.7f + 0.3f, // Never goes below 0.3
                -0.3f
        ).normalize();

        // Dynamic colors based on sun height
        float sunHeight = sunDir.y;
        Vector3f sunColor;
        Vector3f ambient;

        if (sunHeight > 0.6f) {
            // Midday
            sunColor = new Vector3f(1.0f, 0.95f, 0.85f);
            ambient = new Vector3f(0.25f, 0.28f, 0.32f);
        } else if (sunHeight > 0.3f) {
            // Morning/Evening
            float t = (sunHeight - 0.3f) / 0.3f;
            sunColor = new Vector3f(
                    1.0f,
                    0.7f + 0.25f * t,
                    0.4f + 0.45f * t
            );
            ambient = new Vector3f(
                    0.20f + 0.05f * t,
                    0.22f + 0.06f * t,
                    0.25f + 0.07f * t
            );
        } else {
            // Dawn/Dusk/Night
            float t = sunHeight / 0.3f;
            sunColor = new Vector3f(
                    0.3f + 0.7f * t,
                    0.3f + 0.4f * t,
                    0.5f + 0.35f * t
            );
            ambient = new Vector3f(
                    0.10f + 0.10f * t,
                    0.12f + 0.10f * t,
                    0.15f + 0.10f * t
            );
        }

        // Set all uniforms
        shader.setUniform3f("u_LightDir", sunDir);
        shader.setUniform3f("u_LightColor", sunColor);
        shader.setUniform3f("u_Ambient", ambient);
        shader.setUniform3f("u_ViewPos", camera.getPosition());
        shader.setUniform1f("u_Time", System.currentTimeMillis() / 1000.0f);

        // Shader mode settings
        shader.setUniform1i("u_UseFog", 1);      // Disable fog for performance
        shader.setUniform1i("u_UsePBR", 1);      // Simple lighting (faster)
        shader.setUniform1i("u_UseShadows", 1);  // Enable shadows

        // Material properties (can be per-block later)
        shader.setUniform1f("u_Roughness", 0.8f);
        shader.setUniform1f("u_Metallic", 0.0f);
    }

    private void processCompletedAsyncBuilds() {
        asyncBuildsCompleted = 0;
        Iterator<Map.Entry<String, Future<List<ChunkMeshData>>>> iterator =
                pendingBuilds.entrySet().iterator();

        while (iterator.hasNext() && asyncBuildsCompleted < MAX_ASYNC_BUILDS_PER_FRAME) {
            Map.Entry<String, Future<List<ChunkMeshData>>> entry = iterator.next();

            if (entry.getValue().isDone()) {
                try {
                    List<ChunkMeshData> meshData = entry.getValue().get();
                    if (meshData != null && !meshData.isEmpty()) {
                        // Create mesh on main thread (OpenGL context available)
                        ChunkMesh mesh = new ChunkMesh();
                        mesh.buildFromData(meshData);
                        chunkMeshes.put(entry.getKey(), mesh);
                        asyncBuildsCompleted++;

                        System.out.println("✅ Async mesh built for chunk " + entry.getKey());
                    }
                } catch (Exception e) {
                    System.err.println("Failed to get async mesh data for " + entry.getKey() + ": " + e.getMessage());
                }
                iterator.remove();
            }
        }
    }

    private void startNewAsyncBuilds(List<Chunk> visibleChunks) {
        int started = 0;

        for (Chunk chunk : visibleChunks) {
            if (started >= MAX_ASYNC_BUILDS_PER_FRAME) break;

            String key = getChunkKey(chunk.chunkX, chunk.chunkZ);

            // Skip if already have mesh, building, or is priority
            if (chunkMeshes.containsKey(key) ||
                    pendingBuilds.containsKey(key) ||
                    priorityChunks.contains(key)) {
                continue;
            }

            // Skip if chunk is modified (will be handled by priority or sync)
            if (chunk.isModified()) {
                // Modified chunks should be built synchronously to ensure correct state
                continue;
            }

            // Start async build
            Future<List<ChunkMeshData>> future = ChunkMeshBuilder.buildAsync(
                    chunk.chunkX, chunk.chunkZ, chunk, textureAtlas
            );
            pendingBuilds.put(key, future);
            started++;

        }
    }

    private void processPriorityChunks() {
        syncBuildsCompleted = 0;

        for (String chunkKey : priorityChunks) {
            if (syncBuildsCompleted >= MAX_SYNC_BUILDS_PER_FRAME) break;

            String[] parts = chunkKey.split("_");
            int chunkX = Integer.parseInt(parts[0]);
            int chunkZ = Integer.parseInt(parts[1]);

            Chunk chunk = worldManager.getChunkAt(chunkX, chunkZ);
            if (chunk != null) {
                // Cancel any pending async build
                Future<List<ChunkMeshData>> pending = pendingBuilds.remove(chunkKey);
                if (pending != null && !pending.isDone()) {
                    pending.cancel(true);
                    System.out.println("❌ Cancelled async build for priority chunk " + chunkKey);
                }

                // Build synchronously
                ChunkMesh oldMesh = chunkMeshes.get(chunkKey);
                if (oldMesh != null) {
                    oldMesh.cleanup();
                }

                System.out.println("🔨 Building sync mesh for modified chunk " + chunkKey +
                        " (modified: " + chunk.isModified() + ")");

                ChunkMesh newMesh = new ChunkMesh();
                newMesh.buildSync(chunk, textureAtlas);
                chunkMeshes.put(chunkKey, newMesh);
                chunk.markClean(); // Only mark clean after successful build

                syncBuildsCompleted++;
            }
        }

        priorityChunks.clear();
    }

    private void renderChunks(List<Chunk> visibleChunks) {
        Matrix4f projection = camera.getProjection();
        Matrix4f view = camera.getView();

        // Collect all textures from available meshes
        Set<String> allTextures = new HashSet<>();
        for (Chunk chunk : visibleChunks) {
            String key = getChunkKey(chunk.chunkX, chunk.chunkZ);
            ChunkMesh mesh = chunkMeshes.get(key);
            if (mesh != null && mesh.isValid()) {
                allTextures.addAll(mesh.getTextureTypes());
            }
        }

        // Render by texture type
        for (String textureName : allTextures) {
            for (Chunk chunk : visibleChunks) {
                String key = getChunkKey(chunk.chunkX, chunk.chunkZ);
                ChunkMesh mesh = chunkMeshes.get(key);

                if (mesh == null || !mesh.isValid() || !mesh.hasTexture(textureName)) {
                    continue;
                }

                // Set transformation
                Matrix4f model = new Matrix4f().translate(chunk.chunkX * 16, 0, chunk.chunkZ * 16);
                shader.setUniformMat4f("u_Model", model);

                Matrix4f mvp = new Matrix4f(projection).mul(view).mul(model);
                shader.setUniformMat4f("u_MVP", mvp);

                // Render
                mesh.render(textureName);
            }
        }
    }

    private void cleanupRenderState() {
        if (shadowManager != null) {
            GL13.glActiveTexture(GL13.GL_TEXTURE1);
            shadowManager.unbindShadowMap();
        }

        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        textureAtlas.getTexture().unbind();

        shader.unbind();
    }

    private void logStatistics(int visibleChunks) {
        System.out.printf(
                "ChunkRenderer Stats: %d visible, %d cached, %d pending, %d async/sync built%n",
                visibleChunks,
                chunkMeshes.size(),
                pendingBuilds.size(),
                asyncBuildsCompleted + syncBuildsCompleted
        );
    }

    public TextureAtlas getTextureAtlas() {
        return textureAtlas;
    }

    // === NEW: Force rebuild a specific chunk ===
    public void forceRebuildChunk(int chunkX, int chunkZ) {
        String key = getChunkKey(chunkX, chunkZ);

        // Cancel any pending async build
        Future<List<ChunkMeshData>> pending = pendingBuilds.remove(key);
        if (pending != null && !pending.isDone()) {
            pending.cancel(true);
        }

        // Remove old mesh
        ChunkMesh oldMesh = chunkMeshes.remove(key);
        if (oldMesh != null) {
            oldMesh.cleanup();
        }

        // Add to priority for sync rebuild
        if (!priorityChunks.contains(key)) {
            priorityChunks.add(key);
            System.out.println("🔧 Force rebuild queued for chunk " + key);
        }
    }

    // === NEW: Check if chunk mesh exists ===
    public boolean hasMeshForChunk(int chunkX, int chunkZ) {
        return chunkMeshes.containsKey(getChunkKey(chunkX, chunkZ));
    }

    public void cleanup() {
        // Clean up meshes
        for (ChunkMesh mesh : chunkMeshes.values()) {
            mesh.cleanup();
        }
        chunkMeshes.clear();

        // Cancel pending builds
        ChunkMeshBuilder.cancelAll();
        pendingBuilds.clear();

        // Clear lists
        priorityChunks.clear();
        savedChunks.clear();

        // Cleanup resources
        shader.cleanup();
        textureAtlas.getTexture().cleanup();
    }
}