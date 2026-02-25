package com.example.demo.context.mission.infrastructure.persistence;

import com.example.demo.context.mission.application.port.GameLaunchRecordPort;
import com.example.demo.context.mission.application.port.GamePlayRecordPort;
import com.example.demo.context.mission.application.port.LoginRecordPort;
import com.example.demo.context.mission.infrastructure.persistence.repository.GameLaunchRecordEntityRepository;
import com.example.demo.context.mission.infrastructure.persistence.repository.GamePlayRecordEntityRepository;
import com.example.demo.context.mission.infrastructure.persistence.repository.LoginRecordEntityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class UserActionAdapter implements LoginRecordPort, GameLaunchRecordPort, GamePlayRecordPort {

    private final LoginRecordEntityRepository loginRecordEntityRepository;
    private final GameLaunchRecordEntityRepository gameLaunchRecordEntityRepository;
    private final GamePlayRecordEntityRepository gamePlayRecordEntityRepository;

    // ── LoginRecordPort ─────────────────────────────────────────────────────

    @Override
    public boolean recordLogin(Long userId, LocalDate loginDate) {
        return loginRecordEntityRepository.insertIgnore(userId, loginDate);
    }

    @Override
    public int countConsecutiveLoginDays(Long userId, LocalDate asOfDate) {
        List<LocalDate> dates = loginRecordEntityRepository.findRecentLoginDates(userId, asOfDate);
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

    // ── GameLaunchRecordPort ────────────────────────────────────────────────

    @Override
    public boolean recordGameLaunch(Long userId, Long gameId) {
        return gameLaunchRecordEntityRepository.insertIgnore(userId, gameId);
    }

    @Override
    public int countDistinctGamesLaunched(Long userId) {
        return gameLaunchRecordEntityRepository.countDistinctGamesByUserId(userId);
    }

    // ── GamePlayRecordPort ──────────────────────────────────────────────────

    @Override
    public boolean recordGamePlay(Long userId, Long gameId, int score, String idempotencyKey) {
        return gamePlayRecordEntityRepository.insertIgnore(userId, gameId, score, idempotencyKey);
    }

    @Override
    public int sumPlayScores(Long userId) {
        return gamePlayRecordEntityRepository.sumScoresByUserId(userId);
    }

    @Override
    public int countPlaySessions(Long userId) {
        return gamePlayRecordEntityRepository.countByUserId(userId);
    }
}
