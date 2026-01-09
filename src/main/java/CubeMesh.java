import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import org.lwjgl.system.MemoryUtil;
import java.nio.FloatBuffer;

public class CubeMesh {

    private final int vao, vbo;

    private static final float[] VERTICES = {
            // Front face (normal 0,0,1)
            -0.5f, -0.5f,  0.5f,  0f, 0f, 1f,
            0.5f, -0.5f,  0.5f,  0f, 0f, 1f,
            0.5f,  0.5f,  0.5f,  0f, 0f, 1f,
            0.5f,  0.5f,  0.5f,  0f, 0f, 1f,
            -0.5f,  0.5f,  0.5f,  0f, 0f, 1f,
            -0.5f, -0.5f,  0.5f,  0f, 0f, 1f,

            // Back face (normal 0,0,-1)
            -0.5f, -0.5f, -0.5f,  0f, 0f, -1f,
            -0.5f,  0.5f, -0.5f,  0f, 0f, -1f,
            0.5f,  0.5f, -0.5f,  0f, 0f, -1f,
            0.5f,  0.5f, -0.5f,  0f, 0f, -1f,
            0.5f, -0.5f, -0.5f,  0f, 0f, -1f,
            -0.5f, -0.5f, -0.5f,  0f, 0f, -1f,

            // Left face (normal -1,0,0)
            -0.5f,  0.5f,  0.5f,  -1f,0f,0f,
            -0.5f,  0.5f, -0.5f,  -1f,0f,0f,
            -0.5f, -0.5f, -0.5f,  -1f,0f,0f,
            -0.5f, -0.5f, -0.5f,  -1f,0f,0f,
            -0.5f, -0.5f,  0.5f,  -1f,0f,0f,
            -0.5f,  0.5f,  0.5f,  -1f,0f,0f,

            // Right face (normal 1,0,0)
            0.5f,  0.5f,  0.5f,  1f,0f,0f,
            0.5f, -0.5f,  0.5f,  1f,0f,0f,
            0.5f, -0.5f, -0.5f,  1f,0f,0f,
            0.5f, -0.5f, -0.5f,  1f,0f,0f,
            0.5f,  0.5f, -0.5f,  1f,0f,0f,
            0.5f,  0.5f,  0.5f,  1f,0f,0f,

            // Top face (normal 0,1,0)
            -0.5f,  0.5f, -0.5f,  0f,1f,0f,
            -0.5f,  0.5f,  0.5f,  0f,1f,0f,
            0.5f,  0.5f,  0.5f,  0f,1f,0f,
            0.5f,  0.5f,  0.5f,  0f,1f,0f,
            0.5f,  0.5f, -0.5f,  0f,1f,0f,
            -0.5f,  0.5f, -0.5f,  0f,1f,0f,

            // Bottom face (normal 0,-1,0)
            -0.5f, -0.5f, -0.5f,  0f,-1f,0f,
            0.5f, -0.5f, -0.5f,  0f,-1f,0f,
            0.5f, -0.5f,  0.5f,  0f,-1f,0f,
            0.5f, -0.5f,  0.5f,  0f,-1f,0f,
            -0.5f, -0.5f,  0.5f,  0f,-1f,0f,
            -0.5f, -0.5f, -0.5f,  0f,-1f,0f
    };


    public CubeMesh() {
        vao = glGenVertexArrays();
        vbo = glGenBuffers();

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);

        FloatBuffer buffer = MemoryUtil.memAllocFloat(VERTICES.length);
        buffer.put(VERTICES).flip();

        glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);
        MemoryUtil.memFree(buffer);

        int stride = 6 * Float.BYTES; // 3 for position, 3 for normal

        // Position attribute
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);
        glEnableVertexAttribArray(0);

        // Normal attribute
        glVertexAttribPointer(1, 3, GL_FLOAT, false, stride, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    public void render() {
        glBindVertexArray(vao);
        glDrawArrays(GL_TRIANGLES, 0, 36); // 6 faces * 2 triangles * 3 vertices = 36
        glBindVertexArray(0);
    }

    public void cleanup() {
        glDeleteBuffers(vbo);
        glDeleteVertexArrays(vao);
    }
}
