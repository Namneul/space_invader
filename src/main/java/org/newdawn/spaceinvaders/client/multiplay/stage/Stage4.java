package org.newdawn.spaceinvaders.client.multiplay.stage;

import org.newdawn.spaceinvaders.client.multiplay.EntityFactory;
import org.newdawn.spaceinvaders.client.multiplay.EntityManager;
import org.newdawn.spaceinvaders.client.multiplay.ServerGame;

public class Stage4 extends Stage {
    @Override
    public void initialize(ServerGame game, EntityManager manager, EntityFactory factory) {
        int alienCount = 0;
        for (int row = 0; row < 3; row++) { // 행 증가
            for (int x = 0; x < 12; x++) { // 열 증가
                if ((x ==2 || x == 9) && row ==1){
                    factory.createReflectAlien(100 + (x * 50), 50 + row * 30, 90);
                } else{
                    factory.createAlien(100 + (x * 50), 50 + row * 30, 90, 200, true);
                    alienCount++;
                }
            }
        }
        game.setAlienCount(alienCount);
    }

    @Override
    public String getStageName() {
        return "Stage 4";
    }
}

