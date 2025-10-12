package org.newdawn.spaceinvaders.multiplay.ServerEntity;

import org.newdawn.spaceinvaders.multiplay.Server;
import org.newdawn.spaceinvaders.multiplay.ServerGame;

public class ServerAlienShotEntity extends ServerGame.Entity {


    public ServerAlienShotEntity(ServerGame game, double x, double y) {
        super(game, 10, 10, x, y);
        this.type = ServerGame.EntityType.ALIEN_SHOT;
        moveSpeed = 300;
    }

    public void setHorizontalMovement(double dx){
        this.dx = dx;
    }

    @Override
    public void tick() {
        setX(getX()+dx/ Server.TICKS_PER_SECOND);
        setY(getY()+moveSpeed/ Server.TICKS_PER_SECOND);
        if (getY() > 600 || getY() < -50 || getX() < -50 || getX() > 850){ this.game.removeEntity(this.getId()); }
    }

    @Override
    public void handleCollision(ServerGame.Entity otherEntity) {
        if (otherEntity instanceof ServerPlayerShipEntity) {
            game.removeEntity(this.getId());

        }
    }
}
