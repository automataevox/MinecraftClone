package world.blocks;

import world.Block;

public class DirtBlock extends Block {
    public DirtBlock() {
        super("dirt", new String[] {
                "dirt",  // TOP
                "dirt",  // BOTTOM
                "dirt",  // FRONT
                "dirt",  // BACK
                "dirt",  // LEFT
                "dirt"   // RIGHT
        });
    }
}