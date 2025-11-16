package org.newdawn.spaceinvaders.multiplay;

import org.newdawn.spaceinvaders.multiplay.ServerEntity.ServerAlienEntity;
import org.newdawn.spaceinvaders.multiplay.ServerEntity.ServerPlayerShipEntity;
import org.newdawn.spaceinvaders.multiplay.ServerEntity.ServerReflectAlienEntity;
import org.newdawn.spaceinvaders.multiplay.ServerGame.Entity;
import java.util.logging.Logger;

public class GameRules {

    private final ServerGame game; // Server의 정보(DB 접근)가 필요할 때 사용
    private final EntityManager manager;
    private final EntityFactory factory;
    private final StageManager stageManager;

    private int alienCount;
    private boolean bossClear = false;
    private final Logger logger = Logger.getLogger(getClass().getName());

    public GameRules(ServerGame game, EntityManager manager, EntityFactory factory, StageManager stageManager) {
        this.game = game;
        this.manager = manager;
        this.factory = factory;
        this.stageManager = stageManager;
    }

    public void setAlienCount(int alienCount) {
        this.alienCount = alienCount;
    }

    public void incrementAlienCount(int amount) {
        this.alienCount += amount;
    }

    public boolean isBossClear() {
        return bossClear;
    }

    public void processPlayerInput(int playerShipId, PlayerInput receivedInput){
        Entity playerShip = manager.getEntity(playerShipId);
        if (playerShip == null){
            return;
        }

        ServerPlayerShipEntity playerShipEntity = (ServerPlayerShipEntity) playerShip;

        switch (receivedInput.getAction()){
            case MOVE_LEFT:
                if (!playerShipEntity.isPlayerStunned()){
                    playerShip.setHorizontalMovement(-playerShip.getMoveSpeed());
                }
                break;
            case MOVE_RIGHT:
                if (!playerShipEntity.isPlayerStunned()){
                    playerShip.setHorizontalMovement(playerShip.getMoveSpeed());
                }
                break;
            case FIRE:
                if (!playerShipEntity.isPlayerStunned()){
                    tryToFire(playerShipEntity);
                }
                break;
            case STOP:
                playerShip.setHorizontalMovement(0);
                break;
        }
    }

    public void tryToFire(ServerPlayerShipEntity playerShip){
        if (System.currentTimeMillis() - playerShip.getLastFireTime() < 500){
            return;
        }
        playerShip.setLastFireTime();
        factory.createPlayerShot(playerShip);
    }

    public void notifyAlienKilled(Entity alien, int killerId) {
        PlayerData killerData = game.getPlayerDataMap().get(killerId);
        if (killerData != null){
            killerData.increaseScore();
        }

        decrementAlienCount(); // 외계인 수 감소 및 승리 확인

        if(Math.random()<0.2){
            factory.createItemDrop(alien.getX(), alien.getY());
        }

        // 외계인 속도 증가
        for (Entity entity: manager.getEntities().values()){
            if (entity instanceof ServerAlienEntity || entity instanceof ServerReflectAlienEntity) {
                entity.setMoveSpeed(entity.getMoveSpeed()*1.02);
            }
        }
    }

    private void decrementAlienCount() {
        alienCount--;
        if (alienCount <= 0){
            // 맵에 남아있는 반사 외계인(총알)이 있다면 제거
            for(Entity entity: manager.getEntities().values()){
                if (entity instanceof ServerReflectAlienEntity){
                    manager.removeEntity(entity.getId());
                }
            }
            // 스테이지 관리자에게 다음 스테이지 진행을 요청
            stageManager.progressToNextStage();
        }
    }

    public void notifyDeath(int deadPlayerId) {
        PlayerData deadPlayerData = game.getPlayerDataMap().get(deadPlayerId);
        if (deadPlayerData != null){
            deadPlayerData.decreaseLives();

            if (deadPlayerData.getLives() <= 0){
                manager.removeEntity(deadPlayerId);
                // DB에 점수 저장
                game.getLoginHost().insertScore(deadPlayerData.getId(), deadPlayerData.getScore());
            } else {
                // 플레이어 부활
                Entity ship = manager.getEntity(deadPlayerId);
                if (ship != null){
                    ship.setX(370);
                    ship.setY(550);
                }
            }
        }
    }

    public void notifyBossKilled(int killerId) {
        PlayerData killerData = game.getPlayerDataMap().get(killerId);
        if (killerData != null) {
            killerData.increaseBossKilledScore();
            game.getLoginHost().insertScore(killerData.getId(), killerData.getScore());
            logger.info("보스 처치! 최종 승리!");
        }
        this.bossClear = true;
    }

    public void handleGameWin() {
        // 점수 저장 로직
        for (PlayerData data : game.getPlayerDataMap().values()) {
            game.getLoginHost().insertScore(data.getId(), data.getScore());
        }
        this.bossClear = true; // 게임 승리 플래그를 설정합니다.
    }

}
