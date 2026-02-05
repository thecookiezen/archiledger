package com.thecookiezen.ladybugdb.spring.core;

import com.ladybugdb.Connection;
import com.ladybugdb.QueryResult;
import com.thecookiezen.ladybugdb.spring.mapper.RowMapper;
import com.thecookiezen.ladybugdb.spring.connection.LadybugDBConnectionFactory;
import org.neo4j.cypherdsl.core.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Transaction-aware template for LadybugDB operations.
 * Central class for executing Cypher queries with proper
 * connection and transaction management.
 * <p>
 * When used within a Spring transaction, the same connection is reused
 * for all operations. Outside a transaction, a new connection is obtained
 * and released for each operation.
 */
public class LadybugDBTemplate {

    private static final Logger logger = LoggerFactory.getLogger(LadybugDBTemplate.class);

    private final LadybugDBConnectionFactory connectionFactory;

    public LadybugDBTemplate(LadybugDBConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    /**
     * Execute an operation using a callback function.
     * The connection is managed automatically based on transaction context.
     *
     * @param action the callback to execute
     * @param <T>    the result type
     * @return the result of the callback
     */
    public <T> T execute(LadybugDBCallback<T> action) {
        Connection connection = getConnection();
        boolean isNewConnection = !isConnectionBoundToTransaction();
        try {
            return action.doInLadybugDB(connection);
        } finally {
            if (isNewConnection) {
                releaseConnection(connection);
            }
        }
    }

    /**
     * Execute a Cypher statement (typically for write operations).
     *
     * @param statement the Cypher DSL statement
     */
    public void execute(Statement statement) {
        execute(statement.getCypher());
    }

    /**
     * Execute a raw Cypher query (typically for write operations).
     *
     * @param cypher the Cypher query string
     */
    public void execute(String cypher) {
        execute(connection -> {
            logger.debug("Executing Cypher: {}", cypher);
            QueryResult result = connection.query(cypher);
            logger.debug("Execute result: {}", result);
            return null;
        });
    }

    /**
     * Execute a Cypher statement and map results using the provided RowMapper.
     *
     * @param statement the Cypher DSL statement
     * @param rowMapper the mapper to convert each row
     * @param <T>       the result type
     * @return list of mapped results
     */
    public <T> List<T> query(Statement statement, RowMapper<T> rowMapper) {
        return query(statement.getCypher(), rowMapper);
    }

    /**
     * Execute a raw Cypher query and map results using the provided RowMapper.
     *
     * @param cypher    the Cypher query string
     * @param rowMapper the mapper to convert each row
     * @param <T>       the result type
     * @return list of mapped results
     */
    public <T> List<T> query(String cypher, RowMapper<T> rowMapper) {
        return execute(connection -> {
            logger.debug("Querying with Cypher: {}", cypher);

            QueryResult result = connection.query(cypher);
            List<T> results = new ArrayList<>();
            int rowNum = 0;

            while (result.hasNext()) {
                var row = result.getNext();
                try {
                    T mapped = rowMapper.mapRow(row, rowNum++);
                    results.add(mapped);
                } catch (Exception e) {
                    throw new CypherMappingException("Error mapping row " + (rowNum - 1), e);
                }
            }

            logger.debug("Query returned {} results", results.size());
            return results;
        });
    }

    /**
     * Execute a Cypher statement and return a single result.
     *
     * @param statement the Cypher DSL statement
     * @param rowMapper the mapper to convert the row
     * @param <T>       the result type
     * @return Optional containing the result, or empty if no results
     */
    public <T> Optional<T> queryForObject(Statement statement, RowMapper<T> rowMapper) {
        return queryForObject(statement.getCypher(), rowMapper);
    }

    /**
     * Execute a raw Cypher query and return a single result.
     *
     * @param cypher    the Cypher query string
     * @param rowMapper the mapper to convert the row
     * @param <T>       the result type
     * @return Optional containing the result, or empty if no results
     */
    public <T> Optional<T> queryForObject(String cypher, RowMapper<T> rowMapper) {
        List<T> results = query(cypher, rowMapper);
        if (results.isEmpty()) {
            return Optional.empty();
        }
        if (results.size() > 1) {
            logger.warn("queryForObject returned {} results, expected 1", results.size());
        }
        return Optional.of(results.get(0));
    }

    /**
     * Execute a Cypher query and return a list of strings.
     *
     * @param statement   the Cypher DSL statement
     * @param columnIndex the column index to extract (0-indexed)
     * @return list of string values
     */
    public List<String> queryForStringList(Statement statement, int columnIndex) {
        return queryForStringList(statement.getCypher(), columnIndex);
    }

    /**
     * Execute a raw Cypher query and return a list of strings.
     *
     * @param cypher      the Cypher query string
     * @param columnIndex the column index to extract (0-indexed)
     * @return list of string values
     */
    public List<String> queryForStringList(String cypher, int columnIndex) {
        return query(cypher, (row, rowNum) -> row.getValue(columnIndex).getValue().toString());
    }

    public LadybugDBConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }

    /**
     * Obtains a connection, potentially from the current transaction context.
     */
    private Connection getConnection() {
        // Check if we have a connection bound to the current transaction
        ConnectionHolder holder = (ConnectionHolder) TransactionSynchronizationManager
                .getResource(connectionFactory);
        if (holder != null) {
            logger.debug("Using transaction-bound connection");
            return holder.getConnection();
        }

        // No transaction, get a new connection from factory
        logger.debug("Obtaining new connection from factory");
        return connectionFactory.getConnection();
    }

    private void releaseConnection(Connection connection) {
        connectionFactory.releaseConnection(connection);
    }

    private boolean isConnectionBoundToTransaction() {
        return TransactionSynchronizationManager.hasResource(connectionFactory);
    }

    /**
     * Exception thrown when mapping a row fails.
     */
    public static class CypherMappingException extends RuntimeException {
        public CypherMappingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
