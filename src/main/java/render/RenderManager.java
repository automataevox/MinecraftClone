package render;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL13;
import shader.Shader;
import mesh.CubeMesh;
import texture.Texture;
import camera.Camera;
import face.Face;
import world.Block;
import world.WorldManager;

import java.io.IOException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public class RenderManager {

    private final CubeMesh cubeMesh;
    private final Shader shader;
    private final Camera camera;
    private final WorldManager worldManager;
    private final ShadowManager shadowManager;

    // Cache textures to avoid reloading every frame
    private final Map<String, Texture> textureCache = new HashMap<>();

    public RenderManager(WorldManager world, Camera cam, ShadowManager shadow) throws IOException {
        this.worldManager = world;
        this.camera = cam;
        this.shadowManager = shadow;

        this.cubeMesh = new CubeMesh();
        this.shader = new Shader(
                "shader/cube/cube.vert",
                "shader/cube/cube.frag"
        );
    }

    private Texture getTexture(String path) {
        return textureCache.computeIfAbsent(path, p -> {
            try {
                return new Texture(p);
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException("Failed to load texture: " + p);
            }
        });
    }

    public void render() {
        shader.bind();
        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        shadowManager.getDepthTexture().bind(); // shadow map
        shader.setUniform3f("u_LightDir", new Vector3f(-0.5f, -1f, -0.3f).normalize());
        shader.setUniform3f("u_LightColor", new Vector3f(1f,1f,1f));
        shader.setUniform3f("u_Ambient", new Vector3f(0.3f,0.3f,0.3f));
        shader.setUniform1i("shadowMap", 1);
        shader.setUniformMat4f("u_LightSpaceMatrix", shadowManager.getLightSpaceMatrix());

        // Render all cubes
        for (Vector3f cubePos : worldManager.getRenderList()) {

            // Get block and its texture
            Block block = worldManager.getBlock(cubePos);
            if (block == null) continue;

            Texture texture = getTexture(block.getTexture());

            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            texture.bind();
            shader.setUniform1i("u_Texture", 0);

            EnumSet<Face> visibleFaces = getVisibleFaces(cubePos);
            for (Face face : visibleFaces) {
                renderFace(cubePos, face);
            }

            texture.unbind();
        }

        shader.unbind();
    }

    private void renderFace(Vector3f cubePos, Face face) {
        shader.setUniform1f("u_AO", getAmbientOcclusion(face));

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
        cubeMesh.cleanup();
        shader.cleanup();
        // Cleanup all cached textures
        for (Texture tex : textureCache.values()) {
            tex.cleanup();
        }
    }
}
