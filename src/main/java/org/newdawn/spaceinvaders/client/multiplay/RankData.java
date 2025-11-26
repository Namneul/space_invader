package org.newdawn.spaceinvaders.client.multiplay;

import java.io.Serializable;

public class RankData implements Serializable {
    private String username;
    private int score;

    public RankData(String username, int score){
        this.username = username;
        this.score = score;
    }

    public String getUsername(){ return username; }
    public int getScore(){ return score; }
}
