# Installation & Setup

[← Back to README](../README.md)

The plugin runs on **SpigotMC**, **PaperMC**, and **Folia** servers. Use the build for your platform and follow the steps below.

1. Deploy the MCIdentity central server (the `server-expressjs` repository) and note its URL and read-write API token.
2. Place the plugin JAR in your server's `plugins/` folder.
3. Start the server once to generate the default configuration.
4. Point the plugin at your central server in `config.yml`:
   ```yaml
   central_server:
     url: "https://server.com"
     token: "the value of SERVER_API_TOKEN on the central server"
     timeout_seconds: 10
   ```
5. Restart the server. The plugin verifies the connection on startup and logs
   an error if the central server is unreachable.

Every game server on the network uses the same `central_server` settings, so
identities follow players between servers automatically.
