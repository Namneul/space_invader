package org.newdawn.spaceinvaders;

public class User {
    public String Id;
    public int Score=0;
    public void increaseScore() {
        Score++;
    }
    public void compareScore(LoginFrame lf) {
        if(lf.login.getScore(Id)<Score) {
            lf.login.insertScore(Id, Score);
        }
    }

    public void resetScore() {
        Score=0;
    }
}
