package org.newdawn.spaceinvaders.multiplay.ServerEntity;

import org.newdawn.spaceinvaders.multiplay.Server;
import org.newdawn.spaceinvaders.multiplay.ServerGame;

public class ServerMeteoriteEntity extends ServerGame.Entity {

    private long frameDuration = 50;
    private long lastFrameChange;
    private int frameNumber;



    public ServerMeteoriteEntity(ServerGame game, double x, double y) {
        super(game, 64, 64, x, y);
        moveSpeed = 300;
        dy = moveSpeed;
        this.type = ServerGame.EntityType.METEOR;
    }

    @Override
    public void tick() {
        setY(getY()+dy/ Server.TICKS_PER_SECOND);
        if (getY() > 570){
            game.removeEntity(this.getId());
        }
        lastFrameChange += 2;
        if (lastFrameChange > frameDuration){
            lastFrameChange = 0;

            frameNumber++;
            if (frameNumber>15){
                frameNumber = 0;
            }
        }

    }

    public int getFrameNumber(){ return frameNumber; }

    @Override
    public void handleCollision(ServerGame.Entity otherEntity) {
        if (otherEntity instanceof ServerPlayerShipEntity){
            game.removeEntity(this.getId());
        } else if (otherEntity instanceof ServerShotEntity) {
            game.removeEntity(this.getId());
            game.removeEntity(otherEntity.getId());
        }
    }
}
