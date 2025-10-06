package org.newdawn.spaceinvaders.multiplay;

import org.newdawn.spaceinvaders.Game;
import org.newdawn.spaceinvaders.Sprite;
import org.newdawn.spaceinvaders.SpriteStore;

public class ServerAlienEntity extends ServerGame.Entity {

    private double moveSpeed = 75;

    public ServerAlienEntity(ServerGame serverGame, int x, int y){
        super(serverGame,20,20,x, y);

        dx = -moveSpeed;
        this.type = ServerGame.EntityType.ALIEN;
    }


    @Override
    public void tick() {
        setX(getX()+dx/Server.TICKS_PER_SECOND);
        if (dx<0 && getX()<10 || dx>0 && getX()>750){
            this.game.requestLogicUpdate();
        }
    }

    public void doLogic(){
        dx = -dx;
        setY(getY()+10);
    }


    @Override
    public void handleCollision(ServerGame.Entity otherEntity) {
        if (otherEntity instanceof ServerShotEntity) {
            game.removeEntity(this.getId());
            game.removeEntity(otherEntity.getId());
        }
    }
}
