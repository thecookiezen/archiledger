package com.thecookiezen.ladybugdb.spring.mapper;

import com.ladybugdb.Value;

import java.util.Map;
import java.util.Set;

/**
 * Represents a single row from a LadybugDB query result.
 * <p>
 * Provides typed access to columns containing nodes, relationships, or scalar
 * values.
 * This abstraction simplifies mapping of complex query results that return
 * multiple
 * nodes and relationships (e.g., {@code MATCH (a)-[r]->(b) RETURN a, r, b}).
 */
public interface QueryRow {

    /**
     * Gets the raw Value at the specified column.
     *
     * @param column the column name
     * @return the Value, or null if the column doesn't exist
     */
    Value getValue(String column);

    /**
     * Gets the raw Value at the specified column index.
     *
     * @param index the column index (0-based)
     * @return the Value, or null if the index is out of bounds
     */
    Value getValue(int index);

    /**
     * Checks if the row contains a column with the given name.
     *
     * @param column the column name
     * @return true if the column exists
     */
    boolean containsKey(String column);

    /**
     * Checks if the value at the column is a NODE type.
     *
     * @param column the column name
     * @return true if the column contains a node
     */
    boolean isNode(String column);

    /**
     * Checks if the value at the column is a REL (relationship) type.
     *
     * @param column the column name
     * @return true if the column contains a relationship
     */
    boolean isRelationship(String column);

    /**
     * Extracts the properties of a node at the specified column.
     *
     * @param column the column name containing a node
     * @return a map of property names to values
     * @throws IllegalArgumentException if the column doesn't contain a node
     */
    Map<String, Value> getNode(String column);

    /**
     * Extracts relationship data from the specified column.
     *
     * @param column the column name containing a relationship
     * @return the relationship data including IDs, label, and properties
     * @throws IllegalArgumentException if the column doesn't contain a relationship
     */
    RelationshipData getRelationship(String column);

    /**
     * Returns the set of column names in this row.
     *
     * @return the column names
     */
    Set<String> keySet();
}
