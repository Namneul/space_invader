package org.newdawn.spaceinvaders.stage;

import org.newdawn.spaceinvaders.Game;
import org.newdawn.spaceinvaders.entity.Entity;

import java.util.ArrayList;

// 'abstract' 키워드를 사용하여 추상 클래스로 선언
public abstract class Stage {

    // 모든 스테이지 클래스가 반드시 구현해야 하는 추상 메소드
    // 게임에 필요한 정보(Game, entities 리스트)를 파라미터로 받습니다.
    public abstract void initialize(Game game, ArrayList<Entity> entities);

    // (선택 사항) 각 스테이지의 이름을 반환하는 메소드
    public abstract String getStageName();
}