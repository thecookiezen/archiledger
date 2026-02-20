package com.thecookiezen.ladybugdb.spring.core;

import com.ladybugdb.Connection;
import com.ladybugdb.LbugList;
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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

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
     * Execute a Cypher query without parameters.
     * 
     * @param cypher the Cypher query to execute
     */
    public void execute(String cypher) {
        execute(cypher, Map.of());
    }

    /**
     * Execute a Cypher query from a Statement object.
     * 
     * @param statement the statement to execute
     */
    public void execute(Statement statement) {
        execute(statement.getCypher(), Map.of());
    }

    /**
     * Execute a Cypher query from a Statement object.
     * 
     * @param statement the statement to execute
     */
    public void execute(Statement statement, Map<String, Object> parameters) {
        execute(statement.getCypher(), parameters);
    }

    /**
     * Execute a Cypher query with parameters.
     * 
     * @param cypher     the Cypher query to execute
     * @param parameters the query parameters
     */
    public void execute(String cypher, Map<String, Object> parameters) {
        validateCypherSafety(cypher, parameters);
        execute(connection -> {
            logger.debug("Executing Cypher: {}", cypher);
            Map<String, Value> valueParameters = convertParameters(parameters);
            try (PreparedStatement statement = connection.prepare(cypher);
                    QueryResult result = connection.execute(statement, valueParameters)) {
                logger.debug("Execute result success: {}", result.isSuccess());
            } finally {
                valueParameters.values().forEach(v -> {
                    try {
                        v.close();
                    } catch (Exception e) {
                        /* ignore */ }
                });
            }
            return null;
        });
    }

    /**
     * Executes a query and returns a Stream of results.
     * Memory-efficient for large result sets.
     * 
     * @param cypher     the Cypher query to execute
     * @param parameters the query parameters
     * @param rowMapper  the row mapper to use
     * @param <T>        the result type
     * @return a stream of results
     */
    public <T> Stream<T> stream(String cypher, Map<String, Object> parameters, RowMapper<T> rowMapper) {
        validateCypherSafety(cypher, parameters);

        Connection connection = null;
        PreparedStatement statement = null;
        QueryResult result = null;
        Map<String, Value> valueParameters = Collections.emptyMap();
        boolean isNewConnection = false;

        try {
            connection = getConnection();
            isNewConnection = !isConnectionBoundToTransaction();

            valueParameters = convertParameters(parameters);
            statement = connection.prepare(cypher);
            result = connection.execute(statement, valueParameters);

            int numColumns = (int) result.getNumColumns();
            Map<String, Integer> columnToIndex = new HashMap<>(numColumns);
            for (int i = 0; i < numColumns; i++) {
                columnToIndex.put(result.getColumnName(i), i);
            }

            Value[] valuesArray = new Value[numColumns];
            QueryRow queryRow = new DefaultQueryRow(valuesArray, columnToIndex);

            final QueryResult finalResult = result;
            final PreparedStatement finalStatement = statement;
            final Connection finalConnection = connection;
            final boolean finalIsNewConnection = isNewConnection;
            final Map<String, Value> finalValueParameters = valueParameters;

            Iterator<T> iterator = new Iterator<>() {
                @Override
                public boolean hasNext() {
                    return finalResult.hasNext();
                }

                @Override
                public T next() {
                    var row = finalResult.getNext();
                    try {
                        for (int i = 0; i < numColumns; i++) {
                            valuesArray[i] = row.getValue(i);
                        }
                        return rowMapper.mapRow(queryRow);
                    } catch (Exception e) {
                        throw new CypherMappingException("Error mapping row", e);
                    } finally {
                        for (int i = 0; i < numColumns; i++) {
                            if (valuesArray[i] != null) {
                                valuesArray[i].close();
                                valuesArray[i] = null;
                            }
                        }
                    }
                }
            };

            return StreamSupport
                    .stream(Spliterators.spliterator(iterator, result.getNumTuples(), Spliterator.ORDERED), false)
                    .onClose(() -> {
                        closeQuietly(finalResult);
                        closeQuietly(finalStatement);
                        finalValueParameters.values().forEach(this::closeQuietly);
                        if (finalIsNewConnection) {
                            releaseConnection(finalConnection);
                        }
                    });

        } catch (Exception e) {
            closeQuietly(result);
            closeQuietly(statement);
            valueParameters.values().forEach(this::closeQuietly);
            if (isNewConnection && connection != null) {
                releaseConnection(connection);
            }
            throw new RuntimeException("Failed to initialize stream", e);
        }
    }

    private void closeQuietly(AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception e) {
                logger.error("Error closing resource", e);
            }
        }
    }

    /**
     * Execute a query and return a list of results.
     * 
     * @param cypher     the Cypher query to execute
     * @param parameters the query parameters
     * @param rowMapper  the row mapper to use
     * @param <T>        the result type
     * @return a list of results
     */
    public <T> List<T> query(String cypher, Map<String, Object> parameters, RowMapper<T> rowMapper) {
        try (Stream<T> s = stream(cypher, parameters, rowMapper)) {
            return s.toList();
        }
    }

    /**
     * Execute a query and return a list of results.
     * 
     * @param statement the statement to execute
     * @param rowMapper the row mapper to use
     * @param <T>       the result type
     * @return a list of results
     */
    public <T> List<T> query(Statement statement, RowMapper<T> rowMapper) {
        return query(statement.getCypher(), Map.of(), rowMapper);
    }

    /**
     * Execute a query and return a list of results.
     * 
     * @param statement  the statement to execute
     * @param parameters the query parameters
     * @param rowMapper  the row mapper to use
     * @param <T>        the result type
     * @return a list of results
     */
    public <T> List<T> query(Statement statement, Map<String, Object> parameters, RowMapper<T> rowMapper) {
        return query(statement.getCypher(), parameters, rowMapper);
    }

    /**
     * Execute a query and return a list of results.
     * 
     * @param cypher    the Cypher query to execute
     * @param rowMapper the row mapper to use
     * @param <T>       the result type
     * @return a list of results
     */
    public <T> List<T> query(String cypher, RowMapper<T> rowMapper) {
        return query(cypher, Map.of(), rowMapper);
    }

    /**
     * Execute a query and return an optional result.
     * 
     * @param statement the statement to execute
     * @param rowMapper the row mapper to use
     * @param <T>       the result type
     * @return an optional result
     */
    public <T> Optional<T> queryForObject(Statement statement, RowMapper<T> rowMapper) {
        return queryForObject(statement.getCypher(), rowMapper);
    }

    /**
     * Execute a query and return an optional result.
     * 
     * @param statement  the statement to execute
     * @param parameters the query parameters
     * @param rowMapper  the row mapper to use
     * @param <T>        the result type
     * @return an optional result
     */
    public <T> Optional<T> queryForObject(Statement statement, Map<String, Object> parameters, RowMapper<T> rowMapper) {
        return queryForObject(statement.getCypher(), parameters, rowMapper);
    }

    /**
     * Execute a query and return an optional result.
     * 
     * @param cypher    the Cypher query to execute
     * @param rowMapper the row mapper to use
     * @param <T>       the result type
     * @return an optional result
     */
    public <T> Optional<T> queryForObject(String cypher, RowMapper<T> rowMapper) {
        return queryForObject(cypher, Map.of(), rowMapper);
    }

    /**
     * Execute a query and return an optional result.
     * 
     * @param cypher     the Cypher query to execute
     * @param parameters the query parameters
     * @param rowMapper  the row mapper to use
     * @param <T>        the result type
     * @return an optional result
     */
    public <T> Optional<T> queryForObject(String cypher, Map<String, Object> parameters, RowMapper<T> rowMapper) {
        try (Stream<T> s = stream(cypher, parameters, rowMapper)) {
            return s.findFirst();
        }
    }

    /**
     * Execute a query and return a list of strings.
     * 
     * @param statement  the statement to execute
     * @param columnName the column name to return
     * @return a list of strings
     */
    public List<String> queryForStringList(Statement statement, String columnName) {
        return queryForStringList(statement.getCypher(), columnName);
    }

    /**
     * Execute a query and return a list of strings.
     * 
     * @param cypher     the Cypher query to execute
     * @param columnName the column name to return
     * @return a list of strings
     */
    public List<String> queryForStringList(String cypher, String columnName) {
        return query(cypher, (row) -> row.getValue(columnName).getValue().toString());
    }

    private void validateCypherSafety(String cypher, Map<String, Object> parameters) {
        if (cypher == null)
            return;
        if ((parameters == null || parameters.isEmpty()) && cypher.contains("'")) {
            long quoteCount = cypher.chars().filter(ch -> ch == '\'').count();
            if (quoteCount > 2) {
                logger.warn("SECURITY: Detected Cypher query with multiple literals and no parameters. Query: {}",
                        cypher);
            }
        }
    }

    private Connection getConnection() {
        ConnectionHolder holder = (ConnectionHolder) TransactionSynchronizationManager.getResource(connectionFactory);
        if (holder != null) {
            return holder.getConnection();
        }
        return connectionFactory.getConnection();
    }

    private void releaseConnection(Connection connection) {
        connectionFactory.releaseConnection(connection);
    }

    private boolean isConnectionBoundToTransaction() {
        return TransactionSynchronizationManager.hasResource(connectionFactory);
    }

    private Map<String, Value> convertParameters(Map<String, Object> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Value> converted = new HashMap<>();
        parameters.forEach((key, value) -> converted.put(key, toValue(value)));
        return converted;
    }

    private Value toValue(Object obj) {
        if (obj == null)
            return Value.createNull();
        if (obj instanceof Value v)
            return v;
        if (obj instanceof Collection c) {
            Value[] values = new Value[c.size()];
            int i = 0;
            for (Object o : c) {
                values[i++] = toValue(o);
            }
            return new LbugList(values).getValue();
        }
        return new Value(obj);
    }

    public static class CypherMappingException extends RuntimeException {
        public CypherMappingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
