# Database & State Management

The project uses a dual-layer architecture (RAM Cache + Persistent Database).

## 1. Asynchronous Database Access
*   All database interactions MUST go through `io.gitlab.mcoriax.common.MCIdentityProvider.instance`.
*   **CompletableFuture**: All database fetches and updates return `CompletableFuture`. Never block the main thread using `.join()` or `.get()` unless executing within a dedicated asynchronous task.
*   *Example*:
```java
    MCIdentityProvider.instance.getProperties(uuid).thenAccept(optProps -> {
        // Handle result
    });
```

## 2. RAM Caching (Optimistic Updates)
*   Use `io.gitlab.mcoriax.common.MCIdentityManager` for synchronous, immediate state access and updates.
*   When updating data, update the RAM cache synchronously first, then push to the database asynchronously.
*   *Example*:
```java
    // 1. Optimistic RAM Update
    MCIdentityManager.updateProperties(uuid, newProps);
    // 2. Async DB Update
    MCIdentityProvider.instance.updateProperties(uuid, newProps);
```

## 3. Connection Pooling
*   The project uses **HikariCP** for connection pooling across SQLite, PostgreSQL, and MongoDB. Ensure connections are properly closed or returned to the pool after custom queries.
