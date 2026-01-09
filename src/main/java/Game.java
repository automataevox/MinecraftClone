import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import shader.Shader;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

import java.io.IOException;
import java.util.*;

public class Game {

    // --- Fields ---
    private Set<Vector3f> cubes = new HashSet<>();
    private List<Vector3f> renderList = new ArrayList<>(); // faster iteration for rendering
    private Map<Vector3f, Matrix4f> mvpCache = new HashMap<>();
    private Vector3f hoveredCube = null;

    private long window;
    private Camera camera;
    private CubeMesh cube;
    private Shader shader;
    private Shader highlightShader;
    private WireCubeMesh highlightCube;
    private Texture cubeTexture;

    private Vector3f lastCameraPos = new Vector3f();
    private float lastYaw, lastPitch;
    private boolean cameraMoved = true; // force initial mvp update

    // --- Run Game ---
    public void run() {
        init();
        loop();
        cleanup();
    }

    // --- Initialization ---
    private void init() {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) throw new IllegalStateException("GLFW failed");

        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 1);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);

        window = glfwCreateWindow(1280, 720, "Minecraft Clone", 0, 0);
        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);

        GL.createCapabilities();

        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE); // backface culling
        glCullFace(GL_BACK);
        glClearColor(0.53f, 0.81f, 0.92f, 1.0f);

        cubeTexture = new Texture("src/main/java/textures/grass.jpg");
        camera = new Camera(window);
        cube = new CubeMesh();
        highlightCube = new WireCubeMesh();

        // Load shaders
        try {
            shader = new Shader("src/main/java/shader/cube.vert", "src/main/java/shader/cube.frag");
            highlightShader = new Shader("src/main/java/shader/highlight.vert", "src/main/java/shader/highlight.frag");
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to load shaders");
        }

        // Initialize cubes
        for (int x = -5; x <= 5; x++) {
            for (int y = 0; y <= 2; y++) {
                for (int z = -5; z <= 5; z++) {
                    Vector3f c = new Vector3f(x, y, z);
                    cubes.add(c);
                    renderList.add(c);
                }
            }
        }
    }

    // --- Main Loop ---
    private void loop() {
        while (!glfwWindowShouldClose(window)) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            camera.update();

            // Detect if camera moved
            cameraMoved = !camera.getPosition().equals(lastCameraPos) ||
                    camera.getYaw() != lastYaw ||
                    camera.getPitch() != lastPitch;

            if (cameraMoved) {
                updateMVPCache();
                lastCameraPos.set(camera.getPosition());
                lastYaw = camera.getYaw();
                lastPitch = camera.getPitch();
            }

            // Raycast only when mouse moves or camera moves
            hoveredCube = raycastBlock();

            // BREAK BLOCK with left mouse button
            if (hoveredCube != null && glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS) {
                cubes.remove(hoveredCube);
                renderList.remove(hoveredCube);
                mvpCache.remove(hoveredCube);
                hoveredCube = null;
            }

            // Draw all cubes
            shader.bind();
            cubeTexture.bind(); // <-- bind texture

            for (Vector3f c : renderList) {
                shader.setUniformMat4f("u_MVP", mvpCache.get(c));
                shader.setUniform3f("u_LightDir", new Vector3f(-0.5f, -1f, -0.3f).normalize());
                shader.setUniform3f("u_LightColor", new Vector3f(1f, 1f, 1f));
                shader.setUniform3f("u_ObjectColor", new Vector3f(1f, 0.5f, 0.31f));
                cube.render();
            }
            cubeTexture.unbind(); // <-- bind texture
            shader.unbind();

            // Draw highlight on top of hovered cube
            if (hoveredCube != null) {
                highlightShader.bind();
                Matrix4f model = new Matrix4f().translate(hoveredCube.x, hoveredCube.y, hoveredCube.z);
                Matrix4f mvp = new Matrix4f();
                camera.getProjection().mul(camera.getView(), mvp);
                mvp.mul(model);

                highlightShader.setUniformMat4f("u_MVP", mvp);
                highlightShader.setUniform3f("u_Color", new Vector3f(1f, 1f, 0f)); // yellow
                highlightCube.render();
                highlightShader.unbind();
            }

            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }

    // --- Update MVP Cache ---
    private void updateMVPCache() {
        Matrix4f view = camera.getView();
        Matrix4f proj = camera.getProjection();
        mvpCache.clear();
        for (Vector3f c : renderList) {
            Matrix4f model = new Matrix4f().translate(c.x, c.y, c.z);
            Matrix4f mvp = new Matrix4f();
            proj.mul(view, mvp);
            mvp.mul(model);
            mvpCache.put(c, mvp);
        }
    }

    // --- Cleanup ---
    private void cleanup() {
        cube.cleanup();
        highlightCube.cleanup();
        shader.cleanup();
        highlightShader.cleanup();
        glfwTerminate();
    }

    // --- Raycast ---
    private Vector3f raycastBlock() {
        Vector3f origin = camera.getPosition();
        Vector3f dir = camera.getFront();

        float maxDistance = 10f;
        float step = 0.2f; // fewer steps for performance

        for (float t = 0; t < maxDistance; t += step) {
            Vector3f point = new Vector3f(origin).fma(t, dir);
            int x = Math.round(point.x);
            int y = Math.round(point.y);
            int z = Math.round(point.z);

            if (isCubeAt(x, y, z)) return new Vector3f(x, y, z);
        }
        return null;
    }

    // --- Check if cube exists ---
    private boolean isCubeAt(int x, int y, int z) {
        return cubes.contains(new Vector3f(x, y, z));
    }
}
