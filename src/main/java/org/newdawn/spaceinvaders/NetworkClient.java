package org.newdawn.spaceinvaders;

import org.newdawn.spaceinvaders.multiplay.GameState;
import org.newdawn.spaceinvaders.multiplay.communication.LoginResponse;
import org.newdawn.spaceinvaders.multiplay.communication.RankResponse;
import org.newdawn.spaceinvaders.multiplay.communication.SignUpResponse;

import javax.swing.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NetworkClient {

    private final NetworkListener listener;

    private Socket socket;
    private ObjectOutputStream outputStream;
    private ObjectInputStream inputStream;
    private Object connLock = new Object();

    Logger logger = Logger.getLogger(getClass().getName());

    public NetworkClient(NetworkListener listener){
        this.listener = listener;
    }

    public void startMultiplay(String address, int port) throws IOException {
        socket = new Socket(address, port);
        socket.setTcpNoDelay(true);

        // 스트림 생성 순서 + flush
        outputStream = new ObjectOutputStream(socket.getOutputStream());
        outputStream.flush();
        inputStream  = new ObjectInputStream(socket.getInputStream());

        Thread listenerThread = new Thread(() -> {
            final Socket s = socket;
            final ObjectInputStream in = inputStream;
            try {
                logger.info("[클라이언트 로그] 서버로부터 메시지 수신 대기 시작.");
                while (!Thread.currentThread().isInterrupted()
                        && s != null && !s.isClosed()) {
                    Object msg = in.readObject();
                    logger.log(Level.INFO,"[클라이언트 로그] 서버로부터 메시지 수신: {0}", msg.getClass().getSimpleName());

                    if (msg instanceof String) {
                        String signal = (String) msg;
                        // "VICTORY" 신호인지 확인한다.
                        if (signal.equals("VICTORY")) {

                            listener.onVictory();
                            break; // 신호를 처리했으니 리스너 스레드는 종료.
                        }
                    } else if (msg instanceof GameState) {
                        listener.onGameStateUpdate((GameState) msg);
                    } else if (msg instanceof LoginResponse loginResponseMsg) {
                        listener.onLoginResponse(loginResponseMsg);
                    } else if (msg instanceof SignUpResponse signUpResponseMsg) {
                        listener.onSignUpResponse(signUpResponseMsg);
                    } else if (msg instanceof RankResponse res ) {
                        listener.onRankResponse(res);
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                disconnectIfConnected();
                listener.onDisconnected("Server disconnected.");
            }
        }, "server-listener");
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    public void disconnectIfConnected() {
        synchronized (connLock) {
            try { if (inputStream != null)  inputStream.close(); } catch (IOException ignored) {ignored.printStackTrace(); }
            try { if (outputStream != null) outputStream.close(); } catch (IOException ignored) {ignored.printStackTrace(); }
            try { if (socket != null)       socket.close(); }      catch (IOException ignored) {ignored.printStackTrace(); }
            inputStream = null; outputStream = null; socket = null;
        }
    }

    public synchronized boolean sendToServer(Object object){
        if (outputStream == null) return false;
        try {
            outputStream.writeObject(object);
            outputStream.reset();
            outputStream.flush();
            return true;
        } catch (IOException e){
            return false;
        }
    }

    public Object sendRequestWithTempConnection(String host, int port, Object request) throws IOException, ClassNotFoundException {
        try (Socket tempSocket = new Socket(host, port);
             ObjectOutputStream tempOut = new ObjectOutputStream(tempSocket.getOutputStream())) {

            tempOut.writeObject(request);
            tempOut.flush();

            try (ObjectInputStream tempIn = new ObjectInputStream(tempSocket.getInputStream())) {
                return tempIn.readObject();
            }
        }
    }

    public void performLoginOrSignUp(Object request) {
        String host = "localhost";
        int port = 12345;
        new Thread(() -> {
            try {
                Object response = sendRequestWithTempConnection(host, port, request);
                if (response instanceof LoginResponse loginResponse) {
                    listener.onLoginResponse(loginResponse);
                } else if (response instanceof SignUpResponse signUpResponse) {
                    listener.onSignUpResponse(signUpResponse);
                }
            } catch (IOException | ClassNotFoundException ex) {
                listener.onDisconnected("서버 통신 오류: " + ex.getMessage());
            }
        }).start();
    }

    public ObjectOutputStream getOutputStream(){
        return outputStream;
    }
}
