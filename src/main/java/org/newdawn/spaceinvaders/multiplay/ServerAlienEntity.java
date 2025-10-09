package org.newdawn.spaceinvaders.multiplay;

import org.newdawn.spaceinvaders.Game;
import org.newdawn.spaceinvaders.Sprite;
import org.newdawn.spaceinvaders.SpriteStore;

public class ServerAlienEntity extends ServerGame.Entity {

    private long lastFrameChange;
    private long frameDuration = 250;
    private int frameNumber;

    public ServerAlienEntity(ServerGame serverGame, int x, int y){
        super(serverGame,20,20,x, y);
        moveSpeed = 75;
        dx = -moveSpeed;
        this.type = ServerGame.EntityType.ALIEN;
    }


    @Override
    public void tick() {
        setX(getX()+dx/Server.TICKS_PER_SECOND);
        if (dx<0 && getX()<10 || dx>0 && getX()>750){
            this.game.requestLogicUpdate();
        }
        lastFrameChange += 2;
        if (lastFrameChange > frameDuration){

            lastFrameChange = 0;

            frameNumber++;
            if (frameNumber > 3){
                frameNumber = 0;
            }
        }

        if (Math.random()< 0.001){
            game.alienFires(this);
        }
    }

    public void setMoveSpeed(double moveSpeed){
        this.moveSpeed = moveSpeed;
    }
    public double getMoveSpeed(){ return moveSpeed; }

    public int getFrameNumber(){ return frameNumber; }

    public void doLogic(){
        dx = -dx;
        setY(getY()+10);
        if (getY()>570){
            game.notifyDeath();
        }
    }


    @Override
    public void handleCollision(ServerGame.Entity otherEntity) {
        if (otherEntity instanceof ServerShotEntity) {
            game.removeEntity(this.getId());
            game.removeEntity(otherEntity.getId());
            game.notifyAlienKilled(this, otherEntity.getId());
        }
    }
}
