public class Chunk {
    public static final int SIZE = 16;
    private final BlockType[] blocks = new BlockType[SIZE * SIZE * SIZE];

    public Chunk() {
        for (int i = 0; i < blocks.length; i++)
            blocks[i] = BlockType.AIR;
    }

    public BlockType get(int x, int y, int z) {
        return blocks[x + SIZE * (y + SIZE * z)];
    }

    public void set(int x, int y, int z, BlockType type) {
        blocks[x + SIZE * (y + SIZE * z)] = type;
    }
}
