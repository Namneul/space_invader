package org.newdawn.spaceinvaders.multiplay.communication;

import java.io.Serializable;

public class SignUpRequest implements Serializable {
    private String username;
    private String password;

    public SignUpRequest(String id, String pw){
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
