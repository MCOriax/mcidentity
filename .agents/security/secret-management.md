---
name: secret-management
description: Rule that secrets are read only from environment or config, never hardcoded.
---

# Security: Secret Management

## Rule

* Secrets — GitHub Packages tokens and `central_server.token` — are read ONLY
  from environment variables or configuration files. Never hardcode them in
  source, tests, or logic.

## Sources

* Publishing credentials: `GITHUB_ACTOR` and `GITHUB_TOKEN` from the environment
  (CI or local env), never committed.
* Central server token: the REST API auth token for `database.type: central` is
  read from `config.yml` at runtime (the `central_server.token` key, resolvable
  from the `SERVER_API_TOKEN` environment-variable placeholder).

## Practices

* Never log secret values. Redact tokens in error messages and debug output.
* Never commit real tokens, even in fixtures — use standardized placeholders
  such as `your_server_api_key` in docs and examples.
* Fail with a clear, non-leaking message when a required secret is missing.
