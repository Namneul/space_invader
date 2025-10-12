package org.newdawn.spaceinvaders.entity;

import org.newdawn.spaceinvaders.Game;
import org.newdawn.spaceinvaders.Sprite;
import org.newdawn.spaceinvaders.SpriteStore;

import java.awt.*;

public class BossEntity extends Entity {
    private Game game;
    private int maxHP = 5000;
    private int currentHP;
    private double moveSpeed = 50;

    // 보스 패턴 관련 변수
    private enum Pattern { SUMMON_MINIONS, SHOTGUN_BLAST, LASER_BEAM }
    private Pattern currentPattern;
    private long lastPatternTime;
    private long patternCooldown = 4000; // 5초마다 패턴 변경

    // 순간이동 패턴 관련 변수
    private boolean isCharging = false;      // 충전 중인지 여부
    private boolean isFiringLaser = false;   // 레이저 발사 중인지 여부
    private long laserPhaseStartTime;        // 현재 패턴(충전 또는 발사) 시작 시간
    private Rectangle laserHitbox;

    private Sprite chargingSprite;
    private Sprite laserSprite;

    public BossEntity(Game game, int x, int y) {
        super("sprites/boss.png", x, y);
        this.game = game;
        this.currentHP = maxHP;
        this.dx = moveSpeed;
        this.lastPatternTime = System.currentTimeMillis();
        chooseNextPattern();

        chargingSprite = SpriteStore.get().getSprite("sprites/boss_charging.png");
        laserSprite = SpriteStore.get().getSprite("sprites/boss_laser.png");
        laserHitbox = new Rectangle();
    }

    private void chooseNextPattern() {
        int rand = (int) (Math.random() * 3);
        if (rand == 0) {
            currentPattern = Pattern.SUMMON_MINIONS;
        } else if (rand == 1) {
            currentPattern = Pattern.SHOTGUN_BLAST;
        } else {
            currentPattern = Pattern.LASER_BEAM;
        }
    }

    @Override
    public void move(long delta) {
        // 순간이동 패턴 처리
        if (isCharging) {
            // 2초간 충전 후 발사
            if (System.currentTimeMillis() - laserPhaseStartTime > 2000) {
                isCharging = false;
                isFiringLaser = true;
                laserPhaseStartTime = System.currentTimeMillis();
                this.dx *= 2; // 이동 속도 2배 증가
            }
            return; // 충전 중에는 다른 움직임 멈춤
        }

        if (isFiringLaser) {
            // 4초간 레이저 발사
            if (System.currentTimeMillis() - laserPhaseStartTime > 4000) {
                isFiringLaser = false;
                this.dx = moveSpeed; // 이동 속도 원래대로
                lastPatternTime = System.currentTimeMillis(); // 패턴 쿨타임 시작
                chooseNextPattern();
            } else {
                // 레이저 히트박스 업데이트 및 충돌 검사
                updateLaserHitbox();
                if (laserHitbox.intersects(game.getShip().getBounds())) {
                    game.notifyDeath();
                }
            }
        }

        // 일반 좌우 이동
        if ((dx < 0) && (x < 10)) {
            dx = -dx;
        }
        if ((dx > 0) && (x > 750)) {
            dx = -dx;
        }
        super.move(delta);

        // 패턴 실행
        if (System.currentTimeMillis() - lastPatternTime > patternCooldown) {
            executePattern();
        }
    }

    private void executePattern() {
        switch (currentPattern) {
            case SUMMON_MINIONS:
                summonMinions();
                break;
            case SHOTGUN_BLAST:
                shotgunBlast();
                break;
            case LASER_BEAM:
                laserBeam();
                break;
        }
        lastPatternTime = System.currentTimeMillis();
        chooseNextPattern();
    }

    private void summonMinions() {
        // 보스 양옆으로 AlienEntity 5마리 소환
        for (int i = 0; i < 5; i++) {
            AlienEntity minion = new AlienEntity(game, (int)this.x - 100 + (i * 50), (int)this.y + 50);
            minion.setHP(50); // 하수인의 체력은 낮게 설정
            game.addEntity(minion);
        }
    }

    private void shotgunBlast() {
        // 5발의 총알을 부채꼴 모양으로 발사
        for (int i = -2; i <= 2; i++) {
            AlienShotEntity shot = new AlienShotEntity(game, "sprites/alienshot.png", (int)this.x + 20, (int)this.y + 50);
            shot.setHorizontalMovement(i * 50); // 수평 속도를 다르게 하여 부채꼴 모양 생성
            game.addEntity(shot);
        }
    }

    private void laserBeam() {
        isCharging = true;
        laserPhaseStartTime = System.currentTimeMillis();
    }

    private void updateLaserHitbox() {
        int laserX = (int)this.x + (this.sprite.getWidth() / 2) - (laserSprite.getWidth() / 2);
        int laserY = (int)this.y + this.sprite.getHeight();
        laserHitbox.setBounds(laserX, laserY, laserSprite.getWidth(), laserSprite.getHeight());
    }

    // 보스 체력 감소
    public void takeDamage(int damage) {
        currentHP -= damage;
        if (currentHP <= 0) {
            game.removeEntity(this);
            game.notifyWin(); // 보스를 죽이면 게임 승리
        }
    }

    @Override
    public void draw(Graphics g) {
        super.draw(g);
        if (isCharging) {
            chargingSprite.draw(g, (int)this.x, (int)this.y);
        }

        // 레이저 발사 중일 때 레이저 그리기
        if (isFiringLaser) {
            int laserX = (int)this.x + (this.sprite.getWidth() / 2) - (laserSprite.getWidth() / 2);
            int laserY = (int)this.y + this.sprite.getHeight();
            laserSprite.draw(g, laserX, laserY);
        }
        // 체력 바 그리기
        int barWidth = 100;
        int barHeight = 10;
        int barX = (int)this.x + (sprite.getWidth()/2) - (barWidth/2);
        int barY = (int)this.y - 20;

        double healthPercent = (double)currentHP / maxHP;
        g.setColor(Color.RED);
        g.fillRect(barX, barY, barWidth, barHeight);
        g.setColor(Color.GREEN);
        g.fillRect(barX, barY, (int)(barWidth * healthPercent), barHeight);


    }

    @Override
    public void collidedWith(Entity other) {
        // 플레이어와 직접 충돌하면 플레이어 사망
        if (other instanceof ShipEntity) {
            game.notifyDeath();
        }
    }
}