# Environment Variables

[← Back to README](../../README.md)

The plugin itself is configured at runtime through the server's `config.yml`,
not through environment variables — see [Installation & Setup](installation.md)
and [Data Storage](../reference/database.md) for those settings. Environment variables come
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

### Automatic releases

You normally do not publish by hand. `.github/workflows/publish.yml` runs on
every push to `master` and reads `project-version` from `gradle.properties`:

* If the tag `v{version}` (for example `v4.7.0`) already exists, it stops and
  publishes nothing — an ordinary merge is a no-op.
* If the tag does not exist, it runs `./gradlew build`, publishes the `api`,
  `common`, and `bukkit` modules with `./gradlew publish`, and then creates and
  pushes the tag `v{version}` on that commit.

The tag is created after the publish, so a failed publish never leaves a tag
behind. The run can be repeated from the **Actions** tab with *Run workflow*.
The version must be `Major.Minor.Patch`; anything else fails the run.

When that run succeeds, `.github/workflows/release.yml` builds
`MCIdentity-{version}.jar` from the tagged commit and attaches it to a GitHub
release for `v{version}`, using `wiki/logs/{Major}/{Minor}/{Patch}/CHANGELOG.md`
as the notes.

Both files only call the shared workflows in
[MCEngine/actions](https://github.com/MCEngine/actions), which hold the steps
for every plugin in the family.

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

See [Developer API Usage](../reference/api.md) for the full integration guide.

---

## 3. Runtime configuration

The plugin has no runtime environment variables; a running server is configured
entirely through `config.yml`. The main settings are:

- **Storage backend** — `database.type` and its per backend connection section
  (`sqlite`, `mysql`, `postgresql`, `mongodb`). See [Data Storage](../reference/database.md).
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
- **AI agent** — `agents.model` and `agents.token`, both `global` by default. See
  [AI agent model and key](#ai-agent-model-and-key) below.

### AI agent model and key

The `agents` block chooses the AI model and the OpenRouter API key MCIdentity's agent
work runs on:

```yaml
agents:
  token: global
  text: global
  image: global
  video: global
  speech: global
  decisions: global
  embeddings: global
  rerank: global
  transcription: global
  audio: global
```

The nine modality keys (`text`, `image`, `video`, `speech`, `decisions`, `embeddings`,
`rerank`, `transcription`, `audio`) ship as `global`. This release does not read them
yet: its model still comes from `agents.model` when an older `config.yml` carries it,
and otherwise from MCAgents.

- **`global`** (the shipped value of both) means "whatever MCAgents is configured
  with". The value is read at runtime from `agents.model` / `agents.token` in
  `plugins/MCAgents/config.yml`, through the public `getAgentsModel()` and
  `getAgentsToken()` getters MCAgents 6.2.0 added on its plugin class, so the whole
  server shares one model and key and a change in MCAgents applies without touching
  this file.
- **Any other value** is used as written: an OpenRouter model id such as
  `openrouter/free` for `model` (lower-cased on read), or an OpenRouter API key for
  `token`. Use this to run MCIdentity on its own model or key while the rest of the
  server shares MCAgents'.
- **Fallback.** A missing or blank value counts as `global`. An MCAgents older than
  6.2.0 is asked through its provider's `getDefaultModel()` / `getServerApiKey()`,
  which hold the same values. Without MCAgents a `global` value resolves blank.

Other plugins read the resolved pair from `AbstractMCIdentityPlugin#getAgentsModel()`
and `#getAgentsToken()`; `#getAgentsSettings()` returns the block as configured. On
enable the plugin logs `AI agent model: global (MCAgents)` or the explicit model. The
key is never logged.
