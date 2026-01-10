package world;

public abstract class Block {
    private final String name;
    private final String texturePath;

    public Block(String name, String texturePath) {
        this.name = name;
        this.texturePath = texturePath;
    }

    public String getName() {
        return name;
    }

    public String getTexture() {
        return texturePath;
    }
}
