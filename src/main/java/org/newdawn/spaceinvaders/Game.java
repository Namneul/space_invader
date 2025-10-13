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

/**
 * The main hook of our game. This class with both act as a manager
 * for the display and central mediator for the game logic.
 * <p>
 * Display management will consist of a loop that cycles round all
 * entities in the game asking them to move and then drawing them
 * in the appropriate place. With the help of an inner class it
 * will also allow the player to control the main ship.
 * <p>
 * As a mediator it will be informed when entities within our game
 * detect events (e.g. alient killed, played died) and will take
 * appropriate game actions.
 *
 * @author Kevin Glass
 */
public class Game extends Canvas {

    //그리기 변수
    private JFrame container;
    private JPanel gamePanel;
    private BufferStrategy strategy;
    private final String windowTitle = "Space Invaders";

    // sprite 변수
    private Sprite[] shipSprite = new Sprite[4];
    private Sprite[] shotSprite = new Sprite[4];
    private Sprite[] alienFrames = new Sprite[4];
    private Sprite[] meteorFrames = new Sprite[16];
    private Sprite redHeartSprite;
    private Sprite greyHeartSprite;
    private Sprite mainBackground;
    private Sprite gameBackground;
    private Sprite itemSprite;
    private Sprite alienShotSprite;
    private Sprite reflectAlienSprite;
    private Sprite bossSprite;
    private Sprite bossLaserSprite;
    private Sprite bossChargingSprite;

    // 서버 변수
    private Socket socket;
    private ObjectOutputStream outputStream;
    private ObjectInputStream inputStream;

    // 게임 상태 변수
    private volatile boolean isGameLoopRunning = false;
    private volatile boolean isConnecting = false;
    private volatile GameState currentGameState; // 서버가 보내주는 최신 게임 상태
    private final int MAX_LIVES = 3; // UI 그리기를 위한 상수

    private boolean leftPressed = false;
    private boolean rightPressed = false;
    private boolean firePressed = false;

    private LoginFrame loginFrame;
    private Process singlePlayServerProcess;


    public Game() {
        // create a frame to contain our game
        container = new JFrame(windowTitle);
        gamePanel = (JPanel) container.getContentPane();
        gamePanel.setPreferredSize(new Dimension(800, 600));
        gamePanel.setLayout(null);

        this.setBounds(0, 0, 800, 600);
        gamePanel.add(this);

        container.pack();
        container.setResizable(false);
        container.setVisible(true);
        container.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                if (singlePlayServerProcess != null) singlePlayServerProcess.destroy();
                System.exit(0);
            }
        });

        // add a key input system (defined below) to our canvas
        // so we can respond to key pressed
        addKeyListener(new KeyInputHandler());
        requestFocus();

        loadSprites();
    }

    private void loadSprites(){
        SpriteStore store = SpriteStore.get();
        shipSprite[0] = store.getSprite("sprites/ship/ship.gif");
        shipSprite[1] = store.getSprite("sprites/ship/shiptype1.png");
        shipSprite[2] = store.getSprite("sprites/ship/shiptype2.png");
        shipSprite[3] = store.getSprite("sprites/ship/shiptype3.png");

        shotSprite[0] = store.getSprite("sprites/shots/shot0.png");
        shotSprite[1] = store.getSprite("sprites/shots/shot1.png");
        shotSprite[2] = store.getSprite("sprites/shots/shot2.png");
        shotSprite[3] = store.getSprite("sprites/shots/shot3.png");

        alienFrames[0] = store.getSprite("sprites/alien.gif");
        alienFrames[1] = store.getSprite("sprites/alien2.gif");
        alienFrames[2] = alienFrames[0];
        alienFrames[3] = store.getSprite("sprites/alien3.gif");
        redHeartSprite = store.getSprite("sprites/heart_red.png");
        greyHeartSprite = store.getSprite("sprites/heart_grey.png");
        mainBackground = store.getSprite("mainBackground.png");
        itemSprite = store.getSprite("sprites/gems_db16.png");
        alienShotSprite = store.getSprite("sprites/alienshot.png");
        reflectAlienSprite = store.getSprite("sprites/ReflectAlien.png");
        gameBackground = store.getSprite("gameBackground.png");
        for (int i = 0; i < 16; i++) {
            meteorFrames[i] = store.getSprite("sprites/meteor/b1000"+i+".png");
        }
        bossSprite = store.getSprite("sprites/boss.png");
        bossChargingSprite = store.getSprite("sprites/boss_charging.png");
        bossLaserSprite = store.getSprite("sprites/boss_laser.png");
    }

    /**
     * Start a fresh game, this should clear out any old data and
     * create a new set.
     */
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
            }
        }

        final  int targetMsPerFrame = 16;
        long last = System.currentTimeMillis();
        while (isGameLoopRunning) {


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

            long now = System.currentTimeMillis();
            long delta = now - last;

            Graphics2D g = (Graphics2D) strategy.getDrawGraphics();
            g.setColor(Color.black);
            g.fillRect(0, 0, 800, 600); // 배경 색
            gameBackground.draw(g,0,0);

            if (currentGameState == null){
                g.setColor(Color.WHITE);
                g.setFont(new Font("Serif", Font.BOLD, 30));
                String waitMsg = "Waiting for another player...";
                int strWidth = g.getFontMetrics().stringWidth(waitMsg);
                g.drawString(waitMsg, (800 - strWidth) / 2, 300);
            } else{

                TreeMap<Integer, ServerGame.Entity> entitiesToDraw = currentGameState.getEntities();
                if (entitiesToDraw != null){
                    for (ServerGame.Entity entity: entitiesToDraw.values()){
                        Sprite spriteToDraw = null;
                        switch (entity.getType()){
                            case PLAYER:
                                ServerPlayerShipEntity ship = (ServerPlayerShipEntity) entity;
                                spriteToDraw = shipSprite[ship.getUpgradeCount()];
                                break;
                            case ALIEN:
                                int frame = ((ServerAlienEntity) entity).getFrameNumber();
                                spriteToDraw = this.alienFrames[frame];

                                int barWidth = 40;
                                int barHeight = 3;
                                int barx = (int)entity.getX();
                                int bary = (int)entity.getY() +(int)entity.getHeight() + 2;

                                int alienMaxHp = entity.getMaxHP();
                                int alienCurrentHp = entity.getCurrentHP();
                                double healthPercent = (double)alienCurrentHp / alienMaxHp;

                                g.setColor(Color.red);
                                g.fillRect(barx, bary, barWidth, barHeight);

                                g.setColor(Color.green);
                                g.fillRect(barx, bary, (int) (barWidth * healthPercent), barHeight);

                                break;
                            case REFLECT_ALIEN:
                                ServerReflectAlienEntity reflectAlien = (ServerReflectAlienEntity) entity;
                                spriteToDraw = this.reflectAlienSprite;
                                break;
                            case SHOT:
                                ServerShotEntity shot = (ServerShotEntity) entity;
                                spriteToDraw = shotSprite[shot.getUpgradeLevel()];
                                break;
                            case ITEM:
                                spriteToDraw = this.itemSprite;
                                break;
                            case ALIEN_SHOT:
                                spriteToDraw = this.alienShotSprite;
                                break;
                            case METEOR:
                                int frameNumber = ((ServerMeteoriteEntity) entity).getFrameNumber();
                                spriteToDraw = this.meteorFrames[frameNumber];
                                break;
                                case BOSS:
                                int bossFrame = ((ServerBossEntity)entity).getFrameNumber();
                                Sprite baseSprite = this.bossSprite;
                                Sprite effectSprite = (bossFrame == 1) ? this.bossChargingSprite : null; // 충전 중일 때만 effectSprite가 있다.

                                if (baseSprite != null) {
                                    baseSprite.draw(g, (int)entity.getX(), (int)entity.getY());
                                }
                                if (effectSprite != null) {
                                    int effectX = (int)entity.getX() + (baseSprite.getWidth() / 2) - (effectSprite.getWidth() / 2);
                                    int effectY = (int)entity.getY() + (baseSprite.getHeight() / 2) - (effectSprite.getHeight() / 2);
                                    effectSprite.draw(g, effectX, effectY);
                                }
                                int maxHP = entity.getMaxHP();
                                int currentHP = entity.getCurrentHP();

                                if (maxHP > 0) {
                                    int bossBarWidth = 100; // 체력바 너비
                                    int bossBarHeight = 10; // 체력바 높이

                                    // ★★★ 여기가 핵심! 보스 머리 위로 위치를 옮긴다 ★★★
                                    // X 좌표: 보스 중앙에 맞춰 정렬
                                    int barX = (int)entity.getX() + (baseSprite.getWidth() / 2) - (bossBarWidth / 2);
                                    // Y 좌표: 보스 머리 위 15픽셀 지점
                                    int barY = (int)entity.getY() - 15;

                                    // 배경 (빨간색)
                                    g.setColor(Color.RED);
                                    g.fillRect(barX, barY, bossBarWidth, bossBarHeight);

                                    // 현재 체력 (녹색)
                                    double bossHealthPercent = (double)currentHP / maxHP;
                                    g.setColor(Color.GREEN);
                                    g.fillRect(barX, barY, (int)(bossBarWidth * bossHealthPercent), bossBarHeight);

                                    // 테두리
                                    g.setColor(Color.WHITE);
                                    g.drawRect(barX, barY, bossBarWidth, bossBarHeight);
                                }
                                spriteToDraw = null;
                                break;

                            case LASER:
                                spriteToDraw = this.bossLaserSprite;
                                break;
                        }
                        if (spriteToDraw != null){
                            spriteToDraw.draw(g, (int)entity.getX(), (int)entity.getY());
                        }
                    }
                }
                g.setColor(Color.WHITE);
                g.setFont(new Font("Serif", Font.BOLD, 20));
                if (currentGameState != null){
                g.drawString("Score: " + currentGameState.getCurrentScore(), 650, 580);}
                for (int i = 0; i < MAX_LIVES; i++) {
                    Sprite heart = (i < currentGameState.getRemainingLives()) ? redHeartSprite : greyHeartSprite;
                    heart.draw(g, 10 + (i * (redHeartSprite.getWidth() + 5)), 10);
                }
                if (currentGameState.getRemainingLives() <= 0){
                    isGameLoopRunning =  false;
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(container, "GAME OVER");
                        mainMenu();
                    });
                    return;
                }
            }

            g.dispose();
            strategy.show();

            try {
                long sleepTime = targetMsPerFrame - (System.currentTimeMillis() - now);
                if (sleepTime > 0){
                    Thread.sleep(sleepTime);
                    last = now;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * A class to handle keyboard input from the user. The class
     * handles both dynamic input during game play, i.e. left/right
     * and shoot, and more static type input (i.e. press any key to
     * continue)
     * <p>
     * This has been implemented as an inner class more through
     * habbit then anything else. Its perfectly normal to implement
     * this as seperate class if slight less convienient.
     *
     * @author Kevin Glass
     */
    private class KeyInputHandler extends KeyAdapter {
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
            }
        }
        /**
         * Notification from AWT that a key has been released.
         *
         * @param e The details of the key that was released
         */
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

        System.out.println("[게임 로그] 메인 메뉴로 복귀. 게임 상태 초기화");
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
                if (mainBackground != null){
                mainBackground.draw(g, 0, 0);
                }
            }
        };
        panel.setPreferredSize(new Dimension(800, 600));
        panel.setLayout(null);

        ImageIcon startIcon = new ImageIcon(getClass().getClassLoader().getResource("button/startBtn.png"));
        ImageIcon loginIcon = new ImageIcon(getClass().getClassLoader().getResource("button/loginBtn.png"));
        ImageIcon rankIcon = new ImageIcon(getClass().getClassLoader().getResource("button/rankBtn.png"));
        ImageIcon onlineIcon = new ImageIcon(getClass().getClassLoader().getResource("button/onlineBtn.png"));
        ImageIcon startIcon_hover = new ImageIcon(getClass().getClassLoader().getResource("button/hover/startBtn_hover.png"));
        ImageIcon loginIcon_hover = new ImageIcon(getClass().getClassLoader().getResource("button/hover/loginBtn_hover.png"));
        ImageIcon rankIcon_hover = new ImageIcon(getClass().getClassLoader().getResource("button/hover/rankBtn_hover.png"));
        ImageIcon onlineIcon_hover = new ImageIcon(getClass().getClassLoader().getResource("button/hover/onlineBtn_hover.png"));

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
                    System.out.println("--- 로컬 서버 프로세스 시작됨 ---");

                    try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(singlePlayServerProcess.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            System.out.println("[Local Server]: " + line);
                        }
                    }
                    System.out.println("--- 로컬 서버 프로세스 종료됨 ---");

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
            // UI가 멈추지 않도록 모든 작업을 새 스레드에서 실행
            new Thread(() -> {
                Process serverProcess = null;
                // 랭킹 확인만을 위한 임시 포트를 사용 (기존 서버와 충돌 방지)
                String tempPort = "12346";

                try {
                    // 랭킹 확인 전용 임시 서버를 백그라운드에서 실행
                    String classpath = System.getProperty("java.class.path");
                    ProcessBuilder pb = new ProcessBuilder("java", "-Dfile.encoding=UTF-8", "-cp", classpath, "org.newdawn.spaceinvaders.multiplay.Server", tempPort);
                    pb.redirectErrorStream(true); // 에러 로그도 같이 볼 수 있게 설정
                    serverProcess = pb.start();
                    System.out.println("임시 랭킹 서버를 시작합니다 (포트: " + tempPort + ")");

                    // 서버 프로세스가 출력하는 로그를 실시간으로 보여주는 코드 (디버깅용)
                    Process finalServerProcess = serverProcess;
                    new Thread(() -> {
                        try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(finalServerProcess.getInputStream()))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                System.out.println("[Rank Server]: " + line);
                            }
                        } catch (IOException ioException) {}
                    }).start();


                    // 서버가 완전히 켜질 때까지 잠시 대기
                    Thread.sleep(1000); // 0.5초

                    // 이 메소드는 연결하고, 요청하고, 응답받고, 바로 연결을 끊습니다.
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
                        System.out.println("임시 랭킹 서버를 종료합니다.");
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
                menuButtons[0].setIcon(startIcon_hover);
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
                menuButtons[1].setIcon(loginIcon_hover);
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
                menuButtons[2].setIcon(rankIcon_hover);
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
                menuButtons[3].setIcon(onlineIcon_hover);
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
                System.out.println("[클라이언트 로그] 서버로부터 메시지 수신 대기 시작.");
                while (!Thread.currentThread().isInterrupted()
                        && s != null && !s.isClosed()) {
                    Object msg = in.readObject();
                    System.out.println("[클라이언트 로그] 서버로부터 메시지 수신: " + msg.getClass().getSimpleName());

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
                    } else if (msg instanceof LoginResponse) {
                        handleLoginResponse((LoginResponse) msg);
                    } else if (msg instanceof SignUpResponse) {
                        handleSignupResponse((SignUpResponse) msg);
                    } else if (msg instanceof RankResponse) {
                        RankResponse res = (RankResponse) msg;
                        SwingUtilities.invokeLater(() -> {
                            try { new RankBoard(res.getRanking()); }
                            catch (Exception e) { JOptionPane.showMessageDialog(container, "can't show RankBoard"); }
                        });
                    }
                }
            } catch (Exception ex) {

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

    private final Object connLock = new Object();

    private void disconnectIfConnected() {
        synchronized (connLock) {
            try { if (inputStream != null)  inputStream.close(); } catch (IOException ignored) {}
            try { if (outputStream != null) outputStream.close(); } catch (IOException ignored) {}
            try { if (socket != null)       socket.close(); }      catch (IOException ignored) {}
            inputStream = null; outputStream = null; socket = null;
        }
    }

    private boolean isConnected() {
        synchronized (connLock) {
            return socket != null && socket.isConnected() && !socket.isClosed();
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
                if (response instanceof LoginResponse) {
                    handleLoginResponse((LoginResponse) response);
                } else if (response instanceof SignUpResponse) {
                    handleSignupResponse((SignUpResponse) response);
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
    }

    public ObjectOutputStream getOutputStream(){
        return outputStream;
    }
    public ObjectInputStream getInputStream(){
        return inputStream;
    }


    /**
     * The entry point into the game. We'll simply create an
     * instance of class which will start the display and game
     * loop.
     *
     * @param argv The arguments that are passed into our game
     */
    public static void main(String argv[]) {
        Game game = new Game();

        SwingUtilities.invokeLater(game::mainMenu);
        // Start the main game loop, note: this method will not
        // return until the game has finished running. Hence we are
        // using the actual main thread to run the game.

    }
}
