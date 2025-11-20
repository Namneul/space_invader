package org.newdawn.spaceinvaders.multiplay;

import java.io.Serializable;

public class PlayerInput implements Serializable {
    public enum Action{
        MOVE_LEFT,
        MOVE_RIGHT,
        FIRE,
        STOP,
        SKIP_STAGE
    }

    private Action action;

    public PlayerInput(Action action){
        this.action = action;
    }
    public  Action getAction(){
        return action;
    }
}
