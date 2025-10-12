package org.newdawn.spaceinvaders.multiplay.stage;

import org.newdawn.spaceinvaders.multiplay.ServerEntity.ServerAlienEntity;
import org.newdawn.spaceinvaders.multiplay.ServerGame;

import java.util.TreeMap;

public class Stage5 extends Stage {
    @Override
    public void initialize(ServerGame game, TreeMap<Integer, ServerGame.Entity> entities) {
        int alienCount = 0;
            for (int x = 0; x < 12; x++) { // 열 증가
                ServerAlienEntity alien = new ServerAlienEntity(game, 100 + (x * 50), 50);
                alien.setMoveSpeed(120); // 느린 속도
                entities.put(alien.getId(), alien);
                alienCount++;
            }
        game.setAlienCount(alienCount);
    }

    @Override
    public String getStageName() {
        return "Stage 5";
    }
}