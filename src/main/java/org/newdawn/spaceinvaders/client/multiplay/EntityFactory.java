package org.newdawn.spaceinvaders.client.multiplay;

import org.newdawn.spaceinvaders.client.multiplay.ServerEntity.*;
import org.newdawn.spaceinvaders.multiplay.ServerEntity.*;

import java.util.Random;

public class EntityFactory {

    private final ServerGame game;
    private final EntityManager manager;
    private final Random random = new Random();

    public EntityFactory(ServerGame game, EntityManager manager) {
        this.game = game;
        this.manager = manager;
    }
//스테이지 엔티티 생성
    public ServerAlienEntity createAlien(double x, double y, double moveSpeed, int hp, boolean isAttacking) {
        ServerAlienEntity alien = new ServerAlienEntity(game, (int)x, (int)y);
        alien.setMoveSpeed(moveSpeed);
        alien.setHP(hp);
        alien.setAttacking(isAttacking);
        manager.addEntity(alien);
        return alien;
    }

    public ServerReflectAlienEntity createReflectAlien(double x, double y, double moveSpeed) {
        ServerReflectAlienEntity reflectAlien = new ServerReflectAlienEntity(game, x, y);
        reflectAlien.setMoveSpeed(moveSpeed);
        manager.addEntity(reflectAlien);
        return reflectAlien;
    }
    //플레이어 관련 생성
    public ServerPlayerShipEntity createPlayerShip(double x, double y) {
        ServerPlayerShipEntity playerShip = new ServerPlayerShipEntity(game, x, y);
        manager.addEntity(playerShip);
        return playerShip;
    }

    public ServerShotEntity createPlayerShot(ServerPlayerShipEntity playerShip) {
        int owner = playerShip.getId();
        int shipUpgradeCount = playerShip.getUpgradeCount();
        ServerShotEntity shot = new ServerShotEntity(game, playerShip.getX(), playerShip.getY(), owner, shipUpgradeCount);
        manager.addEntity(shot);
        return shot;
    }
    //와계인 보스 관련 생성
    public ServerAlienShotEntity createAlienShot(double x, double y) {
        ServerAlienShotEntity shot = new ServerAlienShotEntity(game, x, y);
        manager.addEntity(shot);
        return shot;
    }

    public ServerEvolveItemEntity createItemDrop(double x, double y) {
        ServerEvolveItemEntity item = new ServerEvolveItemEntity(game, x, y);
        manager.addEntity(item);
        return item;
    }

    public ServerMeteoriteEntity createMeteor() {
        int randomX = random.nextInt(800);
        ServerMeteoriteEntity meteorite = new ServerMeteoriteEntity(game, randomX, -50);
        manager.addEntity(meteorite);
        return meteorite;
    }
    //보스 생성
    public ServerBossEntity createBoss(double x, double y) {
        ServerBossEntity boss = new ServerBossEntity(game, (int)x, (int)y);
        manager.addEntity(boss);
        return boss;
    }

    public ServerAlienEntity createBossMinion(double x, double y) {
        ServerAlienEntity minion = new ServerAlienEntity(game, (int)x, (int)y);
        minion.setHP(50);
        manager.addEntity(minion);
        return minion;
    }

    public ServerAlienShotEntity createBossShotgunShot(double x, double y, double dx) {
        ServerAlienShotEntity shot = new ServerAlienShotEntity(game, x, y);
        shot.setHorizontalMovement(dx);
        manager.addEntity(shot);
        return shot;
    }

    public ServerLaserEntity createBossLaser(ServerBossEntity boss) {
        ServerLaserEntity laser = new ServerLaserEntity(game, boss);
        manager.addEntity(laser);
        return laser;
    }
}
