package org.newdawn.spaceinvaders.multiplay;

public class ServerPlayerShipEntity extends ServerGame.Entity {

    private long lastFireTime = 0;

    public ServerPlayerShipEntity(ServerGame serverGame, double x, double y) {
        super(serverGame,30,30, x, y);
        this.type = ServerGame.EntityType.PLAYER;
    }


    @Override
    public void tick() {

    }

    public long getLastFireTime(){
        return lastFireTime;
    }
    public void setLastFireTime(){
        lastFireTime = System.currentTimeMillis();
    }

    @Override
    public void handleCollision(ServerGame.Entity otherEntity) {

    }
}
