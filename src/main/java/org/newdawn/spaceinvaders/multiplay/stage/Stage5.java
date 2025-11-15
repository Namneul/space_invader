package org.newdawn.spaceinvaders.multiplay.stage;

import org.newdawn.spaceinvaders.multiplay.EntityFactory;
import org.newdawn.spaceinvaders.multiplay.EntityManager;
import org.newdawn.spaceinvaders.multiplay.ServerEntity.ServerAlienEntity;
import org.newdawn.spaceinvaders.multiplay.ServerGame;

import java.util.TreeMap;

public class Stage5 extends Stage {
    @Override
    public void initialize(ServerGame game, EntityManager manager, EntityFactory factory) {
        int alienCount = 0;
            for (int x = 0; x < 12; x++) { // 열 증가
                factory.createAlien(100 + (x * 50), 50, 120, 200, true);
                alienCount++;
            }
        game.setAlienCount(alienCount);
    }

    @Override
    public String getStageName() {
        return "Stage 5";
    }
}