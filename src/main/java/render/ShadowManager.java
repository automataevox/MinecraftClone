package render;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import mesh.CubeMesh;
import world.WorldManager;
import shader.Shader;
import texture.Texture;

import java.io.IOException;

public class ShadowManager {

    private final int SHADOW_WIDTH = 1024;
    private final int SHADOW_HEIGHT = 1024;

    private int depthMapFBO;
    private Texture depthTexture;

    private Shader depthShader;
    private CubeMesh cubeMesh;
    private WorldManager world;

    private Matrix4f lightSpaceMatrix;

    public ShadowManager(WorldManager world) throws IOException {
        this.world = world;
        this.cubeMesh = new CubeMesh();
        this.depthShader = new Shader("shader/depth/depth.vert","shader/depth/depth.frag");
        initDepthMap();
        computeLightSpaceMatrix();
    }

    private void initDepthMap() {
        depthMapFBO = GL30.glGenFramebuffers();

        depthTexture = new Texture(SHADOW_WIDTH, SHADOW_HEIGHT); // depth-only texture

        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, depthMapFBO);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, depthTexture.getid(), 0);
        GL30.glDrawBuffer(GL11.GL_NONE);
        GL30.glReadBuffer(GL11.GL_NONE);
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
    }

    private void computeLightSpaceMatrix() {
        // Simple directional light above the scene
        Vector3f lightPos = new Vector3f(-10, 20, -10);
        Vector3f lightTarget = new Vector3f(0, 0, 0);
        Vector3f up = new Vector3f(0, 1, 0);

        Matrix4f lightProjection = new Matrix4f().ortho(-20, 20, -20, 20, 1f, 50f);
        Matrix4f lightView = new Matrix4f().lookAt(lightPos, lightTarget, up);

        lightSpaceMatrix = new Matrix4f();
        lightProjection.mul(lightView, lightSpaceMatrix);
    }

    public Matrix4f getLightSpaceMatrix() {
        return lightSpaceMatrix;
    }

    public Texture getDepthTexture() {
        return depthTexture;
    }

    public void renderDepthMap() {
        GL30.glViewport(0, 0, SHADOW_WIDTH, SHADOW_HEIGHT);
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, depthMapFBO);
        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);

        depthShader.bind();

        for (Vector3f cubePos : world.getRenderList()) {
            Matrix4f model = new Matrix4f().translate(cubePos.x, cubePos.y, cubePos.z);
            Matrix4f mvp = new Matrix4f();
            lightSpaceMatrix.mul(model, mvp);

            depthShader.setUniformMat4f("u_MVP", mvp);
            //cubeMesh.render(); // render all faces for depth
        }

        depthShader.unbind();
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
    }

    public void cleanup() {
        depthShader.cleanup();
        cubeMesh.cleanup();
        depthTexture.cleanup();
        GL30.glDeleteFramebuffers(depthMapFBO);
    }
}
