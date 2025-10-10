package org.newdawn.spaceinvaders.multiplay;


import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

public class Server implements Runnable{

    public final static int TICKS_PER_SECOND = 120;
    private final int MILLISECONDS_PER_TICK = 1000000000 / TICKS_PER_SECOND;
    public final static int DEFAULT_PORT_NUMBER = 1234;

    private ServerSocket serverSocket;
    private final ServerGame serverGame;
    private final ArrayList<ClientHandler> clientHandlers;
    protected Map<Integer, PlayerData> playerDataMap = new ConcurrentHashMap<>();
    private Login loginHost;
    private final int maxPlayers;
    private boolean gameStarted = false;

    public Server(int port, int maxPlayers){
        this.serverGame = new ServerGame(this);
        this.loginHost = new Login();
        this.maxPlayers = maxPlayers;
        try {
            this.serverSocket = new ServerSocket(port);
            System.out.println("Server started on port: "+port+" for "+maxPlayers);
        } catch (final IOException e) {
            e.printStackTrace();
        }
        clientHandlers = new ArrayList<ClientHandler>();
    }

    @Override
    public void run() {
        startAcceptClientsLoop();
    }

    private void startAcceptClientsLoop() {
        System.out.println("Accepting Clients. Need "+maxPlayers+" players.");
        while (clientHandlers.size() < this.maxPlayers) {
            try {
                final Socket socket = serverSocket.accept();
                System.out.println("A new client has connected. Players: "+clientHandlers.size()+1);
                final ClientHandler clientHandler = new ClientHandler(this, serverGame,socket, serverGame.spawnPlayerEntity(), loginHost);
                clientHandlers.add(clientHandler);
                new Thread(clientHandler).start();
                if (clientHandlers.size() == 1){
                    System.out.println("Waiting for 2P");
                }
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Two players connected. Game start!");
        this.gameStarted = true;
        new Thread(() -> startGameloop()).start();
    }

    private void startGameloop() {
        long lastTickTime = System.nanoTime();

        while (true) {
            final long whenShouldNextTickRun = lastTickTime + MILLISECONDS_PER_TICK;
            if (System.nanoTime() < whenShouldNextTickRun) {
                continue;
            }

            serverGame.tick();

            sendUpdatesToAll();

            lastTickTime = System.nanoTime();
        }
    }

    private void sendUpdatesToAll() {
        TreeMap<Integer, ServerGame.Entity> originEntities = serverGame.getEntities();
        TreeMap<Integer, ServerGame.Entity> entitiesCopy;

        synchronized (originEntities){
            entitiesCopy = new TreeMap<>(originEntities);
        }
        GameState currentState = new GameState(entitiesCopy,0,3, GameState.GameStatus.PLAYING);
        System.out.println("Server: " + currentState.getEntities().size() + "개의 엔티티를 클라이언트로 전송 시도.");
        for (final ClientHandler clientHandler : clientHandlers) {
            clientHandler.sendUpdate(currentState);
        }
    }

    public Map<Integer, PlayerData> getPlayerDataMap(){ return playerDataMap; }

    public Login getLoginHost(){ return loginHost; }

    // server to one client

    public static void main(String[] args) throws IOException {

        int port = DEFAULT_PORT_NUMBER;
        int players = 2;
        if (args.length > 0){
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.out.println("Invalid port number");
            }
        }
        if (args.length > 1 && args[1].equalsIgnoreCase("single")){
            players = 1;
        }
        Server server = new Server(port, players);
        server.run();
    }
}
