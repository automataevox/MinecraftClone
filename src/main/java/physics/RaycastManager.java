package physics;

import org.joml.Vector3f;
import world.WorldManager;

public class RaycastManager {

    private final WorldManager worldManager;
    private static final float MAX_DISTANCE = 10f;
    private static final float STEP = 0.2f;

    public RaycastManager(WorldManager worldManager) {
        this.worldManager = worldManager;
    }

    /**
     * Performs a raycast from the origin in the given direction.
     * Returns the position of the first cube hit, or null if none.
     */
    public Vector3f raycastBlock(Vector3f origin, Vector3f direction) {
        for (float t = 0; t < MAX_DISTANCE; t += STEP) {
            Vector3f point = new Vector3f(origin).fma(t, direction);
            int x = Math.round(point.x);
            int y = Math.round(point.y);
            int z = Math.round(point.z);

            if (worldManager.hasCube(x, y, z)) {
                return new Vector3f(x, y, z);
            }
        }
        return null;
    }

    // Optional cleanup if needed
    public void cleanup() {
        // Currently nothing to clean
    }
}
