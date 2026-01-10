package shader;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;
import java.io.IOException;
import java.io.InputStream;

import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.glDeleteProgram;

public class Shader {

    private final int programId;

    public Shader(String vertexResourcePath, String fragmentResourcePath) throws IOException {
        // Load shader sources from resources
        String vertexSource = loadSource(vertexResourcePath);
        String fragmentSource = loadSource(fragmentResourcePath);

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
            throw new IOException("Program linking failed: " + glGetProgramInfoLog(programId));
        }

        // Clean up individual shaders
        glDetachShader(programId, vertexShader);
        glDetachShader(programId, fragmentShader);
        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);
    }

    private String loadSource(String resourcePath) throws IOException {
        InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath);
        if (in == null) {
            throw new IOException("Shader resource not found: " + resourcePath);
        }
        return new String(in.readAllBytes());
    }

    private int compileShader(String source, int type) throws IOException {
        int shaderId = glCreateShader(type);
        glShaderSource(shaderId, source);
        glCompileShader(shaderId);

        // Check compilation
        if (glGetShaderi(shaderId, GL_COMPILE_STATUS) == GL_FALSE) {
            throw new java.io.IOException((type == GL_VERTEX_SHADER ? "Vertex" : "Fragment") + " shader compilation failed: " + glGetShaderInfoLog(shaderId));
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

    public void setUniform1f(String name, float value) {
        int location = glGetUniformLocation(programId, name);
        if (location != -1) {
            glUniform1f(location, value);
        }
    }

    public void setUniform1i(String name, int value) {
        int location = glGetUniformLocation(programId, name);
        if (location != -1) {
            glUniform1i(location, value);
        }
    }
}
