package org.newdawn.spaceinvaders.multiplay;

import java.io.Serializable;
import java.util.Map;

public class GameState implements Serializable {

    public enum GameStatus {PLAYING, GAME_OVER, STAGE_CLEAR}

    private Map<Integer, Entity> entities;
    private final int currentScore;
    private final int remainingLives;
    private final GameStatus status;

    public GameState(Map<Integer, Entity> entities, int score, int lives, GameStatus status){
        this.entities = entities;
        this.currentScore = score;
        this.remainingLives = lives;
        this.status = status;
    }

    public Map<Integer, Entity> getEntities(){
        return entities;
    }
    public int getCurrentScore(){
        return currentScore;
    }
    public int getRemainingLives(){
        return remainingLives;
    }
    public GameStatus getStatus(){
        return status;
    }
}
