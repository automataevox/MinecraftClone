package shader;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.glDeleteProgram;

public class Shader {

    private final int programId;

    public Shader(String vertexPath, String fragmentPath) throws IOException {
        // Load shader sources
        String vertexSource = Files.readString(Path.of(vertexPath));
        String fragmentSource = Files.readString(Path.of(fragmentPath));

        // Compile shaders
        int vertexShader = compileShader(vertexSource, GL_VERTEX_SHADER);
        int fragmentShader = compileShader(fragmentSource, GL_FRAGMENT_SHADER);

        // Link program
        programId = glCreateProgram();
        glAttachShader(programId, vertexShader);
        glAttachShader(programId, fragmentShader);
        glLinkProgram(programId);

        // Check linking
        if (glGetProgrami(programId, GL_LINK_STATUS) == GL_FALSE) {
            throw new RuntimeException("Program linking failed: " + glGetProgramInfoLog(programId));
        }

        // Clean up individual shaders
        glDetachShader(programId, vertexShader);
        glDetachShader(programId, fragmentShader);
        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);
    }

    private int compileShader(String source, int type) {
        int shaderId = glCreateShader(type);
        glShaderSource(shaderId, source);
        glCompileShader(shaderId);

        // Check compilation
        if (glGetShaderi(shaderId, GL_COMPILE_STATUS) == GL_FALSE) {
            throw new RuntimeException(
                    (type == GL_VERTEX_SHADER ? "Vertex" : "Fragment") +
                            " shader compilation failed: " + glGetShaderInfoLog(shaderId)
            );
        }
        return shaderId;
    }

    public void bind() {
        glUseProgram(programId);
    }

    public void unbind() {
        glUseProgram(0);
    }

    public void cleanup() {
        glDeleteProgram(programId);
    }

    // Set uniform mat4
    public void setUniformMat4f(String name, Matrix4f matrix) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer fb = stack.mallocFloat(16);
            matrix.get(fb);
            int location = glGetUniformLocation(programId, name);
            glUniformMatrix4fv(location, false, fb);
        }
    }

    // Add this for vec3
    public void setUniform3f(String name, Vector3f vec) {
        int location = GL20.glGetUniformLocation(programId, name);
        GL20.glUniform3f(location, vec.x, vec.y, vec.z);
    }
}
