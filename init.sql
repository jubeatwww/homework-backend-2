-- =============================================
-- Schema
-- =============================================

CREATE TABLE IF NOT EXISTS users
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    username   VARCHAR(50) NOT NULL UNIQUE,
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS games
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS login_records
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT NOT NULL,
    login_date DATE   NOT NULL,
    UNIQUE KEY uk_user_login_date (user_id, login_date),
    FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS game_launch_records
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT   NOT NULL,
    game_id     BIGINT   NOT NULL,
    launched_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_game_launch (user_id, game_id),
    FOREIGN KEY (user_id) REFERENCES users (id),
    FOREIGN KEY (game_id) REFERENCES games (id)
);

CREATE TABLE IF NOT EXISTS games_play_record
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    game_id         BIGINT       NOT NULL,
    score           INT          NOT NULL DEFAULT 0,
    idempotency_key VARCHAR(100) NOT NULL,
    played_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_play_idempotency_key (user_id, idempotency_key),
    FOREIGN KEY (user_id) REFERENCES users (id),
    FOREIGN KEY (game_id) REFERENCES games (id)
);

CREATE TABLE IF NOT EXISTS missions
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT      NOT NULL,
    mission_type VARCHAR(30) NOT NULL,
    completed    BOOLEAN     NOT NULL DEFAULT FALSE,
    completed_at DATETIME    NULL,
    expired_at   DATETIME    NOT NULL,
    UNIQUE KEY uk_user_mission (user_id, mission_type),
    FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS rewards
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT   NOT NULL,
    points      INT      NOT NULL DEFAULT 0,
    rewarded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_reward (user_id),
    FOREIGN KEY (user_id) REFERENCES users (id)
);

-- =============================================
-- Seed data
-- =============================================

-- Users
INSERT INTO users (id, username, created_at)
VALUES (1, 'player1', CURRENT_TIMESTAMP),
       (2, 'player2', DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 45 DAY)),
       (3, 'player3', CURRENT_TIMESTAMP);

-- Games
INSERT INTO games (id, name)
VALUES (1, 'Space Invaders'),
       (2, 'Pac-Man'),
       (3, 'Tetris'),
       (4, 'Snake'),
       (5, 'Pong');

-- Missions are created dynamically by the application
-- when a user first triggers an action (login/launchGame/play).
-- The 30-day window starts from that moment.
