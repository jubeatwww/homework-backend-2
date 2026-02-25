package com.example.demo.context.mission.infrastructure.persistence.repository;

import com.example.demo.context.mission.infrastructure.persistence.entity.LoginRecordEntity;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface LoginRecordEntityRepository extends Repository<LoginRecordEntity, Long> {

    @Modifying
    @Query("INSERT IGNORE INTO login_records (user_id, login_date) VALUES (:userId, :loginDate)")
    boolean insertIgnore(@Param("userId") Long userId, @Param("loginDate") LocalDate loginDate);

    @Query("SELECT login_date FROM login_records WHERE user_id = :userId AND login_date <= :asOfDate ORDER BY login_date DESC LIMIT 30")
    List<LocalDate> findRecentLoginDates(@Param("userId") Long userId, @Param("asOfDate") LocalDate asOfDate);
}
