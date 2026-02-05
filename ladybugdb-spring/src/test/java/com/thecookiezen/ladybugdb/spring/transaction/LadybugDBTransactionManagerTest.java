package com.thecookiezen.ladybugdb.spring.transaction;

import com.ladybugdb.Connection;
import com.ladybugdb.Database;
import com.thecookiezen.ladybugdb.spring.connection.SimpleConnectionFactory;
import com.thecookiezen.ladybugdb.spring.core.ConnectionHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterAll;

class LadybugDBTransactionManagerTest {

    private static Database db;
    private static SimpleConnectionFactory connectionFactory;
    private LadybugDBTransactionManager transactionManager;

    @BeforeAll
    static void setupAll() {
        db = new Database(":memory:");
        connectionFactory = new SimpleConnectionFactory(db);
    }

    @BeforeEach
    void setup() {
        transactionManager = new LadybugDBTransactionManager(connectionFactory);

        if (TransactionSynchronizationManager.hasResource(connectionFactory)) {
            TransactionSynchronizationManager.unbindResource(connectionFactory);
        }
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.hasResource(connectionFactory)) {
            TransactionSynchronizationManager.unbindResource(connectionFactory);
        }
    }

    @AfterAll
    static void tearDownAll() {
        if (connectionFactory != null) {
            connectionFactory.close();
        }
        if (db != null) {
            db.close();
        }
    }

    @Test
    void getTransaction_shouldBindConnectionToThread() {
        TransactionDefinition def = new DefaultTransactionDefinition();

        TransactionStatus status = transactionManager.getTransaction(def);

        assertTrue(TransactionSynchronizationManager.hasResource(connectionFactory));
        assertNotNull(status);

        transactionManager.commit(status);
    }

    @Test
    void commit_shouldUnbindConnection() {
        TransactionDefinition def = new DefaultTransactionDefinition();
        TransactionStatus status = transactionManager.getTransaction(def);

        transactionManager.commit(status);

        assertFalse(TransactionSynchronizationManager.hasResource(connectionFactory));
    }

    @Test
    void rollback_shouldUnbindConnection() {
        TransactionDefinition def = new DefaultTransactionDefinition();
        TransactionStatus status = transactionManager.getTransaction(def);

        transactionManager.rollback(status);

        assertFalse(TransactionSynchronizationManager.hasResource(connectionFactory));
    }

    @Test
    void getConnectionFactory_shouldReturnFactory() {
        assertEquals(connectionFactory, transactionManager.getConnectionFactory());
    }

    @Test
    void nestedTransaction_shouldReuseConnection() {
        TransactionDefinition def = new DefaultTransactionDefinition();
        TransactionStatus outer = transactionManager.getTransaction(def);

        ConnectionHolder holderBefore = (ConnectionHolder) TransactionSynchronizationManager
                .getResource(connectionFactory);
        Connection connectionBefore = holderBefore.getConnection();

        // Start nested transaction
        TransactionDefinition innerDef = new DefaultTransactionDefinition();
        innerDef = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRED);
        TransactionStatus inner = transactionManager.getTransaction(innerDef);

        ConnectionHolder holderAfter = (ConnectionHolder) TransactionSynchronizationManager
                .getResource(connectionFactory);

        // Should use same connection holder
        assertSame(holderBefore, holderAfter);

        transactionManager.commit(inner);
        transactionManager.commit(outer);
    }
}
