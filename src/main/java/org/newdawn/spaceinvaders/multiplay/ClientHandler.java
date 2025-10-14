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
            if (!joined) {
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
                    LoginRequest req = (LoginRequest) receivedInput;
                    boolean ok = loginHost.login(req.getUsername(), req.getPassword());

                    // ▼▼▼ 여기가 핵심! ▼▼▼
                    if (ok) {
                        // 1. 로그인에 성공하면, 이 핸들러에 연결된 플레이어의 이름을 가져온다.
                        String newName = req.getUsername();

                        // 2. 서버의 중앙 명단(playerDataMap)에 있는 플레이어 정보를 찾아서
                        //    이름을 새 이름으로 '확실하게' 업데이트한다.
                        server.getPlayerDataMap().computeIfPresent(this.playershipId, (id, playerData) -> {
                            playerData.setName(newName); // PlayerData 객체의 이름을 변경
                            return playerData; // 변경된 PlayerData 객체를 반환
                        });

                        System.out.println("로그인 성공! 플레이어 ID " + this.playershipId + "의 이름이 '" + newName + "'(으)로 업데이트 되었습니다.");
                    }

                    // 3. 클라이언트에게 로그인 성공 여부를 알려준다.
                    outputStream.writeObject(new LoginResponse(ok, req.getUsername()));
                    continue; // 로그인 처리는 끝났으므로 다음 루프로 넘어간다.
                } else if (receivedInput instanceof SignUpRequest) {
                    SignUpRequest req = (SignUpRequest) receivedInput;
                    boolean ok = loginHost.signUp(req.getUsername(), req.getPassword());
                    if (ok) {
                        this.pendingUsername = req.getUsername(); // 매핑은 로그인/조인 시!
                    }
                    outputStream.writeObject(new SignUpResponse(ok, ok ? "Sign up successful!" : "Username already exists"));
                } else if (receivedInput instanceof RankRequest) {
                    RankResponse response = new RankResponse(loginHost.getAllScore());
                    outputStream.writeObject(response);
                } else if (receivedInput instanceof PlayerInput) {
                    // 로그인(= pendingUsername 세팅) 전에는 조인/스폰 금지
                    if (!joined) {
                        if (pendingUsername == null) {
                            // 로그인 아직 안 됨: 입력만 무시(또는 큐에 임시 저장)
                            // System.out.println("입력 무시: 로그인 전");
                            continue;
                        }
                        // 로그인 완료라면 여기서 조인(보조 안전장치)
                        joined = true;
                        if (this.playershipId < 0) {
                            this.playershipId = serverGame.spawnPlayerEntity();
                        }
                        server.getPlayerDataMap().put(this.playershipId, new PlayerData(pendingUsername));
                        System.out.println("Server: PlayerInput 시 조인. Id=" + this.playershipId + ", name=" + pendingUsername);
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

    public void sendUpdate(Object state){
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

    private void applyNameToPlayer(String name) {
        if (this.playershipId < 0) return; // 아직 스폰 전이면 조인 시 반영됨
        server.getPlayerDataMap().compute(this.playershipId, (id, old) -> {
            if (old == null) return new PlayerData(name);
            old.setName(name);               // PlayerData#setName 사용
            return old;
        });
        System.out.println("[NAME] id=" + this.playershipId + ", name=" + name);
    }



}