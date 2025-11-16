package org.newdawn.spaceinvaders.multiplay;

import org.newdawn.spaceinvaders.multiplay.ServerEntity.*;
import java.io.Serializable;
import java.util.Map;
import java.util.logging.*;

public class ServerGame {

//    private int alienCount;
//    private int currentStageIndex;  // 현재 스테이지 인덱스
//    private Stage currentStage;
    private boolean logicUpdateRequested = false;
    private boolean bossLogicUpdateRequested = false;

    private Server server;
//    private ArrayList<Stage> stages;
//    private final Random random = new Random();
//    private boolean bossClear = false;
    Logger logger = Logger.getLogger(getClass().getName());

    private final EntityManager entityManager;
    private final EntityFactory entityFactory;
    private final StageManager stageManager;
    private final GameRules gameRules;


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


    public abstract  static class Entity implements Serializable {
        protected transient ServerGame game;
        private int id;
        protected double x;
        protected double y;
        private double width;
        private double height;
        protected double dx;
        protected double dy;
        protected EntityType type;
        protected double moveSpeed;
        protected int currentHP;
        protected int maxHP;



        protected Entity(final ServerGame game,double width, double height, double x, double y) {
            this.game = game;
            this.id = this.game.getNextAvailableId();
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
        }

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
        this.entityManager = new EntityManager();
        this.entityFactory = new EntityFactory(this, this.entityManager);
        this.stageManager = new StageManager(this, entityManager, entityFactory);
        this.gameRules = new GameRules(this, entityManager, entityFactory, stageManager);
        this.stageManager.initializeFirstStage();

    }

    public java.util.Map<Integer, Entity> getEntities(){
        return entityManager.getEntities();
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

    public void bossSummonsMinions(int bossX, int bossY) {
        logger.info("보스 패턴: 하수인 소환!");
        gameRules.incrementAlienCount(1);
        for (int i = 0; i < 5; i++) {
            entityFactory.createBossMinion(bossX - 100 + (i * 50), bossY + 50);
            gameRules.incrementAlienCount(1);
        }
    }

    public void bossFiresShotgun(int bossX, int bossY) {
        logger.info("보스 패턴: 샷건 발사!");
        for (int i = -2; i <= 2; i++) {

            entityFactory.createBossShotgunShot(bossX + 45.0, bossY + 100.0, i * 40.0);
        }
    }

    public void bossFiresLaser(ServerBossEntity boss) {
        entityFactory.createBossLaser(boss);
    }

    public int getNextAvailableId(){
        return entityManager.getNextAvailableId();
    }

}

