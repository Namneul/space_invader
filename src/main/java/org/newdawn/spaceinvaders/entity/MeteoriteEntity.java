package org.newdawn.spaceinvaders.entity;

import org.newdawn.spaceinvaders.Game;
import org.newdawn.spaceinvaders.Sprite;
import org.newdawn.spaceinvaders.SpriteStore;

public class MeteoriteEntity extends Entity {
    private double moveSpeed = 300;

    private Game game;

    private Sprite[] frames = new Sprite[16];

    private long frameDuration = 50;

    private long lastFrameChange;

    private int frameNumber;
    /**
     * Create a new meteorite
     *
     * @param game The game in which the meteorite has been created
     * @param x The initial x location of the meteorite
     * @param y The initial y location of the meteorite
     */
    public MeteoriteEntity(Game game, int x, int y) {
        super("sprites/meteor/b10000.png", x, y); // 임시 이미지 사용
        this.game = game;
        this.dy = moveSpeed;// 아래로 떨어지도록 수직 속도 설정

        for(int i = 0; i < frames.length; i++) {
            frames[i] = SpriteStore.get().getSprite("sprites/meteor/b1000"+i+".png");
        }
    }

    /**
     * Request that this meteorite moved based on time elapsed
     *
     * @param delta The time that has elapsed since last move
     */
    @Override
    public void move(long delta) {
        // proceed with normal move
        super.move(delta);

        lastFrameChange += delta;

        if (lastFrameChange > frameDuration) {
            // reset our frame change time counter
            lastFrameChange = 0;

            // update the frame
            frameNumber++;
            if (frameNumber >= frames.length) {
                frameNumber = 0;
            }

            sprite = frames[frameNumber];
        }

        // if we shot off the screen, remove ourselfs
        if (y > 600) { // 화면 아래로 벗어나면 제거
            game.removeEntity(this);
        }
    }

    @Override
    public void collidedWith(Entity other) {
        if (other instanceof ShipEntity) {
            // 운석 자신을 게임에서 제거합니다.
            game.removeEntity(this);
            // 게임에 플레이어를 2초간 스턴시키라고 알립니다.
            game.stunPlayer();
        }
    }
}
