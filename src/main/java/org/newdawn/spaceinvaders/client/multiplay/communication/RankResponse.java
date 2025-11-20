package org.newdawn.spaceinvaders.client.multiplay.communication;

import org.newdawn.spaceinvaders.client.multiplay.RankData;

import java.io.Serializable;
import java.util.ArrayList;

public class RankResponse implements Serializable {
    private ArrayList<RankData> ranking;

    public RankResponse(ArrayList<RankData> ranking){
        this.ranking = ranking;
    }

    public ArrayList<RankData> getRanking(){ return ranking; }
}
