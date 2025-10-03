package org.newdawn.spaceinvaders.entity;

import org.newdawn.spaceinvaders.Game;

public class EvolveItemEntity extends Entity {
    private Game game;
    public EvolveItemEntity(Game game, String shipShape, int x, int y) {
        super(shipShape, x, y);
        this.game = game;
    }

    @Override
    public void move(long delta) {
        super.move(delta);
        if(y>600){

        }
    }

    public void collidedWith(Entity other) {
        if (other instanceof ShipEntity) {

        }
    }
}
