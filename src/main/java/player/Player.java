package player;

import camera.Camera;
import org.joml.Vector3f;
import world.WorldManager;

public class Player {

    private final Camera camera;
    private final WorldManager world;

    private final float eyeHeight = 1.62f; // height of eyes above feet
    private final float height = 1.8f;     // total player height
    private final float width = 0.6f;      // player width

    private Vector3f velocity = new Vector3f();
    private boolean onGround = false;

    private final float gravity = -9.8f;
    private final float jumpStrength = 5f;

    public Player(Camera camera, WorldManager world) {
        this.camera = camera;
        this.world = world;

        // Spawn on top of the ground
        Vector3f spawn = world.getSpawnPoint();
        camera.setPosition(new Vector3f(spawn.x, spawn.y + eyeHeight, spawn.z));
    }

    public void update(float deltaTime, boolean forward, boolean backward, boolean left, boolean right, boolean jump) {
        Vector3f move = new Vector3f();

        float speed = 5f * deltaTime;

        if (forward)  move.add(new Vector3f(camera.getFront()).mul(speed, new Vector3f()));
        if (backward) move.sub(new Vector3f(camera.getFront()).mul(speed, new Vector3f()));
        if (left)     move.sub(new Vector3f(camera.getRight()).mul(speed, new Vector3f()));
        if (right)    move.add(new Vector3f(camera.getRight()).mul(speed, new Vector3f()));

        // Apply horizontal movement
        camera.move(move);

        // Apply gravity
        if (!onGround) velocity.y += gravity * deltaTime;

        // Jump
        if (jump && onGround) {
            velocity.y = jumpStrength;
            onGround = false;
        }

        // Apply vertical movement
        Vector3f feetPos = new Vector3f(camera.getPosition()).sub(0, eyeHeight, 0); // feet position
        feetPos.add(velocity.x * deltaTime, velocity.y * deltaTime, velocity.z * deltaTime);

        // Collision detection
        onGround = false;
        if (checkCollision(feetPos)) {
            velocity.y = 0;
            onGround = true;
            // Snap to top of block
            feetPos.y = (float)Math.floor(feetPos.y) + 1.0f;
        }

        // Set camera position at eye height above feet
        camera.setPosition(new Vector3f(feetPos.x, feetPos.y + eyeHeight, feetPos.z));
    }

    private boolean checkCollision(Vector3f feetPos) {
        // Check the blocks inside the player's bounding box
        int minX = (int)(feetPos.x - width / 2);
        int maxX = (int)(feetPos.x + width / 2);
        int minZ = (int)(feetPos.z - width / 2);
        int maxZ = (int)(feetPos.z + width / 2);
        int minY = (int)(feetPos.y);
        int maxY = (int)(feetPos.y + height);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (world.hasCube(x, y, z)) return true;
                }
            }
        }
        return false;
    }

    public Camera getCamera() {
        return camera;
    }
}
