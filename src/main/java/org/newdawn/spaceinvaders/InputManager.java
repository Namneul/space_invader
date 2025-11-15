package org.newdawn.spaceinvaders;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class InputManager extends KeyAdapter{

    private boolean leftPressed = false;
    private boolean rightPressed = false;
    private boolean firePressed = false;
    private boolean waitingForKeyPress = true;

    @Override
    public void keyPressed(KeyEvent e) {
        if (waitingForKeyPress){
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

    @Override
    public void keyTyped(KeyEvent e) {
        if (waitingForKeyPress) {
            waitingForKeyPress = false;
        }

        // ESC 키 처리 (게임 종료)
        if (e.getKeyChar() == 27) {
            System.exit(0);
        }
    }

    public boolean isLeftPressed() {
        return leftPressed;
    }

    public boolean isRightPressed() {
        return rightPressed;
    }

    public boolean isFirePressed() {
        return firePressed;
    }

    public boolean isWaitingForKeyPress() {
        return waitingForKeyPress;
    }

    public void setWaitingForKeyPress(boolean waitingForKeyPress) {
        this.waitingForKeyPress = waitingForKeyPress;
    }

    // 필요하다면 입력 상태를 강제로 초기화하는 메소드
    public void reset() {
        leftPressed = false;
        rightPressed = false;
        firePressed = false;
    }
}
