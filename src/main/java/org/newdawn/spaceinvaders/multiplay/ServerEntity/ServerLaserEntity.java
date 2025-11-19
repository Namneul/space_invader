package org.newdawn.spaceinvaders.multiplay.ServerEntity;

import org.newdawn.spaceinvaders.multiplay.Entity;
import org.newdawn.spaceinvaders.multiplay.EntityType;
import org.newdawn.spaceinvaders.multiplay.ServerGame;

public class ServerLaserEntity extends Entity {

    private final ServerBossEntity owner;
    private final long duration = 4000; // 레이저 지속 시간 (4초)
    private final long creationTime;

    public ServerLaserEntity(ServerGame game, ServerBossEntity owner) {
        super(game, 144, 800, 0, 0);
        this.owner = owner;
        this.type = EntityType.LASER;
        this.creationTime = System.currentTimeMillis();
        updatePosition(); // 생성 즉시 위치 업데이트
    }

    @Override
    public void tick() {
        // 1. 보스가 죽었거나, 지속 시간이 다 되면 레이저는 사라진다.
        if (owner == null || System.currentTimeMillis() - creationTime > duration) {
            game.removeEntity(this.getId());
            return;
        }
        // 2. 매 틱마다 보스의 위치에 맞춰 자신의 위치를 갱신한다.
        updatePosition();
    }

    private void updatePosition() {
        // 보스의 중앙 하단에 위치하도록 좌표 설정
        this.x = owner.getX() + (owner.getWidth() / 2) - (this.getWidth() / 2);
        this.y = owner.getY() + owner.getHeight() / 2;
    }

    @Override
    public void handleCollision(Entity otherEntity) {
        // 레이저는 '지속 피해' 판정이라 tick()에서 처리하는 게 아니라,
        // ServerGame의 중앙 루프가 PlayerShip과의 충돌을 감지하고 처리하도록 한다.
        // 따라서 이 메소드는 비워둬도 괜찮지만, 혹시 모를 다른 충돌을 위해 남겨둔다.
    }
}