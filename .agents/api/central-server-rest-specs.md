---
name: central-server-rest-specs
description: REST communication contract between game servers and the MCIdentity central server.
---

# API: Central Server REST Specs

How game servers communicate with the MCIdentity Central Express.js Server via
`MCIdentityCentralServer` (`common/central`).

## Transport

* Requests are asynchronous JSON over a pooled `HttpClient`. Never block the
  main thread — return `CompletableFuture` and hop back to the
  `PlatformScheduler` only for world/inventory effects.
* Content type `application/json` for both request and response bodies.

## Authentication

* Bearer token auth: send `Authorization: Bearer <central_server.token>`.
* The token is read from environment/config only, never hardcoded. See
  `security/secret-management.md`.

## Timeouts and Failure

* Apply a bounded request timeout. On timeout or transport error, degrade
  safely: empty optionals / zero values, never a thrown exception on the game
  thread. See `ethics/safe-degradation.md`.
* Cold-boot: if the central server is unreachable at player login,
  `PlayerLoginListener` refuses the connection rather than admitting the player
  with empty state.

## Endpoints (5.0.0)

All player routes key by the player UUID.

| Method and path | Body / answer |
|---|---|
| `GET /health` | `200` when ready. |
| `POST /v1/professions` / `GET /v1/professions` | `{systemCode, displayName}` |
| `POST /v1/players` | `{playerUuid, stats{statPoints, energy, health, food, strength, defense, wisdom, luck, agility, resilience}}`, creates only when absent, any `2xx` |
| `GET` / `PUT /v1/players/:uuid/stats` | the stats object above |
| `GET /v1/players/:uuid/stats/:key` | `{value}` |
| `POST /v1/players/:uuid/stats/:key/add` | `{amount}` |
| `GET` / `PUT /v1/players/:uuid/profession` | `{professionCode}`; `404` when none is stored |
| `GET` / `PUT /v1/players/:uuid/professions/:code/progress` | `{level, exp}`; `404` when never worn |
| `POST /v1/profession-logs` | `{playerUuid, professionCode, actionType}` |

## Conventions

* Keep request/response DTOs in sync with the server's contract; document any
  change in `wiki/api.md` and `wiki/database.md`.
