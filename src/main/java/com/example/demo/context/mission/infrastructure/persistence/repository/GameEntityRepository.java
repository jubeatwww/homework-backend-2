package com.example.demo.context.mission.infrastructure.persistence.repository;

import com.example.demo.context.mission.infrastructure.persistence.entity.GameEntity;
import org.springframework.data.repository.CrudRepository;

public interface GameEntityRepository extends CrudRepository<GameEntity, Long> {
}
