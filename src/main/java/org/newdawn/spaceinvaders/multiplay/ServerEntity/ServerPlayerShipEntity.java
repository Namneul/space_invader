package org.newdawn.spaceinvaders.multiplay.ServerEntity;


import org.newdawn.spaceinvaders.multiplay.ServerGame;

public class ServerPlayerShipEntity extends ServerGame.Entity {

    private long lastFireTime = 0;
    public int upgradeCount = 0;
    private boolean playerStunned = false;
    private long stunDuration = 0;
    private long stunStartTime = 0;

    public ServerPlayerShipEntity(ServerGame serverGame, double x, double y) {
        super(serverGame,30,30, x, y);
        this.type = ServerGame.EntityType.PLAYER;
        moveSpeed = 250;
        dx = moveSpeed;
    }


    @Override
    public void tick() {
        super.tick();
    }

    public void upgrade(){
        if (upgradeCount < 3){
            upgradeCount++;
        }
    }

    public void applyStun(long durationMs){
        if (!playerStunned){
            this.playerStunned = true;
            this.stunDuration = durationMs;
            this.stunStartTime = System.currentTimeMillis();
            this.dx = 0;
            System.out.println("Player stunned!");
        }
    }

    public boolean isPlayerStunned(){
        if (playerStunned) {
            long elapsedTime = System.currentTimeMillis() - stunStartTime;
            if (elapsedTime >= stunDuration) {
                this.playerStunned = false;
                System.out.println("Player " + getId() + " is no longer stunned.");
                return playerStunned;
            }
            return playerStunned;
        }
        return playerStunned;
    }

    public void resetUpgrade(){
        upgradeCount = 0;
    }
    public int getUpgradeCount(){
        return upgradeCount;
    }
    private void setPlayerStunned(){
        playerStunned = false;
    }


    public long getLastFireTime(){
        return lastFireTime;
    }
    public void setLastFireTime(){
        lastFireTime = System.currentTimeMillis();
    }

    @Override
    public void handleCollision(ServerGame.Entity otherEntity) {
        if (otherEntity instanceof ServerAlienShotEntity || otherEntity instanceof ServerAlienEntity || otherEntity instanceof  ServerReflectAlienEntity){
            game.notifyDeath(this.getId());
            resetUpgrade();
            setPlayerStunned();

        } else if (otherEntity instanceof ServerEvolveItemEntity) {
            game.removeEntity(otherEntity.getId());
            upgrade();
        } else if (otherEntity instanceof ServerMeteoriteEntity) {
            this.applyStun(1500);
        } else if (otherEntity instanceof ServerLaserEntity) {
            game.notifyDeath(this.getId());
        }
    }
}
