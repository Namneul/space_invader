package org.newdawn.spaceinvaders.multiplay;

import org.newdawn.spaceinvaders.LoginFrame;
import org.newdawn.spaceinvaders.multiplay.stage.*;
import java.awt.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.TreeMap;

public class ServerGame {

    private volatile boolean isGameLoopRunning = false;

    private int alienCount;

    private int currentStageIndex;  // 현재 스테이지 인덱스

    private Stage currentStage;

    public enum GameMode{MAIN_MENU, SINGLEPLAY, MULTIPLAY}

    private boolean logicUpdateRequested = false;

    private ArrayList<Integer> removeList = new ArrayList<>();

    private Server server;

    private int smallestAvailableId = 0;

    private TreeMap<Integer, Entity> entities;

    LoginFrame loginFrame = new LoginFrame();

    private ArrayList<Stage> stages;




    public enum EntityType{
        PLAYER,
        ALIEN,
        SHOT,
        ALIEN_SHOT,
        ITEM
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



        public Entity(final ServerGame game,double width, double height, double x, double y) {
            this.game = game;
            this.id = this.game.getSmallestAvailableId();
            this.width = width;
            this.height = height;
            this.x = x;
            this.y = y;
        }


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

        public abstract void tick();

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


    public class GameSettings {
        public int ALIEN_MOVESPEED = 75;
        public int SHOT_MOVESPEED = -300;
        public int SHIP_MOVESPEED = 150;
    }

    public ServerGame(Server server){
        this.server = server;
        entities = new TreeMap<Integer, Entity>();
    }

    public TreeMap<Integer, Entity> getEntities(){
        return entities;
    }

    public void removeEntity(final int id){

        removeList.add(id);
    }

    public void tick(){
        final TreeMap<Integer, Entity> entitiesCopy = new TreeMap<>(entities);
        for (final Entity entity: entitiesCopy.values()){
            entity.tick();
        }
        if (logicUpdateRequested){
            for (Entity entity: entities.values()){
                if (entity instanceof ServerAlienEntity){
                    ((ServerAlienEntity) entity).doLogic();
                }
            }
            this.logicUpdateRequested = false;
        }

        for (final Entity entity1 : entitiesCopy.values()) {
            for (final Entity entity2 : entitiesCopy.values()) {
                if (entity1.isColliding(entity2)) {
                    entity1.handleCollision(entity2);
                }
            }
        }
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
            notifyWin();
        }

        if(Math.random()<0.5){
            itemDrop(alien);
        }
        // if there are still some aliens left then they all need to get faster, so
        // speed up all the existing aliens
        for (Entity entity: entities.values()){
            if (entity instanceof ServerAlienEntity) {
                // speed up by 2%
                entity.setMoveSpeed(entity.getMoveSpeed()*1.02);
            }
        }
    }

   public void notifyDeath(int deadPlayerId) {

        PlayerData deadPlayerData = server.getPlayerDataMap().get(deadPlayerId);
        if (deadPlayerData != null){
            deadPlayerData.decreaseLives();

            if (deadPlayerData.getLives() <= 0){
                removeEntity(deadPlayerId);
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
        currentStageIndex++; // 다음 스테이지로 인덱스 증가
        // 만약 마지막 스테이지까지 클리어했다면
        if (currentStageIndex >= stages.size()) {
            System.out.println("All Clear!");
            for (PlayerData data:server.getPlayerDataMap().values()){
                server.getLoginHost().insertScore(data.getId(), data.getScore());
            }
        } else {
            // 다음 스테이지가 있다면, 새 스테이지 객체를 가져오고 게임을 다시 시작
            currentStage = stages.get(currentStageIndex);
            initEntities();
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
        System.out.println("Server: 플레이어 생성 완료. 총 엔티티 수: " + entities.size());
        return playerShip.getId();
    }

    public void requestLogicUpdate(){
        logicUpdateRequested = true;
    }

    public void processPlayerInput(int playerShipId, PlayerInput receivedInput){

        Entity playerShip = entities.get(playerShipId);
        if (playerShip == null){
            return;
        }
        switch (receivedInput.getAction()){
            case MOVE_LEFT:
                playerShip.setX(playerShip.getX()-5);
                break;
            case MOVE_RIGHT:
                playerShip.setX(playerShip.getX()+5);
                break;
            case FIRE:
                tryToFire((ServerPlayerShipEntity) playerShip);
                break;
            case STOP:
                break;
        }
    }

    public void tryToFire(ServerPlayerShipEntity playerShip){
        if (System.currentTimeMillis() - playerShip.getLastFireTime() < 500){
            return;
        }
        playerShip.setLastFireTime();
        int shipUpgradeCount = playerShip.getUpgradeCount();
        ServerShotEntity shot = new  ServerShotEntity(this, playerShip.getX(), playerShip.getY());
        entities.put(shot.getId(), shot);
    }


    private int getSmallestAvailableId() {return smallestAvailableId++;}

    private void initEntities(){
        for (int row = 0; row < 3; row++) {
            for (int x = 0; x < 12; x++) {
                ServerGame.Entity alien = new ServerAlienEntity(this, 100 + (x * 50), 50 + (row * 30));
                entities.put(alien.getId(), alien);
                System.out.println("Server: 외계인 생성 완료. 총 엔티티 수: " + entities.size());
            }
        }
    }
}

