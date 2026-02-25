package com.example.demo.context.mission.infrastructure.persistence;

import com.example.demo.context.mission.application.port.GameQueryPort;
import com.example.demo.context.mission.application.port.UserQueryPort;
import com.example.demo.context.mission.infrastructure.persistence.repository.GameEntityRepository;
import com.example.demo.context.mission.infrastructure.persistence.repository.UserEntityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ReferenceDataAdapter implements UserQueryPort, GameQueryPort {

    private final UserEntityRepository userEntityRepository;
    private final GameEntityRepository gameEntityRepository;

    @Override
    public boolean userExists(Long userId) {
        return userEntityRepository.existsById(userId);
    }

    @Override
    public Optional<LocalDateTime> getUserCreatedAt(Long userId) {
        return userEntityRepository.findCreatedAtById(userId);
    }

    @Override
    public boolean gameExists(Long gameId) {
        return gameEntityRepository.existsById(gameId);
    }
}
