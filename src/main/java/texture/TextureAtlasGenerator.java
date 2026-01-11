package texture;

import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBImage;
import org.lwjgl.stb.STBImageWrite;
import org.lwjgl.system.MemoryStack;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.*;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.memAlloc;
import static org.lwjgl.system.MemoryUtil.memFree;

public class TextureAtlasGenerator {

    private static final int ATLAS_SIZE = 256;
    private static final int TILE_SIZE = 16;
    private static final int PADDING = 0; // 1 pixel padding between tiles

    private static class TextureInfo {
        String name;
        String path;
        ByteBuffer data;
        int width, height;
        int gridX, gridY;

        TextureInfo(String name, String path) {
            this.name = name;
            this.path = path;
        }
    }

    public static void generateAtlas(String outputPath) throws IOException {
        System.out.println("Generating texture atlas...");

        // List all textures to include in atlas
        List<TextureInfo> textures = new ArrayList<>();

        // Register all block textures
        // Grass block faces
        textures.add(new TextureInfo("grass_top", "textures/blocks/grass_top.png"));
        textures.add(new TextureInfo("grass_side", "textures/blocks/grass_side.png"));
        textures.add(new TextureInfo("dirt", "textures/blocks/dirt.png"));

        // Stone
        textures.add(new TextureInfo("stone", "textures/blocks/stone.png"));
        textures.add(new TextureInfo("sand", "textures/blocks/sand.png"));

        // Debug textures
        textures.add(new TextureInfo("debug", "textures/blocks/debug.png"));

        // Load all textures
        for (TextureInfo tex : textures) {
            loadTexture(tex);
        }

        // Calculate grid layout
        int tilesPerRow = ATLAS_SIZE / (TILE_SIZE + 2 * PADDING);
        int tileCount = 0;

        for (TextureInfo tex : textures) {
            tex.gridX = tileCount % tilesPerRow;
            tex.gridY = tileCount / tilesPerRow;
            tileCount++;

            System.out.printf("  %-15s -> grid [%d, %d]%n",
                    tex.name, tex.gridX, tex.gridY);
        }

        // Create atlas image buffer (RGBA)
        ByteBuffer atlasBuffer = BufferUtils.createByteBuffer(ATLAS_SIZE * ATLAS_SIZE * 4);

        // Fill with transparent background
        for (int i = 0; i < ATLAS_SIZE * ATLAS_SIZE * 4; i += 4) {
            atlasBuffer.put(i, (byte) 0);     // R
            atlasBuffer.put(i + 1, (byte) 0); // G
            atlasBuffer.put(i + 2, (byte) 0); // B
            atlasBuffer.put(i + 3, (byte) 0); // A (transparent)
        }

        // Place each texture into atlas
        for (TextureInfo tex : textures) {
            placeTextureInAtlas(tex, atlasBuffer);
        }

        // Write atlas to file
        saveAtlasToFile(atlasBuffer, outputPath);

        // Clean up
        for (TextureInfo tex : textures) {
            if (tex.data != null) {
                STBImage.stbi_image_free(tex.data);
            }
        }

        System.out.println("Texture atlas generated: " + outputPath);
    }

    private static void loadTexture(TextureInfo tex) throws IOException {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            // Load texture from resources
            String resourcePath = tex.path;
            InputStream stream = TextureAtlasGenerator.class
                    .getClassLoader()
                    .getResourceAsStream(resourcePath);

            if (stream == null) {
                throw new IOException("Texture not found: " + resourcePath);
            }

            byte[] bytes = stream.readAllBytes();
            ByteBuffer buffer = memAlloc(bytes.length);
            buffer.put(bytes);
            buffer.flip();

            STBImage.stbi_set_flip_vertically_on_load(false);
            tex.data = STBImage.stbi_load_from_memory(buffer, w, h, channels, 4);
            memFree(buffer);

            if (tex.data == null) {
                throw new IOException("Failed to load texture: " + resourcePath +
                        " - " + STBImage.stbi_failure_reason());
            }

            tex.width = w.get();
            tex.height = h.get();

            if (tex.width != TILE_SIZE || tex.height != TILE_SIZE) {
                System.err.printf("Warning: Texture %s is %dx%d, expected %dx%d%n",
                        tex.name, tex.width, tex.height, TILE_SIZE, TILE_SIZE);
            }
        }
    }

    private static void placeTextureInAtlas(TextureInfo tex, ByteBuffer atlasBuffer) {
        int startX = tex.gridX * (TILE_SIZE + 2 * PADDING) + PADDING;
        int startY = tex.gridY * (TILE_SIZE + 2 * PADDING) + PADDING;

        // Copy texture data into atlas
        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                int srcIndex = (y * TILE_SIZE + x) * 4;
                int dstIndex = ((startY + y) * ATLAS_SIZE + (startX + x)) * 4;

                if (srcIndex < tex.width * tex.height * 4) {
                    atlasBuffer.put(dstIndex, tex.data.get(srcIndex));      // R
                    atlasBuffer.put(dstIndex + 1, tex.data.get(srcIndex + 1)); // G
                    atlasBuffer.put(dstIndex + 2, tex.data.get(srcIndex + 2)); // B
                    atlasBuffer.put(dstIndex + 3, tex.data.get(srcIndex + 3)); // A
                }
            }
        }
    }

    private static void addDebugBorder(TextureInfo tex, ByteBuffer atlasBuffer,
                                       int startX, int startY) {
        byte r = (byte) ((tex.name.hashCode() >> 16) & 0xFF);
        byte g = (byte) ((tex.name.hashCode() >> 8) & 0xFF);
        byte b = (byte) (tex.name.hashCode() & 0xFF);

        // Top and bottom borders
        for (int x = -1; x <= TILE_SIZE; x++) {
            setPixel(atlasBuffer, startX + x, startY - 1, r, g, b, (byte) 255);
            setPixel(atlasBuffer, startX + x, startY + TILE_SIZE, r, g, b, (byte) 255);
        }

        // Left and right borders
        for (int y = 0; y < TILE_SIZE; y++) {
            setPixel(atlasBuffer, startX - 1, startY + y, r, g, b, (byte) 255);
            setPixel(atlasBuffer, startX + TILE_SIZE, startY + y, r, g, b, (byte) 255);
        }
    }

    private static void setPixel(ByteBuffer buffer, int x, int y,
                                 byte r, byte g, byte b, byte a) {
        if (x >= 0 && x < ATLAS_SIZE && y >= 0 && y < ATLAS_SIZE) {
            int index = (y * ATLAS_SIZE + x) * 4;
            buffer.put(index, r);
            buffer.put(index + 1, g);
            buffer.put(index + 2, b);
            buffer.put(index + 3, a);
        }
    }

    private static void saveAtlasToFile(ByteBuffer buffer, String outputPath) throws IOException {
        buffer.rewind();

        // Create output directory if it doesn't exist
        File outputFile = new File(outputPath);
        outputFile.getParentFile().mkdirs();

        // Write PNG file
        if (!STBImageWrite.stbi_write_png(
                outputPath,
                ATLAS_SIZE,
                ATLAS_SIZE,
                4,
                buffer,
                ATLAS_SIZE * 4)) {
            throw new IOException("Failed to write PNG: " + outputPath);
        }

        System.out.println("Saved atlas to: " + outputFile.getAbsolutePath());
    }

    // Generate atlas and create UV coordinates
    public static Map<String, TextureAtlas.UVCoords> generateAtlasWithUVs(String outputPath) throws IOException {
        // First, generate the atlas image
        generateAtlas(outputPath);

        // Then create UV coordinates for each texture
        Map<String, TextureAtlas.UVCoords> uvMap = new HashMap<>();

        // Define textures and their grid positions
        // (Same as in TextureAtlas.registerTextures())
        int tileCount = 0;
        int tilesPerRow = ATLAS_SIZE / (TILE_SIZE + 2 * PADDING);

        String[] textureNames = {
                "grass_top", "grass_side", "dirt",
                "stone",
                "debug"
        };

        for (String name : textureNames) {
            int gridX = tileCount % tilesPerRow;
            int gridY = tileCount / tilesPerRow;

            // Calculate UV coordinates with half-pixel offset
            float pixelOffset = 0.5f / ATLAS_SIZE;
            float paddingOffset = (float)PADDING / ATLAS_SIZE;

            float u1 = (gridX * (TILE_SIZE + 2 * PADDING) + PADDING) / (float)ATLAS_SIZE + pixelOffset;
            float v1 = (gridY * (TILE_SIZE + 2 * PADDING) + PADDING) / (float)ATLAS_SIZE + pixelOffset;
            float u2 = u1 + TILE_SIZE / (float)ATLAS_SIZE - 2 * pixelOffset;
            float v2 = v1 + TILE_SIZE / (float)ATLAS_SIZE - 2 * pixelOffset;

            uvMap.put(name, new TextureAtlas.UVCoords(u1, v1, u2, v2));
            tileCount++;
        }

        return uvMap;
    }

    // Modified TextureAtlas class that uses the generator
    public static class DynamicTextureAtlas {
        private final Texture texture;
        private final Map<String, TextureAtlas.UVCoords> textureMap;

        public DynamicTextureAtlas() throws IOException {
            // Generate atlas on the fly
            String atlasPath = "generated/atlas.png";
            this.textureMap = generateAtlasWithUVs(atlasPath);

            // Load the generated texture
            this.texture = new Texture(atlasPath);
            setupTextureFiltering();
        }

        private void setupTextureFiltering() {
            texture.bind();
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
            texture.unbind();
        }

        public TextureAtlas.UVCoords getUV(String textureName) {
            TextureAtlas.UVCoords coords = textureMap.get(textureName);
            if (coords == null) {
                System.err.println("Texture not found in atlas: " + textureName);
                return textureMap.get("debug");
            }
            return coords;
        }

        public Texture getTexture() {
            return texture;
        }
    }
}