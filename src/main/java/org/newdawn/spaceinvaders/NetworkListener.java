package org.newdawn.spaceinvaders;

import org.newdawn.spaceinvaders.multiplay.GameState;
import org.newdawn.spaceinvaders.multiplay.communication.LoginResponse;
import org.newdawn.spaceinvaders.multiplay.communication.RankResponse;
import org.newdawn.spaceinvaders.multiplay.communication.SignUpResponse;

public interface NetworkListener {

    void onGameStateUpdate(GameState newState);

    void onVictory();

    void onLoginResponse(LoginResponse response);

    void onSignUpResponse(SignUpResponse response);

    void onRankResponse(RankResponse response);

    void onDisconnected(String reason);
}
