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
    //서버 호스트 포트 상수
    private static final String DEFAULT_HOST = "localhost";
    private static final int SINGLE_PLAYER_PORT = 1234;
    private static final int MULTIPLAYER_DEFAULT_PORT = 12345;
    private static final int RANK_SERVER_PORT = 12346;

    //그리기 변수
    private JFrame container;
    private JPanel gamePanel;
    private transient BufferStrategy strategy;
    private static final String WINDOW_TITLE = "Space Invaders";

    // sprite 변수
    private transient Sprite[] shipSprite = new Sprite[4];
    private transient Sprite[] shotSprite = new Sprite[4];
    private transient Sprite[] alienFrames = new Sprite[4];
    private transient Sprite[] meteorFrames = new Sprite[16];
    private transient Sprite redHeartSprite;
    private transient Sprite greyHeartSprite;
    private transient Sprite mainBackground;
    private transient Sprite gameBackground;
    private transient Sprite itemSprite;
    private transient Sprite alienShotSprite;
    private transient Sprite reflectAlienSprite;
    private transient Sprite bossSprite;
    private transient Sprite bossLaserSprite;
    private transient Sprite bossChargingSprite;

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
    private static final Logger logger = Logger.getLogger(Game.class.getName());

    public Game() {
        // create a frame to contain our game
        container = new JFrame(WINDOW_TITLE);
        gamePanel = (JPanel) container.getContentPane();
        gamePanel.setPreferredSize(new Dimension(800, 600));
        gamePanel.setLayout(null);

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
                // SonarQube는 이 경고도 잡습니다.
                // 스레드가 중단되었음을 알리는 것이 좋습니다.
                Thread.currentThread().interrupt();
            }
        }

        final int targetMsPerFrame = 16;

        while (isGameLoopRunning) {
            long now = System.currentTimeMillis();

            handleInput();

            Graphics2D g = (Graphics2D) strategy.getDrawGraphics();

            renderGame(g);

            g.dispose();

            strategy.show();

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

    private void renderGame(Graphics2D g){
        g.setColor(Color.black);
        g.fillRect(0, 0, 800, 600); // 배경 색
        gameBackground.draw(g,0,0);

        if (currentGameState == null){
            drawWaitingScreen(g);
        } else{
            drawGameState(g);
        }
    }

    private void drawWaitingScreen(Graphics2D g) {
        g.setColor(Color.WHITE);
        g.setFont(new Font("Serif", Font.BOLD, 30));
        String waitMsg = "Waiting for another player...";
        int strWidth = g.getFontMetrics().stringWidth(waitMsg);
        g.drawString(waitMsg, (800 - strWidth) / 2, 300);
    }

    private void drawGameState(Graphics2D g) {
        drawEntities(g);
        drawHud(g);
    }

    private void drawEntities(Graphics2D g) {
        TreeMap<Integer, ServerGame.Entity> entitiesToDraw = currentGameState.getEntities();
        if (entitiesToDraw == null) {
            return;
        }

        for (ServerGame.Entity entity : entitiesToDraw.values()) {
            drawEntity(g, entity);
        }
    }

    private void drawHud(Graphics2D g) {
        g.setColor(Color.WHITE);
        g.setFont(new Font("Serif", Font.BOLD, 20));
        if (currentGameState != null) {
            g.drawString("Score: " + currentGameState.getCurrentScore(), 650, 580);
            for (int i = 0; i < MAX_LIVES; i++) {
                Sprite heart = (i < currentGameState.getRemainingLives()) ? redHeartSprite : greyHeartSprite;
                heart.draw(g, 10 + (i * (redHeartSprite.getWidth() + 5)), 10);
            }
        }
    }

    private void drawEntity(Graphics2D g, ServerGame.Entity entity) {
        Sprite spriteToDraw = null;

        switch (entity.getType()) {
            case PLAYER:
                ServerPlayerShipEntity ship = (ServerPlayerShipEntity) entity;
                spriteToDraw = shipSprite[ship.getUpgradeCount()];
                break;
            case ALIEN:
                drawAlien(g, entity);
                break;
            case REFLECT_ALIEN:
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
                drawBoss(g, entity);
                break;
            case LASER:
                spriteToDraw = this.bossLaserSprite;
                break;
                default:
                    break;
        }

        if (spriteToDraw != null) {
            spriteToDraw.draw(g, (int) entity.getX(), (int) entity.getY());
        }
    }

    private void drawAlien(Graphics2D g, ServerGame.Entity entity) {
        int frame = ((ServerAlienEntity) entity).getFrameNumber();
        Sprite spriteToDraw = this.alienFrames[frame];
        spriteToDraw.draw(g, (int) entity.getX(), (int) entity.getY());

        int barWidth = 40;
        int barHeight = 3;
        int barx = (int) entity.getX();
        int bary = (int) entity.getY() + (int) entity.getHeight() + 2;

        int alienMaxHp = entity.getMaxHP();
        int alienCurrentHp = entity.getCurrentHP();
        double healthPercent = (double) alienCurrentHp / alienMaxHp;

        g.setColor(Color.red);
        g.fillRect(barx, bary, barWidth, barHeight);

        g.setColor(Color.green);
        g.fillRect(barx, bary, (int) (barWidth * healthPercent), barHeight);
    }

    private void drawBoss(Graphics2D g, ServerGame.Entity entity) {
        int bossFrame = ((ServerBossEntity) entity).getFrameNumber();
        Sprite baseSprite = this.bossSprite;
        Sprite effectSprite = (bossFrame == 1) ? this.bossChargingSprite : null;

        if (baseSprite != null) {
            baseSprite.draw(g, (int) entity.getX(), (int) entity.getY());
            if (effectSprite != null) {
                int effectX = (int) entity.getX() + (baseSprite.getWidth() / 2) - (effectSprite.getWidth() / 2);
                int effectY = (int) entity.getY() + (baseSprite.getHeight() / 2) - (effectSprite.getHeight() / 2);
                effectSprite.draw(g, effectX, effectY);
            }

            int maxHP = entity.getMaxHP();
            int currentHP = entity.getCurrentHP();

            if (maxHP > 0) {
                int bossBarWidth = 100;
                int bossBarHeight = 10;
                int barX = (int) entity.getX() + (baseSprite.getWidth() / 2) - (bossBarWidth / 2);
                int barY = (int) entity.getY() - 15;

                g.setColor(Color.RED);
                g.fillRect(barX, barY, bossBarWidth, bossBarHeight);
                double bossHealthPercent = (double) currentHP / maxHP;
                g.setColor(Color.GREEN);
                g.fillRect(barX, barY, (int) (bossBarWidth * bossHealthPercent), bossBarHeight);
                g.setColor(Color.WHITE);
                g.drawRect(barX, barY, bossBarWidth, bossBarHeight);
            }
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
        /**
         * Notification from AWT that a key has been released.
         *
         * @param e The details of the key that was released
         */
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

    private void setupMenuButton(JButton button, int y, ImageIcon icon, ImageIcon hoverIcon) {
        button.setBounds(275, y, 260, 70);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) { button.setIcon(hoverIcon); }
            @Override
            public void mouseExited(MouseEvent e) { button.setIcon(icon); }
        });
    }

    private void onSinglePlayClicked() {
        if (isGameLoopRunning || isConnecting) return;
        isConnecting = true;

        new Thread(() -> {
            try {
                String classpath = System.getProperty("java.class.path");
                ProcessBuilder pb = new ProcessBuilder("java", "-Dfile.encoding=UTF-8", "-cp", classpath, "org.newdawn.spaceinvaders.multiplay.Server", String.valueOf(SINGLE_PLAYER_PORT), "single");
                pb.redirectErrorStream(true);
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
                Thread.sleep(1000); // 서버가 켜질 때까지 1초 대기
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt(); // 중단 상태 복원
                return;
            }
            startGame(DEFAULT_HOST, SINGLE_PLAYER_PORT);
        }, "client-connect-starter").start();
    }

    private void onLoginClicked() {
        loginFrame = new LoginFrame(this);
        loginFrame.startlogin();
    }

    private void onRankClicked() {
        Process serverProcess = null;
        String tempPort = String.valueOf(RANK_SERVER_PORT);

        try {
            // 1. 랭킹 서버 프로세스를 시작하고, 부팅될 때까지 대기합니다.
            serverProcess = startRankServerProcess(tempPort);

            // 2. 서버에 랭킹을 요청하고 응답을 받습니다.
            Object response = sendRequestWithTempConnection(DEFAULT_HOST, RANK_SERVER_PORT, new RankRequest());

            // 3. 응답을 처리하여 UI에 랭킹 보드를 띄웁니다.
            handleRankResponse(response);

        } catch (IOException | ClassNotFoundException ex) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(container, "랭킹 정보를 가져오는 데 실패했습니다: " + ex.getMessage()));
            ex.printStackTrace();
        } catch (InterruptedException ex){
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(container, "랭킹 확인 작업이 중단되었습니다."));
            Thread.currentThread().interrupt();
        } finally {
            // 4. 모든 작업이 끝나면 서버 프로세스를 확실히 종료시킵니다.
            cleanupServerProcess(serverProcess);
        }
    }

    /**
     * [신규] 랭킹 확인용 임시 서버를 시작하고 1초간 대기합니다.
     */
    private Process startRankServerProcess(String tempPort) throws IOException, InterruptedException {
        String classpath = System.getProperty("java.class.path");
        ProcessBuilder pb = new ProcessBuilder("java", "-Dfile.encoding=UTF-8", "-cp", classpath, "org.newdawn.spaceinvaders.multiplay.Server", tempPort);
        pb.redirectErrorStream(true); // 에러 로그도 같이 볼 수 있게 설정
        Process serverProcess = pb.start();
        logger.log(Level.INFO,"임시 랭킹 서버를 시작합니다 (포트: {0})",tempPort);

        // 서버 프로세스의 로그를 읽는 별도 스레드를 시작
        startProcessLogReader(serverProcess);

        // 서버가 부팅될 때까지 1초 대기
        Thread.sleep(1000);
        return serverProcess;
    }

    /**
     * [신규] 백그라운드 서버 프로세스의 로그를 읽어 콘솔에 출력하는 스레드를 시작합니다.
     */
    private void startProcessLogReader(Process process) {
        new Thread(() -> {
            try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logger.log(Level.INFO,"[Rank Server]: {0}",line);
                }
            } catch (IOException ioException) {
                // 서버 프로세스가 강제 종료되면(finally) 이 예외는 정상입니다.
                // 의도적으로 무시합니다.
            }
        }, "rank-server-log-reader").start();
    }

    /**
     * [신규] 서버로부터 받은 랭킹 응답을 처리하여 RankBoard를 띄웁니다.
     */
    private void handleRankResponse(Object response) {
        if (response instanceof RankResponse res) { // Java 16+ 패턴 매칭
            SwingUtilities.invokeLater(() -> {
                try {
                    new RankBoard(res.getRanking());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(container, "랭킹 보드를 표시할 수 없습니다.");
                }
            });
        }
    }

    /**
     * [신규] 임시 서버 프로세스를 강제로 종료합니다.
     */
    private void cleanupServerProcess(Process serverProcess) {
        if (serverProcess != null && serverProcess.isAlive()) {
            logger.info("임시 랭킹 서버를 종료합니다.");
            serverProcess.destroyForcibly();
        }
    }

    private void onOnlineClicked() {
        if (isConnecting || isGameLoopRunning) return; // SonarQube: Avoid deep nesting
        isConnecting = true;

        String ip = JOptionPane.showInputDialog("Enter Server Ip: ", DEFAULT_HOST);
        if (ip == null || ip.trim().isEmpty()){
            isConnecting = false;
            return; // SonarQube: Avoid deep nesting
        }

        String portStr = JOptionPane.showInputDialog(container,"Enter Port Number",String.valueOf(MULTIPLAYER_DEFAULT_PORT));
        if (portStr == null || portStr.trim().isEmpty()){
            isConnecting = false;
            return; // SonarQube: Avoid deep nesting
        }

        try {
            int port = Integer.parseInt(portStr);
            disconnectIfConnected();
            startGame(ip, port);

            new Thread(() -> {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
                sendToServer(new PlayerInput(PlayerInput.Action.STOP));
            }, "join-signal").start();

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(container, "유효한 숫자로 포트 번호를 입력하세요.", "입력 오류", JOptionPane.ERROR_MESSAGE);
            isConnecting = false;
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

        JPanel panel = new JPanel(){
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if(mainBackground != null) {
                    mainBackground.draw(g,0,0);
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

        setupMenuButton(menuButtons[0], 293, startIcon, startIconHover);
        setupMenuButton(menuButtons[1], 365, loginIcon, loginIconHover);
        setupMenuButton(menuButtons[2], 436, rankIcon, rankIconHover);
        setupMenuButton(menuButtons[3], 508, onlineIcon, onlineIconHover);

        for(JButton btn : menuButtons) {
            panel.add(btn);
        }
        //메인화면 패널로 전환
        container.setContentPane(panel);
        container.revalidate();
        container.repaint();

        menuButtons[0].addActionListener(e -> onSinglePlayClicked());
        menuButtons[1].addActionListener(e -> onLoginClicked());
        menuButtons[2].addActionListener(e -> onRankClicked());
        menuButtons[3].addActionListener(e -> onOnlineClicked());
    }

    public void startMultiplay(String address, int port) throws IOException {
        socket = new Socket(address, port);
        socket.setTcpNoDelay(true);

        // 스트림 생성 순서 + flush
        outputStream = new ObjectOutputStream(socket.getOutputStream());
        outputStream.flush();
        inputStream  = new ObjectInputStream(socket.getInputStream());

        Thread listener = new Thread(this::runServerListener, "server-listener");
        listener.setDaemon(true);
        listener.start();

    }

    private void runServerListener() {
        final Socket s = socket;
        final ObjectInputStream in = inputStream;
        try {
            logger.info("[클라이언트 로그] 서버로부터 메시지 수신 대기 시작.");

            while (isServerConnectionActive(s)) {
                Object msg = in.readObject();
                logger.log(Level.INFO, "[클라이언트 로그] 서버로부터 메시지 수신: {0}", msg.getClass().getSimpleName());

                boolean shouldContinue = handleReceivedMessage(msg);
                if (!shouldContinue) {
                    break;
                }
            }
        }catch(Exception e){
                if(isConnecting || isGameLoopRunning){
                    e.printStackTrace();
                }
        }finally {
            handleServerDisconnection();
        }
    }

    private boolean isServerConnectionActive(Socket s) {
        return !Thread.currentThread().isInterrupted()
                && s != null && !s.isClosed() && s.isConnected();
    }

    private boolean handleReceivedMessage(Object msg) {
        if (msg instanceof String signal) {
            if (signal.equals("VICTORY")) {
                isGameLoopRunning = false;
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(container, "승리!", "게임 클리어", JOptionPane.INFORMATION_MESSAGE);
                    mainMenu();
                });
                return false;
            }
        } else if (msg instanceof GameState gameStateMsg) {
            currentGameState = gameStateMsg;
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
        return true;
    }

    private void handleServerDisconnection() {
        disconnectIfConnected();
        isGameLoopRunning = false;
        isConnecting = false;

        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(container,"서버끊킴");
            isGameLoopRunning = false;
            mainMenu();
        });
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
        String host = DEFAULT_HOST;
        int port = MULTIPLAYER_DEFAULT_PORT;
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
    }

    public ObjectOutputStream getOutputStream(){
        return outputStream;
    }


    /**
     * The entry point into the game. We'll simply create an
     * instance of class which will start the display and game
     * loop.
     *
     * @param argv The arguments that are passed into our game
     */
    public static void main(String[] argv) {
        Game game = new Game();

        SwingUtilities.invokeLater(game::mainMenu);
        // Start the main game loop, note: this method will not
        // return until the game has finished running. Hence we are
        // using the actual main thread to run the game.

    }
}
