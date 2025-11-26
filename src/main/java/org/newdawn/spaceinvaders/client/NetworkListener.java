package org.newdawn.spaceinvaders.client;

import org.newdawn.spaceinvaders.client.multiplay.GameState;
import org.newdawn.spaceinvaders.client.multiplay.communication.LoginResponse;
import org.newdawn.spaceinvaders.client.multiplay.communication.RankResponse;
import org.newdawn.spaceinvaders.client.multiplay.communication.SignUpResponse;

public interface NetworkListener {

    void onGameStateUpdate(GameState newState);

    void onVictory();

    void onLoginResponse(LoginResponse response);

    void onSignUpResponse(SignUpResponse response);

    void onRankResponse(RankResponse response);

    void onDisconnected(String reason);
}
