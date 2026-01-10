package mesh;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

import face.Face;
import world.Block;
import world.Chunk;

import java.nio.FloatBuffer;
import org.lwjgl.system.MemoryUtil;

public class ChunkMesh {

    private final int vao, vbo;
    private int vertexCount;
    private boolean valid = false;  // Track if mesh is valid


    public ChunkMesh() {
        vao = glGenVertexArrays();
        vbo = glGenBuffers();

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);

        // 8 floats per vertex: 3 pos + 3 normal + 2 texcoord
        int stride = 8 * Float.BYTES;

        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);                    // position
        glVertexAttribPointer(1, 3, GL_FLOAT, false, stride, 3 * Float.BYTES);      // normal
        glVertexAttribPointer(2, 2, GL_FLOAT, false, stride, 6 * Float.BYTES);      // texcoord

        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);
        glEnableVertexAttribArray(2);

        glBindVertexArray(0);
    }

    public int getVertexCount() {
        return vertexCount;
    }

    public void build(Chunk chunk) {
        System.out.println("Building chunk mesh at (" + chunk.chunkX + ", " + chunk.chunkZ + ")");

        FloatBuffer buffer = MemoryUtil.memAllocFloat(Chunk.SIZE * Chunk.SIZE * Chunk.SIZE * 6 * 6 * 8);
        int faceCount = 0;

        for (int x = 0; x < Chunk.SIZE; x++) {
            for (int y = 0; y < Chunk.SIZE; y++) {
                for (int z = 0; z < Chunk.SIZE; z++) {
                    Block block = chunk.getBlock(x, y, z);
                    if (block == null) continue;

                    for (Face face : Face.values()) {
                        if (isFaceVisible(chunk, x, y, z, face)) {
                            CubeMesh.addFace(buffer, x, y, z, face);
                            faceCount++;
                        }
                    }
                }
            }
        }

        System.out.println("Built " + faceCount + " faces");

        // FIX: Only ONE flip() here!
        buffer.flip();
        vertexCount = buffer.remaining() / 8;
        System.out.println("Vertex count: " + vertexCount);

        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);

        MemoryUtil.memFree(buffer);
        valid = true;
    }

    public boolean isValid() {
        return valid;
    }

    private boolean isFaceVisible(Chunk chunk, int x, int y, int z, Face face) {
        int nx = x + face.dx;
        int ny = y + face.dy;
        int nz = z + face.dz;

        if (nx < 0 || ny < 0 || nz < 0 ||
                nx >= Chunk.SIZE || ny >= Chunk.SIZE || nz >= Chunk.SIZE) {
            return true;
        }

        return chunk.getBlock(nx, ny, nz) == null;
    }

    public void render() {
        glBindVertexArray(vao);
        glDrawArrays(GL_TRIANGLES, 0, vertexCount);
        glBindVertexArray(0);
    }

    public void cleanup() {
        glDeleteBuffers(vbo);
        glDeleteVertexArrays(vao);
        valid = false;
    }
}
