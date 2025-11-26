package org.newdawn.spaceinvaders.multiplay;

package org.newdawn.spaceinvaders.client.multiplay;

public class PlayerData {
    public String id;
    public String password;
    public int score;
    public int lives;

    public PlayerData(String id){
        this.score = 0;
        this.lives = 3;
    }

    public String  getId(){ return id;}

    public void setName(String name){this.id = name;}

    public int getScore(){ return score; }

    public int getLives(){ return lives; }

    public void increaseScore() { score++; }

    public void increaseBossKilledScore(){ score += 5000; }

    public void decreaseLives() { lives--; }
}

