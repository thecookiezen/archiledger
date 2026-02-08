package com.thecookiezen.ladybugdb.spring.core;

import com.ladybugdb.Connection;
import com.ladybugdb.PreparedStatement;
import com.ladybugdb.QueryResult;
import com.ladybugdb.Value;
import com.thecookiezen.ladybugdb.spring.connection.LadybugDBConnectionFactory;
import com.thecookiezen.ladybugdb.spring.mapper.DefaultQueryRow;
import com.thecookiezen.ladybugdb.spring.mapper.QueryRow;
import com.thecookiezen.ladybugdb.spring.mapper.RowMapper;
import org.neo4j.cypherdsl.core.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    public void execute(String cypher) {
        execute(cypher, Map.of());
    }

    /**
     * Execute a Cypher statement (typically for write operations).
     *
     * @param statement the Cypher DSL statement
     */
    public void execute(Statement statement) {
        execute(statement.getCypher(), Map.of());
    }

    /**
     * Execute a raw Cypher query (typically for write operations).
     *
     * @param cypher the Cypher query string
     */
    public void execute(String cypher, Map<String, Object> parameters) {
        execute(connection -> {
            logger.debug("Executing Cypher: {}", cypher);
            PreparedStatement statement = connection.prepare(cypher);
            Map<String, Value> valueParameters = convertParameters(parameters);
            QueryResult result = connection.execute(statement, valueParameters);
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
        return query(statement.getCypher(), Map.of(), rowMapper);
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
        return query(cypher, Map.of(), rowMapper);
    }

    /**
     * Execute a raw Cypher query and map results using the provided RowMapper.
     *
     * @param cypher    the Cypher query string
     * @param rowMapper the mapper to convert each row
     * @param <T>       the result type
     * @return list of mapped results
     */
    public <T> List<T> query(String cypher, Map<String, Object> parameters, RowMapper<T> rowMapper) {
        return execute(connection -> {
            logger.debug("Querying with Cypher: {}", cypher);

            PreparedStatement statement = connection.prepare(cypher);
            QueryResult result = connection.execute(statement, convertParameters(parameters));
            List<T> results = new ArrayList<>();
            int rowNum = 0;

            // Pre-compute column mapping once
            int numColumns = (int) result.getNumColumns();
            Map<String, Integer> columnToIndex = new HashMap<>(numColumns);
            for (int i = 0; i < numColumns; i++) {
                columnToIndex.put(result.getColumnName(i), i);
            }

            while (result.hasNext()) {
                var row = result.getNext();

                logger.debug("Query result row {}: {}", rowNum, row);

                try {
                    Value[] values = new Value[numColumns];
                    for (int i = 0; i < numColumns; i++) {
                        values[i] = row.getValue(i);
                    }

                    QueryRow queryRow = new DefaultQueryRow(values, columnToIndex);
                    T mapped = rowMapper.mapRow(queryRow);
                    results.add(mapped);
                } catch (Exception e) {
                    throw new CypherMappingException("Error mapping row " + rowNum, e);
                }

                rowNum++;
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
     * @param statement  the Cypher DSL statement
     * @param columnName the column name to extract
     * @return list of string values
     */
    public List<String> queryForStringList(Statement statement, String columnName) {
        return queryForStringList(statement.getCypher(), columnName);
    }

    /**
     * Execute a raw Cypher query and return a list of strings.
     *
     * @param cypher     the Cypher query string
     * @param columnName the column name to extract
     * @return list of string values
     */
    public List<String> queryForStringList(String cypher, String columnName) {
        return query(cypher, (row) -> row.getValue(columnName).getValue().toString());
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

    private Map<String, Value> convertParameters(Map<String, Object> parameters) {
        Map<String, Value> converted = new HashMap<>();
        parameters.forEach((key, value) -> converted.put(key, value != null ? new Value(value) : Value.createNull()));
        return converted;
    }

    public static class CypherMappingException extends RuntimeException {
        public CypherMappingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
