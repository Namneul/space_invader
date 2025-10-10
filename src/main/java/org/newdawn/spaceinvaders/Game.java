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
    private Sprite shipSprite;
    private Sprite shotSprite;
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
        shipSprite = store.getSprite("sprites/ship/shiptype1.png");
        shotSprite = store.getSprite("sprites/shots/shot0.png");
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
                createBufferStrategy(2);
                strategy = getBufferStrategy();

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
        long lastLoopTime = SystemTimer.getTime();
        while (isGameLoopRunning) {

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
                                spriteToDraw = this.shipSprite;
                                break;
                            case ALIEN:
                                int frame = ((ServerAlienEntity) entity).getFrameNumber();
                                spriteToDraw = this.alienFrames[frame];
                                break;
                            case SHOT:
                                spriteToDraw = this.shotSprite;
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
            }

            g.dispose();
            strategy.show();

            try {
                long sleepTime = (lastLoopTime + 10) - System.currentTimeMillis();
                if (sleepTime > 0){
                    Thread.sleep(sleepTime);
                }
                lastLoopTime = System.currentTimeMillis();
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

        menuButtons[0].addActionListener(e -> {

            try {
                //1인용 로컬 서버
                new ProcessBuilder(
                        "java",
                        "-cp",
                        System.getProperty("java.class.path"),
                        "org.newdawn.spaceinvaders.multiplay.Server",
                        "1234",
                        "single"
                ).start();

                Thread.sleep(500);

                startGame("localhost", 1234);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
            startGame("localhost", 1234);

        });

        menuButtons[1].addActionListener(e -> {
            loginFrame = new LoginFrame();
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
            if (ip != null && !ip.trim().isEmpty()){
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
