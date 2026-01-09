import org.joml.Matrix4f;
import org.joml.Vector3f;

import static org.lwjgl.glfw.GLFW.*;

public class Camera {

    private final long window;
    private final Vector3f position = new Vector3f(0, 1.5f, 3);
    private float pitch = 0f;
    private float yaw = -90f;

    private double lastX = 640, lastY = 360;
    private boolean firstMouse = true;

    public Camera(long window) {
        this.window = window;
    }

    public void update() {
        float speed = 0.05f;

        if (glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS)
            position.add(getForward().mul(speed, new Vector3f()));
        if (glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS)
            position.sub(getForward().mul(speed, new Vector3f()));
        if (glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS)
            position.sub(getRight().mul(speed, new Vector3f()));
        if (glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS)
            position.add(getRight().mul(speed, new Vector3f()));

        double[] mx = new double[1];
        double[] my = new double[1];
        glfwGetCursorPos(window, mx, my);

        if (firstMouse) {
            lastX = mx[0];
            lastY = my[0];
            firstMouse = false;
        }

        float sensitivity = 0.1f;
        yaw += (mx[0] - lastX) * sensitivity;
        pitch -= (my[0] - lastY) * sensitivity;

        pitch = Math.max(-89f, Math.min(89f, pitch));

        lastX = mx[0];
        lastY = my[0];
    }

    public Matrix4f getView() {
        return new Matrix4f().lookAt(
                position,
                new Vector3f(position).add(getForward()),
                new Vector3f(0, 1, 0)
        );
    }

    public Matrix4f getProjection() {
        return new Matrix4f().perspective(
                (float) Math.toRadians(70),
                1280f / 720f,
                0.1f,
                1000f
        );
    }

    private Vector3f getForward() {
        return new Vector3f(
                (float) Math.cos(Math.toRadians(yaw)) * (float) Math.cos(Math.toRadians(pitch)),
                (float) Math.sin(Math.toRadians(pitch)),
                (float) Math.sin(Math.toRadians(yaw)) * (float) Math.cos(Math.toRadians(pitch))
        ).normalize();
    }

    private Vector3f getRight() {
        return getForward().cross(0, 1, 0, new Vector3f()).normalize();
    }
}
