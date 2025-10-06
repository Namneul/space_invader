package org.newdawn.spaceinvaders.entity;

import org.newdawn.spaceinvaders.Game;
import org.newdawn.spaceinvaders.SpriteStore;
//진화 아이템 생성과 플레이어와 충돌시 동작 로직
public class EvolveItemEntity extends Entity{
    private Game game;
    private int moveSpeed = 200;
    public EvolveItemEntity(Game game, String evolveitem, int x, int y) {
        super(evolveitem, x, y);
        this.game = game;
        dy = moveSpeed;
    }

    @Override
    public void move(long delta) {
        super.move(delta);
        if( y>600 ){
            game.removeEntity(this);
        }
    }

    public void collidedWith(Entity other) {
        if (other instanceof ShipEntity) {
            game.removeEntity(this);
            ((ShipEntity) other).upgrade();

        }
    }

}
