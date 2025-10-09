package org.newdawn.spaceinvaders.multiplay;

import java.io.Serializable;

public class SignUpResponse implements Serializable {
    private boolean success;
    private String message;

    public SignUpResponse(boolean success, String message){
        this.success = success;
        this.message = message;
    }

    public boolean isSuccess(){
        return success;
    }

    public String getMessage(){
        return message;
    }
}
