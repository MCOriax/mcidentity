package io.github.mcoriax.mcidentity.common.db;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Builds the worker pool every storage backend runs its operations on.
 *
 * <p><strong>Internal.</strong> Package-private: the relational backends and
 * {@link MongoDB} each built the same daemon pool by hand.</p>
 */
final class DatabaseExecutors {

    private DatabaseExecutors() {
    }

    /**
     * Creates a fixed pool of daemon threads, so a backend never keeps the
     * server process alive on shutdown.
     * @param poolSize The number of worker threads.
     * @param poolName The base name given to the worker threads.
     * @return The executor service.
     */
    static ExecutorService daemonPool(int poolSize, String poolName) {
        AtomicInteger counter = new AtomicInteger(1);
        return Executors.newFixedThreadPool(poolSize, runnable -> {
            Thread thread = new Thread(runnable, poolName + "-" + counter.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        });
    }
}
