package org.newdawn.spaceinvaders.stage;

import org.newdawn.spaceinvaders.Game;
import org.newdawn.spaceinvaders.entity.AlienEntity;
import org.newdawn.spaceinvaders.entity.Entity;
import org.newdawn.spaceinvaders.entity.ReflectAlienEntity;

import java.util.ArrayList;

public class Stage3 extends Stage {
    @Override
    public void initialize(Game game, ArrayList<Entity> entities) {
        int alienCount = 0;
        for (int row = 0; row < 3; row++) { // 행 증가
            for (int x = 0; x < 12; x++) {// 열 증가
                Entity alien;
                if ((x == 2 || x == 9) & row ==1) {
                        alien = new ReflectAlienEntity(game, 100 + (x*50),50+ row*30);
                        alien.setHorizontalMovement(90);
                }else{
                   alien = new AlienEntity(game, 100 + (x * 50), 50 + row * 30);
                   ((AlienEntity) alien).setHP(100);
                   alien.setHorizontalMovement(90);
                    alienCount++;
                }
                entities.add(alien);
            }
        }
        game.setAlienCount(alienCount);
    }

    @Override
    public String getStageName() {
        return "Stage 3";
    }
}