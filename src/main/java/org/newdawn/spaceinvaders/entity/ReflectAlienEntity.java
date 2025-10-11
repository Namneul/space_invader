package org.newdawn.spaceinvaders.entity;

import org.newdawn.spaceinvaders.Game;
import org.newdawn.spaceinvaders.Sprite;
import org.newdawn.spaceinvaders.SpriteStore;



public class ReflectAlienEntity extends Entity implements AlienEntityInterface {

    /** The speed at which the alient moves horizontally */
    private double moveSpeed = 75;
    /** The game in which the entity exists */
    private Game game;

    /**
     * Create a new alien entity
     *
     * @param game The game in which this entity is being created
     * @param x    The intial x location of this alien
     * @param y    The intial y location of this alient
     */
    public ReflectAlienEntity(Game game, int x, int y) {
        super("sprites/ReflectAlien.png",x,y);

        this.game = game;
        dx = -moveSpeed;
    }

    public void doLogic() {
        // swap over horizontal movement and move down the
        // screen a bit
        dx = -dx;
        y += 10;
    }

    @Override
    public void collidedWith(Entity other) {

    }

    /**
     * Request that this alien moved based on time elapsed
     *
     * @param delta The time that has elapsed since last move
     */
    @Override
    public void move(long delta) {

        // if we have reached the left hand side of the screen and
        // are moving left then request a logic update
        if ((dx < 0) && (x < 10)) {
            doLogic();
        }
        // and vice vesa, if we have reached the right hand side of
        // the screen and are moving right, request a logic update
        if ((dx > 0) && (x > 750)) {
            doLogic();
        }

        // proceed with normal move
        super.move(delta);
    }
}
