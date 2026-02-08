package com.thecookiezen.archiledger.infrastructure.config;

import com.ladybugdb.Database;
import com.thecookiezen.ladybugdb.spring.config.EnableLadybugDBRepositories;
import com.thecookiezen.ladybugdb.spring.connection.LadybugDBConnectionFactory;
import com.thecookiezen.ladybugdb.spring.connection.PooledConnectionFactory;
import com.thecookiezen.ladybugdb.spring.core.LadybugDBTemplate;
import com.thecookiezen.ladybugdb.spring.transaction.LadybugDBTransactionManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@EnableLadybugDBRepositories(basePackages = "com.thecookiezen.archiledger.infrastructure.persistence.ladybugdb")
public class LadybugDBConfig {

    @Value("${ladybugdb.pool.max-total:10}")
    private int poolMaxTotal;

    @Value("${ladybugdb.pool.max-idle:5}")
    private int poolMaxIdle;

    @Value("${ladybugdb.pool.min-idle:2}")
    private int poolMinIdle;

    @Bean
    public Database database() {
        return new Database();
    }

    @Bean
    public LadybugDBConnectionFactory connectionFactory(Database database) {
        return new PooledConnectionFactory(database);
    }

    @Bean
    public LadybugDBTemplate ladybugDBTemplate(LadybugDBConnectionFactory connectionFactory) {
        return new LadybugDBTemplate(connectionFactory);
    }

    @Bean
    public PlatformTransactionManager transactionManager(LadybugDBConnectionFactory connectionFactory) {
        return new LadybugDBTransactionManager(connectionFactory);
    }
}
