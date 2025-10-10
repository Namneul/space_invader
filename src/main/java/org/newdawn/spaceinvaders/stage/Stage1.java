package org.newdawn.spaceinvaders.stage;

import org.newdawn.spaceinvaders.Game;
import org.newdawn.spaceinvaders.entity.AlienEntity;
import org.newdawn.spaceinvaders.entity.Entity;

import java.util.ArrayList;

public class Stage1 extends Stage {
    @Override
    public void initialize(Game game, ArrayList<Entity> entities) {
        int alienCount = 0;
        for (int row = 0; row < 3; row++) {
            for (int x = 0; x < 10; x++) {
                Entity alien = new AlienEntity(game, 100 + (x * 50), 50 + row * 30);
                ((AlienEntity)alien).setHP(50);
                alien.setHorizontalMovement(75); // 느린 속도
                entities.add(alien);
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