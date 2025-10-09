package org.newdawn.spaceinvaders.multiplay;

import java.io.Serializable;

public class LoginResponse implements Serializable {
    private boolean success;
    private String username;

    LoginResponse(boolean success, String id){
        this.success = success;
        this.username = id;
    }

    public boolean isSuccesss(){
        return success;
    }

    public String getUsername(){
        return username;
    }
}
