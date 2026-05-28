package cn.nju.dataintegration.db;

import cn.nju.dataintegration.config.AppConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * SQL Server JDBC 连接（学院 A 仅使用真实 SQL Server，见 sql/README.md）。
 */
public final class Db {
    static {
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError("缺少 mssql-jdbc 驱动，请执行 mvn compile");
        }
    }

    private final AppConfig config;

    public Db(AppConfig config) {
        this.config = config;
    }

    public Connection open() throws SQLException {
        String url = config.dbUrl();
        String user = config.dbUser();
        String pwd = config.dbPassword();
        if (url == null || url.isBlank()) {
            throw new SQLException("未配置 db.url，请编辑 application.properties");
        }
        if (url.contains("integratedSecurity=true")) {
            return DriverManager.getConnection(url);
        }
        return DriverManager.getConnection(url, user, pwd);
    }
}
