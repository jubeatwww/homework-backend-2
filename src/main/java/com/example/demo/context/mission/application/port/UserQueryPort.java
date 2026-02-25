package com.example.demo.context.mission.application.port;

import java.time.LocalDateTime;
import java.util.Optional;

public interface UserQueryPort {

    boolean userExists(Long userId);

    Optional<LocalDateTime> getUserCreatedAt(Long userId);
}
