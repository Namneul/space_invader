package org.newdawn.spaceinvaders.client;

public class User {
    public String Id;
    public String Password;
    public int Score=0;
    public void increaseScore() {
        Score++;
    }
    public void compareScore(LoginFrame lf) {
        if(lf.getLogin().getScore(Id)<Score) {
            lf.getLogin().insertScore(Id, Score);
        }
    }

    public void resetScore() {
        Score=0;
    }
}
