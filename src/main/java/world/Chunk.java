package world;

import mesh.CubeMesh;
import org.joml.Vector3f;
import world.blocks.AirBlock;
import java.util.ArrayList;
import java.util.List;

public class Chunk {
    public static final int SIZE_XZ = 16;
    public static final int HEIGHT = 32;
    private final Block[] blocks = new Block[SIZE_XZ * HEIGHT * SIZE_XZ];
    private final int chunkX, chunkZ;
    private CubeMesh mesh;

    public Chunk(int chunkX, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        for (int i = 0; i < blocks.length; i++)
            blocks[i] = new AirBlock();
    }

    private int index(int x, int y, int z) {
        if (x < 0 || x >= SIZE_XZ || y < 0 || y >= HEIGHT || z < 0 || z >= SIZE_XZ) throw new IndexOutOfBoundsException("Chunk coordinates out of bounds: " + x + "," + y + "," + z);
        return x + SIZE_XZ * (y + HEIGHT * z);
    }

    public Block get(int x, int y, int z) {
        if (y < 0 || y >= HEIGHT) return null;
        return blocks[index(x, y, z)];
    }

    public void set(int x, int y, int z, Block block) {
        if (y < 0 || y >= HEIGHT) return;
        blocks[index(x, y, z)] = block;
    }

    public int worldX(int x) {
        return chunkX * SIZE_XZ + x;
    }

    public int worldZ(int z) {
        return chunkZ * SIZE_XZ + z;
    }

    public void rebuildMesh() {
        if (mesh != null) mesh.cleanup();
        mesh = new CubeMesh();
        // TODO: only add visible faces
    }

    public CubeMesh getMesh() {
        return mesh;
    }

    public void setMesh(CubeMesh mesh) {
        this.mesh = mesh;
    }

    public List<Vector3f> getNonAirBlockPositions() {
        List<Vector3f> positions = new ArrayList<>();
        for (int x = 0; x < SIZE_XZ; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                for (int z = 0; z < SIZE_XZ; z++) {
                    Block b = get(x, y, z);
                    if (!(b instanceof AirBlock)) {
                        positions.add(new Vector3f(x, y, z));
                    }
                }
            }
        } return positions;
    }
}