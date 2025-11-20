package org.newdawn.spaceinvaders.client.multiplay.ServerEntity;

import org.newdawn.spaceinvaders.client.multiplay.EntityFactory;
import org.newdawn.spaceinvaders.client.multiplay.Server;
import org.newdawn.spaceinvaders.client.multiplay.ServerGame;

import java.io.Serializable;

public abstract class Entity implements Serializable {
    protected transient ServerGame game;
    protected transient EntityFactory factory;
    private int id;
    protected double x, y, width, height, dx, dy, moveSpeed;
    protected EntityType type;
    protected int currentHP, maxHP;

    protected Entity(final ServerGame game, double width, double height, double x, double y) {
        this.game = game;
        this.factory = game.getEntityFactory(); // GameRules에도 접근이 필요하면 game.getGameRules()도 추가
        this.id = this.game.getNextAvailableId();
        this.width = width;
        this.height = height;
        this.x = x;
        this.y = y;
    }

    public void setHorizontalMovement(double dx){ this.dx = dx; }
    public int getCurrentHP(){return currentHP; }
    public int getMaxHP(){return maxHP; }
    public final int getId() { return id; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getWidth(){ return this.width; }
    public double getHeight(){ return this.height; }
    public void setX(final double x) { this.x = x; }
    public void setY(final double y) { this.y = y; }
    public void setMoveSpeed(double moveSpeed){ this.moveSpeed = moveSpeed; }
    public double getMoveSpeed(){ return moveSpeed; }
    public EntityType getType(){ return this.type; }

    public void tick(){
        this.x += this.dx / Server.TICKS_PER_SECOND;
        this.y += this.dy / Server.TICKS_PER_SECOND;
    }

    public abstract void handleCollision(Entity otherEntity);

    public boolean isColliding(Entity otherEntity) {
        return getX() < otherEntity.getX() + otherEntity.getWidth() && getX() + getWidth() > otherEntity.getX()
                && getY() < otherEntity.getY() + otherEntity.getHeight()
                && getY() + getHeight() > otherEntity.getY()
                && this != otherEntity;
    }
}
