package org.newdawn.spaceinvaders.stage;

import org.newdawn.spaceinvaders.Game;
import org.newdawn.spaceinvaders.entity.AlienEntity;
import org.newdawn.spaceinvaders.entity.Entity;

import java.util.ArrayList;

public class Stage5 extends Stage {
    @Override
    public void initialize(Game game, ArrayList<Entity> entities) {
        int alienCount = 0;
        for (int row = 0; row < 5; row++) { // 행 증가
            for (int x = 0; x < 12; x++) { // 열 증가
                Entity alien = new AlienEntity(game, 100 + (x * 50), 50 + row * 30);
                alien.setHorizontalMovement(90); // 속도 증가
                entities.add(alien);
                alienCount++;
            }
        }
        game.setAlienCount(alienCount);
    }

    @Override
    public String getStageName() {
        return "Stage 5";
    }
}