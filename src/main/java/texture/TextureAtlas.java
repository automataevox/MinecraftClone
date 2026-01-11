package texture;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;

public class TextureAtlas {
    private final Texture texture;
    private final Map<String, UVCoords> textureMap;
    private final int atlasSize = 256;
    private final int tileSize = 16;

    public TextureAtlas() throws IOException {
        // Use simple hardcoded coordinates
        this.textureMap = new HashMap<>();
        registerSimpleTextures();

        // Load the atlas
        this.texture = new Texture("textures/atlas.png");
        setupTextureFiltering();

    }

    private void setupTextureFiltering() {
        texture.bind();

        // CRITICAL SETTINGS:
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        texture.unbind();
    }

    private void registerSimpleTextures() {
        // Simple hardcoded UVs - no offsets
        // Grid layout: 16 tiles across (256/16), 16 tiles down

        // Row 0
        registerSimple("grass_top", 0, 0);
        registerSimple("grass_side", 1, 0);
        registerSimple("dirt", 2, 0);
        registerSimple("stone", 3, 0);
        registerSimple("sand", 5, 0);

        // Row 1
        registerSimple("debug", 6, 0);

    }

    private void registerSimple(String name, int gridX, int gridY) {
        float u1 = (gridX * tileSize) / (float)atlasSize;
        float v1 = (gridY * tileSize) / (float)atlasSize;
        float u2 = ((gridX + 1) * tileSize) / (float)atlasSize;
        float v2 = ((gridY + 1) * tileSize) / (float)atlasSize;

        textureMap.put(name, new UVCoords(u1, v1, u2, v2));
    }

    public UVCoords getUV(String textureName) {
        UVCoords coords = textureMap.get(textureName);
        if (coords == null) {
            System.err.println("Texture '" + textureName + "' not found in atlas!");
            System.err.println("Available: " + textureMap.keySet());
            // Return debug texture
            return textureMap.get("debug");
        }
        return coords;
    }

    public Texture getTexture() {
        return texture;
    }

    public static class UVCoords {
        public final float u1, v1, u2, v2;

        public UVCoords(float u1, float v1, float u2, float v2) {
            this.u1 = u1;
            this.v1 = v1;
            this.u2 = u2;
            this.v2 = v2;
        }

        @Override
        public String toString() {
            return String.format("(%.4f,%.4f)-(%.4f,%.4f)", u1, v1, u2, v2);
        }
    }
}