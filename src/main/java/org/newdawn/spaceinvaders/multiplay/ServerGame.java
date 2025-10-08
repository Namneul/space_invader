package org.newdawn.spaceinvaders.multiplay;


import org.newdawn.spaceinvaders.Game;
import org.newdawn.spaceinvaders.Sprite;
import org.newdawn.spaceinvaders.SpriteStore;
import org.newdawn.spaceinvaders.SystemTimer;
import org.newdawn.spaceinvaders.entity.ShipEntity;

import java.awt.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ScheduledExecutorService;

public class ServerGame {

    private long serverFrame = 0;

    private final int WORLD_WIDTH = 800;

    private final int WORLD_HEIGHT = 600;

    private TreeMap<Integer, Integer> playerAndShipId;

    private boolean logicUpdateRequested = false;

    private ArrayList<Integer> removeList = new ArrayList<>();

    private boolean gameRunning = true;






    public enum EntityType{
        PLAYER,
        ALIEN,
        SHOT,
        ALIEN_SHOT
    }


    public static abstract class Entity implements Serializable {
        protected transient ServerGame game;
        private int id;
        protected double x, y;
        private double width, height;
        protected double dx;
        protected double dy;
        protected EntityType type;



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

    private int smallestAvailableId = 0;
    private TreeMap<Integer, Entity> entities;
    public ServerGame(){
        entities = new TreeMap<Integer, Entity>();
        initEntities();
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

