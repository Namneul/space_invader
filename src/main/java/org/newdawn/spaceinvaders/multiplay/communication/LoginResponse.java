package org.newdawn.spaceinvaders.multiplay.communication;

import java.io.Serializable;

public class LoginResponse implements Serializable {
    private boolean success;
    private String username;

    public LoginResponse(boolean success, String id){
        this.success = success;
        this.username = id;
    }

    public boolean isSuccess(){
        return success;
    }

    public String getUsername(){
        return username;
    }
}
