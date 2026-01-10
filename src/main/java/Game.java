import camera.Camera;
import org.lwjgl.opengl.GL;
import physics.RaycastManager;
import player.Player;
import render.HighlightManager;
import render.RenderManager;
import render.ShadowManager;
import world.WorldManager;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFWErrorCallback;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import java.io.IOException;

public class Game {
    private Window window;
    private Camera camera;
    private Player player;
    private WorldManager worldManager;
    private RenderManager renderManager;
    private HighlightManager highlightManager;
    private RaycastManager raycastManager;
    private ShadowManager shadowManager;

    public void run() {
        init();
        loop();
        cleanup();
    }

    // --- Initialization ---
    private void init() {
        GLFWErrorCallback.createPrint(System.err).set();
        window = new Window(1280, 720, "Minecraft Clone");
        window.init();
        initOpenGL();
        initManagers();
    }

    private void initOpenGL() {
        GL.createCapabilities();
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        glClearColor(0.53f, 0.81f, 0.92f, 1.0f);
    }

    private void initManagers() {
        camera = new Camera(window.getHandle());
        worldManager = new WorldManager();

        try {
            shadowManager = new ShadowManager(worldManager);
            renderManager = new RenderManager(worldManager, camera, shadowManager);
            highlightManager = new HighlightManager(camera);
            raycastManager = new RaycastManager(worldManager);
        } catch (IOException e) {
            e.printStackTrace(); throw new RuntimeException("Failed to initialize shaders or textures");
        }

        player = new Player(camera, worldManager);
        Vector3f spawn = worldManager.getSpawnPoint();
        camera.setPosition(spawn);
        worldManager.generateChunksAround(spawn);
    }

    private void loop() {
        float lastTime = (float) glfwGetTime();
        while (!window.shouldClose()) {
            float currentTime = (float) glfwGetTime();
            float deltaTime = currentTime - lastTime;
            lastTime = currentTime;
            window.clear();
            worldManager.generateChunksAround(player.getCamera().getPosition());

            // --- Player Movement ---
            boolean forward = glfwGetKey(window.getHandle(), GLFW_KEY_W) == GLFW_PRESS;
            boolean backward = glfwGetKey(window.getHandle(), GLFW_KEY_S) == GLFW_PRESS;
            boolean left = glfwGetKey(window.getHandle(), GLFW_KEY_A) == GLFW_PRESS;
            boolean right = glfwGetKey(window.getHandle(), GLFW_KEY_D) == GLFW_PRESS;
            boolean jump = glfwGetKey(window.getHandle(), GLFW_KEY_SPACE) == GLFW_PRESS;

            camera.updateRotation();
            player.update(deltaTime, forward, backward, left, right, jump);

            // --- Raycasting / Highlighting ---
            Vector3f hoveredCube = raycastManager.raycastBlock(player.getCamera().getPosition(), player.getCamera().getFront());
            highlightManager.setHoveredCube(hoveredCube);

            // --- Handle block breaking ---
            handleBlockBreak(hoveredCube);

            // --- Render World ---
            renderManager.render();
            highlightManager.render();
            window.swapBuffers();
            window.pollEvents();
        }
    }

    private void handleBlockBreak(Vector3f hoveredCube) {
        if (hoveredCube != null && glfwGetMouseButton(window.getHandle(), GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS) {
            worldManager.breakBlock(hoveredCube);
        }
    }

    // --- Cleanup ---
    private void cleanup() {
        renderManager.cleanup();
        highlightManager.cleanup();
        raycastManager.cleanup();
        worldManager.cleanup();
        window.cleanup();
    }
}