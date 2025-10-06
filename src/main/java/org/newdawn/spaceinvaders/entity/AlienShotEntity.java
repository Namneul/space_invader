package org.newdawn.spaceinvaders.entity;

import org.newdawn.spaceinvaders.Game;
import org.newdawn.spaceinvaders.SpriteStore;

public class AlienShotEntity extends Entity{
    private double moveSpeed = 300;
    private Game game;
    private boolean used = false;

    public AlienShotEntity(Game game, String sprite, int x, int y) {
        super(sprite, x, y);
        this.game = game;
        dy = moveSpeed;
    }

    @Override
    public void move(long delta) {
        super.move(delta);
        if (y > 600) {
            game.removeEntity(this);
        }
    }

    @Override
    public void collidedWith(Entity other) {
        if (used) {
            return;
        }

        if (other instanceof ShipEntity) {
            game.removeEntity(this);
            ((ShipEntity) other).resetUpgrade();
            game.notifyDeath();
            used = true;
        }
    }

}