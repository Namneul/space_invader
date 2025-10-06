package org.newdawn.spaceinvaders.multiplay;

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

    public ClientHandler(Server server,ServerGame serverGame, Socket socket, int playershipId) {
        try {
            this.socket = socket;
            this.server = server;
            this.serverGame = serverGame;
            this.outputStream = new ObjectOutputStream(socket.getOutputStream());
            this.inputStream = new ObjectInputStream(socket.getInputStream());
            this.playershipId = playershipId;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        while (socket.isConnected()) {
            try {
                PlayerInput input = (PlayerInput) inputStream.readObject();
                serverGame.processPlayerInput(this.playershipId, input);

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