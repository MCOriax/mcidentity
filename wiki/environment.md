# Environment Variables

[← Back to README](../README.md)

The plugin itself is configured at runtime through the server's `config.yml`,
not through environment variables — see [Installation & Setup](installation.md)
and [Data Storage](database.md) for those settings. Environment variables come
into play in two places: **building and publishing** the library modules to
GitHub Packages, and **consuming** those packages from another project. This
page documents both.

> All example values below are placeholders. Replace them with your own
> credentials and never commit real tokens.

**Contents**

1. [Publishing to GitHub Packages](#1-publishing-to-github-packages)
2. [Consuming the published packages](#2-consuming-the-published-packages)
3. [Runtime configuration](#3-runtime-configuration)

---

## 1. Publishing to GitHub Packages

The `api`, `common`, and `bukkit` modules publish to the GitHub Packages Maven
registry for `MCOriax/mcidentity`. The Gradle publishing block reads two
environment variables:

| Variable | Required | Description |
|---|---|---|
| `GITHUB_ACTOR` | yes | The GitHub username (or CI actor) performing the publish. |
| `GITHUB_TOKEN` | yes | A token with the `write:packages` scope. In GitHub Actions the automatically provided `GITHUB_TOKEN` is used. |

```bash
export GITHUB_ACTOR=your_github_username
export GITHUB_TOKEN=your_github_token

./gradlew publish
```

In a GitHub Actions workflow these are supplied automatically:

```yaml
env:
  GITHUB_ACTOR: ${{ github.actor }}
  GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

---

## 2. Consuming the published packages

GitHub Packages **always requires authentication**, even to read. A consuming
project supplies a GitHub username and a Personal Access Token (classic) with
the `read:packages` scope. Provide them as environment variables:

| Variable | Required | Description |
|---|---|---|
| `GITHUB_ACTOR` | yes | The GitHub username reading the packages. |
| `GITHUB_TOKEN` | yes | A Personal Access Token with the `read:packages` scope. |

```bash
export GITHUB_ACTOR=your_github_username
export GITHUB_TOKEN=your_github_token
```

The consumer's Gradle repository then reads them:

```groovy
repositories {
    maven {
        url = "https://maven.pkg.github.com/MCOriax/mcidentity"
        credentials {
            username = System.getenv("GITHUB_ACTOR")
            password = System.getenv("GITHUB_TOKEN")
        }
    }
}
```

See [Developer API Usage](api.md) for the full integration guide.

---

## 3. Runtime configuration

The plugin has no runtime environment variables; a running server is configured
entirely through `config.yml`. The main settings are:

- **Storage backend** — `database.type` and its per backend connection section
  (`sqlite`, `mysql`, `postgresql`, `mongodb`). See [Data Storage](database.md).
- **Central server** — when `database.type: central`, the `central_server`
  section holds the deployment URL, bearer token, and timeout:

  ```yaml
  database:
    type: central
  central_server:
    url: "https://server.com"
    token: "your_server_api_token"
    timeout_seconds: 10
  ```

  The `token` value must match the `SERVER_API_TOKEN` configured on the central
  server ([`server-expressjs`](https://github.com/MCOriax/server-expressjs)).
- **Death behaviour** — `drop_on_death` (default `false`). See
  [Installation & Setup](installation.md#death-behaviour).
