package render;

import camera.Camera;
import mesh.WireCubeMesh;
import shader.Shader;
import org.joml.Vector3f;
import org.joml.Matrix4f;

import java.io.IOException;

public class HighlightManager {

    private final WireCubeMesh highlightMesh;
    private final Shader highlightShader;
    private final Camera camera;
    private Vector3f hoveredCube = null;

    public HighlightManager(Camera camera) throws IOException {
        this.camera = camera;
        this.highlightMesh = new WireCubeMesh();
        this.highlightShader = new Shader(
                "shader/highlight/highlight.vert",
                "shader/highlight/highlight.frag"
        );
    }

    public void setHoveredCube(Vector3f hoveredCube) {
        this.hoveredCube = hoveredCube;
    }

    public void render() {
        if (hoveredCube == null) return;

        highlightShader.bind();
        Matrix4f model = new Matrix4f().translate(hoveredCube.x, hoveredCube.y, hoveredCube.z);
        Matrix4f mvp = new Matrix4f();
        camera.getProjection().mul(camera.getView(), mvp);
        mvp.mul(model);

        highlightShader.setUniformMat4f("u_MVP", mvp);
        highlightShader.setUniform3f("u_Color", new Vector3f(1f, 1f, 0f));
        highlightMesh.render();
        highlightShader.unbind();
    }

    public void cleanup() {
        highlightMesh.cleanup();
        highlightShader.cleanup();
    }
}
