package org.newdawn.spaceinvaders;


import org.newdawn.spaceinvaders.multiplay.GameState;
import org.newdawn.spaceinvaders.multiplay.ServerEntity.*;
import org.newdawn.spaceinvaders.multiplay.ServerGame;

import java.awt.*;
import java.awt.image.BufferStrategy;
import java.util.TreeMap;

public class GameRenderer {

    private Sprite[] shipSprite = new Sprite[4];
    private Sprite[] shotSprite = new Sprite[4];
    private Sprite[] alienFrames = new Sprite[4];
    private Sprite[] meteorFrames = new Sprite[16];
    private Sprite redHeartSprite;
    private Sprite greyHeartSprite;
    private Sprite mainBackground;
    private Sprite gameBackground;
    private Sprite itemSprite;
    private Sprite alienShotSprite;
    private Sprite reflectAlienSprite;
    private Sprite bossSprite;
    private Sprite bossLaserSprite;
    private Sprite bossChargingSprite;

    private BufferStrategy strategy;
    private GameState currentGameState;
    private static final int MAX_LIVES = 3; // UI 그리기를 위한 상수

    public GameRenderer(){
        loadSprites();
    }
    public void setStrategy(BufferStrategy strategy){
        this.strategy = strategy;
    }
    public void render(GameState state){
        this.currentGameState = state;

        if (strategy == null) return;

        Graphics2D g = (Graphics2D) strategy.getDrawGraphics();
        renderGame(g);

        g.dispose();
        strategy.show();
    }


    private void loadSprites(){
        SpriteStore store = SpriteStore.get();
        shipSprite[0] = store.getSprite("sprites/ship/ship.gif");
        shipSprite[1] = store.getSprite("sprites/ship/shiptype1.png");
        shipSprite[2] = store.getSprite("sprites/ship/shiptype2.png");
        shipSprite[3] = store.getSprite("sprites/ship/shiptype3.png");

        shotSprite[0] = store.getSprite("sprites/shots/shot0.png");
        shotSprite[1] = store.getSprite("sprites/shots/shot1.png");
        shotSprite[2] = store.getSprite("sprites/shots/shot2.png");
        shotSprite[3] = store.getSprite("sprites/shots/shot3.png");

        alienFrames[0] = store.getSprite("sprites/alien.gif");
        alienFrames[1] = store.getSprite("sprites/alien2.gif");
        alienFrames[2] = alienFrames[0];
        alienFrames[3] = store.getSprite("sprites/alien3.gif");
        redHeartSprite = store.getSprite("sprites/heart_red.png");
        greyHeartSprite = store.getSprite("sprites/heart_grey.png");
        mainBackground = store.getSprite("mainBackground.png");
        itemSprite = store.getSprite("sprites/gems_db16.png");
        alienShotSprite = store.getSprite("sprites/alienshot.png");
        reflectAlienSprite = store.getSprite("sprites/ReflectAlien.png");
        gameBackground = store.getSprite("gameBackground.png");
        for (int i = 0; i < 16; i++) {
            meteorFrames[i] = store.getSprite("sprites/meteor/b1000"+i+".png");
        }
        bossSprite = store.getSprite("sprites/boss.png");
        bossChargingSprite = store.getSprite("sprites/boss_charging.png");
        bossLaserSprite = store.getSprite("sprites/boss_laser.png");
    }

    private void renderGame(Graphics2D g){
        g.setColor(Color.black);
        g.fillRect(0, 0, 800, 600); // 배경 색
        gameBackground.draw(g,0,0);

        if (currentGameState == null){
            drawWaitingScreen(g);
        } else{
            drawGameState(g);
        }
    }
    private void drawWaitingScreen(Graphics2D g) {
        g.setColor(Color.WHITE);
        g.setFont(new Font("Serif", Font.BOLD, 30));
        String waitMsg = "Waiting for another player...";
        int strWidth = g.getFontMetrics().stringWidth(waitMsg);
        g.drawString(waitMsg, (800 - strWidth) / 2, 300);
    }

    private void drawGameState(Graphics2D g) {
        drawEntities(g);
        drawHud(g);
    }

    private void drawEntities(Graphics2D g) {
        java.util.Map<Integer, ServerGame.Entity> entitiesToDraw = currentGameState.getEntities();
        if (entitiesToDraw == null) {
            return;
        }

        for (ServerGame.Entity entity : entitiesToDraw.values()) {
            drawEntity(g, entity);
        }
    }
    private void drawHud(Graphics2D g) {
        g.setColor(Color.WHITE);
        g.setFont(new Font("Serif", Font.BOLD, 20));
        if (currentGameState != null) {
            g.drawString("Score: " + currentGameState.getCurrentScore(), 650, 580);
            for (int i = 0; i < MAX_LIVES; i++) {
                Sprite heart = (i < currentGameState.getRemainingLives()) ? redHeartSprite : greyHeartSprite;
                heart.draw(g, 10 + (i * (redHeartSprite.getWidth() + 5)), 10);
            }
        }
    }

    private void drawEntity(Graphics2D g, ServerGame.Entity entity) {
        Sprite spriteToDraw = null;

        switch (entity.getType()) {
            case PLAYER:
                ServerPlayerShipEntity ship = (ServerPlayerShipEntity) entity;
                spriteToDraw = shipSprite[ship.getUpgradeCount()];
                break;
            case ALIEN:
                drawAlien(g, entity);
                break;
            case REFLECT_ALIEN:
                spriteToDraw = this.reflectAlienSprite;
                break;
            case SHOT:
                ServerShotEntity shot = (ServerShotEntity) entity;
                spriteToDraw = shotSprite[shot.getUpgradeLevel()];
                break;
            case ITEM:
                spriteToDraw = this.itemSprite;
                break;
            case ALIEN_SHOT:
                spriteToDraw = this.alienShotSprite;
                break;
            case METEOR:
                int frameNumber = ((ServerMeteoriteEntity) entity).getFrameNumber();
                spriteToDraw = this.meteorFrames[frameNumber];
                break;
            case BOSS:
                drawBoss(g, entity);
                break;
            case LASER:
                spriteToDraw = this.bossLaserSprite;
                break;
            default:
                break;
        }

        if (spriteToDraw != null) {
            spriteToDraw.draw(g, (int) entity.getX(), (int) entity.getY());
        }
    }
    private void drawAlien(Graphics2D g, ServerGame.Entity entity) {
        int frame = ((ServerAlienEntity) entity).getFrameNumber();
        Sprite spriteToDraw = this.alienFrames[frame];
        spriteToDraw.draw(g, (int) entity.getX(), (int) entity.getY());

        int barWidth = 40;
        int barHeight = 3;
        int barx = (int) entity.getX();
        int bary = (int) entity.getY() + (int) entity.getHeight() + 2;

        int alienMaxHp = entity.getMaxHP();
        int alienCurrentHp = entity.getCurrentHP();
        double healthPercent = (double) alienCurrentHp / alienMaxHp;

        g.setColor(Color.red);
        g.fillRect(barx, bary, barWidth, barHeight);

        g.setColor(Color.green);
        g.fillRect(barx, bary, (int) (barWidth * healthPercent), barHeight);
    }

    private void drawBoss(Graphics2D g, ServerGame.Entity entity) {
        int bossFrame = ((ServerBossEntity) entity).getFrameNumber();
        Sprite baseSprite = this.bossSprite;
        Sprite effectSprite = (bossFrame == 1) ? this.bossChargingSprite : null;

        if (baseSprite != null) {
            baseSprite.draw(g, (int) entity.getX(), (int) entity.getY());
            if (effectSprite != null) {
                int effectX = (int) entity.getX() + (baseSprite.getWidth() / 2) - (effectSprite.getWidth() / 2);
                int effectY = (int) entity.getY() + (baseSprite.getHeight() / 2) - (effectSprite.getHeight() / 2);
                effectSprite.draw(g, effectX, effectY);
            }
        }
        int maxHP = entity.getMaxHP();
        int currentHP = entity.getCurrentHP();

        if (maxHP > 0) {
            int bossBarWidth = 100;
            int bossBarHeight = 10;
            int barX = (int) entity.getX() + (baseSprite.getWidth() / 2) - (bossBarWidth / 2);
            int barY = (int) entity.getY() - 15;

            g.setColor(Color.RED);
            g.fillRect(barX, barY, bossBarWidth, bossBarHeight);
            double bossHealthPercent = (double) currentHP / maxHP;
            g.setColor(Color.GREEN);
            g.fillRect(barX, barY, (int) (bossBarWidth * bossHealthPercent), bossBarHeight);
            g.setColor(Color.WHITE);
            g.drawRect(barX, barY, bossBarWidth, bossBarHeight);
        }
    }

    public Sprite getMainBackground(){
        return this.mainBackground;
    }
}
