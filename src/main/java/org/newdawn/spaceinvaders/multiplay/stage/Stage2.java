package org.newdawn.spaceinvaders.multiplay.stage;

import org.newdawn.spaceinvaders.multiplay.ServerEntity.ServerAlienEntity;
import org.newdawn.spaceinvaders.multiplay.ServerGame;
import java.util.TreeMap;

public class Stage2 extends Stage {
    @Override
    public void initialize(ServerGame game, TreeMap<Integer, ServerGame.Entity> entities) {
        int alienCount = 0;
        for (int row = 0; row < 3; row++) { // 행 증가
            for (int x = 0; x < 12; x++) { // 열 증가
                ServerAlienEntity alien = new ServerAlienEntity(game, 100 + (x * 50), 50 + row * 30);
                alien.setMoveSpeed(90); // 느린 속도
                alien.setHP(100);
                entities.put(alien.getId(), alien);
                alienCount++;
            }
        }
        game.setAlienCount(alienCount);
    }

    @Override
    public String getStageName() {
        return "Stage 2";
    }
}