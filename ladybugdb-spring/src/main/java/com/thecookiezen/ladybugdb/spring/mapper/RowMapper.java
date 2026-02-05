package com.thecookiezen.ladybugdb.spring.mapper;

import com.ladybugdb.FlatTuple;

/**
 * Functional interface for mapping a row from a LadybugDB query result to a
 * domain object.
 *
 * @param <T> the type of object to map to
 */
@FunctionalInterface
public interface RowMapper<T> {

    /**
     * Maps a single row of the query result to a domain object.
     *
     * @param row    the FlatTuple representing the current row
     * @param rowNum the number of the current row (0-indexed)
     * @return the mapped object
     * @throws Exception if an error occurs during mapping
     */
    T mapRow(FlatTuple row, int rowNum) throws Exception;
}
