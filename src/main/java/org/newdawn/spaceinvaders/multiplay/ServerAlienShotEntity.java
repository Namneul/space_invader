package org.newdawn.spaceinvaders.multiplay;

public class ServerAlienShotEntity extends ServerGame.Entity {

    private double moveSpeed = 300;

    public ServerAlienShotEntity(ServerGame game, double x, double y) {
        super(game, 10, 10, x, y);
        this.type = ServerGame.EntityType.ALIEN_SHOT;
    }

    @Override
    public void tick() {
        setY(getY()+moveSpeed/Server.TICKS_PER_SECOND);
        if (getY() > 600){ this.game.removeEntity(this.getId()); }
    }

    @Override
    public void handleCollision(ServerGame.Entity otherEntity) {
        if (otherEntity instanceof ServerPlayerShipEntity) {
            game.removeEntity(this.getId());
            //playership 죽이기

        }
    }
}
