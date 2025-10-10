package org.newdawn.spaceinvaders;

import org.newdawn.spaceinvaders.multiplay.*;
import org.newdawn.spaceinvaders.multiplay.communication.LoginResponse;
import org.newdawn.spaceinvaders.multiplay.communication.RankRequest;
import org.newdawn.spaceinvaders.multiplay.communication.SignUpResponse;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
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
    private Sprite redHeartSprite;
    private Sprite greyHeartSprite;
    private Sprite mainBackground;
    private Sprite itemSprite;
    private Sprite alienShotSprite;

    // 서버 변수
    private Socket socket;
    private ObjectOutputStream outputStream;
    private ObjectInputStream inputStream;

    // 게임 상태 변수
    private volatile boolean isGameLoopRunning = false;
    private volatile GameState currentGameState; // 서버가 보내주는 최신 게임 상태
    private final int MAX_LIVES = 3; // UI 그리기를 위한 상수

    private LoginFrame loginFrame;


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
                gameLoop();
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(container, "NOT CONNECTED: " +e.getMessage());
                    mainMenu();
                });
                isGameLoopRunning =false;
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

            long now = System.currentTimeMillis();
            long delta = now - last;

            Graphics2D g = (Graphics2D) strategy.getDrawGraphics();
            g.setColor(Color.black);
            g.fillRect(0, 0, 800, 600); // 배경 색

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

            if (outputStream == null) return;

            try {
                PlayerInput.Action action = null;
                switch (e.getKeyCode()){
                    case KeyEvent.VK_LEFT:
                        action = PlayerInput.Action.MOVE_LEFT;
                        break;
                    case KeyEvent.VK_RIGHT:
                        action = PlayerInput.Action.MOVE_RIGHT;
                        break;
                    case KeyEvent.VK_SPACE:
                        action = PlayerInput.Action.FIRE;
                        break;
                }

                if (action != null){
                    outputStream.writeObject(new PlayerInput(action));
                    outputStream.reset();
                    outputStream.flush();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        /**
         * Notification from AWT that a key has been released.
         *
         * @param e The details of the key that was released
         */
        public void keyReleased(KeyEvent e) {
            if (outputStream == null) return;
            try {
                if (e.getKeyCode() == KeyEvent.VK_LEFT || e.getKeyCode() == KeyEvent.VK_RIGHT){
                    outputStream.writeObject(new PlayerInput(PlayerInput.Action.STOP));
                    outputStream.reset();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }


    public void mainMenu() {
        new Thread(() -> {
            try {
                startMultiplay("localhost", 12345);
                System.out.println("서버 자동 연결 성공!");
            } catch (IOException e){
                System.out.println("서버 미연결 상태. Online 상태에서 재시도 하세요.");
            }
        }).start();
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

        ButtonController buttonController = new ButtonController(this);

        //버튼 생성
        JButton[] menuButtons = {new JButton("SinglePlay"),
                new JButton("login"),
                new JButton("Rank"),
                new JButton("OnLine"),
                new JButton("exit")};

        for (int i = 0; i < menuButtons.length; i++) {
            panel.add(menuButtons[i]);
        }
        menuButtons[0].setBounds(300, 335, 200, 50);
        menuButtons[1].setBounds(300, 395, 200, 50);
        menuButtons[2].setBounds(300, 455, 200, 50);
        menuButtons[3].setBounds(300, 515, 200, 50);
        //메인화면 패널로 전환
        container.setContentPane(panel);
        container.revalidate();
        container.repaint();

        menuButtons[0].addActionListener(e -> { // SinglePlay 버튼
            if (isGameLoopRunning) return;

            container.setContentPane(gamePanel);
            container.revalidate();
            container.repaint();
            requestFocusInWindow();

            // --- 2. 서버 실행 및 접속 (백그라운드 스레드) ---
            new Thread(() -> {
                // 2-1. 서버 프로세스 시작
                Process serverProcess;
                try {
                    ProcessBuilder pb = new ProcessBuilder("java", "-Dfile.encoding=UTF-8", "-cp", "target/classes", "org.newdawn.spaceinvaders.multiplay.Server", "1234", "single");
                    pb.directory(new File("."));
                    serverProcess = pb.start();
                } catch (IOException ex) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(container, "Could not start local server process.");
                        mainMenu();
                    });
                    return;
                }

                // 2-2. '소켓 연결'만 성공할 때까지 재시도
                Socket tempSocket = null;
                for (int i = 0; i < 25; i++) { // 최대 5초
                    try {
                        tempSocket = new Socket("localhost", 1234);
                        break; // 소켓 연결 성공! 루프 탈출.
                    } catch (IOException ex) {
                        try { Thread.sleep(200); } catch (InterruptedException ignored) {}
                    }
                }

                // 2-3. 최종 결과 처리
                if (tempSocket != null) {
                    // 소켓 연결에 최종 성공했을 경우
                    try {
                        // 성공한 소켓을 사용해 스트림을 만들고 리스너를 시작
                        this.socket = tempSocket;
                        this.outputStream = new ObjectOutputStream(socket.getOutputStream());
                        this.outputStream.flush();
                        this.inputStream = new ObjectInputStream(socket.getInputStream());

                        // startMultiplay에 있던 리스너 스레드를 여기에 직접 생성
                        Thread listenerThread = new Thread(() -> {
                            while (socket.isConnected()) {
                                try {
                                    Object response = inputStream.readObject();
                                    if (response instanceof GameState){
                                        currentGameState = (GameState) response;
                                    } // ... (이하 다른 Response 처리 로직)
                                } catch (Exception ex) {
                                    break;
                                }
                            }
                        });
                        listenerThread.start();

                        // 모든 준비가 끝났으니 게임 루프 시작
                        isGameLoopRunning = true;
                        gameLoop();

                    } catch (IOException ex) {
                        // 스트림 생성 실패 시 처리
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(container, "Failed to establish stream with server.");
                            mainMenu();
                        });
                        serverProcess.destroy();
                    }
                } else {
                    // 소켓 연결에 최종 실패했을 경우
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(container, "Failed to connect to the local server.");
                        mainMenu();
                    });
                    serverProcess.destroy();
                }
            }).start();
        });

        menuButtons[1].addActionListener(e -> {
            loginFrame = new LoginFrame(this);
            loginFrame.startlogin();

        });

        menuButtons[2].addActionListener(e -> {
            try {
                if (outputStream != null){
                    outputStream.writeObject(new RankRequest());
                    outputStream.reset();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
        menuButtons[3].addActionListener(e -> {
            String ip = JOptionPane.showInputDialog("Enter Server Ip: ", "localhost");
            if (ip == null && ip.trim().isEmpty()){
                return;
            }

            String portStr = JOptionPane.showInputDialog(container,"Enter Port Number");
            if (portStr == null || portStr.trim().isEmpty()){
                return;
            }

            try {
                int port = Integer.parseInt(portStr);
                startGame(ip, port);
            } catch (NumberFormatException ex) {
                throw new RuntimeException(ex);
            }
        });

    }

    public void startMultiplay(String address, int port) throws IOException {
        socket = new Socket(address, port);
        outputStream = new ObjectOutputStream(socket.getOutputStream());
        inputStream = new ObjectInputStream(socket.getInputStream());

        Thread ListenForServerUpdates = new Thread() {
            public void run() {
                while (socket.isConnected()) {
                    try {
                        Object response = inputStream.readObject();

                        if (response instanceof GameState){
                            currentGameState = (GameState) response;
                        } else if (response instanceof LoginResponse) {
                            handleLoginResponse((LoginResponse) response);
                        } else if (response instanceof SignUpResponse) {
                            handleSignupResponse((SignUpResponse) response);
                        }
                    } catch (Exception exception) {
                        System.out.println("server lost");
                        break;
                    }
                }
            }
        };
        ListenForServerUpdates.start();
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
