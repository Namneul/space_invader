package org.newdawn.spaceinvaders.multiplay;

import org.newdawn.spaceinvaders.multiplay.communication.*;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class ClientHandler implements Runnable {

    private ServerGame serverGame;
    private Socket socket;
    private Server server;
    private ObjectOutputStream outputStream;
    private ObjectInputStream inputStream;
    private int playershipId;
    private Login loginHost;
    private boolean joined = false;
    private String pendingUsername = null;
    private boolean isSinglePlayer;


    public ClientHandler(Server server,ServerGame serverGame, Socket socket, int playershipId, Login loginHost, boolean isSinglePlayer) {
        try {
            this.socket = socket;
            this.server = server;
            this.serverGame = serverGame;
            this.outputStream = new ObjectOutputStream(socket.getOutputStream());
            this.outputStream.flush();
            this.inputStream = new ObjectInputStream(socket.getInputStream());
            this.playershipId = playershipId;
            this.loginHost = loginHost;
            this.isSinglePlayer = isSinglePlayer;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        try {
            if (isSinglePlayer && !joined) {
                System.out.println("[핸들러 로그] 싱글플레이어 자동 참가 로직 실행.");
                this.joined = true;
                this.playershipId = serverGame.spawnPlayerEntity();
                String name = "Player 1";
                server.getPlayerDataMap().putIfAbsent(this.playershipId, new PlayerData(name));
                System.out.println("[핸들러 로그] 싱글플레이어 Id=" + this.playershipId + " 생성 완료.");
                server.onPlayerJoined(this);
            }
            while (!socket.isClosed()) {
                Object receivedInput = inputStream.readObject();

                if (receivedInput instanceof LoginRequest) {
                    LoginRequest request = (LoginRequest) receivedInput;
                    boolean success = loginHost.login(request.getUsername(), request.getPassword());
                    if (success) {
                        this.pendingUsername = request.getUsername();
                        System.out.println("Server: login ok for " + request.getUsername());
                    }

                    LoginResponse response = new LoginResponse(success, request.getUsername());
                    outputStream.writeObject(response);
                } else if (receivedInput instanceof SignUpRequest) {
                    SignUpRequest request = (SignUpRequest) receivedInput;
                    boolean success = loginHost.signUp(request.getUsername(), request.getPassword());
                    if (success) {
                        PlayerData playerData = new PlayerData(request.getUsername());
                        server.getPlayerDataMap().put(this.playershipId, playerData);
                    }
                    String message = success ? "Sgin up sccessful!" : "Username already exist";
                    SignUpResponse response = new SignUpResponse(success, message);
                    outputStream.writeObject(response);
                } else if (receivedInput instanceof RankRequest) {
                    RankResponse response = new RankResponse(loginHost.getAllScore());
                    outputStream.writeObject(response);
                } else if (receivedInput instanceof PlayerInput) {

                    if (!joined) {
                        joined = true;
                        if (this.playershipId < 0) {
                            int spawned = serverGame.spawnPlayerEntity();
                            this.playershipId = spawned;

                            String name = (pendingUsername != null) ? pendingUsername : "guest";
                            server.getPlayerDataMap().putIfAbsent(this.playershipId, new PlayerData(name));
                            System.out.println("Server: 플레이어 생성 완료. Id=" + this.playershipId + ", name=" + name);
                        }
                        server.onPlayerJoined(this);
                    }

                    serverGame.processPlayerInput(this.playershipId, (PlayerInput) receivedInput);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            // 클라이언트 연결이 끊기는 등 예외가 발생하면 루프가 종료되고 이 부분이 실행됩니다.
            serverGame.removeEntity(this.playershipId);
        } finally {
            // 루프가 완전히 끝났을 때 (정상 종료 또는 예외 발생 시) 딱 한 번만 실행됩니다.
            try { if (inputStream != null) inputStream.close(); } catch (IOException ignored) {}
            try { if (outputStream != null) outputStream.close(); } catch (IOException ignored) {}
            try { if (socket != null) socket.close(); } catch (IOException ignored) {}
            server.onClientDisconnected(this);
        }
    }

    public int getPlayershipId(){ return playershipId; }

    public void sendUpdate(GameState state){
        try {
            System.out.println("[핸들러 로그] 클라이언트로 GameState 업데이트 전송 시도...");
            outputStream.writeObject(state);
            outputStream.reset();
            outputStream.flush();
        } catch (IOException e) {
            try{
                socket.close();
            } catch (IOException ex){}
            server.onClientDisconnected(this);
        }
    }


}