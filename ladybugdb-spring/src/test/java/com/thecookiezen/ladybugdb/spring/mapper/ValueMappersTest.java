package com.thecookiezen.ladybugdb.spring.mapper;

import com.ladybugdb.Connection;
import com.ladybugdb.Database;
import com.ladybugdb.QueryResult;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ValueMappersTest {

    private static Connection conn;
    private static Database db;

    @BeforeAll
    static void setup() {
        db = new Database(":memory:");
        conn = new Connection(db);
    }

    @AfterEach
    void tearDownEach() {
        conn.query("MATCH (n) DETACH DELETE n");
        conn.query("DROP TABLE IF EXISTS Test");
    }

    @AfterAll
    static void tearDown() {
        if (conn != null) {
            conn.close();
        }
        if (db != null) {
            db.close();
        }
    }

    @Nested
    class ScalarTypes {

        @Test
        void asString_shouldMapStringValue() {
            conn.query("CREATE NODE TABLE Test(id STRING PRIMARY KEY)");
            conn.query("CREATE (t:Test {id: 'hello'})");

            QueryResult result = conn.query("MATCH (t:Test) RETURN t.id");
            assertTrue(result.hasNext());
            var row = result.getNext();

            assertEquals("hello", ValueMappers.asString(row.getValue(0)));
        }

        @Test
        void asString_shouldReturnNullForNullValue() {
            assertNull(ValueMappers.asString(null));
        }

        @Test
        void asInteger_shouldMapIntegerValue() {
            conn.query("CREATE NODE TABLE Test(id STRING PRIMARY KEY, num INT64)");
            conn.query("CREATE (t:Test {id: 'test', num: 42})");

            QueryResult result = conn.query("MATCH (t:Test) RETURN t.num");
            assertTrue(result.hasNext());
            var row = result.getNext();

            assertEquals(42, ValueMappers.asInteger(row.getValue(0)));
        }

        @Test
        void asInteger_shouldReturnNullForNullValue() {
            assertNull(ValueMappers.asInteger(null));
        }

        @Test
        void asLong_shouldMapLongValue() {
            conn.query("CREATE NODE TABLE Test(id STRING PRIMARY KEY, num INT64)");
            conn.query("CREATE (t:Test {id: 'test', num: 9999999999})");

            QueryResult result = conn.query("MATCH (t:Test) RETURN t.num");
            assertTrue(result.hasNext());
            var row = result.getNext();

            assertEquals(9999999999L, ValueMappers.asLong(row.getValue(0)));
        }

        @Test
        void asDouble_shouldMapDoubleValue() {
            conn.query("CREATE NODE TABLE Test(id STRING PRIMARY KEY, num DOUBLE)");
            conn.query("CREATE (t:Test {id: 'test', num: 3.14})");

            QueryResult result = conn.query("MATCH (t:Test) RETURN t.num");
            assertTrue(result.hasNext());
            var row = result.getNext();

            assertEquals(3.14, ValueMappers.asDouble(row.getValue(0)), 0.001);
        }

        @Test
        void asBoolean_shouldMapBooleanValue() {
            conn.query("CREATE NODE TABLE Test(id STRING PRIMARY KEY, flag BOOL)");
            conn.query("CREATE (t:Test {id: 'test', flag: true})");

            QueryResult result = conn.query("MATCH (t:Test) RETURN t.flag");
            assertTrue(result.hasNext());
            var row = result.getNext();

            assertTrue(ValueMappers.asBoolean(row.getValue(0)));
        }
    }

    @Nested
    class ListTypes {

        @Test
        void asStringList_shouldMapStringArray() {
            conn.query("CREATE NODE TABLE Test(id STRING PRIMARY KEY, tags STRING[])");
            conn.query("CREATE (t:Test {id: 'test', tags: ['a', 'b', 'c']})");

            QueryResult result = conn.query("MATCH (t:Test) RETURN t.tags");
            assertTrue(result.hasNext());
            var row = result.getNext();

            List<String> tags = ValueMappers.asStringList(row.getValue(0));
            assertEquals(List.of("a", "b", "c"), tags);
        }

        @Test
        void asStringList_shouldReturnEmptyListForNull() {
            assertEquals(List.of(), ValueMappers.asStringList(null));
        }

        @Test
        void asIntegerList_shouldMapIntegerArray() {
            conn.query("CREATE NODE TABLE Test(id STRING PRIMARY KEY, nums INT64[])");
            conn.query("CREATE (t:Test {id: 'test', nums: [1, 2, 3]})");

            QueryResult result = conn.query("MATCH (t:Test) RETURN t.nums");
            assertTrue(result.hasNext());
            var row = result.getNext();

            List<Integer> nums = ValueMappers.asIntegerList(row.getValue(0));
            assertEquals(List.of(1, 2, 3), nums);
        }

        @Test
        void asLongList_shouldMapLongArray() {
            conn.query("CREATE NODE TABLE Test(id STRING PRIMARY KEY, nums INT64[])");
            conn.query("CREATE (t:Test {id: 'test', nums: [100, 200, 300]})");

            QueryResult result = conn.query("MATCH (t:Test) RETURN t.nums");
            assertTrue(result.hasNext());
            var row = result.getNext();

            List<Long> nums = ValueMappers.asLongList(row.getValue(0));
            assertEquals(List.of(100L, 200L, 300L), nums);
        }

        @Test
        void asDoubleList_shouldMapDoubleArray() {
            conn.query("CREATE NODE TABLE Test(id STRING PRIMARY KEY, nums DOUBLE[])");
            conn.query("CREATE (t:Test {id: 'test', nums: [1.1, 2.2, 3.3]})");

            QueryResult result = conn.query("MATCH (t:Test) RETURN t.nums");
            assertTrue(result.hasNext());
            var row = result.getNext();

            List<Double> nums = ValueMappers.asDoubleList(row.getValue(0));
            assertEquals(3, nums.size());
            assertEquals(1.1, nums.get(0), 0.001);
            assertEquals(2.2, nums.get(1), 0.001);
            assertEquals(3.3, nums.get(2), 0.001);
        }

        @Test
        void asList_shouldMapWithCustomMapper() {
            conn.query("CREATE NODE TABLE Test(id STRING PRIMARY KEY, nums INT64[])");
            conn.query("CREATE (t:Test {id: 'test', nums: [10, 20, 30]})");

            QueryResult result = conn.query("MATCH (t:Test) RETURN t.nums");
            assertTrue(result.hasNext());
            var row = result.getNext();

            List<String> numsAsStrings = ValueMappers.asList(row.getValue(0), s -> "num:" + s);
            assertEquals(List.of("num:10", "num:20", "num:30"), numsAsStrings);
        }
    }
}
