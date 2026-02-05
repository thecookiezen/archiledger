package com.thecookiezen.ladybugdb.spring.connection;

import com.ladybugdb.Connection;
import com.ladybugdb.Database;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SimpleConnectionFactoryTest {

    private Database db;
    private SimpleConnectionFactory factory;

    @BeforeEach
    void setup() {
        db = new Database(":memory:");
        factory = new SimpleConnectionFactory(db);
    }

    @AfterEach
    void tearDown() {
        if (factory != null) {
            factory.close();
        }
    }

    @Test
    void getConnection_shouldReturnNewConnection() {
        Connection conn = factory.getConnection();

        assertNotNull(conn);
    }

    @Test
    void getConnection_shouldReturnDifferentConnectionsEachTime() {
        Connection conn1 = factory.getConnection();
        Connection conn2 = factory.getConnection();

        assertNotNull(conn1);
        assertNotNull(conn2);
        assertNotSame(conn1, conn2);
    }

    @Test
    void releaseConnection_shouldNotThrow() {
        Connection conn = factory.getConnection();

        assertDoesNotThrow(() -> factory.releaseConnection(conn));
    }

    @Test
    void close_shouldNotThrowWhenCalledMultipleTimes() {
        assertDoesNotThrow(() -> {
            factory.close();
            factory.close();
        });
    }
}
