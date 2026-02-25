package com.example.demo.context.mission.infrastructure.persistence;

import com.example.demo.context.mission.application.port.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JdbcUserActionRepository implements UserQueryPort, GameQueryPort, LoginRecordPort, GameLaunchRecordPort, GamePlayRecordPort {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public boolean userExists(Long userId) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM users WHERE id = ?",
            Integer.class,
            userId
        );
        return count != null && count > 0;
    }

    @Override
    public Optional<LocalDateTime> getUserCreatedAt(Long userId) {
        List<LocalDateTime> results = jdbcTemplate.query(
            "SELECT created_at FROM users WHERE id = ?",
            (rs, rowNum) -> rs.getTimestamp("created_at").toLocalDateTime(),
            userId
        );
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public boolean gameExists(Long gameId) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM games WHERE id = ?",
            Integer.class,
            gameId
        );
        return count != null && count > 0;
    }

    @Override
    public boolean recordLogin(Long userId, LocalDate loginDate) {
        int rows = jdbcTemplate.update(
            "INSERT IGNORE INTO login_records (user_id, login_date) VALUES (?, ?)",
            userId,
            loginDate
        );
        return rows > 0;
    }

    @Override
    public boolean recordGameLaunch(Long userId, Long gameId) {
        int rows = jdbcTemplate.update(
            "INSERT IGNORE INTO game_launch_records (user_id, game_id) VALUES (?, ?)",
            userId,
            gameId
        );
        return rows > 0;
    }

    @Override
    public boolean recordGamePlay(Long userId, Long gameId, int score, String idempotencyKey) {
        int rows = jdbcTemplate.update(
            "INSERT IGNORE INTO games_play_record (user_id, game_id, score, idempotency_key) VALUES (?, ?, ?, ?)",
            userId,
            gameId,
            score,
            idempotencyKey
        );
        return rows > 0;
    }

    @Override
    public int countDistinctGamesLaunched(Long userId) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(DISTINCT game_id) FROM game_launch_records WHERE user_id = ?",
            Integer.class,
            userId
        );
        return count != null ? count : 0;
    }

    @Override
    public int sumPlayScores(Long userId) {
        Integer sum = jdbcTemplate.queryForObject(
            "SELECT COALESCE(SUM(score), 0) FROM games_play_record WHERE user_id = ?",
            Integer.class,
            userId
        );
        return sum != null ? sum : 0;
    }

    @Override
    public int countPlaySessions(Long userId) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM games_play_record WHERE user_id = ?",
            Integer.class,
            userId
        );
        return count != null ? count : 0;
    }

    @Override
    public int countConsecutiveLoginDays(Long userId, LocalDate asOfDate) {
        List<LocalDate> dates = jdbcTemplate.query(
            "SELECT login_date FROM login_records WHERE user_id = ? AND login_date <= ? ORDER BY login_date DESC LIMIT 30",
            (rs, rowNum) -> rs.getDate("login_date").toLocalDate(),
            userId,
            asOfDate
        );

        int count = 0;
        LocalDate expected = asOfDate;
        for (LocalDate date : dates) {
            if (date.equals(expected)) {
                count++;
                expected = expected.minusDays(1);
            } else {
                break;
            }
        }
        return count;
    }
}
