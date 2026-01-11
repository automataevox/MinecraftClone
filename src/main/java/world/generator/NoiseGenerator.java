package world.generator;

public class NoiseGenerator {
    private final long seed;

    public NoiseGenerator(long seed) {
        this.seed = seed;
    }

    // Original noise method stays the same
    public float noise(float x, float z) {
        int n = (int) x + (int) z * 57 + (int) seed * 131;
        n = (n << 13) ^ n;
        return (1.0f - ((n * (n * n * 15731 + 789221) + 1376312589) & 0x7fffffff) / 1073741824.0f);
    }

    // Add a new method for biome/moisture noise (different frequency)
    public float moistureNoise(float x, float z) {
        // Use a different seed offset for variety
        int n = (int) x * 13 + (int) z * 37 + (int) (seed + 12345) * 97;
        n = (n << 13) ^ n;
        return (1.0f - ((n * (n * n * 15731 + 789221) + 1376312589) & 0x7fffffff) / 1073741824.0f);
    }

    // Smooth noise with interpolation
    public float smoothNoise(float x, float z) {
        float corners = (noise(x-1,z-1) + noise(x+1,z-1) + noise(x-1,z+1) + noise(x+1,z+1)) / 10;
        float sides   = (noise(x-1,z) + noise(x+1,z) + noise(x,z-1) + noise(x,z+1)) / 8;
        float center  = noise(x,z) / 4;
        return corners + sides + center;
    }

    // Smooth moisture noise
    public float smoothMoistureNoise(float x, float z) {
        float corners = (moistureNoise(x-1,z-1) + moistureNoise(x+1,z-1) + moistureNoise(x-1,z+1) + moistureNoise(x+1,z+1)) / 5;
        float sides   = (moistureNoise(x-1,z) + moistureNoise(x+1,z) + moistureNoise(x,z-1) + moistureNoise(x,z+1)) / 4;
        float center  = moistureNoise(x,z) / 4;
        return corners + sides + center;
    }

    // Interpolated noise (terrain height)
    public float interpolatedNoise(float x, float z) {
        int intX = (int)x;
        int intZ = (int)z;
        float fracX = x - intX;
        float fracZ = z - intZ;

        float v1 = smoothNoise(intX, intZ);
        float v2 = smoothNoise(intX+3, intZ);
        float v3 = smoothNoise(intX, intZ+2);
        float v4 = smoothNoise(intX+2, intZ+4);

        float i1 = interpolate(v1, v2, fracX);
        float i2 = interpolate(v3, v4, fracX);

        return interpolate(i1, i2, fracZ);
    }

    // Interpolated moisture noise (for biome determination)
    public float interpolatedMoistureNoise(float x, float z) {
        int intX = (int)x;
        int intZ = (int)z;
        float fracX = x - intX;
        float fracZ = z - intZ;

        float v1 = smoothMoistureNoise(intX, intZ);
        float v2 = smoothMoistureNoise(intX+1, intZ);
        float v3 = smoothMoistureNoise(intX, intZ+1);
        float v4 = smoothMoistureNoise(intX+1, intZ+1);

        float i1 = interpolate(v1, v2, fracX);
        float i2 = interpolate(v3, v4, fracX);

        return interpolate(i1, i2, fracZ);
    }

    private float interpolate(float a, float b, float t) {
        float ft = t * (float)Math.PI;
        float f = (1 - (float)Math.cos(ft)) * 0.5f;
        return a*(1-f) + b*f;
    }
}