package org.newdawn.spaceinvaders.multiplay;

import com.sun.source.tree.Tree;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.TreeMap;

public class Server implements Runnable{

    public final static int TICKS_PER_SECOND = 120;
    private final int MILLISECONDS_PER_TICK = 1000000000 / TICKS_PER_SECOND;
    public final static int DEFAULT_PORT_NUMBER = 1234;

    private ServerSocket serverSocket;
    private final ServerGame serverGame;
    private final ArrayList<ClientHandler> clientHandlers;


    public Server(final ServerGame serverGame, final int port){
        this.serverGame = serverGame;
        try {
            this.serverSocket = new ServerSocket(port);
        } catch (final IOException e) {
            e.printStackTrace();
        }
        clientHandlers = new ArrayList<ClientHandler>();
    }

    @Override
    public void run() {
        new Thread(() -> startAcceptClientsLoop()).start();
        new Thread(() -> startGameloop()).start();
    }

    private void startAcceptClientsLoop() {
        System.out.println("Accepting Clients.");
        while (true) {
            System.out.println("Waiting for new client.");
            try {
                final Socket socket = serverSocket.accept();
                System.out.println("A new client has connected.");
                final ClientHandler clientHandler = new ClientHandler(this, serverGame,socket, serverGame.spawnPlayerEntity());
                clientHandlers.add(clientHandler);
                new Thread(clientHandler).start();
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }
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

    // server to one client

    public static void main(String[] args) throws IOException {

        Server server = new Server(new ServerGame(), DEFAULT_PORT_NUMBER);
        server.run();
    }
}
