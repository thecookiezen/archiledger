package com.thecookiezen.ladybugdb.spring.connection;

import com.ladybugdb.Connection;
import com.ladybugdb.Database;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PooledConnectionFactoryTest {

    private Database db;
    private PooledConnectionFactory factory;

    @BeforeEach
    void setup() {
        db = new Database(":memory:");
        factory = new PooledConnectionFactory(db);
    }

    @AfterEach
    void tearDown() {
        if (factory != null) {
            factory.close();
        }
    }

    @Test
    void getConnection_shouldReturnConnection() {
        Connection conn = factory.getConnection();

        assertNotNull(conn);
        assertEquals(1, factory.getNumActive());
        assertEquals(0, factory.getNumIdle());
    }

    @Test
    void releaseConnection_shouldReturnConnectionToPool() {
        Connection conn = factory.getConnection();
        assertEquals(1, factory.getNumActive());

        factory.releaseConnection(conn);

        assertEquals(0, factory.getNumActive());
        assertEquals(1, factory.getNumIdle());
    }

    @Test
    void getConnection_shouldReusePooledConnection() {
        Connection conn1 = factory.getConnection();
        factory.releaseConnection(conn1);

        Connection conn2 = factory.getConnection();

        assertNotNull(conn2);
        assertEquals(1, factory.getNumActive());
    }

    @Test
    void multipleConnections_shouldBeTrackedCorrectly() {
        Connection conn1 = factory.getConnection();
        Connection conn2 = factory.getConnection();

        assertEquals(2, factory.getNumActive());

        factory.releaseConnection(conn1);
        assertEquals(1, factory.getNumActive());
        assertEquals(1, factory.getNumIdle());

        factory.releaseConnection(conn2);
        assertEquals(0, factory.getNumActive());
        assertEquals(2, factory.getNumIdle());
    }

    @Test
    void close_shouldClosePool() {
        Connection conn = factory.getConnection();
        factory.releaseConnection(conn);

        assertDoesNotThrow(() -> factory.close());
    }

    @Test
    void getConnection_afterClose_shouldThrow() {
        factory.close();

        assertThrows(LadybugDBConnectionException.class, () -> factory.getConnection());
    }
}
