package render;

import org.joml.Matrix4f;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL13;
import shader.Shader;
import mesh.CubeMesh;
import texture.Texture;
import camera.Camera;
import face.Face;
import texture.TextureAtlas;
import world.Block;
import world.Chunk;
import world.WorldManager;

import java.io.IOException;
import java.util.*;

public class RenderManager {
    private final CubeMesh cubeMesh;
    private final Shader shader;
    private final Camera camera;
    private final WorldManager worldManager;
    private final ShadowManager shadowManager;
    private final ChunkRenderer chunkRenderer;

    // Cache textures to avoid reloading every frame
    private final Map<String, Texture> textureCache = new HashMap<>();

    public RenderManager(WorldManager world, Camera cam, ShadowManager shadow) throws IOException {
        this.worldManager = world;
        this.camera = cam;
        this.shadowManager = shadow;

        this.cubeMesh = new CubeMesh();
        this.shader = new Shader("shader/cube/cube.vert", "shader/cube/cube.frag");
        this.chunkRenderer = new ChunkRenderer(world, cam, shadow);
    }

    // ADD THIS METHOD
    public TextureAtlas getTextureAtlas() {
        return chunkRenderer.getTextureAtlas();
    }

    // ADD THIS TEST METHOD TOO
    public void testMeshBuilding() {
        System.out.println("=== Testing Mesh Building ===");

        Vector3f spawn = worldManager.getSpawnPoint();
        int chunkX = (int)Math.floor(spawn.x / 16);
        int chunkZ = (int)Math.floor(spawn.z / 16);

        Chunk chunk = worldManager.getChunkAt(chunkX, chunkZ);
        if (chunk == null) {
            System.out.println("No chunk found at spawn!");
            return;
        }

        System.out.println("Chunk has " + chunk.getVisibleBlockCount() + " visible blocks");

        if (chunk.getVisibleBlockCount() > 0) {
            try {
                mesh.ChunkMesh testMesh = new mesh.ChunkMesh();
                testMesh.buildSync(chunk, getTextureAtlas());
                System.out.println("Test mesh built: " + testMesh.isValid() +
                        " (" + testMesh.getVertexCount() + " vertices)");
                testMesh.cleanup();
            } catch (Exception e) {
                System.err.println("Failed to build mesh: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("Chunk has no visible blocks to render!");
        }
    }

    public void setPriorityChunks(List<String> priorityChunks) {
        // Pass through to chunk renderer
        chunkRenderer.setPriorityChunks(priorityChunks);
    }

    public void render() {
        // Use chunk rendering (optimized)
        chunkRenderer.render();

        // Uncomment for debugging per-block rendering (slow)
        // renderPerBlock();
    }

    private void renderPerBlock() {
        // Old per-block rendering for debugging only
        shader.bind();

        // Simple lighting setup for debug rendering
        float time = (System.currentTimeMillis() % 120000) / 120000.0f; // 2 min day cycle
        float sunAngle = time * 2.0f * (float)Math.PI;
        Vector3f sunDir = new Vector3f((float)Math.sin(sunAngle), (float)Math.cos(sunAngle), -0.3f).normalize();
        Vector3f sunColor = new Vector3f(1.0f, 0.95f, 0.85f);

        // --- Global illumination ---
        Vector3f globalIllum = new Vector3f(0.18f, 0.20f, 0.22f);

        shader.setUniform3f("u_LightDir", sunDir);
        shader.setUniform3f("u_LightColor", sunColor);
        shader.setUniform3f("u_Ambient", globalIllum);
        shader.setUniform1i("shadowMap", 1);
        shader.setUniformMat4f("u_LightSpaceMatrix", shadowManager.getLightSpaceMatrix());

        // Render each block
        for (Vector3f pos : worldManager.getRenderList()) {
            EnumSet<Face> visibleFaces = getVisibleFaces(pos);
            for (Face face : visibleFaces) {
                renderFace(pos, face);
            }
        }

        shader.unbind();
    }

    private void renderFace(Vector3f cubePos, Face face) {
        shader.setUniform1f("u_AO", getAmbientOcclusion(face));

        // Check if face is sunlit (no block above in sun direction)
        float sunlit = 1.0f;
        if (face == Face.TOP) {
            int aboveX = (int) cubePos.x;
            int aboveY = (int) cubePos.y + 1;
            int aboveZ = (int) cubePos.z;
            if (worldManager.hasCube(aboveX, aboveY, aboveZ)) {
                sunlit = 0.0f;
            }
        }
        shader.setUniform1f("u_Sunlit", sunlit);

        Matrix4f model = new Matrix4f().translate(cubePos.x, cubePos.y, cubePos.z);
        shader.setUniformMat4f("u_Model", model);

        Matrix4f mvp = new Matrix4f();
        camera.getProjection().mul(camera.getView(), mvp);
        mvp.mul(model);
        shader.setUniformMat4f("u_MVP", mvp);

        cubeMesh.renderFace(face);
    }

    private float getAmbientOcclusion(Face face) {
        return switch (face) {
            case BOTTOM -> 0.5f;
            case LEFT, RIGHT -> 0.7f;
            default -> 1.0f;
        };
    }

    private EnumSet<Face> getVisibleFaces(Vector3f pos) {
        EnumSet<Face> faces = EnumSet.allOf(Face.class);

        for (Face f : Face.values()) {
            int nx = (int) pos.x + f.dx;
            int ny = (int) pos.y + f.dy;
            int nz = (int) pos.z + f.dz;

            if (worldManager.hasCube(nx, ny, nz)) {
                faces.remove(f);
            }
        }

        return faces;
    }



    public void cleanup() {
        chunkRenderer.cleanup();
        shader.cleanup();
        for (Texture tex : textureCache.values()) {
            tex.cleanup();
        }
    }
}