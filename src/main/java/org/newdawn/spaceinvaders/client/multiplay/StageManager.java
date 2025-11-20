package org.newdawn.spaceinvaders.client.multiplay;

import org.newdawn.spaceinvaders.client.multiplay.ServerEntity.Entity;
import org.newdawn.spaceinvaders.client.multiplay.ServerEntity.EntityType;
import org.newdawn.spaceinvaders.client.multiplay.ServerEntity.ServerBossEntity;
import org.newdawn.spaceinvaders.client.multiplay.stage.*;
import org.newdawn.spaceinvaders.multiplay.stage.*;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StageManager {

    private final ServerGame game;
    private final EntityManager manager;
    private final EntityFactory factory;
    private ArrayList<Stage> stages;
    private int currentStageIndex;
    private Stage currentStage;

    private final Logger logger = Logger.getLogger(getClass().getName());

    public StageManager(ServerGame game, EntityManager manager, EntityFactory factory) {
        this.game = game;
        this.manager = manager;
        this.factory = factory;
        loadStages();
    }

    private void loadStages(){
        stages = new ArrayList<>();
        stages.add(new Stage1());
        stages.add(new Stage2());
        stages.add(new Stage3());
        stages.add(new Stage4());
        stages.add(new Stage5());
    }

//첫 번째 스테이지 시작
    public void initializeFirstStage(){
        currentStageIndex = 0;
        currentStage = stages.get(currentStageIndex);
        currentStage.initialize(game, manager, factory);
        logger.info("ServerGame: " + currentStage.getStageName() + " initialized.");
    }
    //스테이지에 따라 메테오 생성
    public void spawnMeteorsIfNeeded() {
        if (currentStageIndex > 2 && Math.random() < 0.003) {
            factory.createMeteor();
        }
    }

    //현재 스테이지 클리어 후 다음 스테이지로 진행
    public void progressToNextStage() {
        clearStageEntities();

        // 현재 스테이지가 5스테이지(인덱스 4)의 첫 웨이브였다면, 보스를 생성합니다.
        if (currentStageIndex == 4) {
            // 보스가 아직 없다면 보스를 생성
            boolean bossExists = false;
            for (Entity e : manager.getEntities().values()) {
                if (e instanceof ServerBossEntity) {
                    bossExists = true;
                    break;
                }
            }
            if (!bossExists) {
                logger.info("[서버 로그] 5스테이지 첫 웨이브 클리어. 보스를 생성합니다.");
                factory.createBoss(350, 50);

                game.setAlienCount(1);

                return; // 다음 스테이지로 넘어가지 않고 보스전을 시작합니다.
            }
        }

        currentStageIndex++; // 다음 스테이지로 인덱스를 증가시킵니다.

        // 만약 마지막 스테이지까지 모두 클리어했다면
        if (currentStageIndex >= stages.size()) {
            logger.info("[서버 로그] 모든 스테이지 클리어! 게임에서 승리했습니다.");
            // ServerGame의 게임 승리 처리 로직을 호출합니다.
            game.handleGameWin();
        }
        // 다음 스테이지가 남아있다면
        else {
            logger.log(Level.INFO,"[서버 로그] 다음 스테이지({0})를 시작합니다.", (currentStageIndex + 1));
            currentStage = stages.get(currentStageIndex);
            currentStage.initialize(game, manager, factory); // 다음 스테이지의 적들을 생성합니다.
        }
    }

    private void clearStageEntities() {
        for (Entity entity : manager.getEntities().values()) {

            if (entity.getType() != EntityType.PLAYER) {
                manager.removeEntity(entity.getId());
            }
        }
    }

}
