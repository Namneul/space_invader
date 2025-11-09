package org.newdawn.spaceinvaders.multiplay;

import org.newdawn.spaceinvaders.multiplay.communication.*;
import java.io.*;
import java.net.Socket;
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
            if (!joined) {
                logger.info("[핸들러 로그] 싱글플레이어 자동 참가 로직 실행.");
                this.joined = true;
                this.playershipId = serverGame.spawnPlayerEntity();
                String name = "Player 1";
                server.getPlayerDataMap().putIfAbsent(this.playershipId, new PlayerData(name));
                logger.info("[핸들러 로그] 싱글플레이어 생성 완료.");
                server.onPlayerJoined(this);
            }
            while (!socket.isClosed()) {
                Object receivedInput = inputStream.readObject();

                if (receivedInput instanceof LoginRequest req) {
                    boolean ok = loginHost.login(req.getUsername(), req.getPassword());

                    if (ok) {
                        String newName = req.getUsername();

                        server.getPlayerDataMap().computeIfPresent(this.playershipId, (id, playerData) -> {
                            playerData.setName(newName); // PlayerData 객체의 이름을 변경
                            return playerData; // 변경된 PlayerData 객체를 반환
                        });
                    }

                    outputStream.writeObject(new LoginResponse(ok, req.getUsername()));
                } else if (receivedInput instanceof SignUpRequest req) {
                    boolean ok = loginHost.signUp(req.getUsername(), req.getPassword());
                    if (ok) {
                        this.pendingUsername = req.getUsername(); // 매핑은 로그인/조인 시!
                    }
                    outputStream.writeObject(new SignUpResponse(ok, ok ? "Sign up successful!" : "Username already exists"));
                } else if (receivedInput instanceof RankRequest) {
                    RankResponse response = new RankResponse(loginHost.getAllScore());
                    outputStream.writeObject(response);
                } else if (receivedInput instanceof PlayerInput playerInput) {
                    if (!joined) {
                        if (pendingUsername == null) {
                            continue;
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

            }
        } catch (IOException | ClassNotFoundException e) {
            // 클라이언트 연결이 끊기는 등 예외가 발생하면 루프가 종료되고 이 부분이 실행됩니다.
            serverGame.removeEntity(this.playershipId);
        } finally {
            // 루프가 완전히 끝났을 때 (정상 종료 또는 예외 발생 시) 딱 한 번만 실행됩니다.
            try { if (inputStream != null) inputStream.close(); } catch (IOException ignored) {
                // intentionally ignored
            }
            try { if (outputStream != null) outputStream.close(); } catch (IOException ignored) {
                // intentionally ignored
            }
            try { if (socket != null) socket.close(); } catch (IOException ignored) {
                // intentionally ignored
            }
            server.onClientDisconnected(this);
        }
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