#!/usr/bin/env bash
# Smoke test — full mission lifecycle
# Prerequisites: app running on :9090, MySQL, Redis, RocketMQ all up
#
# DB seed (init.sql) provides:
#   user 1 (player1) — created_at = now  (eligible)
#   user 2 (player2) — need manual UPDATE to expire (see below)
#   user 3 (player3) — created_at = now  (used for full reward flow)
#   user 999          — does not exist
#   games 1-5

BASE=http://localhost:9090
SLEEP_FOR_MQ=2  # seconds to wait for async MQ processing
GREEN='\033[0;32m'
RED='\033[0;31m'
CYAN='\033[0;36m'
NC='\033[0m'
TODAY=$(date +%Y-%m-%d)
DAY2=$(date -d "+1 day" +%Y-%m-%d 2>/dev/null || date -v+1d +%Y-%m-%d)
DAY3=$(date -d "+2 days" +%Y-%m-%d 2>/dev/null || date -v+2d +%Y-%m-%d)
SEQ=0
idem() { SEQ=$((SEQ+1)); echo "test-smoke-$SEQ-$RANDOM-$(date +%s%N)"; }

section() { echo -e "\n${CYAN}=== $1 ===${NC}"; }
pass_fail() {
  local desc=$1 expected=$2 actual=$3
  if [ "$actual" -eq "$expected" ]; then
    echo -e "  ${GREEN}PASS${NC} $desc (HTTP $actual)"
  else
    echo -e "  ${RED}FAIL${NC} $desc — expected $expected, got $actual"
  fi
}
# Check response body contains a string
check_body() {
  local desc=$1 expected_http=$2 body_contains=$3 actual_http=$4 actual_body=$5
  if [ "$actual_http" -eq "$expected_http" ]; then
    if echo "$actual_body" | grep -q "$body_contains"; then
      echo -e "  ${GREEN}PASS${NC} $desc (HTTP $actual_http, body contains '$body_contains')"
    else
      echo -e "  ${RED}FAIL${NC} $desc — HTTP $actual_http OK but body missing '$body_contains'"
    fi
  else
    echo -e "  ${RED}FAIL${NC} $desc — expected HTTP $expected_http, got $actual_http"
  fi
}

# -------------------------------------------------------
section "Setup: mark user 2 as registered 31 days ago"
# -------------------------------------------------------
echo "  Run this SQL if not already done:"
echo "    UPDATE users SET created_at = NOW() - INTERVAL 31 DAY WHERE id = 2;"
echo "  Press Enter to continue..."
read -r

# -------------------------------------------------------
section "1. Non-existent user → 404 (UserNotFound)"
# -------------------------------------------------------
HTTP=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE/login" \
  -H "Content-Type: application/json" \
  -d "{\"userId\": 999, \"loginDate\": \"$TODAY\"}")
pass_fail "POST /login userId=999" 404 "$HTTP"

HTTP=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE/launchGame" \
  -H "Content-Type: application/json" \
  -d '{"userId": 999, "gameId": 1}')
pass_fail "POST /launchGame userId=999" 404 "$HTTP"

# Non-existent game → 404
HTTP=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE/launchGame" \
  -H "Content-Type: application/json" \
  -d '{"userId": 1, "gameId": 9999}')
pass_fail "POST /launchGame gameId=9999" 404 "$HTTP"

# Missing idempotency key → 400
HTTP=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE/play" \
  -H "Content-Type: application/json" \
  -d '{"userId": 1, "gameId": 1, "score": 100}')
pass_fail "POST /play without idempotencyKey" 400 "$HTTP"

# -------------------------------------------------------
section "2. Eligible user (user 1) → 202, event sent to MQ"
# -------------------------------------------------------
HTTP=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE/login" \
  -H "Content-Type: application/json" \
  -d "{\"userId\": 1, \"loginDate\": \"$TODAY\"}")
pass_fail "POST /login userId=1" 202 "$HTTP"

HTTP=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE/launchGame" \
  -H "Content-Type: application/json" \
  -d '{"userId": 1, "gameId": 1}')
pass_fail "POST /launchGame userId=1 gameId=1" 202 "$HTTP"

KEY=$(idem)
HTTP=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE/play" \
  -H "Content-Type: application/json" \
  -H "X-Idempotency-Key: $KEY" \
  -d '{"userId": 1, "gameId": 1, "score": 100}')
pass_fail "POST /play userId=1 gameId=1 (via header key)" 202 "$HTTP"

echo "  Waiting ${SLEEP_FOR_MQ}s for MQ processing..."
sleep "$SLEEP_FOR_MQ"

# Verify missions exist via GET /missions
RESP=$(curl -s -w "\n%{http_code}" "$BASE/missions?userId=1")
BODY=$(echo "$RESP" | head -n -1)
CODE=$(echo "$RESP" | tail -n 1)
check_body "GET /missions userId=1" 200 "CONSECUTIVE_LOGIN" "$CODE" "$BODY"

# -------------------------------------------------------
section "3. Expired user (user 2) → 202 (event sent), but consumer skips"
# -------------------------------------------------------
HTTP=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE/login" \
  -H "Content-Type: application/json" \
  -d "{\"userId\": 2, \"loginDate\": \"$TODAY\"}")
pass_fail "POST /login userId=2 (expired)" 202 "$HTTP"

HTTP=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE/launchGame" \
  -H "Content-Type: application/json" \
  -d '{"userId": 2, "gameId": 1}')
pass_fail "POST /launchGame userId=2 (expired)" 202 "$HTTP"

KEY=$(idem)
HTTP=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE/play" \
  -H "Content-Type: application/json" \
  -d "{\"userId\": 2, \"gameId\": 1, \"score\": 50, \"idempotencyKey\": \"$KEY\"}")
pass_fail "POST /play userId=2 (expired)" 202 "$HTTP"

echo ""
echo "  -> All 3 should return 202 (event dispatched to MQ)"
echo "  -> Check app logs: consumer should log 'expired' and skip mission processing"
echo "  -> Check Redis: key 'user:expired:2' should exist after first request"

# -------------------------------------------------------
section "4. Repeat expired user 2 → should hit Redis cache (no DB query)"
# -------------------------------------------------------
HTTP=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE/login" \
  -H "Content-Type: application/json" \
  -d "{\"userId\": 2, \"loginDate\": \"$TODAY\"}")
pass_fail "POST /login userId=2 (cached)" 202 "$HTTP"

echo ""
echo "  -> Check app logs for 'User 2 expired (cached)' — means Redis cache hit, no DB"

# -------------------------------------------------------
section "5. Quick Redis check"
# -------------------------------------------------------
echo "  Run: redis-cli EXISTS user:expired:2"
echo "  Expected: 1"
echo "  Run: redis-cli EXISTS user:expired:1"
echo "  Expected: 0"

# =========================================================
#  Mission completion → Reward flow (user 3)
# =========================================================
section "6. Mission: CONSECUTIVE_LOGIN — login 3 consecutive days"
# -------------------------------------------------------
HTTP=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE/login" \
  -H "Content-Type: application/json" \
  -d "{\"userId\": 3, \"loginDate\": \"$TODAY\"}")
pass_fail "POST /login userId=3 day1=$TODAY" 202 "$HTTP"

HTTP=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE/login" \
  -H "Content-Type: application/json" \
  -d "{\"userId\": 3, \"loginDate\": \"$DAY2\"}")
pass_fail "POST /login userId=3 day2=$DAY2" 202 "$HTTP"

HTTP=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE/login" \
  -H "Content-Type: application/json" \
  -d "{\"userId\": 3, \"loginDate\": \"$DAY3\"}")
pass_fail "POST /login userId=3 day3=$DAY3" 202 "$HTTP"

echo "  Waiting ${SLEEP_FOR_MQ}s for MQ processing..."
sleep "$SLEEP_FOR_MQ"

# Verify progress
RESP=$(curl -s -w "\n%{http_code}" "$BASE/missions?userId=3")
BODY=$(echo "$RESP" | head -n -1)
CODE=$(echo "$RESP" | tail -n 1)
check_body "GET /missions userId=3 (after login)" 200 "CONSECUTIVE_LOGIN" "$CODE" "$BODY"

# -------------------------------------------------------
section "7. Mission: DIFFERENT_GAMES — launch 3 different games"
# -------------------------------------------------------
HTTP=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE/launchGame" \
  -H "Content-Type: application/json" \
  -d '{"userId": 3, "gameId": 1}')
pass_fail "POST /launchGame userId=3 gameId=1" 202 "$HTTP"

HTTP=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE/launchGame" \
  -H "Content-Type: application/json" \
  -d '{"userId": 3, "gameId": 2}')
pass_fail "POST /launchGame userId=3 gameId=2" 202 "$HTTP"

HTTP=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE/launchGame" \
  -H "Content-Type: application/json" \
  -d '{"userId": 3, "gameId": 3}')
pass_fail "POST /launchGame userId=3 gameId=3" 202 "$HTTP"

echo "  Waiting ${SLEEP_FOR_MQ}s for MQ processing..."
sleep "$SLEEP_FOR_MQ"
echo "  -> Check logs: DIFFERENT_GAMES mission should be completed"

# -------------------------------------------------------
section "8. Mission: PLAY_SCORE — 3 sessions, total score > 1000"
# -------------------------------------------------------
KEY1=$(idem)
HTTP=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE/play" \
  -H "Content-Type: application/json" \
  -d "{\"userId\": 3, \"gameId\": 1, \"score\": 400, \"idempotencyKey\": \"$KEY1\"}")
pass_fail "POST /play userId=3 score=400" 202 "$HTTP"

KEY2=$(idem)
HTTP=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE/play" \
  -H "Content-Type: application/json" \
  -d "{\"userId\": 3, \"gameId\": 2, \"score\": 400, \"idempotencyKey\": \"$KEY2\"}")
pass_fail "POST /play userId=3 score=400" 202 "$HTTP"

KEY3=$(idem)
HTTP=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE/play" \
  -H "Content-Type: application/json" \
  -d "{\"userId\": 3, \"gameId\": 3, \"score\": 300, \"idempotencyKey\": \"$KEY3\"}")
pass_fail "POST /play userId=3 score=300 (total=1100, need >1000)" 202 "$HTTP"

echo "  Waiting ${SLEEP_FOR_MQ}s for MQ processing..."
sleep "$SLEEP_FOR_MQ"
echo "  -> Check logs: PLAY_SCORE mission should be completed"

# -------------------------------------------------------
section "9. Verify: all missions done → reward granted"
# -------------------------------------------------------
RESP=$(curl -s -w "\n%{http_code}" "$BASE/missions?userId=3")
BODY=$(echo "$RESP" | head -n -1)
CODE=$(echo "$RESP" | tail -n 1)
pass_fail "GET /missions userId=3 (final)" 200 "$CODE"
echo "  Response: $BODY"
echo ""
echo "  -> Check app logs for: 'Reward granted and event dispatched for userId=3'"
echo "  -> Check DB: SELECT * FROM rewards WHERE user_id = 3;"
echo "  -> Expected: 1 row, points = 777"

# -------------------------------------------------------
section "10. Idempotency: replay same play → should still 202, no double reward"
# -------------------------------------------------------
HTTP=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE/play" \
  -H "Content-Type: application/json" \
  -d "{\"userId\": 3, \"gameId\": 3, \"score\": 300, \"idempotencyKey\": \"$KEY3\"}")
pass_fail "POST /play replay same idempotencyKey" 202 "$HTTP"

echo "  -> DB rewards table should still have exactly 1 row for userId=3"

echo -e "\n${CYAN}Done.${NC}"
