# Deploy: Engine Jar Export

Deploy the universal shaded jar to a local test server.

## Procedure

1. Build the shaded jar via the `platforms:engine` shadow task so all three
   platform modules (SpigotMC, PaperMC, Folia) are bundled into one jar.
2. Use the custom `exportJar` Gradle task to copy the shaded jar into the local
   test server's `plugins/` directory.
3. Start the test server; `MCIdentityEnginePlugin` detects the running server at
   enable time and installs the matching `PlatformScheduler`.

## Notes

* The engine jar carries only its own `plugin.yml`; duplicate `plugin.yml` files
  must be excluded during shading (`dependencies/gradle_multi_project_rules.md`).
* Verify boot on SpigotMC, PaperMC, and Folia — a single jar must support all
  three without code changes.
