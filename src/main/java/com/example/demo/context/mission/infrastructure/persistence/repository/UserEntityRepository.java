package com.example.demo.context.mission.infrastructure.persistence.repository;

import com.example.demo.context.mission.infrastructure.persistence.entity.UserEntity;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface UserEntityRepository extends CrudRepository<UserEntity, Long> {

    @Query("SELECT created_at FROM users WHERE id = :id")
    Optional<LocalDateTime> findCreatedAtById(@Param("id") Long id);
}
