package gg.azura.bridges.services;

import gg.azura.bridges.Bridges;

import javax.sql.PooledConnection;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

public class DatabaseManager {

    private final Bridges plugin;

    private final String host;

    private final String database;

    private final String username;

    private final String password;

    private final int port;

    private final boolean ssl;

    final int MAX_RETRIES = 3;
    final int QUERY_TIMEOUT = 5;
    private static final int MAX_POOL_SIZE = 10;
    private static final int MIN_POOL_SIZE = 3;
    private static final int INITIAL_POOL_SIZE = 5;
    private static final long CONNECTION_TIMEOUT = 5000L; // 5 seconds
    private static final long VALIDATION_TIMEOUT = 1000L; // 1 second
    private static final long IDLE_TIMEOUT = 300000L; // 5 minutes
    private static final String CURRENT_TIME = "2025-02-04 18:33:46";
    private static final String CURRENT_USER = "SyncFocus17";
    // Connection pool
    private BlockingQueue<PooledConnection> connectionPool;
    private Set<PooledConnection> activeConnections;
    private ReentrantLock poolLock;
    private volatile boolean shutdownInProgress;

    private class PooledConnection {
        private final Connection connection;
        private long lastUsed;
        private int useCount;
        private boolean isValid;

        public PooledConnection(Connection connection) {
            this.connection = connection;
            this.lastUsed = System.currentTimeMillis();
            this.useCount = 0;
            this.isValid = true;
        }

        public void markUsed() {
            this.lastUsed = System.currentTimeMillis();
            this.useCount++;
        }

        public boolean isIdle() {
            return System.currentTimeMillis() - lastUsed > IDLE_TIMEOUT;
        }
    }

    /**
     * Initializes the connection pool
     */
    private void initializePool() {
        poolLock = new ReentrantLock();
        connectionPool = new ArrayBlockingQueue<>(MAX_POOL_SIZE);
        activeConnections = Collections.newSetFromMap(new ConcurrentHashMap<>());
        shutdownInProgress = false;

        // Create initial connections
        try {
            for (int i = 0; i < INITIAL_POOL_SIZE; i++) {
                addNewConnection();
            }
            logPoolInitialization();
        } catch (SQLException e) {
            logPoolError("Failed to initialize pool", e);
        }

        // Start maintenance task
        startPoolMaintenance();
    }


    /**
     * Adds a new connection to the pool
     */
    private void addNewConnection() throws SQLException {
        if (connectionPool.size() + activeConnections.size() < MAX_POOL_SIZE) {
            Connection conn = createNewConnection();
            PooledConnection pooledConn = new PooledConnection(conn);
            connectionPool.offer(pooledConn);

            logConnectionCreated(pooledConn);
        }
    }

    /**
     * Gets a connection from the pool
     */
    public Connection getPooledConnection() throws SQLException {
        long startTime = System.nanoTime();
        PooledConnection conn = null;

        try {
            poolLock.lock();

            // Try to get from pool
            conn = connectionPool.poll(CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS);

            if (conn == null && connectionPool.size() + activeConnections.size() < MAX_POOL_SIZE) {
                // Create new if pool is empty but not at max size
                conn = new PooledConnection(createNewConnection());
            }

            if (conn != null) {
                // Validate connection
                if (!validateConnection(conn)) {
                    conn = new PooledConnection(createNewConnection());
                }

                // Mark as active
                conn.markUsed();
                activeConnections.add(conn);

                logConnectionAcquired(conn, startTime);
                return conn.connection;
            }

            throw new SQLException("Unable to acquire connection from pool");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SQLException("Interrupted while waiting for connection", e);
        } finally {
            poolLock.unlock();
        }
    }

    /**
     * Returns a connection to the pool
     */
    public void returnConnection(Connection conn) {
        if (conn == null) return;

        try {
            poolLock.lock();

            // Find the pooled connection
            Optional<PooledConnection> pooledConn = activeConnections.stream()
                    .filter(pc -> pc.connection == conn)
                    .findFirst();

            if (pooledConn.isPresent()) {
                PooledConnection pc = pooledConn.get();
                activeConnections.remove(pc);

                if (!shutdownInProgress && validateConnection(pc)) {
                    connectionPool.offer(pc);
                    logConnectionReturned(pc);
                } else {
                    closeConnection(pc);
                }
            }

        } finally {
            poolLock.unlock();
        }
    }

    /**
     * Validates a pooled connection
     */
    private boolean validateConnection(PooledConnection conn) {
        try {
            if (conn.connection.isClosed() || !conn.connection.isValid((int)VALIDATION_TIMEOUT)) {
                conn.isValid = false;
                return false;
            }
            return true;
        } catch (SQLException e) {
            logConnectionError("Validation failed", e, conn);
            return false;
        }
    }


    /**
     * Starts the pool maintenance task
     */
    private void startPoolMaintenance() {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "DB-Pool-Maintenance");
            t.setDaemon(true);
            return t;
        });

        executor.scheduleAtFixedRate(() -> {
            try {
                maintainPool();
            } catch (Exception e) {
                logPoolError("Maintenance failed", e);
            }
        }, 1, 1, TimeUnit.MINUTES);
    }

    /**
     * Maintains the connection pool
     */
    private void maintainPool() {
        try {
            poolLock.lock();

            // Remove idle connections
            Iterator<PooledConnection> iterator = connectionPool.iterator();
            while (iterator.hasNext()) {
                PooledConnection conn = iterator.next();
                if (conn.isIdle() && connectionPool.size() > MIN_POOL_SIZE) {
                    iterator.remove();
                    closeConnection(conn);
                }
            }

            // Ensure minimum connections
            while (connectionPool.size() < MIN_POOL_SIZE) {
                addNewConnection();
            }

            logPoolStatistics();

        } catch (Exception e) {
            logPoolError("Pool maintenance failed", (SQLException) e);
        } finally {
            poolLock.unlock();
        }
    }

    /**
     * Logs pool statistics
     */
    private void logPoolStatistics() {
        plugin.getLogger().info(String.format(
                "[POOL] Stats | Active: %d | Idle: %d | Total: %d | User: %s | Time: %s",
                activeConnections.size(),
                connectionPool.size(),
                activeConnections.size() + connectionPool.size(),
                CURRENT_USER,
                CURRENT_TIME
        ));
    }

    // Additional logging methods...

    public DatabaseManager(Variables variables, Bridges plugin, BlockingQueue<PooledConnection> connectionPool, Set<PooledConnection> activeConnections, ReentrantLock poolLock) {
        this.plugin = plugin;
        this.host = variables.mysqlHost;
        this.port = variables.mysqlPort;
        this.database = variables.mysqlDatabase;
        this.username = variables.mysqlUsername;
        this.password = variables.mysqlPassword;
        this.ssl = variables.mysqlSSL;
        this.connectionPool = connectionPool;
        this.activeConnections = activeConnections;
        this.poolLock = poolLock;
        setup();
    }

    public CompletableFuture<Connection> getConnection() {
        CompletableFuture<Connection> future = new CompletableFuture<>();
        try {
            future.complete(DriverManager.getConnection(String.format("jdbc:mysql://%s:%d/%s?autoReconnect=true&useSSL=%b", new Object[] { this.host, Integer.valueOf(this.port), this.database, Boolean.valueOf(this.ssl) }), this.username, this.password));
        } catch (SQLException e) {
            future.complete(null);
        }
        return future;
    }
    /**
     * Fetches a player's UUID asynchronously with caching and retry mechanism
     *
     * @author SyncFocus17
     * @version 2.0
     * @since 2025-02-04 18:29:01
     * @param name The player's name to fetch UUID for
     * @return CompletableFuture containing the UUID or null if not found
     */
    public CompletableFuture<UUID> fetchUUID(String name) {
        final String CURRENT_TIME = "2025-02-04 18:29:01";
        final String CURRENT_USER = "SyncFocus17";

        // Performance tracking
        long startTime = System.nanoTime();

        return CompletableFuture.supplyAsync(() -> {
            // SQL query with index hint and parameter validation
            String query = """
            SELECT uuid 
            FROM player_data 
            FORCE INDEX (idx_player_name)
            WHERE LOWER(name) = LOWER(?) 
            AND deleted_at IS NULL
            LIMIT 1
            """;

            // Track attempts for retry mechanism
            for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
                try {
                    // Get connection from pool with timeout
                    Connection conn = getConnection()
                            .get(QUERY_TIMEOUT, TimeUnit.SECONDS);

                    if (conn == null || conn.isClosed()) {
                        logDatabaseError(
                                "Invalid connection during UUID fetch",
                                null,
                                name,
                                attempt
                        );
                        continue;
                    }

                    try (PreparedStatement stmt = conn.prepareStatement(query)) {
                        // Set query parameters
                        stmt.setString(1, name);
                        stmt.setQueryTimeout(QUERY_TIMEOUT);

                        // Execute query
                        try (ResultSet rs = stmt.executeQuery()) {
                            if (rs.next()) {
                                String uuidStr = rs.getString("uuid");
                                UUID result = UUID.fromString(uuidStr);

                                // Log success with performance metrics
                                logQuerySuccess(
                                        "UUID fetch",
                                        name,
                                        result,
                                        startTime
                                );

                                return result;
                            }
                        }
                    } finally {
                        // Return connection to pool
                        returnConnection(conn);
                    }

                } catch (SQLException e) {
                    logDatabaseError(
                            "SQL error during UUID fetch",
                            e,
                            name,
                            attempt
                    );

                    // Retry after delay if not last attempt
                    if (attempt < MAX_RETRIES) {
                        try {
                            Thread.sleep(1000L * attempt); // Exponential backoff
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new CompletionException(ie);
                        }
                    }

                } catch (IllegalArgumentException e) {
                    logDatabaseError(
                            "Invalid UUID format",
                            e,
                            name,
                            attempt
                    );
                    // Don't retry for format errors
                    break;

                } catch (TimeoutException e) {
                    logDatabaseError(
                            "Connection timeout",
                            e,
                            name,
                            attempt
                    );
                    // Continue to next attempt

                } catch (Exception e) {
                    logDatabaseError(
                            "Unexpected error",
                            e,
                            name,
                            attempt
                    );
                    // Don't retry for unexpected errors
                    break;
                }
            }

            // Log final failure
            plugin.getLogger().severe(String.format(
                    "[DATABASE] All attempts to fetch UUID failed for %s | User: %s | Time: %s",
                    name,
                    CURRENT_USER,
                    CURRENT_TIME
            ));

            return null;
        }, CompletableFuture.delayedExecutor(0, TimeUnit.SECONDS,
                Executors.newCachedThreadPool(r -> {
                    Thread thread = new Thread(r, "Database-Worker");
                    thread.setDaemon(true);
                    return thread;
                })
        ));

        }

    /**
     * Safely closes a connection with error handling
     */
    private void safelyCloseConnection(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
                logConnectionEvent("CLOSE", "Connection closed successfully", null);
            } catch (SQLException e) {
                logConnectionEvent("ERROR", "Error closing connection", e);
            }
        }
    }

    /**
     * Logs connection events with detailed information
     */
    private void logConnectionEvent(String status, String message, Exception e) {
        String logMessage = String.format(
                "[CONNECTION] Status: %s | Message: %s | Pool size: %d/%d | User: %s | Time: %s",
                status,
                message,
                connectionPool.size(),
                MAX_POOL_SIZE,
                "SyncFocus17",
                "2025-02-04 18:31:34"
        );

        if (e != null) {
            plugin.getLogger().warning(logMessage + String.format(" | Error: %s", e.getMessage()));
        } else {
            plugin.getLogger().info(logMessage);
        }
    }

    /**
     * Logs database errors with detailed information
     */
    private void logDatabaseError(
            String type,
            Exception e,
            String playerName,
            int attempt
    ) {
        String errorMessage = e != null ? e.getMessage() : "Unknown error";

        plugin.getLogger().warning(String.format(
                "[DATABASE] %s for player %s (Attempt %d/%d) | Error: %s | User: %s | Time: %s",
                type,
                playerName,
                attempt,
                MAX_RETRIES,
                errorMessage,
                "SyncFocus17",
                "2025-02-04 18:29:01"
        ));

        if (!(e instanceof SQLException) && e != null) {
            plugin.getLogger().severe(String.format(
                    "[STACK] Full error for UUID fetch: %s",
                    Arrays.toString(e.getStackTrace())
            ));
        }
    }

    /**
     * Logs successful queries with performance metrics
     */
    private void logQuerySuccess(
            String operation,
            String playerName,
            UUID result,
            long startTime
    ) {
        long duration = (System.nanoTime() - startTime) / 1_000_000; // ms

        plugin.getLogger().info(String.format(
                "[DATABASE] %s successful for %s | UUID: %s | Duration: %dms | User: %s | Time: %s",
                operation,
                playerName,
                result,
                duration,
                "SyncFocus17",
                "2025-02-04 18:29:01"
        ));

        // Log performance warning if slow
        if (duration > 100) { // 100ms threshold
            plugin.getLogger().warning(String.format(
                    "[PERFORMANCE] Slow %s for %s (%dms) | User: %s | Time: %s",
                    operation,
                    playerName,
                    duration,
                    "SyncFocus17",
                    "2025-02-04 18:29:01"
            ));
        }
    }

    /**
     * Creates a new connection
     */
    private Connection createNewConnection() throws SQLException {
        String url = String.format("jdbc:mysql://%s:%d/%s?autoReconnect=true&useSSL=%b&serverTimezone=UTC",
                host, port, database, ssl);
        return DriverManager.getConnection(url, username, password);
    }

    /**
     * Logs pool initialization
     */
    private void logPoolInitialization() {
        plugin.getLogger().info(String.format(
                "[POOL] Initialized with %d/%d connections | User: %s | Time: %s",
                connectionPool.size(),
                MAX_POOL_SIZE,
                CURRENT_USER,
                CURRENT_TIME
        ));
    }

    /**
     * Logs pool errors
     */
    private void logPoolError(String message, Exception e) {
        plugin.getLogger().severe(String.format(
                "[POOL] %s | Error: %s | User: %s | Time: %s",
                message,
                e.getMessage(),
                CURRENT_USER,
                CURRENT_TIME
        ));
        if (!(e instanceof SQLException)) {
            plugin.getLogger().severe(String.format(
                    "[STACK] %s",
                    Arrays.toString(e.getStackTrace())
            ));
        }
    }

    /**
     * Logs connection creation
     */
    private void logConnectionCreated(PooledConnection conn) {
        plugin.getLogger().info(String.format(
                "[POOL] Created new connection | Pool size: %d/%d | User: %s | Time: %s",
                connectionPool.size(),
                MAX_POOL_SIZE,
                CURRENT_USER,
                CURRENT_TIME
        ));
    }

    /**
     * Logs connection acquisition
     */
    private void logConnectionAcquired(PooledConnection conn, long startTime) {
        long duration = (System.nanoTime() - startTime) / 1_000_000;
        plugin.getLogger().info(String.format(
                "[POOL] Connection acquired | Duration: %dms | Pool size: %d/%d | Use count: %d | User: %s | Time: %s",
                duration,
                connectionPool.size(),
                MAX_POOL_SIZE,
                conn.useCount,
                CURRENT_USER,
                CURRENT_TIME
        ));
    }

    /**
     * Logs connection return
     */
    private void logConnectionReturned(PooledConnection conn) {
        plugin.getLogger().info(String.format(
                "[POOL] Connection returned | Pool size: %d/%d | Use count: %d | User: %s | Time: %s",
                connectionPool.size(),
                MAX_POOL_SIZE,
                conn.useCount,
                CURRENT_USER,
                CURRENT_TIME
        ));
    }

    /**
     * Logs connection errors
     */
    private void logConnectionError(String message, SQLException e, PooledConnection conn) {
        plugin.getLogger().warning(String.format(
                "[POOL] %s | Error: %s | Pool size: %d/%d | Use count: %d | User: %s | Time: %s",
                message,
                e.getMessage(),
                connectionPool.size(),
                MAX_POOL_SIZE,
                conn.useCount,
                CURRENT_USER,
                CURRENT_TIME
        ));
    }

    /**
     * Closes a pooled connection
     */
    private void closeConnection(PooledConnection conn) {
        if (conn != null) {
            try {
                conn.connection.close();
                plugin.getLogger().info(String.format(
                        "[POOL] Connection closed | Pool size: %d/%d | Use count: %d | User: %s | Time: %s",
                        connectionPool.size(),
                        MAX_POOL_SIZE,
                        conn.useCount,
                        CURRENT_USER,
                        CURRENT_TIME
                ));
            } catch (SQLException e) {
                plugin.getLogger().warning(String.format(
                        "[POOL] Error closing connection | Error: %s | User: %s | Time: %s",
                        e.getMessage(),
                        CURRENT_USER,
                        CURRENT_TIME
                ));
            }
        }
    }

    /**
     * Updates the constructor to initialize pool
     */
    public DatabaseManager(Variables variables, Bridges plugin) {
        this.plugin = plugin;
        this.host = variables.mysqlHost;
        this.port = variables.mysqlPort;
        this.database = variables.mysqlDatabase;
        this.username = variables.mysqlUsername;
        this.password = variables.mysqlPassword;
        this.ssl = variables.mysqlSSL;

        // Initialize pool
        initializePool();

        // Setup database
        setup();
    }

    /**
     * Updates the setup method with proper resource management
     */
    public void setup() {
        String createTableQuery = """
        CREATE TABLE IF NOT EXISTS bridgeffa_players(
            uuid VARCHAR(36) PRIMARY KEY,
            coins INTEGER DEFAULT 0,
            blocks_unlocked LONGTEXT DEFAULT '{}',
            block_selected LONGTEXT DEFAULT 'AIR',
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
        )
    """;

        try (Connection conn = getPooledConnection();
             PreparedStatement stmt = conn.prepareStatement(createTableQuery)) {

            stmt.executeUpdate();
            plugin.getLogger().info(String.format(
                    "[DATABASE] Tables created successfully | User: %s | Time: %s",
                    CURRENT_USER,
                    CURRENT_TIME
            ));

        } catch (SQLException e) {
            plugin.getLogger().severe(String.format(
                    "[DATABASE] Failed to create tables: %s | User: %s | Time: %s",
                    e.getMessage(),
                    CURRENT_USER,
                    CURRENT_TIME
            ));
        }
    }
}
