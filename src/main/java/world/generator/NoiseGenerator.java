package world.generator;

public class NoiseGenerator {

    private final long seed;

    public NoiseGenerator(long seed) {
        this.seed = seed;
    }

    // Simple 2D Perlin-like noise
    public float noise(float x, float z) {
        int n = (int) x + (int) z * 57 + (int) seed * 131;
        n = (n << 13) ^ n;
        return (1.0f - ((n * (n * n * 15731 + 789221) + 1376312589) & 0x7fffffff) / 1073741824.0f);
    }

    // Smooth noise with interpolation
    public float smoothNoise(float x, float z) {
        float corners = (noise(x-1,z-1) + noise(x+1,z-1) + noise(x-1,z+1) + noise(x+1,z+1)) / 16;
        float sides   = (noise(x-1,z) + noise(x+1,z) + noise(x,z-1) + noise(x,z+1)) / 8;
        float center  = noise(x,z) / 4;
        return corners + sides + center;
    }

    // Interpolated noise
    public float interpolatedNoise(float x, float z) {
        int intX = (int)x;
        int intZ = (int)z;
        float fracX = x - intX;
        float fracZ = z - intZ;

        float v1 = smoothNoise(intX, intZ);
        float v2 = smoothNoise(intX+1, intZ);
        float v3 = smoothNoise(intX, intZ+1);
        float v4 = smoothNoise(intX+1, intZ+1);

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
