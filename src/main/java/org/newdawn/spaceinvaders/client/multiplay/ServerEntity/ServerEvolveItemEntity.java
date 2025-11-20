package org.newdawn.spaceinvaders.client.multiplay.ServerEntity;

import org.newdawn.spaceinvaders.client.multiplay.ServerGame;

public class ServerEvolveItemEntity extends Entity {



    public ServerEvolveItemEntity(ServerGame game, double x, double y) {
        super(game, 29, 26, x, y);
        moveSpeed = 200;
        dy = moveSpeed;
        this.type = EntityType.ITEM;
    }

    @Override
    public void tick() {
        super.tick();
        if (getY()>700){
            game.removeEntity(this.getId());
        }

    }

    @Override
    public void handleCollision(Entity otherEntity) {
        if (otherEntity instanceof ServerPlayerShipEntity){
            game.removeEntity(this.getId());
        }
    }
}
