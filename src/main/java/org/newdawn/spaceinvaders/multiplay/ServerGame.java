package org.newdawn.spaceinvaders.multiplay;

import org.newdawn.spaceinvaders.multiplay.ServerEntity.*;
import org.newdawn.spaceinvaders.multiplay.stage.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Random;
import java.util.TreeMap;
import java.util.logging.*;

public class ServerGame {

    private volatile boolean isGameLoopRunning = false;
    private int alienCount;
    private int currentStageIndex;  // 현재 스테이지 인덱스
    private Stage currentStage;
    public enum GameMode{MAIN_MENU, SINGLEPLAY, MULTIPLAY}
    private boolean logicUpdateRequested = false;
    private boolean bossLogicUpdateRequested = false;
    private ArrayList<Integer> removeList = new ArrayList<>();
    private Server server;
    private int smallestAvailableId = 0;
    private TreeMap<Integer, Entity> entities;
    private ArrayList<Stage> stages;
    private final Random random = new Random();
    private boolean bossClear = false;
    Logger logger = Logger.getLogger(getClass().getName());


    public enum EntityType{
        PLAYER,
        ALIEN,
        REFLECT_ALIEN,
        SHOT,
        ALIEN_SHOT,
        ITEM,
        METEOR,
        BOSS,
        LASER
    }


    public static abstract class Entity implements Serializable {
        protected transient ServerGame game;
        private int id;
        protected double x, y;
        private double width, height;
        protected double dx;
        protected double dy;
        protected EntityType type;
        protected double moveSpeed;
        protected int currentHP;
        protected int maxHP;



        public Entity(final ServerGame game,double width, double height, double x, double y) {
            this.game = game;
            this.id = this.game.getSmallestAvailableId();
            this.width = width;
            this.height = height;
            this.x = x;
            this.y = y;
        }

        public void setHorizontalMovement(double dx){
            this.dx = dx;
        }

        public int getCurrentHP(){return currentHP; }
        public int getMaxHP(){return maxHP; }

        public final int getId() {
            return id;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public double getWidth(){ return this.width; }

        public double getHeight(){ return this.height; }

        public void setX(final double x) {
            this.x = x;
        }

        public void setY(final double y) {
            this.y = y;
        }

        public void setMoveSpeed(double moveSpeed){
            this.moveSpeed = moveSpeed;
        }

        public double getMoveSpeed(){ return moveSpeed; }

        public void tick(){
            this.x += this.dx / Server.TICKS_PER_SECOND;
            this.y += this.dy / Server.TICKS_PER_SECOND;
        };

        public EntityType getType(){
            return this.type;
        }

        public abstract void handleCollision(Entity otherEntity);

        public boolean isColliding(Entity otherEntity) {

            return getX() < otherEntity.getX() + otherEntity.getWidth() && getX() + getWidth() > otherEntity.getX()
                    && getY() < otherEntity.getY() + otherEntity.getHeight()
                    && getY() + getHeight() > otherEntity.getY()
                    && this != otherEntity;
        }
    }


    public ServerGame(Server server){
        this.server = server;
        entities = new TreeMap<Integer, Entity>();
    }

    public java.util.Map<Integer, Entity> getEntities(){
        return entities;
    }

    public void removeEntity(final int id){

        removeList.add(id);
    }

    public void tick(){
        final TreeMap<Integer, Entity> entitiesCopy = new TreeMap<>(entities);

        // 2. 모든 엔티티의 상태를 업데이트합니다.
        tickEntities(entitiesCopy);

        // 3. 필요시 운석을 스폰합니다.
        spawnMeteorsIfNeeded();

        // 4. 요청된 외계인 로직을 업데이트합니다.
        updateAlienLogic();
        // 5. 요청된 보스 로직을 업데이트합니다.
        updateBossLogic();

        // 6. 엔티티 간의 충돌을 감지하고 처리합니다.
        handleCollisions(entitiesCopy);

        // 7. 제거 목록에 포함된 엔티티를 정리합니다.
        removeDeadEntities();
    }

    private void tickEntities(java.util.Map<Integer, Entity> entitiesToTick){
        for (final Entity entity: entitiesToTick.values()) {
            entity.tick();
        }
    }

    private void spawnMeteorsIfNeeded(){
        if(currentStageIndex > 2 && Math.random()<0.003){
            spawnMeteor();
        }
    }

    private void updateAlienLogic() {
        if(logicUpdateRequested){
            for(Entity entity: entities.values()){
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
            for (Entity entity: entities.values()){
                if (entity instanceof ServerBossEntity serverBossEntity){
                    serverBossEntity.doLogic();
                }
            }
            this.bossLogicUpdateRequested = false;
        }
    }

    private void handleCollisions(java.util.Map<Integer, Entity> entitiesToCheck) {
        // 이중 루프는 여전히 O(n^2)이지만, 이제 별도 메소드로 분리되었습니다.
        for (final Entity entity1 : entitiesToCheck.values()) {
            for (final Entity entity2 : entitiesToCheck.values()) {
                if (entity1.isColliding(entity2)) {
                    entity1.handleCollision(entity2);
                }
            }
        }
    }

    private void removeDeadEntities() {
        for (Integer id: removeList){
            entities.remove(id);
        }
        removeList.clear();
    }

    public void alienFires(Entity alien){
        ServerAlienShotEntity shot = new ServerAlienShotEntity(this, alien.getX(), alien.getY());
        entities.put(shot.getId(), shot);
    }

    public void notifyAlienKilled(Entity alien, int killerId) {

        PlayerData killerData = server.getPlayerDataMap().get(killerId);
        if (killerData != null){
            killerData.increaseScore();
        }

        alienCount--;
        if (alienCount <= 0){
            for(Entity entity: entities.values()){
                if (entity instanceof ServerReflectAlienEntity){
                    removeEntity(entity.getId());
                }
            }
            notifyWin();
        }

        if(Math.random()<0.2){
            itemDrop(alien);
        }
        // if there are still some aliens left then they all need to get faster, so
        // speed up all the existing aliens
        for (Entity entity: entities.values()){
            if (entity instanceof ServerAlienEntity || entity instanceof ServerReflectAlienEntity) {
                // speed up by 2%
                entity.setMoveSpeed(entity.getMoveSpeed()*1.02);
            }
        }
    }

    public boolean isBossClear(){return bossClear;}

   public void notifyDeath(int deadPlayerId) {

        PlayerData deadPlayerData = server.getPlayerDataMap().get(deadPlayerId);
        if (deadPlayerData != null){
            deadPlayerData.decreaseLives();

            if (deadPlayerData.getLives() <= 0){
                removeEntity(deadPlayerId);
                    server.getLoginHost().insertScore(deadPlayerData.getId(), deadPlayerData.getScore());
            } else {
                Entity ship = entities.get(deadPlayerId);
                if (ship != null){
                    ship.setX(370);
                    ship.setY(550);
                }
            }
        }
    }

    public void notifyWin() {
        // 현재 스테이지가 5스테이지(인덱스 4)의 첫 웨이브였다면, 보스를 생성합니다.
        if (currentStageIndex == 4) {
            // 보스가 아직 없다면 보스를 생성
            boolean bossExists = false;
            for (Entity e : entities.values()) {
                if (e instanceof ServerBossEntity) {
                    bossExists = true;
                    break;
                }
            }
            if (!bossExists) {
                logger.info("[서버 로그] 5스테이지 첫 웨이브 클리어. 보스를 생성합니다.");
                ServerBossEntity boss = new ServerBossEntity(this, 350, 50);
                entities.put(boss.getId(), boss);
                return; // 다음 스테이지로 넘어가지 않고 보스전을 시작합니다.
            }
        }

        currentStageIndex++; // 다음 스테이지로 인덱스를 증가시킵니다.

        // 만약 마지막 스테이지까지 모두 클리어했다면
        if (currentStageIndex >= stages.size()) {
            logger.info("[서버 로그] 모든 스테이지 클리어! 게임에서 승리했습니다.");
            // 점수 저장 로직
            for (PlayerData data : server.getPlayerDataMap().values()) {
                server.getLoginHost().insertScore(data.getId(), data.getScore());
            }
            this.bossClear = true; // 게임 승리 플래그를 설정합니다.
        }
        // 다음 스테이지가 남아있다면
        else {
           logger.log(Level.INFO,"[서버 로그] 다음 스테이지({0})를 시작합니다.", (currentStageIndex + 1));
            currentStage = stages.get(currentStageIndex);
            currentStage.initialize(this, entities); // 다음 스테이지의 적들을 생성합니다.
        }
    }

    public void itemDrop(Entity alien){
        ServerEvolveItemEntity item = new ServerEvolveItemEntity(this, alien.getX(), alien.getY());
        entities.put(item.getId(), item);
    }

    private void loadStages(){
        stages = new ArrayList<>();
        stages.add(new Stage1());
        stages.add(new Stage2());
        stages.add(new Stage3());
        stages.add(new Stage4());
        stages.add(new Stage5());
    }

    public void setAlienCount(int alienCount){
        this.alienCount = alienCount;
    }

    public int spawnPlayerEntity() {
        ServerPlayerShipEntity playerShip = new ServerPlayerShipEntity(this, 370, 550);
        entities.put(playerShip.getId(), playerShip);

        server.getPlayerDataMap().computeIfAbsent(playerShip.getId(), id -> new PlayerData("Gues- "+id));
        logger.log(Level.INFO,"Server: 플레이어 생성 완료. 총 엔티티 수: {0}",entities.size());
        return playerShip.getId();
    }

    public void requestLogicUpdate(){
        logicUpdateRequested = true;
    }
    public void requestBossLogicUpdate(){
        bossLogicUpdateRequested = true;
    }

    public void processPlayerInput(int playerShipId, PlayerInput receivedInput){

        Entity playerShip = entities.get(playerShipId);
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
                tryToFire((ServerPlayerShipEntity) playerShip);
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
        int owner = playerShip.getId();
        int shipUpgradeCount = playerShip.getUpgradeCount();
        ServerShotEntity shot = new  ServerShotEntity(this, playerShip.getX(), playerShip.getY(), owner, shipUpgradeCount);
        entities.put(shot.getId(), shot);
    }

    public void initializeFirstStage(){
        if (stages == null || stages.isEmpty()){
            loadStages();
        }

        currentStageIndex = 0;
        currentStage = stages.get(currentStageIndex);
        currentStage.initialize(this, entities);
        logger.info("ServerGame: "+currentStage.getStageName()+" initialized.");
    }

    public void spawnMeteor(){
        int randomX = random.nextInt(800);

        ServerMeteoriteEntity meteorite = new ServerMeteoriteEntity(this, randomX, -50);
        entities.put(meteorite.getId(), meteorite);
    }

    public void addEntity(Entity entity){
        entities.put(entity.getId(), entity);
    }


    public void notifyBossKilled(int killerId) {
        PlayerData killerData = server.getPlayerDataMap().get(killerId);
        if (killerData != null) {
            killerData.increaseBossKilledScore();
            server.getLoginHost().insertScore(killerData.getId(), killerData.getScore());
            logger.info("보스 처치! 최종 승리!");
        }

        this.bossClear = true;
    }

    public void bossSummonsMinions(int bossX, int bossY) {
        logger.info("보스 패턴: 하수인 소환!");
        alienCount++;
        for (int i = 0; i < 5; i++) {
            // 기존 방식대로 ServerGame이 직접 생성하고 추가합니다.
            ServerAlienEntity minion = new ServerAlienEntity(this, bossX - 100 + (i * 50), bossY + 50);
            minion.setHP(50);
            entities.put(minion.getId(), minion);
            alienCount++;
        }
    }

    public void bossFiresShotgun(int bossX, int bossY) {
        logger.info("보스 패턴: 샷건 발사!");
        for (int i = -2; i <= 2; i++) {
            // 기존 방식대로 ServerGame이 직접 생성하고 추가합니다.
            ServerAlienShotEntity shot = new ServerAlienShotEntity(this, bossX + 45.0, bossY + 100.0);
            shot.setHorizontalMovement(i * 40.0);
            entities.put(shot.getId(), shot);
        }
    }

    public void bossFiresLaser(ServerBossEntity boss) {
        // ServerGame이 직접 레이저를 생성하고 entities 맵에 추가합니다.
        ServerLaserEntity laser = new ServerLaserEntity(this, boss);
        entities.put(laser.getId(), laser);
    }



    private int getSmallestAvailableId() {return smallestAvailableId++;}

}

