package org.newdawn.spaceinvaders.multiplay;

import org.newdawn.spaceinvaders.multiplay.communication.*;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class ClientHandler implements Runnable {

    private ServerGame serverGame;
    public static ArrayList<ClientHandler> clientHandlers = new ArrayList<>();
    private Socket socket;
    private Server server;
    private ObjectOutputStream outputStream;
    private ObjectInputStream inputStream;
    private int playershipId;
    private Login loginHost;

    public ClientHandler(Server server,ServerGame serverGame, Socket socket, int playershipId, Login loginHost) {
        try {
            this.socket = socket;
            this.server = server;
            this.serverGame = serverGame;
            this.outputStream = new ObjectOutputStream(socket.getOutputStream());
            this.inputStream = new ObjectInputStream(socket.getInputStream());
            this.playershipId = playershipId;
            this.loginHost = loginHost;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        while (socket.isConnected()) {
            try {
                Object receivedInput = inputStream.readObject();
                if (receivedInput instanceof LoginRequest){
                    LoginRequest request = (LoginRequest) receivedInput;
                    boolean success = loginHost.login(request.getUsername(), request.getPassword());
                    if (success){
                        PlayerData playerData = new PlayerData(request.getUsername());
                        server.getPlayerDataMap().put(this.playershipId, playerData);
                        System.out.println("Server: playerData for "+ request.getUsername());
                    }

                    LoginResponse response = new LoginResponse(success, request.getUsername());
                    outputStream.writeObject(response);

                } else if (receivedInput instanceof SignUpRequest) {
                    SignUpRequest request = (SignUpRequest) receivedInput;
                    boolean success = loginHost.signUp(request.getUsername(), request.getPassword());
                    if (success){
                        PlayerData playerData = new PlayerData(request.getUsername());
                        server.getPlayerDataMap().put(this.playershipId, playerData);
                    }
                    String message = success ? "Sgin up sccessful!" :"Username already exist";
                    SignUpResponse response = new SignUpResponse(success, message);
                    outputStream.writeObject(response);
                } else if (receivedInput instanceof RankRequest) {
                    RankResponse response = new RankResponse(loginHost.getAllScore());
                    outputStream.writeObject(response);
                } else{
                serverGame.processPlayerInput(this.playershipId, (PlayerInput) receivedInput);
                }
            } catch (IOException | ClassNotFoundException e) {
                serverGame.removeEntity(this.playershipId);
                break;
            }
        }
    }

    public void sendUpdate(GameState state){
        try {
            outputStream.writeObject(state);
            outputStream.reset();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}