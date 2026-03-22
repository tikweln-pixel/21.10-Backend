package com.votify.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * Render, Railway y otros PaaS suelen exponer {@code DATABASE_URL} como
 * {@code postgres://user:pass@host:5432/db}. Spring JDBC espera {@code jdbc:postgresql://...}.
 */
@Configuration
@AutoConfigureBefore(DataSourceAutoConfiguration.class)
@ConditionalOnProperty(name = "DATABASE_URL")
public class PostgresUrlDataSourceConfiguration {

    @Bean
    @Primary
    public DataSource dataSource(@org.springframework.beans.factory.annotation.Value("${DATABASE_URL}") String databaseUrl) {
        return buildDataSource(databaseUrl);
    }

    static DataSource buildDataSource(String databaseUrl) {
        if (databaseUrl == null || databaseUrl.isBlank()) {
            throw new IllegalArgumentException("DATABASE_URL is empty");
        }
        if (!databaseUrl.startsWith("postgres://") && !databaseUrl.startsWith("postgresql://")) {
            throw new IllegalArgumentException("DATABASE_URL must start with postgres:// or postgresql://");
        }
        try {
            String normalized = databaseUrl.replaceFirst("^postgresql://", "http://").replaceFirst("^postgres://", "http://");
            URI uri = new URI(normalized);
            String userInfo = uri.getUserInfo();
            String username = null;
            String password = null;
            if (userInfo != null) {
                int colon = userInfo.indexOf(':');
                if (colon >= 0) {
                    username = URLDecoder.decode(userInfo.substring(0, colon), StandardCharsets.UTF_8);
                    password = URLDecoder.decode(userInfo.substring(colon + 1), StandardCharsets.UTF_8);
                } else {
                    username = URLDecoder.decode(userInfo, StandardCharsets.UTF_8);
                }
            }
            String host = uri.getHost();
            if (host == null || host.isEmpty()) {
                throw new IllegalArgumentException("DATABASE_URL has no host");
            }
            int port = uri.getPort() > 0 ? uri.getPort() : 5432;
            String path = uri.getPath();
            if (path == null || path.length() <= 1) {
                throw new IllegalArgumentException("DATABASE_URL path must include database name");
            }
            String database = path.substring(1);
            String query = uri.getRawQuery();
            StringBuilder jdbc = new StringBuilder();
            jdbc.append("jdbc:postgresql://").append(host).append(":").append(port).append("/").append(database);
            if (query != null && !query.isEmpty()) {
                jdbc.append("?").append(query);
            } else {
                jdbc.append("?sslmode=require");
            }
            HikariDataSource ds = new HikariDataSource();
            ds.setJdbcUrl(jdbc.toString());
            if (username != null) {
                ds.setUsername(username);
            }
            if (password != null) {
                ds.setPassword(password);
            }
            ds.setDriverClassName("org.postgresql.Driver");
            return ds;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Invalid DATABASE_URL", e);
        }
    }
}
