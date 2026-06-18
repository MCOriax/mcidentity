# Installation & Setup

[← Back to README](../README.md)

The plugin runs on **SpigotMC**, **PaperMC**, and **Folia** servers. Use the build for your platform and follow the steps below.

1. Place the plugin JAR in your server's `plugins/` folder.
2. Start the server once to generate the default configuration.
3. Configure your database type in `config.yml`:
   ```yaml
   database:
     type: "sqlite" # or "postgresql"
     sqlite:
       file: "mcidentity.db"
     postgresql:
       host: "localhost"
       port: 5432
       database: "mcidentity"
       username: "postgres"
       password: "password"
   ```
4. Restart the server.
