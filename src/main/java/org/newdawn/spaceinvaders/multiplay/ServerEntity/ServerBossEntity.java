package org.newdawn.spaceinvaders.multiplay.ServerEntity;

import org.newdawn.spaceinvaders.multiplay.PlayerData;
import org.newdawn.spaceinvaders.multiplay.Server;
import org.newdawn.spaceinvaders.multiplay.ServerGame;
import java.awt.Rectangle;

public class ServerBossEntity extends ServerGame.Entity {

    private int maxHP = 5000;
    private int currentHP;

    private enum Pattern { SUMMON_MINIONS, SHOTGUN_BLAST, LASER_BEAM }
    private Pattern currentPattern;
    private long lastPatternTime;
    private long patternCooldown = 5000;

    private boolean isCharging = false;
    private boolean isFiringLaser = false;
    private long laserPhaseStartTime;
    private final Rectangle laserHitbox;
    private int frameNumber;

    public ServerBossEntity(ServerGame game, int x, int y) {
        super(game, 100, 100, x, y); // 네 Entity 생성자 그대로 사용
        this.currentHP = maxHP;
        this.moveSpeed = 50;
        this.dx = moveSpeed;
        this.type = ServerGame.EntityType.BOSS;

        this.lastPatternTime = System.currentTimeMillis();
        chooseNextPattern();
        this.laserHitbox = new Rectangle();
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
            this.game.requestLogicUpdate();
        }
        if (isCharging) {
            frameNumber = 1;
            if (System.currentTimeMillis() - laserPhaseStartTime > 2000) {
                isCharging = false;
                game.bossFiresLaser(this);

                laserPhaseStartTime = System.currentTimeMillis();
                chooseNextPattern();
            }
            return;
        } else frameNumber = 0;

        if (isFiringLaser) {
            if (System.currentTimeMillis() - laserPhaseStartTime > 4000) {
                isFiringLaser = false;
                game.bossFiresLaser(this);
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

    public void hit(ServerGame.Entity otherEntity,int dmg){
        this.currentHP -= dmg;
        if (currentHP <= 0){
            game.notifyBossKilled(this, ((ServerShotEntity) otherEntity).getOwnerId());
            game.removeEntity(this.getId());
        }
        game.removeEntity(otherEntity.getId());
        if (Math.random()<0.2){
            game.itemDrop(this);
        }
    }


    private void executePattern() {
        switch (currentPattern) {
            case SUMMON_MINIONS:
                // ▼▼▼ 여기가 핵심! ServerGame에 '요청'만 한다 ▼▼▼
                game.bossSummonsMinions((int)this.x, (int)this.y);
                break;
            case SHOTGUN_BLAST:
                // ▼▼▼ 여기도 '요청'만 한다 ▼▼▼
                game.bossFiresShotgun((int)this.x, (int)this.y);
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
    public void handleCollision(ServerGame.Entity otherEntity) {
        if (otherEntity instanceof ServerShotEntity) {
            hit(otherEntity,((ServerShotEntity)otherEntity).getDamage());
        } else if (otherEntity instanceof ServerPlayerShipEntity) {
            game.notifyDeath(otherEntity.getId());
        }
    }

}