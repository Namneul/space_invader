package org.newdawn.spaceinvaders.multiplay;

import org.newdawn.spaceinvaders.multiplay.ServerEntity.*;
import java.io.Serializable;
import java.util.Map;
import java.util.logging.*;

public class ServerGame {

    private boolean logicUpdateRequested = false;
    private boolean bossLogicUpdateRequested = false;

    private Server server;

    Logger logger = Logger.getLogger(getClass().getName());

    private final EntityManager entityManager;
    private final EntityFactory entityFactory;
    private final StageManager stageManager;
    private final GameRules gameRules;

    public ServerGame(Server server){
        this.server = server;
        this.entityManager = new EntityManager();
        this.entityFactory = new EntityFactory(this, this.entityManager);
        this.stageManager = new StageManager(this, entityManager, entityFactory);
        this.gameRules = new GameRules(this, entityManager, entityFactory, stageManager);
        this.stageManager.initializeFirstStage();

    }

    public java.util.Map<Integer, Entity> getEntities(){
        return entityManager.getEntities();
    }

    public EntityFactory getEntityFactory(){
        return entityFactory;
    }

    public GameRules getGameRules(){
        return gameRules;
    }

    public Login getLoginHost() {
        return server.getLoginHost();
    }

    public Map<Integer, PlayerData> getPlayerDataMap() {
        return server.getPlayerDataMap();
    }

    public void removeEntity(final int id){
        entityManager.removeEntity(id);
    }

    public void tick(){
        entityManager.updateAll();

        stageManager.spawnMeteorsIfNeeded();

        // 4. 요청된 외계인 로직을 업데이트합니다.
        updateAlienLogic();
        // 5. 요청된 보스 로직을 업데이트합니다.
        updateBossLogic();
    }

    private void updateAlienLogic() {
        if(logicUpdateRequested){
            for(Entity entity: entityManager.getEntities().values()){
                if(entity instanceof ServerAlienEntity serverAlienEntity){
                    serverAlienEntity.doLogic();
                } else if(entity instanceof ServerReflectAlienEntity serverReflectAlienEntity){
                    serverReflectAlienEntity.doLogic();
                }
            }
            this.logicUpdateRequested = false;
        }
    }

    private void updateBossLogic() {
        if (bossLogicUpdateRequested){
            for (Entity entity: entityManager.getEntities().values()){
                if (entity instanceof ServerBossEntity serverBossEntity){
                    serverBossEntity.doLogic();
                }
            }
            this.bossLogicUpdateRequested = false;
        }
    }

    public void alienFires(Entity alien){
        entityFactory.createAlienShot(alien.getX(), alien.getY());
    }

    public void notifyAlienKilled(Entity alien, int killerId) {
        gameRules.notifyAlienKilled(alien, killerId);
    }

    public boolean isBossClear(){return gameRules.isBossClear();}

   public void notifyDeath(int deadPlayerId) {
        gameRules.notifyDeath(deadPlayerId);
    }

    public void handleGameWin() {
        // 점수 저장 로직
        gameRules.handleGameWin();
    }

    public void setAlienCount(int alienCount){
        gameRules.setAlienCount(alienCount);
    }

    public int spawnPlayerEntity() {
        ServerPlayerShipEntity playerShip = entityFactory.createPlayerShip(370, 550);

        server.getPlayerDataMap().computeIfAbsent(playerShip.getId(), id -> new PlayerData("Gues- "+id));
        logger.log(Level.INFO,"Server: 플레이어 생성 완료. 총 엔티티 수: {0}",entityManager.getEntities().size());
        return playerShip.getId();
    }

    public void requestLogicUpdate(){
        logicUpdateRequested = true;
    }
    public void requestBossLogicUpdate(){
        bossLogicUpdateRequested = true;
    }

    public void processPlayerInput(int playerShipId, PlayerInput receivedInput){
        gameRules.processPlayerInput(playerShipId, receivedInput);
    }

//    public void addEntity(Entity entity){
//        entityManager.addEntity(entity);
//    }


    public void notifyBossKilled(int killerId) {
        gameRules.notifyBossKilled(killerId);
    }

    public int getNextAvailableId(){
        return entityManager.getNextAvailableId();
    }

}

