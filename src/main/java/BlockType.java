public enum BlockType {
    AIR(false),
    DIRT(true),
    GRASS(true),
    STONE(true);

    public final boolean solid;

    BlockType(boolean solid) {
        this.solid = solid;
    }
}
