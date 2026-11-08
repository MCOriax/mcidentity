# Changelog — 4.11.1

**Released:** 2026-11-08

A patch release. The build now applies the shared
[`io.github.mcengine.build-logic`](https://github.com/MCEngine/gradle-plugin) convention
plugin instead of carrying its own copy of the root build logic. The plugin jar it
produces is built the same way.

## Changed

- The root `build.gradle` applies `io.github.mcengine.build-logic` 0.1.0, which sets the
  Java 25 toolchain, UTF-8, Maven Central and GitHub Packages publishing; `settings.gradle`
  resolves it from `https://maven.pkg.github.com/MCEngine/gradle-plugin` (#142).
- Module builds apply `com.gradleup.shadow` without a version; the version comes from the
  root build (#142).

## Upgrading

Drop in the new jar. No config, storage or command change.
