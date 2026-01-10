package mesh;

import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class WireCubeMesh {

    private final int vao, vbo;

    private static final float[] VERTICES = {
            // 12 edges as lines (24 vertices)
            -0.5f,-0.5f,-0.5f,  0.5f,-0.5f,-0.5f,
            0.5f,-0.5f,-0.5f,  0.5f, 0.5f,-0.5f,
            0.5f, 0.5f,-0.5f, -0.5f, 0.5f,-0.5f,
            -0.5f, 0.5f,-0.5f, -0.5f,-0.5f,-0.5f,

            -0.5f,-0.5f, 0.5f,  0.5f,-0.5f, 0.5f,
            0.5f,-0.5f, 0.5f,  0.5f, 0.5f, 0.5f,
            0.5f, 0.5f, 0.5f, -0.5f, 0.5f, 0.5f,
            -0.5f, 0.5f, 0.5f, -0.5f,-0.5f, 0.5f,

            -0.5f,-0.5f,-0.5f, -0.5f,-0.5f, 0.5f,
            0.5f,-0.5f,-0.5f,  0.5f,-0.5f, 0.5f,
            0.5f, 0.5f,-0.5f,  0.5f, 0.5f, 0.5f,
            -0.5f, 0.5f,-0.5f, -0.5f, 0.5f, 0.5f
    };

    public WireCubeMesh() {
        vao = glGenVertexArrays();
        vbo = glGenBuffers();

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);

        FloatBuffer buffer = MemoryUtil.memAllocFloat(VERTICES.length);
        buffer.put(VERTICES).flip();

        glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);
        MemoryUtil.memFree(buffer);

        glVertexAttribPointer(0, 3, GL_FLOAT, false, 3 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    public void render() {
        glBindVertexArray(vao);
        glDrawArrays(GL_LINES, 0, VERTICES.length / 3);
        glBindVertexArray(0);
    }

    public void cleanup() {
        glDeleteBuffers(vbo);
        glDeleteVertexArrays(vao);
    }
}