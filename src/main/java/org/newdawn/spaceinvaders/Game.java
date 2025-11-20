package org.newdawn.spaceinvaders;

import org.newdawn.spaceinvaders.multiplay.*;
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
import java.io.ObjectOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Game extends Canvas implements NetworkListener{

    private static final String DEFAULT_HOST = "localhost";
    private static final int SINGLE_PLAYER_PORT = 1234;
    private static final int MULTIPLAYER_DEFAULT_PORT = 12345;
    private static final int RANK_SERVER_PORT = 12346;

    //그리기 변수
    private JFrame container;
    private JPanel gamePanel;
    private transient GameRenderer gameRenderer;
    private transient BufferStrategy strategy;
    private static final String WINDOW_TITLE = "Space Invaders";

    private transient NetworkClient networkClient;

    private boolean wasPPressed = false;

    // 게임 상태 변수
    private volatile boolean isGameLoopRunning = false;
    private volatile boolean isConnecting = false;
    private GameState currentGameState; // 서버가 보내주는 최신 게임 상태

    private transient InputManager inputManager;

    private transient LoginFrame loginFrame;
    private transient Process singlePlayServerProcess;
    private static final Logger logger = Logger.getLogger(Game.class.getName());

    public Game() {
        // create a frame to contain our game
        container = new JFrame(WINDOW_TITLE);
        gamePanel = (JPanel) container.getContentPane();
        gamePanel.setPreferredSize(new Dimension(800, 600));
        gamePanel.setLayout(null);

        this.gameRenderer = new GameRenderer();
        this.networkClient = new NetworkClient(this);

        this.setBounds(0, 0, 800, 600);
        gamePanel.add(this);

        container.pack();
        container.setResizable(false);
        container.setVisible(true);
        container.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (singlePlayServerProcess != null) singlePlayServerProcess.destroyForcibly();
                System.exit(0);
            }
        });

        inputManager = new InputManager();
        addKeyListener(inputManager);

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

                networkClient.startMultiplay(serverAdress, port);
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
        if (inputManager.isLeftPressed()){
            networkClient.sendToServer(new PlayerInput(PlayerInput.Action.MOVE_LEFT));
        }
        if (inputManager.isRightPressed()){
            networkClient.sendToServer(new PlayerInput(PlayerInput.Action.MOVE_RIGHT));
        }
        if (inputManager.isFirePressed()){
            networkClient.sendToServer(new PlayerInput(PlayerInput.Action.FIRE));
        }
        if (!inputManager.isLeftPressed() && !inputManager.isRightPressed()){
            networkClient.sendToServer(new PlayerInput(PlayerInput.Action.STOP));
        }
        boolean isPPressed = inputManager.isPPressed();
        if (isPPressed && !wasPPressed) {
            networkClient.sendToServer(new PlayerInput(PlayerInput.Action.SKIP_STAGE));
        }
        wasPPressed = isPPressed;
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

    private void onRankClicked() {
        Process serverProcess = null;
        String tempPort = String.valueOf(RANK_SERVER_PORT);

        try {
            // 1. 랭킹 서버 프로세스를 시작하고, 부팅될 때까지 대기합니다.
            serverProcess = startRankServerProcess(tempPort);

            // 2. 서버에 랭킹을 요청하고 응답을 받습니다.
            Object response = networkClient.sendRequestWithTempConnection(DEFAULT_HOST, RANK_SERVER_PORT, new RankRequest());

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
            networkClient.disconnectIfConnected();
            startGame(ip, port);

            new Thread(() -> {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
                networkClient.sendToServer(new PlayerInput(PlayerInput.Action.STOP));
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
        networkClient.disconnectIfConnected();

        if (this.singlePlayServerProcess != null) {
            this.singlePlayServerProcess.destroyForcibly(); // 실행 중인 싱글플레이 서버 종료
            this.singlePlayServerProcess = null;
        }

        MainMenuPanel menuPanel = new MainMenuPanel(
                e -> onSinglePlayClicked(),
                e -> showLoginFrame(),
                e -> onRankClicked(),
                e -> onOnlineClicked()
        );

        container.setContentPane(menuPanel);
        container.revalidate();
        container.repaint();

        ImageIcon startIcon = new ImageIcon(getClass().getClassLoader().getResource("button/startBtn.png"));
        ImageIcon loginIcon = new ImageIcon(getClass().getClassLoader().getResource("button/loginBtn.png"));
        ImageIcon rankIcon = new ImageIcon(getClass().getClassLoader().getResource("button/rankBtn.png"));
        ImageIcon onlineIcon = new ImageIcon(getClass().getClassLoader().getResource("button/onlineBtn.png"));

        //버튼 생성
        JButton[] menuButtons = {
                new JButton(startIcon),
                new JButton(loginIcon),
                new JButton(rankIcon),
                new JButton(onlineIcon)
        };

        menuButtons[0].addActionListener(e -> onSinglePlayClicked());
        menuButtons[1].addActionListener(e -> showLoginFrame());
        menuButtons[2].addActionListener(e -> onRankClicked());
        menuButtons[3].addActionListener(e -> onOnlineClicked());
    }

    public void performLoginOrSignUp(Object request) {
        networkClient.performLoginOrSignUp(request);
    }

    @Override
    public void addNotify(){
        super.addNotify();

        createBufferStrategy(2);
        strategy = getBufferStrategy();
        gameRenderer.setStrategy(strategy);
    }

    public ObjectOutputStream getOutputStream(){
        return networkClient.getOutputStream();
    }

    private void showLoginFrame(){
        if (loginFrame == null || !loginFrame.frame.isVisible()){
            loginFrame = new LoginFrame(this);
            loginFrame.startlogin();
        }
    }

    @Override
    public void onGameStateUpdate(GameState newState) {
        this.currentGameState = newState;
    }

    @Override
    public void onVictory() {
        isGameLoopRunning = false;

        SwingUtilities.invokeLater(()->{
            JOptionPane.showMessageDialog(container, "승리!", "게임 클리어", JOptionPane.INFORMATION_MESSAGE);
            mainMenu();
        });
    }

    @Override
    public void onLoginResponse(LoginResponse response) {
        SwingUtilities.invokeLater(() ->{
            if (response.isSuccess()){
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

    @Override
    public void onSignUpResponse(SignUpResponse response) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(container, response.getMessage());
            if (response.isSuccess() && loginFrame != null){
                loginFrame.frame.dispose();
            }
        });
    }

    @Override
    public void onRankResponse(RankResponse response) {
        SwingUtilities.invokeLater(() -> {
            try { new RankBoard(response.getRanking()); }
            catch (Exception e) { JOptionPane.showMessageDialog(container, "can't show RankBoard"); }
        });
    }

    @Override
    public void onDisconnected(String reason) {
        isGameLoopRunning = false;
        isConnecting = false;

        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(container, "서버 끊김: " + reason);
            mainMenu();
        });
    }

    public static void main(String[] argv) {
        Game game = new Game();

        SwingUtilities.invokeLater(game::mainMenu);
    }
}