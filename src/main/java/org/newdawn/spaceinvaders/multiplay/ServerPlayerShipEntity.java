package org.newdawn.spaceinvaders.multiplay;

import org.newdawn.spaceinvaders.entity.AlienEntity;

public class ServerPlayerShipEntity extends ServerGame.Entity {

    private long lastFireTime = 0;
    public int upgradeCount = 0;
    private int damage = 50;

    public ServerPlayerShipEntity(ServerGame serverGame, double x, double y) {
        super(serverGame,30,30, x, y);
        this.type = ServerGame.EntityType.PLAYER;
    }


    @Override
    public void tick() {

    }

    public void upgrade(){
        if (upgradeCount < 3){
            upgradeCount++;
            damage += 50;
        }
    }

    public void resetUpgrade(){
        upgradeCount = 0;
    }
    public int getUpgradeCount(){
        return upgradeCount;
    }

    public long getLastFireTime(){
        return lastFireTime;
    }
    public void setLastFireTime(){
        lastFireTime = System.currentTimeMillis();
    }

    @Override
    public void handleCollision(ServerGame.Entity otherEntity) {
        if (otherEntity instanceof ServerAlienShotEntity || otherEntity instanceof ServerAlienEntity){
            game.notifyDeath();
            resetUpgrade();
        }

        if (otherEntity instanceof ServerEvolveItemEntity){
            game.removeEntity(otherEntity.getId());
            upgrade();
        }

    }
}
