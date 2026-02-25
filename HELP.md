# HELP – Mission Center Service

## Prerequisites

| Tool              | Version  |
|-------------------|----------|
| Java (JDK)        | 21+      |
| Maven             | 3.9+     |
| Docker & Compose  | latest   |

---

## Quick Start (Recommended)

> **⚠️ Important: It is strongly recommended to use Docker Compose to start the entire system.**
>
> RocketMQ Broker registers with Namesrv using its Docker-internal container hostname (e.g. `rocketmq-namesrv`).
> When the application runs outside the Docker network (i.e. on the host machine), it cannot resolve that hostname,
> causing Producers/Consumers to fail connecting to the Broker.
> To solve this, the project includes a `Dockerfile` and an `app` service in `docker-compose.yaml`,
> so the application runs inside the same Docker network as the infrastructure, completely avoiding domain resolving issues.

### Start

```bash
docker-compose up -d
```

Wait until all services are healthy (~30 seconds). You can verify with:

```bash
docker-compose ps
```

The application starts on **http://localhost:9090**.

| Service            | Port  | Description            |
|--------------------|-------|------------------------|
| **app**            | 9090  | Mission Center Service |
| MySQL              | 3306  | Persistence            |
| Redis              | 6379  | Caching                |
| RocketMQ Namesrv   | 9876  | MQ Name Server         |
| RocketMQ Broker    | 10911 | MQ Broker              |
| RocketMQ Console   | 8088  | MQ Management UI       |

MySQL credentials: `taskuser` / `taskpass`, database: `taskdb`

### Local Development (For Reference Only — RocketMQ Will Not Work)

If you only need to run unit tests or do not require MQ functionality, you can start the infrastructure separately and run the app on the host:

```bash
# Start infrastructure only
docker-compose up -d mysql redis rocketmq-namesrv rocketmq-broker

# Run app locally (RocketMQ features will not work properly)
./mvnw spring-boot:run
```

---

## Database Schema

Tables are automatically created by `init.sql` (mounted into MySQL's `docker-entrypoint-initdb.d`):

| Table                | Purpose                                       |
|----------------------|-----------------------------------------------|
| `users`              | User information (seeded with 3 players)      |
| `games`              | Game metadata (seeded with 5 games)           |
| `login_records`      | Tracks daily login per user (unique per date)  |
| `game_launch_records`| Tracks distinct game launches per user         |
| `games_play_record`  | Game play sessions with score and idempotency  |
| `missions`           | Per-user mission completion status (3 types)   |
| `rewards`            | Reward records (777 points on all-complete)    |

Seed data includes users `player1`–`player3` and games `Space Invaders`, `Pac-Man`, `Tetris`, `Snake`, `Pong`.

---

## API Reference

### POST /login

Simulate a user login event.

```bash
curl -X POST http://localhost:9090/login \
  -H "Content-Type: application/json" \
  -d '{"userId": 1, "loginDate": "2026-01-10"}'
```

### POST /launchGame

Record a game launch event.

```bash
curl -X POST http://localhost:9090/launchGame \
  -H "Content-Type: application/json" \
  -d '{"userId": 1, "gameId": 1}'
```

### POST /play

Record a gameplay session. Requires an idempotency key (header or body).

```bash
curl -X POST http://localhost:9090/play \
  -H "Content-Type: application/json" \
  -H "X-Idempotency-Key: play-001" \
  -d '{"userId": 1, "gameId": 1, "score": 500}'
```

### GET /missions

Get mission list with progress for a user.

```bash
curl http://localhost:9090/missions?userId=1
```

Response example:

```json
[
  {
    "id": 1,
    "missionType": "CONSECUTIVE_LOGIN",
    "description": "Log in for 3 consecutive days",
    "criteria": [
      { "label": "consecutiveDays", "progress": 2, "target": 3 }
    ],
    "completed": false,
    "completedAt": null,
    "expiredAt": "2026-02-10T00:00:00"
  },
  {
    "id": 2,
    "missionType": "DIFFERENT_GAMES",
    "description": "Launch at least 3 different games",
    "criteria": [
      { "label": "distinctGames", "progress": 3, "target": 3 }
    ],
    "completed": true,
    "completedAt": "2026-01-15T14:30:00",
    "expiredAt": "2026-02-10T00:00:00"
  },
  {
    "id": 3,
    "missionType": "PLAY_SCORE",
    "description": "Play at least 3 game sessions with combined score over 1,000",
    "criteria": [
      { "label": "sessions", "progress": 5, "target": 3 },
      { "label": "totalScore", "progress": 800, "target": 1000 }
    ],
    "completed": false,
    "completedAt": null,
    "expiredAt": "2026-02-10T00:00:00"
  }
]
```

---

## Sample Workflow

Complete all missions for user 1:

```bash
# Day 1 login
curl -X POST http://localhost:9090/login -H "Content-Type: application/json" \
  -d '{"userId": 1, "loginDate": "2026-01-01"}'

# Day 2 login
curl -X POST http://localhost:9090/login -H "Content-Type: application/json" \
  -d '{"userId": 1, "loginDate": "2026-01-02"}'

# Day 3 login → completes CONSECUTIVE_LOGIN
curl -X POST http://localhost:9090/login -H "Content-Type: application/json" \
  -d '{"userId": 1, "loginDate": "2026-01-03"}'

# Launch 3 different games → completes DIFFERENT_GAMES
curl -X POST http://localhost:9090/launchGame -H "Content-Type: application/json" \
  -d '{"userId": 1, "gameId": 1}'
curl -X POST http://localhost:9090/launchGame -H "Content-Type: application/json" \
  -d '{"userId": 1, "gameId": 2}'
curl -X POST http://localhost:9090/launchGame -H "Content-Type: application/json" \
  -d '{"userId": 1, "gameId": 3}'

# Play 3 sessions with total score > 1000 → completes PLAY_SCORE
curl -X POST http://localhost:9090/play -H "Content-Type: application/json" \
  -H "X-Idempotency-Key: s1" -d '{"userId": 1, "gameId": 1, "score": 400}'
curl -X POST http://localhost:9090/play -H "Content-Type: application/json" \
  -H "X-Idempotency-Key: s2" -d '{"userId": 1, "gameId": 2, "score": 400}'
curl -X POST http://localhost:9090/play -H "Content-Type: application/json" \
  -H "X-Idempotency-Key: s3" -d '{"userId": 1, "gameId": 3, "score": 300}'

# Check mission progress
curl http://localhost:9090/missions?userId=1
```

When all 3 missions are completed, the system automatically grants a **777-point reward**.

---

## Architecture

```
context.mission
├── interfaces.rest          # Controllers, DTOs (inbound adapters)
├── application
│   ├── command              # CQRS command handlers (LoginCommand, LaunchGameCommand, PlayGameCommand)
│   ├── query                # CQRS query handlers (GetMissionsQuery)
│   ├── service              # Application services (MissionProgressService, MissionInitializationService, UserEligibilityService)
│   └── port                 # Outbound port interfaces (repositories, caches, event publishers)
├── domain
│   ├── model                # Aggregate root (Mission), value objects (MissionType)
│   ├── event                # Domain events (UserLoggedInEvent, GameLaunchedEvent, etc.)
│   ├── exception            # Domain exceptions (UserNotFoundException, GameNotFoundException)
│   └── repository           # Repository interfaces
└── infrastructure
    ├── persistence          # Spring Data JDBC adapters, entities, repositories
    ├── messaging            # RocketMQ consumers and publishers
    └── cache                # Redis caching adapters
```

**Key design decisions:**

- **CQRS** — Commands (write) and Queries (read) are handled by separate handler classes via a bus
- **Domain-Driven Design** — Domain layer has zero Spring dependency; pure Java models
- **Event-driven** — User actions publish domain events to RocketMQ; consumers update mission progress asynchronously
- **One-way completion latch** — Mission completion uses a conditional `UPDATE ... WHERE completed = false`, making it naturally idempotent without optimistic locking
- **Computed progress** — Progress is derived at query time from source tables (login/launch/play records), not stored redundantly
- **Idempotency** — Play requests require an idempotency key to prevent duplicate scoring
- **30-day expiration** — Missions expire 30 days after user registration; checked on every progress update

---

## Running Tests

```bash
./mvnw test
```

Tests use Mockito for unit testing and `@WebMvcTest` for controller-layer tests. No external infrastructure required.
