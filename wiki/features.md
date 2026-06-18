# Key Features

[← Back to README](../README.md)

- **Multi-Platform Support**: Fully supports **SpigotMC**, **PaperMC**, and **Folia**.
- **Interactive GUI System**: Multi-tier virtual menus for managing identities, runes, and abilities.
- **Dynamic Data Binding**: Ability and Rune GUIs fetch real-time data from the database, showing equipped items or placeholders.
- **Regionalized Threading**: Fully compatible with Folia's `RegionScheduler` and `GlobalRegionScheduler`.
- **Database Connection Pooling**: High-performance data access using **HikariCP**.
- **Multi-Database Support**: Supports **SQLite** (for easy setup), **PostgreSQL** (for production environments), and **MongoDB**.
- **Extensible API**: Provides a central `MCIdentityProvider` for third-party plugins to interact with the identity system.
- **Secure Commands**: Role-based permissions for administrative actions.
- **Visual Identity Keys**: Identities are represented as physical items in the game with custom colored display names.
- **Unbound Identities**: Identity items created via `/identity get` are initially "unbound". They lack a unique ID and database entry. Both the unique ID and the identity's initial stats are generated and registered in the database only when the item is first used.
