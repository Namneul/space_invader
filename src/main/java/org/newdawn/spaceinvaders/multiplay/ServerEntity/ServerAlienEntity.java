package org.newdawn.spaceinvaders.multiplay.ServerEntity;

import org.newdawn.spaceinvaders.multiplay.Server;
import org.newdawn.spaceinvaders.multiplay.ServerGame;

public class ServerAlienEntity extends ServerGame.Entity {

    private long lastFrameChange;
    private long frameDuration = 250;
    private int frameNumber;
    private final int maxHP = 200;
    private int currentHP;

    public ServerAlienEntity(ServerGame serverGame, int x, int y){
        super(serverGame,20,20,x, y);
        moveSpeed = 75;
        dx = -moveSpeed;
        this.type = ServerGame.EntityType.ALIEN;
        currentHP = this.maxHP;
    }


    @Override
    public void tick() {
        setX(getX()+dx/ Server.TICKS_PER_SECOND);
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

        if (Math.random()< 0.000001){
            game.alienFires(this);
        }
    }

    public void hit(ServerGame.Entity otherEntity,int dmg){
        this.currentHP -= dmg;
        if (currentHP <= 0){
            game.notifyAlienKilled(this, ((ServerShotEntity) otherEntity).getOwnerId());
            game.removeEntity(this.getId());
        }
        game.removeEntity(otherEntity.getId());
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
            game.notifyDeath(this.getId());
        }
    }


    @Override
    public void handleCollision(ServerGame.Entity otherEntity) {
        if (otherEntity instanceof ServerShotEntity) {
           hit(otherEntity, ((ServerShotEntity) otherEntity).getDamage());
        }
    }
}
