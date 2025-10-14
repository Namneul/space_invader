package org.newdawn.spaceinvaders.multiplay.ServerEntity;

import org.newdawn.spaceinvaders.multiplay.Server;
import org.newdawn.spaceinvaders.multiplay.ServerGame;

public class ServerReflectAlienEntity extends ServerGame.Entity {
    public ServerReflectAlienEntity(ServerGame game, double x, double y) {
        super(game, 20, 20, x, y);
        moveSpeed = 75;
        dx = -moveSpeed;
        this.type = ServerGame.EntityType.REFLECT_ALIEN;
    }

    @Override
    public void tick() {
        setX(getX()+dx/ Server.TICKS_PER_SECOND);
        if (dx<0 && getX()<10 || dx>0 && getX()>750){
            this.game.requestLogicUpdate();
        }
    }


    public void setMoveSpeed(double moveSpeed){
        this.moveSpeed = moveSpeed;
    }
    public double getMoveSpeed(){ return moveSpeed; }

    public void doLogic(){
        dx = -dx;
        setY(getY()+10);
        if (getY()>570){
            game.notifyDeath(this.getId());
        }
    }

    @Override
    public void handleCollision(ServerGame.Entity otherEntity) {
        if (otherEntity instanceof ServerShotEntity){
            game.removeEntity(otherEntity.getId());
            game.notifyDeath(this.getId());
        }
    }
}
