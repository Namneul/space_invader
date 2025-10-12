package org.newdawn.spaceinvaders.stage;

import org.newdawn.spaceinvaders.Game;
import org.newdawn.spaceinvaders.entity.AlienEntity;
import org.newdawn.spaceinvaders.entity.Entity;

import java.util.ArrayList;

public class Stage5 extends Stage {
    @Override
    public void initialize(Game game, ArrayList<Entity> entities) {
        int alienCount = 0;
        // 보스전 앞의 첫 웨이브: 12마리 한 줄
        for (int x = 0; x < 12; x++) {
            Entity alien = new AlienEntity(game, 100 + (x * 50), 50);
            ((AlienEntity) alien).setHP(200); // 5스테이지 잡몹 체력
            alien.setHorizontalMovement(120); // 속도 증가
            entities.add(alien);
            alienCount++;
        }
        game.setAlienCount(alienCount);
    }

    @Override
    public String getStageName() {
        return "Stage 5 - The Final Boss";
    }
}