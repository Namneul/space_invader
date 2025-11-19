package org.newdawn.spaceinvaders.multiplay.ServerEntity;

import org.newdawn.spaceinvaders.multiplay.Entity;
import org.newdawn.spaceinvaders.multiplay.EntityType;
import org.newdawn.spaceinvaders.multiplay.Server;
import org.newdawn.spaceinvaders.multiplay.ServerGame;

public class ServerShotEntity extends Entity {


    private final int ownerId;
    private int upgradeLevel;
    private int damage = 50;

    public ServerShotEntity(ServerGame serverGame, double x, double y, int ownerId, int upgradeLevel) {
        super(serverGame,10,10, x, y);
        this.type = EntityType.SHOT;
        this.ownerId = ownerId;
        this.upgradeLevel = upgradeLevel;
        moveSpeed = 300;
    }

    @Override
    public void tick() {
        setY(getY()-moveSpeed/ Server.TICKS_PER_SECOND);
        if (getY()<0){this.game.removeEntity(this.getId());}

    }

    public int getUpgradeLevel(){ return upgradeLevel; }

    public int getOwnerId(){ return ownerId; }

    public int getDamage(){
        return damage*(getUpgradeLevel()+1);
    }

    @Override
    public void handleCollision(Entity otherEntity) {
    }
}
