package org.newdawn.spaceinvaders.client.multiplay.communication;

import java.io.Serializable;

public class LoginRequest implements Serializable {
    private String username;
    private String password;

    public LoginRequest(String id, String pw){
        this.username = id;
        this.password = pw;
    }

    public String getUsername(){
        return username;
    }

    public String getPassword(){
        return password;
    }
}

