# Changelog — 4.7.0

**Released:** 2026-10-11

Publishes the central-server transport as a reusable class, so the add-on plugins
can stop carrying a copy each. No plugin behavior changed. Add-on authors: nothing
to do until you choose to depend on it; the class lives in the published `common`
module.

## Added

- `CentralHttpClient` (`common`, package `central`) — the pooled, no-redirect,
  bearer-authenticated HTTP client with `send`, `write`, `read`, `checkHealth`,
  `pingHealth` and `close`.
- `CentralHttpClientTest` — five tests covering authentication, read fallbacks,
  write logging and the health probes.

## Changed

- `MCIdentityCentralServer` delegates all transport to `CentralHttpClient` and
  keeps only the protocol mapping. Transport failures are now logged under the
  logger name `CentralHttpClient` instead of `MCIdentityCentralServer`; a log
  filter keyed on the old name needs updating.
