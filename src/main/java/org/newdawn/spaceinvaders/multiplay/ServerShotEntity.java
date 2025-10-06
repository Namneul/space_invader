package org.newdawn.spaceinvaders.multiplay;

public class ServerShotEntity extends ServerGame.Entity {

    private double moveSpeed = 300;


    private boolean isUsed = false;

    public ServerShotEntity(ServerGame serverGame, double x, double y) {
        super(serverGame,10,10, x, y);
        this.type = ServerGame.EntityType.SHOT;
    }

    @Override
    public void tick() {
        setY(getY()-moveSpeed/Server.TICKS_PER_SECOND);
        if (getY()<0){this.game.removeEntity(this.getId());}

    }

    @Override
    public void handleCollision(ServerGame.Entity otherEntity) {
        if (otherEntity instanceof ServerAlienEntity) {
            game.removeEntity(this.getId());
        }
    }
}
