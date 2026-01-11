import camera.Camera;
import mesh.ChunkMesh;
import org.lwjgl.opengl.GL;
import physics.RaycastManager;
import player.Player;
import render.*;
import texture.TextureAtlasGenerator;
import world.Chunk;
import world.WorldManager;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFWErrorCallback;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Game {
    private Window window;
    private Camera camera;
    private Player player;
    private WorldManager worldManager;
    private RenderManager renderManager;
    private HighlightManager highlightManager;
    private RaycastManager raycastManager;
    private ShadowManager shadowManager;

    // Add these fields
    private boolean saveOnExit = true;
    private float saveTimer = 0;
    private final float SAVE_INTERVAL = 30.0f; // Auto-save every 30 seconds

    // FPS optimization variables
    private int frameCount = 0;
    private float fpsTimer = 0;
    private float chunkUpdateTimer = 0;
    private float lastFPS = 0;

    // Shadow update optimization
    private int shadowUpdateCounter = 0;
    public boolean shadowsEnabled = true;

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
        setupCallbacks();
    }

    private void initOpenGL() {
        GL.createCapabilities();
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        glClearColor(0.53f, 0.81f, 0.92f, 1.0f);
    }

    private void initManagers() {
        try {
            ensureTextureAtlasExists();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        camera = new Camera(window.getHandle());
        worldManager = new WorldManager();

        try {
            shadowManager = new ShadowManager(worldManager);
            renderManager = new RenderManager(worldManager, camera, shadowManager);
            highlightManager = new HighlightManager(camera);
            raycastManager = new RaycastManager(worldManager);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize shaders or textures");
        }

        player = new Player(camera, worldManager);
        Vector3f spawn = worldManager.getSpawnPoint();
        camera.setPosition(spawn);

        // TEST: Force generate and check a chunk
        worldManager.generateChunksAround(spawn);

        // Immediately test mesh building
        testMeshBuilding();
    }

    private void testMeshBuilding() {
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
                // Force build a mesh synchronously
                mesh.ChunkMesh testMesh = new mesh.ChunkMesh();
                testMesh.buildSync(chunk, renderManager.getTextureAtlas()); // Add getter to RenderManager
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

    private void setupCallbacks() {
        // Set up window resize callback
        glfwSetFramebufferSizeCallback(window.getHandle(), (windowHandle, width, height) -> {
            glViewport(0, 0, width, height);
        });

        // Add key callbacks
        glfwSetKeyCallback(window.getHandle(), (windowHandle, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_F11 && action == GLFW_PRESS) {
                toggleFullscreen();
            }
            if (key == GLFW_KEY_F10 && action == GLFW_PRESS) {
                shadowsEnabled = !shadowsEnabled;
                System.out.println("Shadows " + (shadowsEnabled ? "enabled" : "disabled"));
            }
            if (key == GLFW_KEY_F5 && action == GLFW_PRESS) {
                System.out.println("Manual save triggered");
                worldManager.saveModifiedChunks();
            }
        });
    }

    private void toggleFullscreen() {
        // Simple fullscreen toggle implementation
        long monitor = glfwGetPrimaryMonitor();
        var vidmode = glfwGetVideoMode(monitor);

        if (glfwGetWindowMonitor(window.getHandle()) == 0) {
            // Go to fullscreen
            glfwSetWindowMonitor(
                    window.getHandle(),
                    monitor,
                    0, 0,
                    vidmode.width(),
                    vidmode.height(),
                    vidmode.refreshRate()
            );
        } else {
            // Go back to windowed
            glfwSetWindowMonitor(
                    window.getHandle(),
                    0,
                    100, 100,
                    1280, 720,
                    GLFW_DONT_CARE
            );
        }
    }

    private void ensureTextureAtlasExists() throws IOException {
        File atlasFile = new File("src/main/resources/textures/atlas.png");
        if (!atlasFile.exists()) {
            System.out.println("Texture atlas not found, generating...");
            TextureAtlasGenerator.generateAtlas(atlasFile.getPath());
        }
    }

    private InputState getInputState() {
        return new InputState(
                glfwGetKey(window.getHandle(), GLFW_KEY_W) == GLFW_PRESS,
                glfwGetKey(window.getHandle(), GLFW_KEY_S) == GLFW_PRESS,
                glfwGetKey(window.getHandle(), GLFW_KEY_A) == GLFW_PRESS,
                glfwGetKey(window.getHandle(), GLFW_KEY_D) == GLFW_PRESS,
                glfwGetKey(window.getHandle(), GLFW_KEY_SPACE) == GLFW_PRESS
        );
    }

    private void loop() {
        float lastTime = (float) glfwGetTime();

        // Track modified chunks
        List<String> modifiedChunks = new ArrayList<>();

        // Chunk generation rate limiting
        float chunkGenTimer = 0;
        final float CHUNK_GEN_INTERVAL = 0.1f;

        glClearColor(0.0f, 0.01f, 0.035f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        while (!window.shouldClose()) {
            float currentTime = (float) glfwGetTime();
            float deltaTime = currentTime - lastTime;
            lastTime = currentTime;

            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            // Update FPS counter
            updateFPSCounter(deltaTime);

            // Auto-save timer
            saveTimer += deltaTime;
            if (saveTimer >= SAVE_INTERVAL) {
                System.out.println("Auto-saving...");
                worldManager.saveModifiedChunks();
                saveTimer = 0;
            }

            // --- Get input ---
            InputState input = getInputState();

            // --- Player Movement ---
            camera.updateRotation();
            player.update(deltaTime, input.forward, input.backward, input.left, input.right, input.jump);

            // --- Raycasting / Highlighting ---
            Vector3f hoveredCube = raycastManager.raycastBlock(
                    player.getCamera().getPosition(),
                    player.getCamera().getFront()
            );
            highlightManager.setHoveredCube(hoveredCube);

            // --- Handle block breaking ---
            if (hoveredCube != null && glfwGetMouseButton(window.getHandle(), GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS) {
                int chunkX = (int)Math.floor(hoveredCube.x / 16);
                int chunkZ = (int)Math.floor(hoveredCube.z / 16);
                String chunkKey = chunkX + "_" + chunkZ;

                if (!modifiedChunks.contains(chunkKey)) {
                    modifiedChunks.add(chunkKey);
                }

                worldManager.breakBlock(hoveredCube);
            }

            // --- Handle block placing (right click) ---
            if (hoveredCube != null && glfwGetMouseButton(window.getHandle(), GLFW_MOUSE_BUTTON_RIGHT) == GLFW_PRESS) {
                // Calculate position to place block (adjacent to hovered cube)
                Vector3f placePos = calculatePlacePosition(hoveredCube, player.getCamera().getFront());
                if (placePos != null && !worldManager.hasCube((int)placePos.x, (int)placePos.y, (int)placePos.z)) {
                    int chunkX = (int)Math.floor(placePos.x / 16);
                    int chunkZ = (int)Math.floor(placePos.z / 16);
                    String chunkKey = chunkX + "_" + chunkZ;

                    if (!modifiedChunks.contains(chunkKey)) {
                        modifiedChunks.add(chunkKey);
                    }

                    // Place a stone block (you can make this selectable)
                    worldManager.placeBlock(placePos, new world.blocks.StoneBlock());
                }
            }

            // --- Update shadows ---
            shadowUpdateCounter++;
            if (shadowsEnabled && shadowUpdateCounter % 10 == 0) {
                shadowManager.renderDepthMap();
            }

            // --- Generate chunks ---
            chunkGenTimer += deltaTime;
            if (chunkGenTimer >= CHUNK_GEN_INTERVAL) {
                worldManager.generateChunksAround(player.getCamera().getPosition());
                chunkGenTimer = 0;
            }

            // --- Render World ---
            renderManager.setPriorityChunks(modifiedChunks);
            renderManager.render();
            modifiedChunks.clear();

            // Render highlights
            highlightManager.render();

            window.swapBuffers();
            window.pollEvents();

            // Limit FPS to reduce CPU usage
            limitFPS(deltaTime, 60);
        }
    }

    // Helper method for block placing
    private Vector3f calculatePlacePosition(Vector3f hoveredCube, Vector3f lookDirection) {
        // Find which face we're looking at
        Vector3f blockCenter = new Vector3f(
                hoveredCube.x + 0.5f,
                hoveredCube.y + 0.5f,
                hoveredCube.z + 0.5f
        );

        Vector3f toPlayer = new Vector3f(player.getCamera().getPosition()).sub(blockCenter);

        // Determine which face is most aligned with the look direction
        float maxDot = -1;
        Vector3f placeOffset = new Vector3f();

        Vector3f[] faceNormals = {
                new Vector3f(1, 0, 0),   // Right
                new Vector3f(-1, 0, 0),  // Left
                new Vector3f(0, 1, 0),   // Top
                new Vector3f(0, -1, 0),  // Bottom
                new Vector3f(0, 0, 1),   // Front
                new Vector3f(0, 0, -1)   // Back
        };

        for (Vector3f normal : faceNormals) {
            float dot = lookDirection.dot(normal);
            if (dot > maxDot) {
                maxDot = dot;
                placeOffset.set(normal);
            }
        }

        // Place block in adjacent position
        return new Vector3f(
                hoveredCube.x + placeOffset.x,
                hoveredCube.y + placeOffset.y,
                hoveredCube.z + placeOffset.z
        );
    }

    private void limitFPS(float deltaTime, int targetFPS) {
        float targetFrameTime = 1.0f / targetFPS;
        if (deltaTime < targetFrameTime) {
            try {
                Thread.sleep((long) ((targetFrameTime - deltaTime) * 1000));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // FPS counter method
    private void updateFPSCounter(float deltaTime) {
        frameCount++;
        fpsTimer += deltaTime;

        if (fpsTimer >= 1.0f) {
            lastFPS = frameCount;
            System.out.printf("FPS: %.1f | Chunks: %d | Blocks: %d | Shadows: %s%n",
                    lastFPS,
                    worldManager.getLoadedChunks().size(),
                    worldManager.getRenderList().size(),
                    shadowsEnabled ? "ON" : "OFF");
            frameCount = 0;
            fpsTimer = 0;
        }
    }

    // --- Cleanup ---
    private void cleanup() {
        if (saveOnExit) {
            System.out.println("Saving world before exit...");
            worldManager.saveModifiedChunks();
        }

        renderManager.cleanup();
        highlightManager.cleanup();
        raycastManager.cleanup();

        // IMPORTANT: Shutdown async builder
        ChunkMeshBuilder.shutdown();

        if (shadowManager != null) {
            shadowManager.cleanup();
        }

        worldManager.cleanup(); // This also saves
        window.cleanup();
    }

    // Helper class to hold input state
    private static class InputState {
        final boolean forward;
        final boolean backward;
        final boolean left;
        final boolean right;
        final boolean jump;

        InputState(boolean forward, boolean backward, boolean left, boolean right, boolean jump) {
            this.forward = forward;
            this.backward = backward;
            this.left = left;
            this.right = right;
            this.jump = jump;
        }
    }
}