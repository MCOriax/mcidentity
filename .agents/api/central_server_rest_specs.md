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
  `security/secret_management.md`.

## Timeouts and Failure

* Apply a bounded request timeout. On timeout or transport error, degrade
  safely: empty optionals / zero values, never a thrown exception on the game
  thread. See `ethics/safe_degradation.md`.
* Cold-boot: if the central server is unreachable at player login,
  `PlayerLoginListener` refuses the connection rather than admitting the player
  with empty state.

## Conventions

* Keep request/response DTOs in sync with the server's contract; document any
  change in `wiki/api.md` and `wiki/database.md`.
