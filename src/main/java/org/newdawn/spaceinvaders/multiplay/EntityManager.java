package org.newdawn.spaceinvaders.multiplay;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

public class EntityManager {

    private final Map<Integer, Entity> entities = new TreeMap<>();
    private final ArrayList<Integer> removeList = new ArrayList<>();
    private int smallestAvailableId = 0;

    public EntityManager() {

    }

    public int getNextAvailableId() {
        return smallestAvailableId++;
    }

    public void addEntity(Entity entity) {
        entities.put(entity.getId(), entity);
    }

    public void removeEntity(int id) {
        removeList.add(id);
    }

    public Entity getEntity(int id) {
        return entities.get(id);
    }

    public Map<Integer, Entity> getEntities() {
        return entities;
    }

    public void updateAll() {
        // 동시성 문제를 피하기 위해 엔티티 목록의 복사본을 만듭니다.
        final Map<Integer, Entity> entitiesCopy = new TreeMap<>(entities);

        // 1. 모든 엔티티의 상태를 업데이트합니다. (ServerGame.tickEntities)
        for (final Entity entity: entitiesCopy.values()) {
            entity.tick();
        }

        // 2. 엔티티 간의 충돌을 감지하고 처리합니다. (ServerGame.handleCollisions)
        for (final Entity entity1 : entitiesCopy.values()) {
            for (final Entity entity2 : entitiesCopy.values()) {
                if (entity1.isColliding(entity2)) {
                    entity1.handleCollision(entity2);
                }
            }
        }

        // 3. 제거 목록에 포함된 엔티티를 정리합니다. (ServerGame.removeDeadEntities)
        for (Integer id: removeList){
            entities.remove(id);
        }
        removeList.clear();
    }
}

