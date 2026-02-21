# DevTamagochi Backend API Specification

> **Base URL:** `https://dev.taisu.site`
>
> **인증 방식:** JWT Bearer Token
>
> **WebSocket:** `wss://dev.taisu.site/ws?token={JWT}`

---

## 목차

1. [GitHub OAuth 로그인](#1-github-oauth-로그인)
2. [GitHub OAuth 콜백](#2-github-oauth-콜백)
3. [내 타마고치 상태 조회](#3-내-타마고치-상태-조회)
4. [밥주기 (Feed)](#4-밥주기-feed)
5. [Organization 목록 조회](#5-organization-목록-조회)
6. [Organization 레포 목록 조회](#6-organization-레포-목록-조회)
7. [Webhook 등록](#7-webhook-등록)
8. [GitHub Webhook 수신](#8-github-webhook-수신)
9. [WebSocket 실시간 알림](#9-websocket-실시간-알림)
10. [게임 결과 전송](#10-게임-결과-전송)
11. [게임 시스템 레퍼런스](#11-게임-시스템-레퍼런스)

---

## 공통 에러 응답

모든 API에서 에러 발생 시 아래 형식으로 응답합니다.

```json
{
  "status": 400,
  "message": "에러 설명",
  "timestamp": "2026-02-22T14:30:00"
}
```

| 상태 코드 | 설명 |
|-----------|------|
| `400 Bad Request` | 요청 형식 오류, 유효하지 않은 파라미터 |
| `401 Unauthorized` | 인증 실패 (JWT 없음 또는 만료) |
| `403 Forbidden` | 권한 부족 |
| `404 Not Found` | 리소스 없음 |
| `429 Too Many Requests` | GitHub API 사용량 초과 |
| `500 Internal Server Error` | 서버 내부 오류 |

---

## 1. GitHub OAuth 로그인

### Domain
AUTH

### HTTP Method
GET

### API Path
`/oauth/github/login`

### 권한
ALL (누구나 접근 가능)

### 토큰 유무
불필요

### 개요
사용자를 GitHub OAuth 인증 페이지로 리다이렉트합니다.

---

### Request

#### Headers
없음

#### Query Parameters
없음

#### Body
없음

---

### Response

#### Success (302 Redirect)

GitHub OAuth 인증 페이지로 리다이렉트됩니다.

```
Location: https://github.com/login/oauth/authorize?client_id=xxx&redirect_uri=xxx&scope=repo%20read:org%20user:follow
```

요청 OAuth Scope:
- `repo` — 레포지토리 접근
- `read:org` — Organization 읽기
- `user:follow` — 팔로워 정보

---

### Error List

| 상태 코드 | 설명 |
|-----------|------|
| `500 Internal Server Error` | OAuth URL 생성 실패 |

---

## 2. GitHub OAuth 콜백

### Domain
AUTH

### HTTP Method
GET

### API Path
`/oauth/github/callback`

### 권한
ALL (GitHub에서 리다이렉트)

### 토큰 유무
불필요

### 개요
GitHub에서 리다이렉트된 authorization code를 access token으로 교환하고, 사용자를 생성/업데이트한 후 JWT를 발급합니다.

---

### Request

#### Headers
없음

#### Query Parameters

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `code` | string | O | GitHub에서 전달한 authorization code |

---

### Response

#### Success (200 OK)

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIi...",
  "username": "octocat",
  "xp": 0,
  "level": "NEWBIE"
}
```

| 필드 | 타입 | 설명 |
|-----|------|------|
| `token` | string | JWT 액세스 토큰 (24시간 유효) |
| `username` | string | GitHub 사용자명 |
| `xp` | int | 누적 총 XP |
| `level` | string | 현재 레벨 (`NEWBIE` / `JUNIOR` / `MIDDLE` / `SENIOR`) |

---

### Error List

| 상태 코드 | 설명 |
|-----------|------|
| `400 Bad Request` | code 파라미터 누락 |
| `500 Internal Server Error` | GitHub 토큰 교환 실패 또는 사용자 정보 조회 실패 |

---

## 3. 내 타마고치 상태 조회

### Domain
TAMAGOTCHI

### HTTP Method
GET

### API Path
`/api/me/status`

### 권한
AUTHENTICATED

### 토큰 유무
필요

### 개요
인증된 사용자의 타마고치 현재 상태(레벨, XP, 알 개수)를 조회합니다.

---

### Request

#### Headers

| 헤더 | 값 | 필수 |
|-----|---|------|
| `Authorization` | `Bearer {access_token}` | O |

#### Body
없음

---

### Response

#### Success (200 OK)

```json
{
  "username": "octocat",
  "level": "JUNIOR",
  "currentLevelXp": 120,
  "xpToNextLevel": 240,
  "eggCount": 30,
  "totalXp": 480
}
```

| 필드 | 타입 | 설명 |
|-----|------|------|
| `username` | string | GitHub 사용자명 |
| `level` | string | 현재 레벨 (`NEWBIE` / `JUNIOR` / `MIDDLE` / `SENIOR`) |
| `currentLevelXp` | int | 현재 레벨 내 누적 XP (0~359) |
| `xpToNextLevel` | int | 다음 레벨까지 남은 XP. SENIOR일 경우 `0` |
| `eggCount` | int | 보유 알 개수 (Feed로 XP 변환 가능) |
| `totalXp` | int | 전체 누적 XP (레벨업 시에도 리셋되지 않음) |

---

### Error List

| 상태 코드 | 설명 |
|-----------|------|
| `401 Unauthorized` | JWT 토큰 없음 또는 만료 |
| `500 Internal Server Error` | 사용자 조회 실패 |

---

## 4. 밥주기 (Feed)

### Domain
TAMAGOTCHI

### HTTP Method
POST

### API Path
`/api/me/feed`

### 권한
AUTHENTICATED

### 토큰 유무
필요

### 개요
보유한 모든 알(egg)을 XP로 변환합니다. 알 1개 = XP 1. XP가 360 이상 누적되면 자동 레벨업됩니다.

---

### Request

#### Headers

| 헤더 | 값 | 필수 |
|-----|---|------|
| `Authorization` | `Bearer {access_token}` | O |

#### Body
없음

---

### Response

#### Success (200 OK)

```json
{
  "username": "octocat",
  "level": "JUNIOR",
  "currentLevelXp": 50,
  "xpToNextLevel": 310,
  "eggCount": 0,
  "totalXp": 410,
  "eggsConsumed": 30,
  "leveledUp": true
}
```

| 필드 | 타입 | 설명 |
|-----|------|------|
| `username` | string | GitHub 사용자명 |
| `level` | string | Feed 후 현재 레벨 |
| `currentLevelXp` | int | Feed 후 현재 레벨 내 XP (0~359) |
| `xpToNextLevel` | int | 다음 레벨까지 남은 XP |
| `eggCount` | int | Feed 후 남은 알 (항상 `0`) |
| `totalXp` | int | Feed 후 전체 누적 XP |
| `eggsConsumed` | int | 이번 Feed에서 소모된 알 개수 |
| `leveledUp` | boolean | 레벨업 발생 여부 |

#### Error (400 Bad Request) — 알이 없을 때

```json
{
  "status": 400,
  "message": "No eggs to feed",
  "timestamp": "2026-02-22T14:30:00"
}
```

---

### Error List

| 상태 코드 | 설명 |
|-----------|------|
| `400 Bad Request` | 보유 알이 0개일 때 |
| `401 Unauthorized` | JWT 토큰 없음 또는 만료 |

---

## 5. Organization 목록 조회

### Domain
ORG

### HTTP Method
GET

### API Path
`/api/orgs`

### 권한
AUTHENTICATED

### 토큰 유무
필요

### 개요
인증된 사용자가 속한 GitHub Organization 목록을 조회합니다.

---

### Request

#### Headers

| 헤더 | 값 | 필수 |
|-----|---|------|
| `Authorization` | `Bearer {access_token}` | O |

#### Body
없음

---

### Response

#### Success (200 OK)

GitHub API 응답을 그대로 프록시합니다.

```json
[
  {
    "login": "TEAM-HIGHTHON-10",
    "id": 123456,
    "url": "https://api.github.com/orgs/TEAM-HIGHTHON-10",
    "avatar_url": "https://avatars.githubusercontent.com/u/123456?v=4",
    "description": "Highthon Team 10"
  }
]
```

| 필드 | 타입 | 설명 |
|-----|------|------|
| `login` | string | Organization 이름 |
| `id` | long | GitHub Organization ID |
| `avatar_url` | string | 프로필 이미지 URL |
| `description` | string | Organization 설명 |

---

### Error List

| 상태 코드 | 설명 |
|-----------|------|
| `401 Unauthorized` | JWT 토큰 없음 또는 만료 |
| `429 Too Many Requests` | GitHub API 사용량 초과 |

---

## 6. Organization 레포 목록 조회

### Domain
ORG

### HTTP Method
GET

### API Path
`/api/orgs/{org}/repos`

### 권한
AUTHENTICATED

### 토큰 유무
필요

### 개요
특정 Organization의 레포지토리 목록을 조회합니다.

---

### Request

#### Headers

| 헤더 | 값 | 필수 |
|-----|---|------|
| `Authorization` | `Bearer {access_token}` | O |

#### Path Parameters

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `org` | string | O | Organization 이름 (예: `TEAM-HIGHTHON-10`) |

---

### Response

#### Success (200 OK)

GitHub API 응답을 그대로 프록시합니다.

```json
[
  {
    "id": 789012,
    "name": "BE",
    "full_name": "TEAM-HIGHTHON-10/BE",
    "private": false,
    "html_url": "https://github.com/TEAM-HIGHTHON-10/BE",
    "description": "DevTamagochi Backend",
    "language": "Java"
  }
]
```

| 필드 | 타입 | 설명 |
|-----|------|------|
| `name` | string | 레포지토리 이름 |
| `full_name` | string | 전체 경로 (`owner/repo`) |
| `private` | boolean | 비공개 여부 |
| `html_url` | string | GitHub 웹 URL |

---

### Error List

| 상태 코드 | 설명 |
|-----------|------|
| `401 Unauthorized` | JWT 토큰 없음 또는 만료 |
| `429 Too Many Requests` | GitHub API 사용량 초과 |

---

## 7. Webhook 등록

### Domain
WEBHOOK

### HTTP Method
POST

### API Path
`/api/webhook/register`

### 권한
AUTHENTICATED

### 토큰 유무
필요

### 개요
지정한 GitHub 레포지토리에 webhook을 등록하고, 해당 사용자의 레포 권한(UserRepoGrant)을 저장합니다. 이후 이 레포에서 발생하는 이벤트가 해당 사용자에게 알(egg)로 지급됩니다.

---

### Request

#### Headers

| 헤더 | 값 | 필수 |
|-----|---|------|
| `Authorization` | `Bearer {access_token}` | O |
| `Content-Type` | `application/json` | O |

#### Body

```json
{
  "owner": "TEAM-HIGHTHON-10",
  "repo": "BE"
}
```

| 필드 | 타입 | 필수 | 설명 |
|-----|------|------|------|
| `owner` | string | O | 레포 소유자 (사용자명 또는 Organization) |
| `repo` | string | O | 레포지토리 이름 |

---

### Response

#### Success (200 OK)

GitHub Webhook 생성 API 응답을 그대로 반환합니다.

```json
{
  "id": 456789,
  "type": "Repository",
  "name": "web",
  "active": true,
  "events": ["push", "pull_request", "issues", "member"],
  "config": {
    "url": "https://dev.taisu.site/github/webhook",
    "content_type": "json"
  }
}
```

---

### Error List

| 상태 코드 | 설명 |
|-----------|------|
| `401 Unauthorized` | JWT 토큰 없음 또는 만료 |
| `422 Unprocessable Entity` | 이미 동일 webhook이 등록되어 있음 (GitHub 응답) |
| `429 Too Many Requests` | GitHub API 사용량 초과 |
| `500 Internal Server Error` | GitHub API 호출 실패 |

---

## 8. GitHub Webhook 수신

### Domain
WEBHOOK

### HTTP Method
POST

### API Path
`/github/webhook`

### 권한
ALL (GitHub 서버에서 호출)

### 토큰 유무
불필요 (X-Hub-Signature-256 서명 검증)

### 개요
GitHub에서 발생한 이벤트를 수신합니다. HMAC-SHA256 서명을 검증한 후, 해당 레포를 등록한 사용자에게 퀘스트 완료 알(egg)을 지급합니다.

---

### Request

#### Headers

| 헤더 | 값 | 필수 |
|-----|---|------|
| `X-GitHub-Event` | 이벤트 타입 (`push`, `pull_request`, `issues`) | O |
| `X-Hub-Signature-256` | `sha256=...` (HMAC-SHA256 서명) | O |
| `Content-Type` | `application/json` | O |

#### Body

GitHub Webhook 표준 payload (JSON)

---

### 처리 로직

| GitHub Event | 조건 | 퀘스트 | 알 지급 |
|-------------|------|--------|---------|
| `push` | 항상 (head_commit 기준) | COMMIT | +10 eggs |
| `pull_request` | `action == "opened"` | PR | +10 eggs |
| `issues` | `action == "opened"` | ISSUE | +10 eggs |

- 레포를 등록한 모든 사용자에게 독립적으로 알 지급 (다중 사용자 격리)
- 중복 이벤트 방지: `(userId, eventType, eventUniqueId)` 기준 dedup

---

### Response

#### Success (200 OK)

```
OK
```

#### Error (401 Unauthorized) — 서명 불일치

```
Invalid signature
```

---

## 9. WebSocket 실시간 알림

### Domain
WEBSOCKET

### Protocol
WebSocket (wss)

### Endpoint
`wss://dev.taisu.site/ws?token={JWT}`

### 권한
AUTHENTICATED (JWT로 인증)

### 개요
서버에서 클라이언트로의 단방향 푸시 채널입니다. 퀘스트 완료 시 실시간으로 알림을 전송합니다.

---

### 연결 방법

```javascript
const ws = new WebSocket("wss://dev.taisu.site/ws?token=" + jwtToken);

ws.onmessage = (event) => {
  const data = JSON.parse(event.data);
  console.log(data);
};
```

- JWT가 유효하지 않으면 연결 즉시 종료 (CloseStatus: 1008 Policy Violation)
- 클라이언트 → 서버 메시지는 무시됩니다 (서버 푸시 전용)

---

### 이벤트: QUEST_COMPLETED

퀘스트 완료 시 서버가 전송하는 메시지입니다.

```json
{
  "type": "QUEST_COMPLETED",
  "questType": "COMMIT",
  "eggsEarned": 10,
  "totalEggs": 30
}
```

| 필드 | 타입 | 설명 |
|-----|------|------|
| `type` | string | 항상 `"QUEST_COMPLETED"` |
| `questType` | string | 퀘스트 종류: `"COMMIT"` / `"PR"` / `"ISSUE"` / `"FOLLOWER"` / `"GAME"` |
| `eggsEarned` | int | 이번 퀘스트로 획득한 알 개수 |
| `totalEggs` | int | 현재 보유 총 알 개수 |

### questType 상세

| questType | 발생 조건 | 알 지급 |
|-----------|---------|---------|
| `COMMIT` | push 이벤트 (head_commit 기준) | +10 |
| `PR` | pull_request opened | +10 |
| `ISSUE` | issues opened | +10 |
| `FOLLOWER` | GitHub 팔로워 증가 (5분 주기 동기화) | +10 per new follower |
| `GAME` | 게임 성공 완료 (POST /api/game/result) | +10 |

---

## 10. 게임 결과 전송

### Domain
GAME

### HTTP Method
POST

### API Path
`/api/game/result`

### 권한
AUTHENTICATED

### 토큰 유무
필요

### 개요
프론트에서 실행한 게임의 결과를 서버로 전송합니다. 서버는 JWT 인증만 검증하며, 별도의 게임 세션을 발급하지 않습니다. `result`가 `"SUCCESS"`이면 10개의 알(egg)이 지급되고 WebSocket으로 `QUEST_COMPLETED`/`GAME` 알림이 전송됩니다. `"FAIL"`이면 알이 지급되지 않습니다.

---

### Request

#### Headers

| 헤더 | 값 | 필수 |
|-----|---|------|
| `Authorization` | `Bearer {access_token}` | O |
| `Content-Type` | `application/json` | O |

#### Body

```json
{
  "result": "SUCCESS"
}
```

| 필드 | 타입 | 필수 | 설명 |
|-----|------|------|------|
| `result` | string | O | 게임 결과: `"SUCCESS"` 또는 `"FAIL"` |

---

### Response

#### Success (200 OK) — 게임 성공

```json
{
  "result": "SUCCESS",
  "eggs_earned": 10,
  "total_eggs": 40
}
```

#### Success (200 OK) — 게임 실패

```json
{
  "result": "FAIL",
  "eggs_earned": 0,
  "total_eggs": 30
}
```

| 필드 | 타입 | 설명 |
|-----|------|------|
| `result` | string | 게임 결과 (`"SUCCESS"` / `"FAIL"`) |
| `eggs_earned` | int | 획득한 알 개수 (SUCCESS: 10, FAIL: 0) |
| `total_eggs` | int | 현재 보유 총 알 개수 |

#### Error (400 Bad Request) — 잘못된 result 값

```json
{
  "status": 400,
  "message": "Invalid result value. Must be SUCCESS or FAIL",
  "timestamp": "2026-02-22T14:30:00"
}
```

---

### Error List

| 상태 코드 | 설명 |
|-----------|------|
| `400 Bad Request` | result 값이 SUCCESS 또는 FAIL이 아닌 경우 |
| `401 Unauthorized` | JWT 토큰 없음 또는 만료 |
| `500 Internal Server Error` | 서버 내부 오류 |

---

## 11. 게임 시스템 레퍼런스

### 레벨 시스템

| 레벨 | 표시명 | XP 범위 (currentLevelXp) |
|------|--------|------------------------|
| `NEWBIE` | 입문 | 0 ~ 359 |
| `JUNIOR` | 주니어 | 0 ~ 359 |
| `MIDDLE` | 미들 | 0 ~ 359 |
| `SENIOR` | 시니어 | 0+ (최대 레벨, 캡) |

- 레벨업 기준: **360 XP** 마다 다음 레벨로 진행
- 레벨업 시 `currentLevelXp`는 0으로 리셋 (초과분은 이월)
- `SENIOR`는 최대 레벨 — XP는 계속 쌓이지만 더 이상 레벨업 없음

### 게임 흐름

```
GitHub Event (push, PR, issue) 또는 게임 성공
    ↓
Quest Completed → +10 Eggs 지급
    ↓
WebSocket으로 QUEST_COMPLETED 알림
    ↓
사용자가 POST /api/me/feed 호출 (밥주기)
    ↓
모든 Eggs → XP로 변환
    ↓
currentLevelXp >= 360 이면 자동 레벨업
```

### 알(Egg) 지급 규칙

| 이벤트 | 알 지급량 |
|--------|----------|
| Commit 1회 (push) | +10 |
| PR 생성 1회 | +10 |
| Issue 생성 1회 | +10 |
| 팔로워 1명 증가 | +10 |
| 게임 성공 완료 | +10 |

- 알은 자동으로 XP가 되지 않음
- 반드시 Feed API를 호출해야 XP로 변환됨
- Feed 시 보유한 **모든** 알이 한번에 XP로 변환됨

### 팔로워 동기화 (Scheduled Job)

- **주기:** 5분마다 자동 실행
- **로직:** GitHub API로 현재 팔로워 수를 조회하여 저장된 값과 비교
- **증가 시:** 증가분 x 10 eggs 지급 + WebSocket `QUEST_COMPLETED`/`FOLLOWER` 알림
- **감소 시:** 팔로워 수만 갱신 (XP/알 차감 없음)
- **중복 방지:** `(userId, "follower", "follower_to:{count}")` 기준 dedup

### 다중 사용자 격리

- 같은 레포를 여러 사용자가 등록 가능 — 각자 독립적으로 알 지급
- Webhook 이벤트 수신 시 `repository.full_name`으로 `UserRepoGrant` 테이블 조회
- 해당 레포를 등록하지 않은 사용자에게는 알이 지급되지 않음
- WebSocket 알림도 해당 사용자에게만 전송 (브로드캐스트 아님)
