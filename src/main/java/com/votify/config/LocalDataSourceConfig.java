package com.votify.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;

/**
 * DataSource para desarrollo local con Supabase Transaction Pooler.
 * Se activa solo con el perfil "local" y tiene prioridad maxima sobre
 * cualquier variable de entorno o propiedad del sistema.
 */
@Configuration
@Profile("local")
@AutoConfigureBefore(DataSourceAutoConfiguration.class)
public class LocalDataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(LocalDataSourceConfig.class);

    private static final String URL =
            "jdbc:postgresql://aws-1-eu-west-1.pooler.supabase.com:6543/postgres?sslmode=require";
    private static final String USERNAME = "postgres.bmulgijtddwdwaajktay";
    private static final String PASSWORD = "19VX5FTtdniXvkho";

    @Bean
    @Primary
    public DataSource dataSource() {
        log.info(">>> LocalDataSourceConfig ACTIVO - conectando como: {}", USERNAME);
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(URL);
        ds.setUsername(USERNAME);
        ds.setPassword(PASSWORD);
        ds.setDriverClassName("org.postgresql.Driver");
        ds.addDataSourceProperty("prepareThreshold", "0");
        return ds;
    }
}
