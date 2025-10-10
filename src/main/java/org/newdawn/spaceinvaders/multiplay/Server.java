package org.newdawn.spaceinvaders.multiplay;


import org.newdawn.spaceinvaders.multiplay.stage.Stage;
import org.newdawn.spaceinvaders.multiplay.stage.Stage1;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

public class Server implements Runnable{

    public final static int TICKS_PER_SECOND = 120;
    public final static int DEFAULT_PORT_NUMBER = 12345;

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
                System.out.println("A new client has connected. Players: "+(clientHandlers.size()+1));
                final ClientHandler clientHandler = new ClientHandler(this, serverGame,socket, serverGame.spawnPlayerEntity(), loginHost);
                clientHandlers.add(clientHandler);
                new Thread(clientHandler).start();
                int remaining = maxPlayers - clientHandlers.size();
                if (remaining > 0){
                    System.out.println("Waiting for "+remaining+" more player...");
                }
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println(maxPlayers+" player(s) connected. Game Start!");
        this.gameStarted = true;

        serverGame.initializeFirstStage();
        new Thread(() -> startGameloop()).start();
    }

    private void startGameloop() {
        final long nanosPerTick =  1_000_000_000L / TICKS_PER_SECOND;
        long last = System.nanoTime();

        while (true) {
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

            serverGame.tick();
            sendUpdatesToAll();
            last = next;
        }
    }

    private void sendUpdatesToAll() {
        TreeMap<Integer, ServerGame.Entity> originEntities = serverGame.getEntities();
        TreeMap<Integer, ServerGame.Entity> entitiesCopy;

        synchronized (originEntities){
            entitiesCopy = new TreeMap<>(originEntities);
        }
        int remaininglives = 3;
        if (!playerDataMap.isEmpty()){
            PlayerData player = playerDataMap.values().iterator().next();
            remaininglives = player.getLives();
        }
        int currentScore = 0;
        if (!playerDataMap.isEmpty()){
            PlayerData player = playerDataMap.values().iterator().next();
            currentScore = player.getScore();
        }
        GameState currentState = new GameState(entitiesCopy,currentScore,remaininglives, GameState.GameStatus.PLAYING);
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
            port = Integer.parseInt(args[0]);
        }
        if (args.length > 1 && args[1].equalsIgnoreCase("single")){
            players = 1;
        }
        Server server = new Server(port, players);
        server.run();
    }
}
