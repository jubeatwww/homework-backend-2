package com.example.demo.context.mission.infrastructure.persistence;

import com.example.demo.context.mission.application.port.RewardRepository;
import com.example.demo.context.mission.infrastructure.persistence.repository.RewardEntityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RewardRepositoryAdapter implements RewardRepository {

    private final RewardEntityRepository rewardEntityRepository;

    @Override
    public boolean grantReward(Long userId, int points) {
        return rewardEntityRepository.insertIgnore(userId, points);
    }

    @Override
    public boolean isRewarded(Long userId) {
        return rewardEntityRepository.countByUserId(userId) > 0;
    }
}
