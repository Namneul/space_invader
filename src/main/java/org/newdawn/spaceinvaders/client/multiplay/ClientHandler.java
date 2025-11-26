package org.newdawn.spaceinvaders.client.multiplay;

import org.newdawn.spaceinvaders.client.multiplay.communication.*;

import java.io.*;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    private static final Logger logger = Logger.getLogger(ClientHandler.class.getName());


    public ClientHandler(Server server,ServerGame serverGame, Socket socket, int playershipId, Login loginHost) {
        try {
            this.socket = socket;
            this.server = server;
            this.serverGame = serverGame;
            this.outputStream = new ObjectOutputStream(socket.getOutputStream());
            this.outputStream.flush();
            this.inputStream = new ObjectInputStream(socket.getInputStream());
            this.playershipId = playershipId;
            this.loginHost = loginHost;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        try {
           handleSinglePlayerAutoJoin();
            while (!socket.isClosed()) {
                Object receivedInput = inputStream.readObject();
                processMessage(receivedInput);

            }
        } catch (IOException | ClassNotFoundException e) {
            // 클라이언트 연결이 끊기는 등 예외가 발생하면 루프가 종료되고 이 부분이 실행됩니다.
            serverGame.removeEntity(this.playershipId);
        } finally {
            cleanupResources();
        }
    }

    private void handleSinglePlayerAutoJoin() {
        if (!joined) {
            logger.info("[핸들러 로그] 싱글플레이어 자동 참가 로직 실행.");
            this.joined = true;
            this.playershipId = serverGame.spawnPlayerEntity();
            String name = "Player 1";
            server.getPlayerDataMap().putIfAbsent(this.playershipId, new PlayerData(name));
            logger.info("[핸들러 로그] 싱글플레이어 생성 완료.");
            server.onPlayerJoined(this);
        }
    }

    private void processMessage(Object receivedInput) throws IOException {
        if (receivedInput instanceof LoginRequest req) {
            handleLoginRequest(req);
        } else if (receivedInput instanceof SignUpRequest req) {
            handleSignUpRequest(req);
        } else if (receivedInput instanceof RankRequest) {
            handleRankRequest();
        } else if (receivedInput instanceof PlayerInput playerInput) {
            handlePlayerInput(playerInput);
        } else {
            logger.log(Level.WARNING, "알 수 없는 타입의 메시지 수신: {0}", receivedInput.getClass().getName());
        }
    }

    private void handleLoginRequest(LoginRequest req) throws IOException {
        boolean ok = loginHost.login(req.getUsername(), req.getPassword());
        if (ok) {
            String newName = req.getUsername();
            server.getPlayerDataMap().computeIfPresent(this.playershipId, (id, playerData) -> {
                playerData.setName(newName);
                return playerData;
            });
        }
        outputStream.writeObject(new LoginResponse(ok, req.getUsername()));
    }

    private void handleSignUpRequest(SignUpRequest req) throws IOException {
        boolean ok = loginHost.signUp(req.getUsername(), req.getPassword());
        if (ok) {
            this.pendingUsername = req.getUsername();
        }
        outputStream.writeObject(new SignUpResponse(ok, ok ? "Sign up successful!" : "Username already exists"));
    }

    private void handleRankRequest() throws IOException {
        RankResponse response = new RankResponse(loginHost.getAllScore());
        outputStream.writeObject(response);
    }

    private void handlePlayerInput(PlayerInput playerInput) {
        if (!joined) {
            if (pendingUsername == null) {
                return; // 아직 이름이 없으면 입력을 무시
            }
            joined = true;
            if (this.playershipId < 0) {
                this.playershipId = serverGame.spawnPlayerEntity();
            }
            server.getPlayerDataMap().put(this.playershipId, new PlayerData(pendingUsername));
            server.onPlayerJoined(this);
        }
        serverGame.processPlayerInput(this.playershipId, playerInput);
    }

    private void cleanupResources() {
        // finally 블록에 있던 복잡한 try-catch-ignored 로직
        try { if (inputStream != null) inputStream.close(); } catch (IOException ignored) { /* ignored */ }
        try { if (outputStream != null) outputStream.close(); } catch (IOException ignored) { /* ignored */ }
        try { if (socket != null) socket.close(); } catch (IOException ignored) { /* ignored */ }

        // 연결 종료 사실을 서버에 알리는 것도 여기서 처리
        server.onClientDisconnected(this);
    }

    public int getPlayershipId(){ return playershipId; }

    public void sendUpdate(Object state){
        try {
            logger.info("[핸들러 로그] 클라이언트로 GameState 업데이트 전송 시도...");
            outputStream.writeObject(state);
            outputStream.reset();
            outputStream.flush();
        } catch (IOException e) {
            try{
                socket.close();
            } catch (IOException ex){
                // intentionally ignored
            }
            server.onClientDisconnected(this);
        }
    }


}