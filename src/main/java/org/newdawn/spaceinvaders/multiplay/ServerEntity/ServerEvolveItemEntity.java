package org.newdawn.spaceinvaders.multiplay.ServerEntity;

import org.newdawn.spaceinvaders.multiplay.Server;
import org.newdawn.spaceinvaders.multiplay.ServerGame;

public class ServerEvolveItemEntity extends ServerGame.Entity {



    public ServerEvolveItemEntity(ServerGame game, double x, double y) {
        super(game, 10, 10, x, y);
        moveSpeed = 200;
        this.type = ServerGame.EntityType.ITEM;
    }

    @Override
    public void tick() {
        setY(getY()+moveSpeed/ Server.TICKS_PER_SECOND);
        if (getY()>700){
            game.removeEntity(this.getId());
        }

    }

    @Override
    public void handleCollision(ServerGame.Entity otherEntity) {
        if (otherEntity instanceof ServerPlayerShipEntity){
            game.removeEntity(this.getId());
        }
    }
}
