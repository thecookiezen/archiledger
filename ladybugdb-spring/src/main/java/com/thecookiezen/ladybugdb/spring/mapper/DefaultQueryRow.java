package com.thecookiezen.ladybugdb.spring.mapper;

import com.ladybugdb.DataTypeID;
import com.ladybugdb.LbugStruct;
import com.ladybugdb.Value;
import com.ladybugdb.ValueRelUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Default implementation of {@link QueryRow} that wraps the raw column-to-value
 * map from a query result.
 * <p>
 * Uses {@link LbugStruct} to extract node properties and {@link ValueRelUtil}
 * to extract relationship data.
 */
public final class DefaultQueryRow implements QueryRow {

    private final Value[] values;
    private final Map<String, Integer> columnToIndex;

    /**
     * Creates a QueryRow from the raw values and column index map.
     *
     * @param values        the array of values for this row
     * @param columnToIndex the shared map of column names to indices
     */
    public DefaultQueryRow(Value[] values, Map<String, Integer> columnToIndex) {
        this.values = values;
        this.columnToIndex = columnToIndex;
    }

    @Override
    public Value getValue(int index) {
        return index >= 0 && index < values.length ? values[index] : null;
    }

    @Override
    public Value getValue(String column) {
        Integer index = columnToIndex.get(column);
        return index != null && index < values.length ? values[index] : null;
    }

    @Override
    public boolean containsKey(String column) {
        return columnToIndex.containsKey(column);
    }

    @Override
    public boolean isNode(String column) {
        Value value = getValue(column);
        return value != null && value.getDataType().getID() == DataTypeID.NODE;
    }

    @Override
    public boolean isRelationship(String column) {
        Value value = getValue(column);
        return value != null && value.getDataType().getID() == DataTypeID.REL;
    }

    @Override
    public Map<String, Value> getNode(String column) {
        Value value = getValue(column);
        if (value == null) {
            throw new IllegalArgumentException("Column '" + column + "' does not exist");
        }
        if (value.getDataType().getID() != DataTypeID.NODE) {
            throw new IllegalArgumentException(
                    "Column '" + column + "' is not a NODE (type: " + value.getDataType().getID() + ")");
        }
        try (LbugStruct struct = new LbugStruct(value)) {
            return struct.toMap();
        }
    }

    @Override
    public RelationshipData getRelationship(String column) {
        Value value = getValue(column);
        if (value == null) {
            throw new IllegalArgumentException("Column '" + column + "' does not exist");
        }
        if (value.getDataType().getID() != DataTypeID.REL) {
            throw new IllegalArgumentException(
                    "Column '" + column + "' is not a REL (type: " + value.getDataType().getID() + ")");
        }

        var id = ValueRelUtil.getID(value);
        var labelName = ValueRelUtil.getLabelName(value);
        var sourceId = ValueRelUtil.getSrcID(value);
        var targetId = ValueRelUtil.getDstID(value);

        long propertySize = ValueRelUtil.getPropertySize(value);
        Map<String, Value> properties = new HashMap<>();
        for (long i = 0; i < propertySize; i++) {
            String propName = ValueRelUtil.getPropertyNameAt(value, i);
            Value propValue = ValueRelUtil.getPropertyValueAt(value, i);
            properties.put(propName, propValue);
        }

        return new RelationshipData(id, labelName, sourceId, targetId, properties);
    }

    @Override
    public Set<String> keySet() {
        return columnToIndex.keySet();
    }
}
