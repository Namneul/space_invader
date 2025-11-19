package org.newdawn.spaceinvaders.multiplay.ServerEntity;

import org.newdawn.spaceinvaders.multiplay.*;

import java.awt.Rectangle;
import org.newdawn.spaceinvaders.multiplay.GameRules;

public class ServerBossEntity extends Entity {


    private enum Pattern { SUMMON_MINIONS, SHOTGUN_BLAST, LASER_BEAM }
    private Pattern currentPattern;
    private long lastPatternTime;
    private long patternCooldown = 5000;

    private boolean isCharging = false;
    private boolean isFiringLaser = false;
    private long laserPhaseStartTime;
    private final Rectangle laserHitbox;
    private int frameNumber;

    private GameRules rules;

    public ServerBossEntity(ServerGame game, int x, int y) {
        super(game, 120, 100, x, y); // 네 Entity 생성자 그대로 사용
        maxHP = 5000;
        currentHP = maxHP;
        this.moveSpeed = 50;
        this.dx = moveSpeed;
        this.type = EntityType.BOSS;

        this.lastPatternTime = System.currentTimeMillis();
        chooseNextPattern();
        this.laserHitbox = new Rectangle();
        this.rules = game.getGameRules();
    }

    private void chooseNextPattern() {
        int rand = (int) (Math.random() * 3);
        if (rand == 0) currentPattern = Pattern.SUMMON_MINIONS;
        else if (rand == 1) currentPattern = Pattern.SHOTGUN_BLAST;
        else currentPattern = Pattern.LASER_BEAM;
    }

    @Override
    public void tick() {
        if (dx<0 && getX()<10 || dx>0 && getX()>700){
            this.game.requestBossLogicUpdate();
        }
        if (isCharging) {
            frameNumber = 1;
            if (System.currentTimeMillis() - laserPhaseStartTime > 2000) {
                isCharging = false;
                factory.createBossLaser(this);

                laserPhaseStartTime = System.currentTimeMillis();
                chooseNextPattern();
            }
            return;
        } else frameNumber = 0;

        if (isFiringLaser) {
            if (System.currentTimeMillis() - laserPhaseStartTime > 4000) {
                isFiringLaser = false;
                factory.createBossLaser(this);
                lastPatternTime = System.currentTimeMillis();
                chooseNextPattern();
            }
            return;
        }

        // 일반 이동 및 패턴 실행 타이머
        if (!isCharging && !isFiringLaser) {
            super.tick();
            if (System.currentTimeMillis() - lastPatternTime > patternCooldown) {
                executePattern();
            }
        }
    }

    public int getFrameNumber(){return frameNumber; }

    public void doLogic(){
        dx = -dx;
    }

    public void hit(Entity otherEntity,int dmg){
        this.currentHP -= dmg;
        if (currentHP <= 0){
            game.notifyBossKilled(((ServerShotEntity) otherEntity).getOwnerId());
            game.removeEntity(this.getId());
        }
        game.removeEntity(otherEntity.getId());
        if (Math.random()<0.2){
            factory.createItemDrop(this.x, this.y);
        }
    }


    private void executePattern() {
        switch (currentPattern) {
            case SUMMON_MINIONS:
                if (rules != null) {
                    rules.incrementAlienCount(1); // 보스 본체 카운트 (기존 로직 유지)
                    for (int i = 0; i < 5; i++) {
                        factory.createBossMinion(this.x - 100 + (i * 50), this.y + 50);
                        rules.incrementAlienCount(1); // 하수인 카운트 증가
                    }
                }
                break;
            case SHOTGUN_BLAST:
                for (int i = -2; i <= 2; i++) {
                    factory.createBossShotgunShot(this.x + 45.0, this.y + 100.0, i * 40.0);
                }
                break;
            case LASER_BEAM:
                laserBeam(); // 이건 상태 변경이라 보스가 직접 처리
                break;
        }
        lastPatternTime = System.currentTimeMillis();
        chooseNextPattern();
    }

    // 레이저 패턴은 상태 변경이므로 보스가 직접 관리
    private void laserBeam() {
        isCharging = true;
        laserPhaseStartTime = System.currentTimeMillis();
    }

    @Override
    public void handleCollision(Entity otherEntity) {
        if (otherEntity instanceof ServerShotEntity) {
            hit(otherEntity,((ServerShotEntity)otherEntity).getDamage());
        } else if (otherEntity instanceof ServerPlayerShipEntity) {
            game.notifyDeath(otherEntity.getId());
        }
    }

}