package org.newdawn.spaceinvaders.multiplay;

public class ServerShotEntity extends ServerGame.Entity {


    private final int ownerId;
    private boolean isUsed = false;
    private int upgradeLevel;

    public ServerShotEntity(ServerGame serverGame, double x, double y, int ownerId, int upgradeLevel) {
        super(serverGame,10,10, x, y);
        this.type = ServerGame.EntityType.SHOT;
        this.ownerId = ownerId;
        this.upgradeLevel = upgradeLevel;
        moveSpeed = 300;
    }

    @Override
    public void tick() {
        setY(getY()-moveSpeed/Server.TICKS_PER_SECOND);
        if (getY()<0){this.game.removeEntity(this.getId());}

    }

    public int getUpgradeLevel(){ return upgradeLevel; }

    public int getOwnerId(){ return ownerId; }

    @Override
    public void handleCollision(ServerGame.Entity otherEntity) {

        if (isUsed) return;

        if (otherEntity instanceof ServerAlienEntity) {
            game.removeEntity(this.getId());
            game.removeEntity(otherEntity.getId());
            isUsed =true;
        }
    }
}
