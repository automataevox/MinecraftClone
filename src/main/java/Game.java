import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import shader.Shader;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Game {

    private long window;
    private Camera camera;
    private CubeMesh cube;
    private Shader shader;

    public void run() {
        init();
        loop();
        cleanup();
    }



    private void init() {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) throw new IllegalStateException("GLFW failed");

        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 6);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);

        window = glfwCreateWindow(1280, 720, "Minecraft Clone", 0, 0);
        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);

        GL.createCapabilities();

        glEnable(GL_DEPTH_TEST);
        glClearColor(0.53f, 0.81f, 0.92f, 1.0f);

        camera = new Camera(window);
        cube = new CubeMesh();
        try {
            shader = new Shader("D:/minecraft_dev/untitled/src/main/java/shader/cube.vert", "D:/minecraft_dev/untitled/src/main/java/shader/cube.frag");
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to load shader");
        }    }

    private void loop() {
        while (!glfwWindowShouldClose(window)) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            camera.update();

            shader.bind();
            for (int x = -5; x <= 5; x++) {
                for (int z = -5; z <= 5; z++) {
                    Matrix4f model = new Matrix4f().translate(x, 0, z);

                    shader.setUniformMat4f("u_Model", model);
                    shader.setUniform3f("u_LightDir", new Vector3f(-0.5f, -1f, -0.3f).normalize());
                    shader.setUniform3f("u_LightColor", new Vector3f(1f, 1f, 1f));
                    shader.setUniform3f("u_ObjectColor", new Vector3f(1f, 0.5f, 0.31f)); // orange cube

                    Matrix4f mvp = new Matrix4f();
                    camera.getProjection().mul(camera.getView(), mvp);
                    mvp.mul(model);

                    shader.setUniformMat4f("u_MVP", mvp);
                    cube.render();
                }
            }
            shader.unbind();

            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }


    private void cleanup() {
        cube.cleanup();
        shader.cleanup();
        glfwTerminate();
    }
}
