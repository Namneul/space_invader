package org.newdawn.spaceinvaders;

import org.newdawn.spaceinvaders.multiplay.*;
import org.newdawn.spaceinvaders.multiplay.ServerEntity.*;
import org.newdawn.spaceinvaders.multiplay.communication.LoginResponse;
import org.newdawn.spaceinvaders.multiplay.communication.RankRequest;
import org.newdawn.spaceinvaders.multiplay.communication.RankResponse;
import org.newdawn.spaceinvaders.multiplay.communication.SignUpResponse;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferStrategy;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Game extends Canvas {

    //그리기 변수
    private JFrame container;
    private JPanel gamePanel;
    private GameRenderer gameRenderer;
    private transient BufferStrategy strategy;
    private static final String windowTitle = "Space Invaders";


    // 서버 변수
    private transient Socket socket;
    private transient ObjectOutputStream outputStream;
    private transient ObjectInputStream inputStream;

    // 게임 상태 변수
    private volatile boolean isGameLoopRunning = false;
    private volatile boolean isConnecting = false;
    private GameState currentGameState; // 서버가 보내주는 최신 게임 상태
    private static final int MAX_LIVES = 3; // UI 그리기를 위한 상수

    private boolean leftPressed = false;
    private boolean rightPressed = false;
    private boolean firePressed = false;

    private transient LoginFrame loginFrame;
    private transient Process singlePlayServerProcess;
    Logger logger = Logger.getLogger(getClass().getName());

    public Game() {
        // create a frame to contain our game
        container = new JFrame(windowTitle);
        gamePanel = (JPanel) container.getContentPane();
        gamePanel.setPreferredSize(new Dimension(800, 600));
        gamePanel.setLayout(null);

        this.gameRenderer = new GameRenderer();

        this.setBounds(0, 0, 800, 600);
        gamePanel.add(this);

        container.pack();
        container.setResizable(false);
        container.setVisible(true);
        container.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (singlePlayServerProcess != null) singlePlayServerProcess.destroy();
                System.exit(0);
            }
        });

        // add a key input system (defined below) to our canvas
        // so we can respond to key pressed
        addKeyListener(new KeyInputHandler());
        requestFocus();

    }


    private void startGame(String serverAdress, int port) {

        if (isGameLoopRunning){
            return;
        }
        container.setContentPane(gamePanel);
        container.revalidate();
        container.repaint();
        requestFocus();

        new Thread(() -> {
            try {

                startMultiplay(serverAdress, port);
                isGameLoopRunning = true;
                isConnecting = false;
                gameLoop();
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(container, "NOT CONNECTED: " +e.getMessage());
                    mainMenu();
                });
                isGameLoopRunning =false;
                isConnecting = false;
            }
        }).start();

        // blank out any keyboard settings we might currently have
    }

    public void gameLoop() {
        while (strategy == null) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                // SonarQube는 이 경고도 잡습니다.
                // 스레드가 중단되었음을 알리는 것이 좋습니다.
                Thread.currentThread().interrupt();
            }
        }

        final int targetMsPerFrame = 16;

        while (isGameLoopRunning) {
            long now = System.currentTimeMillis();

            handleInput();

            gameRenderer.render(currentGameState);

            if (checkGameOver()) {
                isGameLoopRunning = false;
                showGameOverScreen();
                return;
            }

            limitFrameRate(now, targetMsPerFrame);
        }
    }

    private void handleInput(){
        if (leftPressed){
            sendToServer(new PlayerInput(PlayerInput.Action.MOVE_LEFT));
        }
        if (rightPressed){
            sendToServer(new PlayerInput(PlayerInput.Action.MOVE_RIGHT));
        }
        if (firePressed){
            sendToServer(new PlayerInput(PlayerInput.Action.FIRE));
        }
        if (!leftPressed && !rightPressed){
            sendToServer(new PlayerInput(PlayerInput.Action.STOP));
        }
    }


    private boolean checkGameOver() {
        return (currentGameState != null && currentGameState.getRemainingLives() <= 0);
    }

    private void showGameOverScreen() {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(container, "GAME OVER");
            mainMenu();
        });
    }

    private void limitFrameRate(long now, int targetMsPerFrame) {
        try {
            long sleepTime = targetMsPerFrame - (System.currentTimeMillis() - now);
            if (sleepTime > 0) {
                Thread.sleep(sleepTime);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }


    private class KeyInputHandler extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {

            switch (e.getKeyCode()) {
                case KeyEvent.VK_LEFT:
                    leftPressed = true;
                    break;
                case KeyEvent.VK_RIGHT:
                    rightPressed = true;
                    break;
                case KeyEvent.VK_SPACE:
                    firePressed = true;
                    break;
                default:
                    break;
            }
        }
        @Override
        public void keyReleased(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                leftPressed = false;
            }
            if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                rightPressed = false;
            }
            if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                firePressed = false;
            }
        }
    }


    public void mainMenu() {

        logger.info("[게임 로그] 메인 메뉴로 복귀. 게임 상태 초기화");
        isGameLoopRunning = false;
        isConnecting = false;
        currentGameState = null;
        disconnectIfConnected();

        if (this.singlePlayServerProcess != null) {
            this.singlePlayServerProcess.destroy(); // 실행 중인 싱글플레이 서버 종료
            this.singlePlayServerProcess = null;
        }

        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (gameRenderer != null && gameRenderer.getMainBackground() != null){
                gameRenderer.getMainBackground().draw(g, 0, 0);
                }
            }
        };
        panel.setPreferredSize(new Dimension(800, 600));
        panel.setLayout(null);

        ImageIcon startIcon = new ImageIcon(getClass().getClassLoader().getResource("button/startBtn.png"));
        ImageIcon loginIcon = new ImageIcon(getClass().getClassLoader().getResource("button/loginBtn.png"));
        ImageIcon rankIcon = new ImageIcon(getClass().getClassLoader().getResource("button/rankBtn.png"));
        ImageIcon onlineIcon = new ImageIcon(getClass().getClassLoader().getResource("button/onlineBtn.png"));
        ImageIcon startIconHover = new ImageIcon(getClass().getClassLoader().getResource("button/hover/startBtn_hover.png"));
        ImageIcon loginIconHover = new ImageIcon(getClass().getClassLoader().getResource("button/hover/loginBtn_hover.png"));
        ImageIcon rankIconHover = new ImageIcon(getClass().getClassLoader().getResource("button/hover/rankBtn_hover.png"));
        ImageIcon onlineIconHover = new ImageIcon(getClass().getClassLoader().getResource("button/hover/onlineBtn_hover.png"));

        //버튼 생성
        JButton[] menuButtons = {
                new JButton(startIcon),
                new JButton(loginIcon),
                new JButton(rankIcon),
                new JButton(onlineIcon)
        };

        for(int i=0;i<menuButtons.length;i++) {
            panel.add(menuButtons[i]);
        }
        menuButtons[0].setBounds(275,293,260,70);
        menuButtons[0].setBorderPainted(false);       // 버튼 테두리 설정 해제
        menuButtons[0].setFocusPainted(false);        // 포커스가 갔을 때 생기는 테두리 설정 해제
        menuButtons[0].setContentAreaFilled(false);   // 버튼 영역 배경 표시 해제
        menuButtons[1].setBounds(275,365,260,70);
        menuButtons[1].setBorderPainted(false);       // 버튼 테두리 설정 해제
        menuButtons[1].setFocusPainted(false);        // 포커스가 갔을 때 생기는 테두리 설정 해제
        menuButtons[1].setContentAreaFilled(false);   // 버튼 영역 배경 표시 해제
        menuButtons[2].setBounds(275,436,260,70);
        menuButtons[2].setBorderPainted(false);       // 버튼 테두리 설정 해제
        menuButtons[2].setFocusPainted(false);        // 포커스가 갔을 때 생기는 테두리 설정 해제
        menuButtons[2].setContentAreaFilled(false);   // 버튼 영역 배경 표시 해제
        menuButtons[3].setBounds(275,508,260,70);
        menuButtons[3].setBorderPainted(false);       // 버튼 테두리 설정 해제
        menuButtons[3].setFocusPainted(false);        // 포커스가 갔을 때 생기는 테두리 설정 해제
        menuButtons[3].setContentAreaFilled(false);   // 버튼 영역 배경 표시 해제

        //메인화면 패널로 전환
        container.setContentPane(panel);
        container.revalidate();
        container.repaint();


        menuButtons[0].addActionListener(e -> { // SinglePlay 버튼
            if (isGameLoopRunning || isConnecting) return;
            isConnecting = true;

            new Thread(() -> {
                try {
                    String classpath = System.getProperty("java.class.path");
                    ProcessBuilder pb = new ProcessBuilder("java", "-Dfile.encoding=UTF-8", "-cp", classpath, "org.newdawn.spaceinvaders.multiplay.Server", "1234", "single");
                    pb.redirectErrorStream(true); // 에러 출력을 표준 출력으로 합쳐서 보기 편하게 함
                    pb.directory(new File("."));
                    this.singlePlayServerProcess = pb.start();
                    logger.info("--- 로컬 서버 프로세스 시작됨 ---");

                    try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(singlePlayServerProcess.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            logger.log(Level.INFO,"[Local Server]: {0}",line);
                        }
                    }
                    logger.info("--- 로컬 서버 프로세스 종료됨 ---");

                } catch (IOException ex) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(container, "로컬 서버 실행 실패: " + ex.getMessage());
                        mainMenu();
                    });
                } finally {
                    isConnecting = false;
                }
            }, "local-server-runner").start();

            new Thread(() -> {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    return;
                }
                startGame("localhost", 1234);
            }, "client-connect-starter").start();

        });

        menuButtons[1].addActionListener(e -> {
            String host = "localhost";
            int port = 12345;
            loginFrame = new LoginFrame(this);
            loginFrame.startlogin();
        });

        menuButtons[2].addActionListener(e -> {
            new Thread(() -> {
                Process serverProcess = null;
                String tempPort = "12346";

                try {
                    String classpath = System.getProperty("java.class.path");
                    ProcessBuilder pb = new ProcessBuilder("java", "-Dfile.encoding=UTF-8", "-cp", classpath, "org.newdawn.spaceinvaders.multiplay.Server", tempPort);
                    pb.redirectErrorStream(true); // 에러 로그도 같이 볼 수 있게 설정
                    serverProcess = pb.start();
                    logger.log(Level.INFO,"임시 랭킹 서버를 시작합니다 (포트: {0})",tempPort);

                    Process finalServerProcess = serverProcess;
                    new Thread(() -> {
                        try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(finalServerProcess.getInputStream()))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                logger.log(Level.INFO,"[Rank Server]: {0}",line);
                            }
                        } catch (IOException ioException) {}
                    }).start();


                    Thread.sleep(1000); // 0.5초

                    Object response = sendRequestWithTempConnection("localhost", Integer.parseInt(tempPort), new RankRequest());

                    if (response instanceof RankResponse) {
                        RankResponse res = (RankResponse) response;
                        SwingUtilities.invokeLater(() -> {
                            try {
                                new RankBoard(res.getRanking());
                            } catch (Exception ex) {
                                JOptionPane.showMessageDialog(container, "랭킹 보드를 표시할 수 없습니다.");
                            }
                        });
                    }

                } catch (IOException | InterruptedException | ClassNotFoundException ex) {
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(container, "랭킹 정보를 가져오는 데 실패했습니다: " + ex.getMessage()));
                    ex.printStackTrace();

                } finally {
                    // 모든 작업이 끝나면 (성공하든 실패하든) 임시 서버 프로세스를 강제 종료
                    if (serverProcess != null && serverProcess.isAlive()) {
                        logger.info("임시 랭킹 서버를 종료합니다.");
                        serverProcess.destroyForcibly();
                    }
                }
            }, "rank-requester-thread").start();
        });

        menuButtons[3].addActionListener(e -> {
            if (isConnecting || isGameLoopRunning) return;
            isConnecting = true;

            String ip = JOptionPane.showInputDialog("Enter Server Ip: ", "localhost");
            if (ip == null || ip.trim().isEmpty()){
                isConnecting = false;
                return;
            }

            String portStr = JOptionPane.showInputDialog(container,"Enter Port Number","12345");
            if (portStr == null || portStr.trim().isEmpty()){
                isConnecting = false;
                return;
            }
            try {
                int port = Integer.parseInt(portStr);
                disconnectIfConnected();
                startGame(ip, port);

                new Thread(() -> {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ex) {}
                    sendToServer(new PlayerInput(PlayerInput.Action.STOP));
                }, "join-signal").start();

            } catch (NumberFormatException ex) {
                throw new RuntimeException(ex);
            }
        });
        menuButtons[0].addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                // 아이콘을 hover 이미지로 변경
                menuButtons[0].setIcon(startIconHover);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                // 아이콘을 기본 이미지로 복원
                menuButtons[0].setIcon(startIcon);
            }
        });

        menuButtons[1].addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                // 아이콘을 hover 이미지로 변경
                menuButtons[1].setIcon(loginIconHover);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                // 아이콘을 기본 이미지로 복원
                menuButtons[1].setIcon(loginIcon);
            }
        });

        menuButtons[2].addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                // 아이콘을 hover 이미지로 변경
                menuButtons[2].setIcon(rankIconHover);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                // 아이콘을 기본 이미지로 복원
                menuButtons[2].setIcon(rankIcon);
            }
        });

        menuButtons[3].addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                // 아이콘을 hover 이미지로 변경
                menuButtons[3].setIcon(onlineIconHover);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                // 아이콘을 기본 이미지로 복원
                menuButtons[3].setIcon(onlineIcon);
            }
        });


    }

    public void startMultiplay(String address, int port) throws IOException {
        socket = new Socket(address, port);
        socket.setTcpNoDelay(true);

        // 스트림 생성 순서 + flush
        outputStream = new ObjectOutputStream(socket.getOutputStream());
        outputStream.flush();
        inputStream  = new ObjectInputStream(socket.getInputStream());

        Thread listener = new Thread(() -> {
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
                            isGameLoopRunning = false; // 게임 루프를 멈춘다.

                            SwingUtilities.invokeLater(() -> {
                                JOptionPane.showMessageDialog(container, "승리!", "게임 클리어", JOptionPane.INFORMATION_MESSAGE);
                                mainMenu(); // 메인 메뉴로 돌아간다.
                            });

                            break; // 신호를 처리했으니 리스너 스레드는 종료.
                        }
                    } else if (msg instanceof GameState) {
                        currentGameState = (GameState) msg;
                    } else if (msg instanceof LoginResponse loginResponseMsg) {
                        handleLoginResponse(loginResponseMsg);
                    } else if (msg instanceof SignUpResponse signUpResponseMsg) {
                        handleSignupResponse(signUpResponseMsg);
                    } else if (msg instanceof RankResponse res ) {
                        SwingUtilities.invokeLater(() -> {
                            try { new RankBoard(res.getRanking()); }
                            catch (Exception e) { JOptionPane.showMessageDialog(container, "can't show RankBoard"); }
                        });
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                disconnectIfConnected();
                isGameLoopRunning = false;
                isConnecting = false;

                SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(container, "서버 끊김");
                        isGameLoopRunning = false;
                        mainMenu();
                });
            }
        }, "server-listener");
        listener.setDaemon(true);
        listener.start();
    }


    private void handleLoginResponse(LoginResponse res){
        SwingUtilities.invokeLater(() ->{
            if (res.isSuccess()){
                if (loginFrame != null){
                    loginFrame.frame.dispose();
                }
                    JOptionPane.showMessageDialog(container, "Login success!");
            }
            else {
                    JOptionPane.showMessageDialog(container, "Login failed.");
            }
        });
    }

    private void handleSignupResponse(SignUpResponse res){
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(container, res.getMessage());
            if (res.isSuccess() && loginFrame != null){
                loginFrame.frame.dispose();
            }
        });
    }

    private transient Object connLock = new Object();

    private void disconnectIfConnected() {
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


    private Object sendRequestWithTempConnection(String host, int port, Object request) throws IOException, ClassNotFoundException {
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
                    handleLoginResponse(loginResponse);
                } else if (response instanceof SignUpResponse signUpResponse) {
                    handleSignupResponse(signUpResponse);
                }
            } catch (IOException | ClassNotFoundException ex) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(container, "서버 통신 오류: " + ex.getMessage()));
            }
        }).start();
    }

    @Override
    public void addNotify(){
        super.addNotify();

        createBufferStrategy(2);
        strategy = getBufferStrategy();
        gameRenderer.setStrategy(strategy);
    }

    public ObjectOutputStream getOutputStream(){
        return outputStream;
    }


    public static void main(String[] argv) {
        Game game = new Game();

        SwingUtilities.invokeLater(game::mainMenu);
    }
}