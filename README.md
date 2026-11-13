# MCOriax Identity Plugin

A robust, high-performance profession manager for Minecraft servers,
supporting **SpigotMC**, **PaperMC**, and **Folia** — including native
compatibility with Folia's regionalized multi-threading architecture.

Each player has one character with nine **Stats** and wears one **Profession** at
a time, each with its own level and EXP. Skills, runes, and cosmetics ship as
separate add-ons.

## Key features

- Multi-platform: SpigotMC, PaperMC, and Folia from a single shaded engine jar.
- One character per player, keyed by the player UUID; wear a profession with
  `/identity wear` or the profession GUI, gated by per profession permissions.
- Selectable storage: SQLite (default), MySQL, PostgreSQL, MongoDB, or the
  MCIdentity central REST server.
- RAM-cached and fully asynchronous — no blocking work on the game thread.
- Custom `MCProfessionEquipEvent`, `MCProfessionUnequipEvent` and `MCIdentityStatAddEvent` types and a public `MCIdentityProvider` API for
  third-party plugins.
- Optional read-only AI tools for [MCAgents](wiki/reference/agent-tools.md),
  so players can ask about their worn profession and stats.

## Quick start

```bash
./gradlew build                            # compile every module and run tests
./gradlew :platforms:engine:shadowJar      # build the universal shaded jar
```

Drop the resulting jar into your server's `plugins/` directory and restart. The
default SQLite backend needs no configuration.

## Documentation

Full documentation lives in [`wiki/INDEX.md`](wiki/INDEX.md).

Start here:

- [Project Overview](wiki/information/overview.md) — what the plugin is and what
  it deliberately leaves to the add-ons.
- [Installation & Setup](wiki/environments/installation.md) — getting it running
  and connecting it to the central server.
- [Commands & Permissions](wiki/reference/commands.md) — every `/identity`
  subcommand and its permission node.

The project website is <https://mcoriax.github.io/>.

## License

See [`LICENSE`](LICENSE).
