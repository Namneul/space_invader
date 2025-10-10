package org.newdawn.spaceinvaders.multiplay.stage;

import org.newdawn.spaceinvaders.multiplay.ServerAlienEntity;
import org.newdawn.spaceinvaders.multiplay.ServerGame;
import java.util.TreeMap;

public class Stage1 extends Stage {
    @Override
    public void initialize(ServerGame game, TreeMap<Integer, ServerGame.Entity> entities) {
        int alienCount = 0;
        for (int row = 0; row < 1; row++) {
            for (int x = 0; x < 10; x++) {
                ServerAlienEntity alien = new ServerAlienEntity(game, 100 + (x * 50), 50 + row * 30);
                alien.setMoveSpeed(75); // 느린 속도
                entities.put(alien.getId(), alien);
                alienCount++;
            }
        }
        game.setAlienCount(alienCount); // Game 클래스에 에일리언 수 전달
    }

    @Override
    public String getStageName() {
        return "Stage 1";
    }
}