# Dependencies: Gradle Multi-Project Rules

Constraints for the Gradle multi-project build (`settings.gradle`,
`rootProject.name = mcidentity`).

## Platform APIs

* Server platform APIs — Spigot, Paper, Folia — must ALWAYS be declared as
  `compileOnly`. They are provided by the running server at runtime and must
  never be bundled.

## Module Rules

* `api` and `common` are pure Java. They must not depend on any platform API.
* `bukkit` is the shared platform core; `platforms:*` modules extend it.

## Shading

* The `platforms:engine` module is responsible for shading. It bundles the
  `spigotmc`, `papermc`, and `foliamc` modules into one universal jar.
* Strictly exclude duplicate `plugin.yml` files during shading — the engine jar
  carries only its own `plugin.yml`.
* Keep relocations consistent so shaded dependencies never clash with other
  plugins on the server.
