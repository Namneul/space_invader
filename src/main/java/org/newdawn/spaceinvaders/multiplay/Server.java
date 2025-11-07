package org.newdawn.spaceinvaders.multiplay;


import org.newdawn.spaceinvaders.multiplay.stage.Stage;
import org.newdawn.spaceinvaders.multiplay.stage.Stage1;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;


public class Server implements Runnable{

    Logger logger = Logger.getLogger(getClass().getName());
    public final static int TICKS_PER_SECOND = 120;
    public final static int DEFAULT_PORT_NUMBER = 12345;


    private ServerSocket serverSocket;
    private volatile boolean isRunning = true;
    private final ServerGame serverGame;
    private final ArrayList<ClientHandler> clientHandlers;
    protected Map<Integer, PlayerData> playerDataMap = new ConcurrentHashMap<>();
    private Login loginHost;
    private final int maxPlayers;
    private final java.util.Set<ClientHandler> joined = java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());
    private volatile boolean gameStarted = false;
    private List<ClientHandler> toRemove = new java.util.ArrayList<>();

    public Server(int port, int maxPlayers){
        this.serverGame = new ServerGame(this);
        this.loginHost = new Login();
        this.maxPlayers = maxPlayers;
        try {
            this.serverSocket = new ServerSocket(port);
            logger.info("Server started on port: "+port+" for "+maxPlayers);
        } catch (final IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        clientHandlers = new ArrayList<ClientHandler>();
    }

    @Override
    public void run() {
        startAcceptClientsLoop();
    }

    private void startAcceptClientsLoop() {
        logger.info("Accepting Clients. Max players: "+maxPlayers);
        while (true) {
            try {
                final Socket socket = serverSocket.accept();

                if (!isRunning) {
                    socket.close(); // 방금 연결된 소켓은 닫아줌
                    break;
                }

                logger.info("A new client has connected. Players: "+(clientHandlers.size()+1));
                final boolean isSinglePlayer = (maxPlayers == 1);
                final ClientHandler clientHandler = new ClientHandler(this, serverGame,socket, -1, loginHost, isSinglePlayer);
                clientHandlers.add(clientHandler);
                new Thread(clientHandler).start();
            } catch (final IOException e) {
                if (!isRunning) {
                    logger.info("서버 종료 신호 감지. 클라이언트 수락 루프를 종료.");
                    break; // 루프 탈출
                } else {
                    e.printStackTrace();
                }
            }
        }
    }

    private void startGameloop() {
        logger.info("[서버 로그] startGameloop 스레드 시작됨.");
        final long nanosPerTick =  1_000_000_000L / TICKS_PER_SECOND;
        long last = System.nanoTime();

        while (isRunning) {
            long next = last + nanosPerTick;
            long now = System.nanoTime();
            long remain = next - now;
            if (remain > 0){
                long ms = remain / 1_000_000L;
                int ns = (int)(remain % 1_000_000L);
                try {
                    Thread.sleep(ms, ns);
                } catch (InterruptedException e){}
            }

            if (!isRunning) {
                break;
            }

            serverGame.tick();
            sendUpdatesToAll();
            last = next;
        }
    }

    private void sendUpdatesToAll() {


        if (serverGame.isBossClear()) {
            for (final ClientHandler clientHandler : clientHandlers) {
                // "VICTORY" 글자를 보낸다.
                clientHandler.sendUpdate("VICTORY");
            }
        }{
            TreeMap<Integer, ServerGame.Entity> originEntities = serverGame.getEntities();
            TreeMap<Integer, ServerGame.Entity> entitiesCopy;

            synchronized (originEntities) {
                entitiesCopy = new TreeMap<>(originEntities);
            }
            for (final ClientHandler clientHandler : clientHandlers) {
                int lives = 3;
                int score = 0;

                Integer pid = clientHandler.getPlayershipId();
                if (pid != null && pid >= 0) {
                    PlayerData pd = playerDataMap.get(pid);
                    if (pd != null) {
                        lives = pd.getLives();
                        score = pd.getScore();
                    }
                }
                GameState currentState = new GameState(entitiesCopy, score, lives, GameState.GameStatus.PLAYING);
                try {
                    clientHandler.sendUpdate(currentState);
                } catch (RuntimeException e) {
                    toRemove.add(clientHandler);
                }
            }
            for (ClientHandler ch : toRemove) {
                onClientDisconnected(ch);
            }
        }
    }


    // Server.java 파일

    public synchronized void onPlayerJoined(ClientHandler ch) {
        if (gameStarted) {
            logger.info("[서버 로그] 게임이 이미 시작되어 플레이어가 참가할 수 없습니다.");
            return;
        }
        if (!joined.add(ch)) {
            logger.info("[서버 로그] 이미 참가 처리된 클라이언트입니다.");
            return; // 이미 참가 처리된 클라이언트면 무시
        }

        // ▼▼▼ 로그 추가 ▼▼▼
        logger.info("[서버 로그] onPlayerJoined 호출됨. 현재 참가자: " + joined.size() + " / " + maxPlayers);

        if (joined.size() >= maxPlayers) {
            logger.info("[서버 로그] 참가자 수 충족! 게임 루프를 시작합니다."); // ▼▼▼ 로그 추가 ▼▼▼
            gameStarted = true;
            serverGame.initializeFirstStage();
            new Thread(this::startGameloop).start();
        } else {
            logger.info("[서버 로그] 아직 참가자를 더 기다립니다."); // ▼▼▼ 로그 추가 ▼▼▼
        }
    }

    public synchronized void onClientDisconnected(ClientHandler ch) {
        clientHandlers.remove(ch);   // 접속 목록에서 제거
        joined.remove(ch);           // 참가 집합에서도 제거
        logger.info("Client removed. Now: " + clientHandlers.size());
    }

    public Map<Integer, PlayerData> getPlayerDataMap(){ return playerDataMap; }

    public Login getLoginHost(){ return loginHost; }

    // server to one client

    public static void main(String[] args) throws IOException {

        int port = DEFAULT_PORT_NUMBER;
        int players = 2;
        if (args.length > 0){
            port = Integer.parseInt(args[0]);
        }
        if (args.length > 1 && args[1].equalsIgnoreCase("single")){
            players = 1;
        }
        Server server = new Server(port, players);
        server.run();
    }
}
