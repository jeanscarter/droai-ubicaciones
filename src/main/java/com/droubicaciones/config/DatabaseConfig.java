package com.droubicaciones.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Singleton HikariCP connection pool para SQL Server (DROA_A).
 */
public class DatabaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);

    private static String SERVER;
    private static String PORT;
    private static String DATABASE;
    private static String USER;
    private static String PASSWORD;
    private static int WEB_PORT = 8085;

    static {
        loadProperties();
    }

    private static void loadProperties() {
        Properties props = new Properties();
        File propFile = new File("db.properties");

        if (propFile.exists()) {
            try (FileInputStream in = new FileInputStream(propFile)) {
                props.load(in);
                SERVER = props.getProperty("DB_SERVER", "srvdb0101");
                PORT = props.getProperty("DB_PORT", "1433");
                DATABASE = props.getProperty("DB_DATABASE", "DROA_A");
                USER = props.getProperty("DB_USER", "profit");
                PASSWORD = props.getProperty("DB_PASSWORD", "profit");
                String wp = props.getProperty("WEB_PORT", "8085");
                try {
                    WEB_PORT = Integer.parseInt(wp.trim());
                } catch (Exception ignored) {
                    WEB_PORT = 8085;
                }
                logger.info("Propiedades cargadas correctamente. Servidor: {}, Puerto Web: {}", SERVER, WEB_PORT);
                return;
            } catch (Exception e) {
                logger.error("Error al leer db.properties: {}", e.getMessage());
            }
        }

        SERVER = "srvdb0101";
        PORT = "1433";
        DATABASE = "DROA_A";
        USER = "profit";
        PASSWORD = "profit";
        WEB_PORT = 8085;
    }

    public static int getWebPort() {
        return WEB_PORT;
    }

    private static HikariDataSource dataSource;

    public static synchronized DataSource getDataSource() {
        if (dataSource == null) {
            HikariConfig config = new HikariConfig();
            String jdbcUrl = String.format(
                    "jdbc:sqlserver://%s:%s;databaseName=%s;encrypt=false;trustServerCertificate=true",
                    SERVER, PORT, DATABASE);

            config.setJdbcUrl(jdbcUrl);
            config.setUsername(USER);
            config.setPassword(PASSWORD);
            config.setDriverClassName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            config.setPoolName("DroUbicaciones-Pool");

            config.setMaximumPoolSize(5);
            config.setMinimumIdle(2);
            config.setIdleTimeout(600000);
            config.setConnectionTimeout(5000);

            dataSource = new HikariDataSource(config);
            logger.info("[DroUbicaciones-DB] ✔ Conexión inicializada con éxito.");
        }
        return dataSource;
    }

    public static java.sql.Connection getConnection() throws java.sql.SQLException {
        return getDataSource().getConnection();
    }

    public static synchronized void closePool() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            dataSource = null;
        }
    }
}
