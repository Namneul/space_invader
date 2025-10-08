package org.newdawn.spaceinvaders;

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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.TreeMap;

import javax.swing.*;

import org.newdawn.spaceinvaders.entity.*;
import org.newdawn.spaceinvaders.stage.*;
import org.newdawn.spaceinvaders.multiplay.GameState;
import org.newdawn.spaceinvaders.multiplay.PlayerInput;
import org.newdawn.spaceinvaders.multiplay.ServerGame;

/**
 * The main hook of our game. This class with both act as a manager
 * for the display and central mediator for the game logic. 
 * 
 * Display management will consist of a loop that cycles round all
 * entities in the game asking them to move and then drawing them
 * in the appropriate place. With the help of an inner class it
 * will also allow the player to control the main ship.
 * 
 * As a mediator it will be informed when entities within our game
 * detect events (e.g. alient killed, played died) and will take
 * appropriate game actions.
 * 
 * @author Kevin Glass
 */
public class Game extends Canvas 
{

    private volatile boolean isGameLoopRunning = false;
	/** The stragey that allows us to use accelerate page flipping */
	private BufferStrategy strategy;

    private Sprite alienSprite;

    private Sprite shotSprite;
	/** True if the game is currently "running", i.e. the game loop is looping */
	private boolean gameRunning = true;
	/** The list of all the entities that exist in our game */
	private ArrayList entities = new ArrayList();
	/** The list of entities that need to be removed from the game this loop */
	private ArrayList removeList = new ArrayList();
	/** The entity representing the player */
	private Entity ship;
	/** The speed at which the player's ship should move (pixels/sec) */
	private double moveSpeed = 300;
	/** The time at which last fired a shot */
	private long lastFire = 0;
	/** The interval between our players shot (ms) */
	private long firingInterval = 500;
	/** The number of aliens left on the screen */
	private int alienCount;
	
	/** The message to display which waiting for a key press */
	private String message = "";
	/** True if we're holding up game play until a key has been pressed */
	private boolean waitingForKeyPress = true;
	/** True if the left cursor key is currently pressed */
	private boolean leftPressed = false;
	/** True if the right cursor key is currently pressed */
	private boolean rightPressed = false;
	/** True if we are firing */
	private boolean firePressed = false;
	/** True if game logic needs to be applied this loop, normally as a result of a game event */
	private boolean logicRequiredThisLoop = false;
	/** The last time at which we recorded the frame rate */
	private long lastFpsTime;
	/** The current number of frames recorded */
	private int fps;
	/** The normal title of the game window */
	private String windowTitle = "Space Invaders";
	/** The game window that we'll update with the frame count */
	private JFrame container;
	private JPanel gamePanel;
    private JButton[] menuButtons;

	private int playerLives;
	private Sprite redHeartSprite;
	private Sprite greyHeartSprite;
	private Sprite mainBackground;
	private final int MAX_LIVES = 3;
    private ArrayList<Stage> stages;      // 스테이지 목록
    private int currentStageIndex;          // 현재 스테이지 인덱스
    private Stage currentStage;

	/**
	 * Construct our game and set it running.
	 */

	LoginFrame loginFrame = new LoginFrame();


	private Image mainImage;

    public enum GameMode{MAIN_MENU, SINGLEPLAY, MULTIPLAY}
    private GameMode currentMode = GameMode.MAIN_MENU;
    private Socket socket;
    private ObjectOutputStream outputStream;
    private ObjectInputStream inputStream;
    private volatile TreeMap<Integer, ServerGame.Entity> networkEntities = new TreeMap<>();

	public Game() {
		// create a frame to contain our game
		container = new JFrame("Space Invaders");



		// get hold the content of the frame and set up the resolution of the game
		gamePanel = (JPanel) container.getContentPane();
		gamePanel.setPreferredSize(new Dimension(800,600));
		gamePanel.setLayout(null);

		// setup our canvas size and put it into the content of the frame
		setBounds(0,0,800,600);
		gamePanel.add(this);

		// Tell AWT not to bother repainting our canvas since we're
		// going to do that our self in accelerated mode
		setIgnoreRepaint(true);

		// finally make the window visible
		container.pack();
		container.setResizable(false);
		container.setVisible(true);

		// add a listener to respond to the user closing the window. If they
		// do we'd like to exit the game
		container.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});

		// add a key input system (defined below) to our canvas
		// so we can respond to key pressed
		addKeyListener(new KeyInputHandler());

		// request the focus so key events come to us
		requestFocus();

		// create the buffering strategy which will allow AWT
		// to manage our accelerated graphics
//		createBufferStrategy(2);
//		strategy = getBufferStrategy();
		//메인 화면 생성으로 인한 패널 전환으로 새로운 메소드로 분리


		// initialise the entities in our game so there's something
		// to see at startup
		initEntities();
        loadStages(); // 스테이지 목록 로드
		redHeartSprite = SpriteStore.get().getSprite("sprites/heart_red.png");
		greyHeartSprite = SpriteStore.get().getSprite("sprites/heart_grey.png");
		mainBackground = SpriteStore.get().getSprite("mainBackground.png");
        this.alienSprite = SpriteStore.get().getSprite("sprites/alien.gif");
        this.shotSprite = SpriteStore.get().getSprite("sprites/shots/shot0.png");
	}
	
	/**
	 * Start a fresh game, this should clear out any old data and
	 * create a new set.
	 */
	private void startGame() {
		// clear out any existing entities and intialise a new set
		entities.clear();
		initEntities();

		//점수 초기화
		loginFrame.user.resetScore();

		// blank out any keyboard settings we might currently have
		leftPressed = false;
		rightPressed = false;
		firePressed = false;
	}
	
	/**
	 * Initialise the starting state of the entities (ship and aliens). Each
	 * entitiy will be added to the overall list of entities in the game.
	 */
	private void initEntities() {
		// create the player ship and place it roughly in the center of the screen
		ship = new ShipEntity(this,"sprites/ship.gif",370,550);
		entities.add(ship);
		playerLives = MAX_LIVES;

		// create a block of aliens (5 rows, by 12 aliens, spaced evenly)
        if (currentStage != null) {
            currentStage.initialize(this, entities);
		}
	}

	public void alienFires(Entity alien) {
		AlienShotEntity shot = new AlienShotEntity(this, "sprites/alienshot.png", alien.getX(), alien.getY() + 20);
		entities.add(shot);
	}

	//아이템 생성 로직
	public void itemDrop(Entity alien) {
		EvolveItemEntity item = new EvolveItemEntity(this, "sprites/gems_db16.png",alien.getX(),alien.getY());
		entities.add(item);
	}

	/**
	 * Notification from a game entity that the logic of the game
	 * should be run at the next opportunity (normally as a result of some
	 * game event)
	 */
	public void updateLogic() {
		logicRequiredThisLoop = true;
	}
	
	/**
	 * Remove an entity from the game. The entity removed will
	 * no longer move or be drawn.
	 * 
	 * @param entity The entity that should be removed
	 */
	public void removeEntity(Entity entity) {
		removeList.add(entity);
	}
	
	/**
	 * Notification that the player has died. 
	 */
	public void notifyDeath() {
		playerLives--;

		if (playerLives <= 0) {
			message = "Oh no! They got you, try again?";
			loginFrame.user.compareScore(loginFrame);
			removeEntity(ship);
			waitingForKeyPress = true;
		} else {
			ship.setPosition(370, 550);
		}
	}
	
	/**
	 * Notification that the player has won since all the aliens
	 * are dead.
	 */
	public void notifyWin() {
        currentStageIndex++; // 다음 스테이지로 인덱스 증가
        // 만약 마지막 스테이지까지 클리어했다면
        if (currentStageIndex >= stages.size()) {
            message = "Well done! You Win!";
            waitingForKeyPress = true;
        } else {
            // 다음 스테이지가 있다면, 새 스테이지 객체를 가져오고 게임을 다시 시작
            currentStage = stages.get(currentStageIndex);
            startGame();
        }
	}
	
	/**
	 * Notification that an alien has been killed
	 */
	//죽은 에일리언 객체를 넘겨줌 ->아이템 드랍위치를 위해서
	public void notifyAlienKilled(Entity alien) {
		// reduce the alient count, if there are none left, the player has won!
		alienCount--;

		if(Math.random()<0.5){
			itemDrop(alien);
		}
		loginFrame.user.increaseScore();
		if (alienCount == 0) {
			loginFrame.user.compareScore(loginFrame);
			notifyWin();
		}
		
		// if there are still some aliens left then they all need to get faster, so
		// speed up all the existing aliens
		for (int i=0;i<entities.size();i++) {
			Entity entity = (Entity) entities.get(i);
			
			if (entity instanceof AlienEntity) {
				// speed up by 2%
				entity.setHorizontalMovement(entity.getHorizontalMovement() * 1.02);
			}
		}
	}
	
	/**
	 * Attempt to fire a shot from the player. Its called "try"
	 * since we must first check that the player can fire at this 
	 * point, i.e. has he/she waited long enough between shots
	 */
	public void tryToFire() {
		// check that we have waiting long enough to fire
		if (System.currentTimeMillis() - lastFire < firingInterval) {
			return;
		}

		// if we waited long enough, create the shot entity, and record the time.
		lastFire = System.currentTimeMillis();
		ShotEntity shot = new ShotEntity(this,"sprites/shots/shot"+((ShipEntity)ship).upgradeCount+".png",ship.getX()+10,ship.getY()-30,((ShipEntity)ship).damage);
		entities.add(shot);
	}
	
	/**
	 * The main game loop. This loop is running during all game
	 * play as is responsible for the following activities:
	 * <p>
	 * - Working out the speed of the game loop to update moves
	 * - Moving the game entities
	 * - Drawing the screen contents (entities, text)
	 * - Updating game events
	 * - Checking Input
	 * <p>
	 */
    private void loadStages() {
        stages = new ArrayList<>();
        stages.add(new Stage1());
        stages.add(new Stage2());
        stages.add(new Stage3());
        stages.add(new Stage4());
        stages.add(new Stage5());

        currentStageIndex = 0; // 0번 인덱스(Stage 1)부터 시작
        currentStage = stages.get(currentStageIndex);
    }

    public void setAlienCount(int count) {
        this.alienCount = count;
    }

	public void gameLoop() {
		long lastLoopTime = SystemTimer.getTime();
		JFrame frame = new JFrame("Space Invaders");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		// keep looping round til the game ends
		while (gameRunning) {
			// work out how long its been since the last update, this
			// will be used to calculate how far the entities should
			// move this loop
			long delta = SystemTimer.getTime() - lastLoopTime;
			lastLoopTime = SystemTimer.getTime();

			// update the frame counter
			lastFpsTime += delta;
			fps++;
			
			// update our FPS counter if a second has passed since
			// we last recorded
			if (lastFpsTime >= 1000) {
				container.setTitle(windowTitle+" (FPS: "+fps+")");
				lastFpsTime = 0;
				fps = 0;
			}
			
			// Get hold of a graphics context for the accelerated 
			// surface and blank it out
			Graphics2D g = (Graphics2D) strategy.getDrawGraphics();
			g.setColor(Color.black);
			g.fillRect(0,0,800,600); // 배경 색
            if (waitingForKeyPress) {
                g.setColor(Color.white);


                g.drawString(message,(800-g.getFontMetrics().stringWidth(message))/2,250);
                g.drawString("Press any key",(800-g.getFontMetrics().stringWidth("Press any key"))/2,300);
            }

			g.setColor(Color.WHITE);
			g.setFont(new Font("Serif", Font.BOLD, 20));
			g.drawString("Score: " + loginFrame.user.Score, 650, 580);
            for (int i = 0; i < MAX_LIVES; i++) {
                Sprite heart = (i < playerLives) ? redHeartSprite : greyHeartSprite;
                heart.draw(g, 10 + (i * (redHeartSprite.getWidth() + 5)), 10);
            }


            // finally, we've completed drawing so clear up the graphics
            // and flip the buffer over
            if (currentMode == GameMode.SINGLEPLAY){
			// cycle round asking each entity to move itself
			if (!waitingForKeyPress) {
				for (int i=0;i<entities.size();i++) {
					Entity entity = (Entity) entities.get(i);
					
					entity.move(delta);
				}
			}
			
			// cycle round drawing all the entities we have in the game
			for (int i=0;i<entities.size();i++) {
				Entity entity = (Entity) entities.get(i);
				
				entity.draw(g);
			}
			
			// brute force collisions, compare every entity against
			// every other entity. If any of them collide notify 
			// both entities that the collision has occured
			for (int p=0;p<entities.size();p++) {
				for (int s=p+1;s<entities.size();s++) {
					Entity me = (Entity) entities.get(p);
					Entity him = (Entity) entities.get(s);
					
					if (me.collidesWith(him)) {
						me.collidedWith(him);
						him.collidedWith(me);
					}
				}
			}

			for (int i = 0; i < MAX_LIVES; i++) {
				Sprite heart = (i < playerLives) ? redHeartSprite : greyHeartSprite;
				heart.draw(g, 10 + (i * (redHeartSprite.getWidth() + 5)), 10);
			}
			// remove any entity that has been marked for clear up
			entities.removeAll(removeList);
			removeList.clear();

			// if a game event has indicated that game logic should
			// be resolved, cycle round every entity requesting that
			// their personal logic should be considered.
			if (logicRequiredThisLoop) {
				for (int i=0;i<entities.size();i++) {
					Entity entity = (Entity) entities.get(i);
					entity.doLogic();
				}
				
				logicRequiredThisLoop = false;
			}


			// if we're waiting for an "any key" press then draw the 
			// current message

			
			// resolve the movement of the ship. First assume the ship 
			// isn't moving. If either cursor key is pressed then
			// update the movement appropraitely
			ship.setHorizontalMovement(0);
			
			if ((leftPressed) && (!rightPressed)) {
				ship.setHorizontalMovement(-moveSpeed);
			} else if ((rightPressed) && (!leftPressed)) {
				ship.setHorizontalMovement(moveSpeed);
			}
			
			// if we're pressing fire, attempt to fire
			if (firePressed) {
				tryToFire();
			}
			
			// we want each frame to take 10 milliseconds, to do this
			// we've recorded when we started the frame. We add 10 milliseconds
			// to this and then factor in the current time to give 
			// us our final value to wait for
        }
         else if (currentMode == GameMode.MULTIPLAY) {

             if (networkEntities != null){

                 for (ServerGame.Entity entity: networkEntities.values()){
//                     System.out.println("Drawing: " + entity.getType() + " at X="+entity.getX() + ", Y=" + entity.getY());
                     Sprite spriteToDraw = null;
                     switch (entity.getType()){
                         case PLAYER:
                             spriteToDraw = this.ship.getSprite();
                             break;
                         case SHOT:
                             spriteToDraw = this.shotSprite;
                             break;
                         case ALIEN:
                             spriteToDraw = this.alienSprite;
                             break;
                     }
                     if (spriteToDraw != null){
                         spriteToDraw.draw(g, (int) entity.getX(), (int) entity.getY());

                     }
                 }
             }
         }
                if (loginFrame.user != null) {
                    g.setColor(Color.WHITE);
                    g.setFont(new Font("Serif", Font.BOLD, 20));
                    g.drawString("Score: " + loginFrame.user.Score, 650, 580);
                }
                for (int i = 0; i < MAX_LIVES; i++) {
                    Sprite heart = (i < playerLives) ? redHeartSprite : greyHeartSprite;
                    heart.draw(g, 10 + (i * (redHeartSprite.getWidth() + 5)), 10);
            }
            g.dispose();
            strategy.show();
            SystemTimer.sleep(lastLoopTime+10-SystemTimer.getTime());
        }

    }

	/**
	 * A class to handle keyboard input from the user. The class
	 * handles both dynamic input during game play, i.e. left/right 
	 * and shoot, and more static type input (i.e. press any key to
	 * continue)
	 * 
	 * This has been implemented as an inner class more through 
	 * habbit then anything else. Its perfectly normal to implement
	 * this as seperate class if slight less convienient.
	 * 
	 * @author Kevin Glass
	 */
	private class KeyInputHandler extends KeyAdapter {
		/** The number of key presses we've had while waiting for an "any key" press */
		private int pressCount = 1;
		
		/**
		 * Notification from AWT that a key has been pressed. Note that
		 * a key being pressed is equal to being pushed down but *NOT*
		 * released. Thats where keyTyped() comes in.
		 *
		 * @param e The details of the key that was pressed 
		 */
		public void keyPressed(KeyEvent e) {
            if (currentMode == GameMode.SINGLEPLAY){
			// if we're waiting for an "any key" typed then we don't
			// want to do anything with just a "press"
			if (waitingForKeyPress) {
				return;
			}
			
			
			if (e.getKeyCode() == KeyEvent.VK_LEFT) {
				leftPressed = true;
			}
			if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
				rightPressed = true;
			}
			if (e.getKeyCode() == KeyEvent.VK_SPACE) {
				firePressed = true;
			}
		} else if (currentMode == GameMode.MULTIPLAY) {
                if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                    try {
                        outputStream.writeObject(new PlayerInput(PlayerInput.Action.MOVE_LEFT));
                        outputStream.reset();
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
                if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                    try {
                        outputStream.writeObject(new PlayerInput(PlayerInput.Action.MOVE_RIGHT));
                        outputStream.reset();
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
                if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    try {
                        outputStream.writeObject(new PlayerInput(PlayerInput.Action.FIRE));
                        outputStream.reset();
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
        }
		
		/**
		 * Notification from AWT that a key has been released.
		 *
		 * @param e The details of the key that was released 
		 */
		public void keyReleased(KeyEvent e) {
			// if we're waiting for an "any key" typed then we don't 
			// want to do anything with just a "released"
            if (currentMode == GameMode.SINGLEPLAY) {
                if (waitingForKeyPress) {
                    return;
                }

                if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                    leftPressed = false;
                }
                if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                    rightPressed = false;
                }
                if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    firePressed = false;
                }
            }else if (currentMode == GameMode.MULTIPLAY) {
                try {
                    if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                        // 1. 왼쪽 키를 뗐다고 클라이언트에 기록합니다.
                        leftPressed = false;
                        // 2. 만약 오른쪽 키도 눌려있지 않다면, '움직임 멈춤'을 보고합니다.
                        if (!rightPressed) {
                            outputStream.writeObject(new PlayerInput(PlayerInput.Action.STOP));
                        }
                    } else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                        // 1. 오른쪽 키를 뗐다고 클라이언트에 기록합니다.
                        rightPressed = false;
                        // 2. 만약 왼쪽 키도 눌려있지 않다면, '움직임 멈춤'을 보고합니다.
                        if (!leftPressed) {
                            outputStream.writeObject(new PlayerInput(PlayerInput.Action.STOP));
                        }
                    }
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
		}

		/**
		 * Notification from AWT that a key has been typed. Note that
		 * typing a key means to both press and then release it.
		 *
		 * @param e The details of the key that was typed. 
		 */
		public void keyTyped(KeyEvent e) {
			// if we're waiting for a "any key" type then
			// check if we've recieved any recently. We may
			// have had a keyType() event from the user releasing
			// the shoot or move keys, hence the use of the "pressCount"
			// counter.
			if (waitingForKeyPress) {
				if (pressCount == 1) {
					// since we've now recieved our key typed
					// event we can mark it as such and start 
					// our new game
					waitingForKeyPress = false;
					startGame();
					pressCount = 0;
				} else {
					pressCount++;
				}
			}
			
			// if we hit escape, then quit the game
			if (e.getKeyChar() == 27) {
				System.exit(0);
			}
		}
	}


	public void mainMenu() {
		JFrame frame = container;
		JPanel panel = new JPanel(){
			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				mainBackground.draw(g, 0, 0);
			}
		};
		panel.setPreferredSize(new Dimension(800, 600));
		panel.setLayout(null);

		ButtonController buttonController = new ButtonController();

		//버튼 생성
		JButton[] menuButtons = {new JButton("startGame"),
				new JButton("login"),
				new JButton("Rank"),
				new JButton("OnLine"),
				new JButton("exit")};

		for(int i=0;i<menuButtons.length;i++) {
			panel.add(menuButtons[i]);
		}
		menuButtons[0].setBounds(300,335,200,50);
		menuButtons[1].setBounds(300,395,200,50);
		menuButtons[2].setBounds(300,455,200,50);
		menuButtons[3].setBounds(300,515,200,50);
		//메인화면 패널로 전환
		frame.setContentPane(panel);
		frame.revalidate();
		frame.repaint();

        menuButtons[0].addActionListener(e -> {
            if (isGameLoopRunning) return;
            currentMode = GameMode.SINGLEPLAY;

            if(loginFrame.user == null){
                loginFrame.user = new User();
                loginFrame.user.Id = "Guest";
            }
            frame.setContentPane(gamePanel);
            frame.revalidate();
            frame.repaint();
            requestFocus();

            createBufferStrategy(2);
            strategy = getBufferStrategy();

			new Thread(() -> {gameLoop();}).start();
		});

        menuButtons[1].addActionListener(e -> {
            buttonController.pressLoginBtn(loginFrame,panel,frame);
        });

        menuButtons[2].addActionListener(e -> {
            try{
                buttonController.pressRankBtn(loginFrame);
            }catch(SQLException ex){
                ex.printStackTrace();
            }
        });
        menuButtons[3].addActionListener(e ->{
            if (isGameLoopRunning) return;
            frame.setContentPane(gamePanel);
            frame.revalidate();
            frame.repaint();
            requestFocusInWindow();
            try {
                createBufferStrategy(2);
                strategy = getBufferStrategy();
                System.out.println("[DEBUG] BufferStrategy 생성 시도. 생성된 strategy 객체: " + strategy);
            } catch (Exception ex) {
                System.err.println("[FATAL ERROR] BufferStrategy 생성 중 예외 발생!");
                ex.printStackTrace();
                return; // strategy 생성 실패 시 더 이상 진행하지 않음
            }

            new Thread(() -> {
                try {
                    startMultiplay();
                    currentMode = GameMode.MULTIPLAY;
                    isGameLoopRunning = true;
                    gameLoop();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }).start();
        });

    }

    public void startMultiplay() throws IOException{
        socket = new Socket("localhost", 1234);
        outputStream = new ObjectOutputStream(socket.getOutputStream());
        inputStream = new ObjectInputStream(socket.getInputStream());
        Thread ListenForServerUpdates = new Thread(){
            public void run(){
                while (socket.isConnected()){
                    try {
                        GameState newState = (GameState) inputStream.readObject();
                        networkEntities = newState.getEntities();
                        System.out.println("Client: 서버로부터 " + networkEntities.size() + "개의 엔티티 수신 완료.");
                    } catch (Exception exception){
                        break;
                    }
                }
            }
        };
        ListenForServerUpdates.start();
        this.currentMode = GameMode.MULTIPLAY;
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
		game.mainMenu();
		// Start the main game loop, note: this method will not
		// return until the game has finished running. Hence we are
		// using the actual main thread to run the game.

	}
}
