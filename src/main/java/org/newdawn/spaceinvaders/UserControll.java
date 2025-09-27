package org.newdawn.spaceinvaders;

import java.util.HashMap;

public class UserControll {
    HashMap<String, User> users = new HashMap<>();
    public void AddUser(User user){
        users.put(user.Id, user);
    }

    public String GetUser(String id){
        return users.get(id).Id;
    }
}
