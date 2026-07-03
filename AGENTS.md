# Agent Instructions & Workflow

MCOriax Identity Plugin — a Gradle multi project identity system for the Bukkit
family of Minecraft servers (**SpigotMC**, **PaperMC**, **Folia**). Root project
`mcidentity`, group `io.github.mcoriax`, Java 25 toolchain. Identity data is owned
by the MCIdentity central server and accessed over REST with RAM caching; there is
no local database.

**Strict Rules & Execution**
* **Read First (Structure):** Always read [`INDEX.md`](INDEX.md) to understand the module and package layout before touching code, and keep it updated whenever the structure or indexed packages change.
* **Read Second (Context):** Always read [`README.md`](README.md) and the relevant [`wiki/`](wiki/) page for the area you are changing (`features`, `installation`, `commands`, `gui`, `custom-exp`, `database`, `api`, `bukkit`, `testing`).
* **Agent Rules:** Detailed agent behaviour and Minecraft platform guidance are not stored in this repository; they live in the portable `.agents` instruction set used alongside it (the plugin guidance is at `platforms/MINECRAFT.md`). Follow that set when it is present.
* **Execution:** Create a solid plan, then change one module at a time and verify. Keep `api` and `common` pure Java — they must never import a platform type; platform code belongs in `bukkit` and the `platforms:*` entry points. Stat identifiers must go through the `StatKeys` allow-list. Build and test before completing a task: `./gradlew build` and `./gradlew :common:test`. Review security for modified files.
* **No Master/Main:** Never work directly on `master` (the default branch) or `main`. Create a new branch if the task scope changes; otherwise, continue on the active branch.

**Repository Layout**
* `api/` — Pure Java contracts (`io.github.mcoriax.api`): interfaces and models only (`IdentityProvider`, `IMCIdentityDB`, the `api/model` value types), no implementation.
* `common/` — Cross platform data layer: `MCIdentityProvider`, `MCIdentityManager`, the `StatKeys` allow-list, and `MCIdentityCentralServer` (the REST client). Persistence is delegated to the central server. Unit tests live under `common/src/test/java`.
* `bukkit/` — Shared Bukkit core reused by every entry point: `AbstractMCIdentityPlugin` lifecycle, the `/identity` command, listeners, custom `MCIdentity*Event`s, the `PlatformScheduler` abstraction, and utilities.
* `platforms/spigotmc`, `platforms/papermc`, `platforms/foliamc` — Concrete plugin entry points extending the bukkit core, plus their `plugin.yml`, `config.yml`, `textures.yml`, and default `professions/`.
* `wiki/` — Human facing documentation.
* `build.gradle`, `settings.gradle`, `gradle.properties`, `gradle/libs.versions.toml` — Gradle multi project build and version catalog.
*(Keep universal rules in this `AGENTS.md` at the root. Reflect any structural change in `INDEX.md`, and suggest larger changes to the user before applying them.)*

**Git & Branching Workflow (STRICT)**
* **Branch Naming:** Must follow `{type}/{primary-noun}` (e.g., `feat/identity-session`, `fix/stat-keys`, `docs/index`). Allowed types: `feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`, `build`, `ci`, `chore`, `revert`.
* **Commit Frequency & Verification:** Commit each change or group of related changes. Do not wait for the entire session to finish. Always check the diff before creating a commit.
* **Commits:** Must use Conventional Commits (`type[optional scope]: description`) in plain text with no links or issue IDs. Use a module scope where it helps (e.g., `refactor(platforms):`, `fix(db):`, `refactor(common):`).
* **Pull Requests (PR):** Open sequentially. Always ask for user approval first. Provide a detailed report of added/modified/deleted behaviour in the PR body. The repository is cloud-hosted at `github.com/MCOriax/plugin`.
