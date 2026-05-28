package cn.nju.dataintegration.db;

import cn.nju.dataintegration.config.AppConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * JDBC 连接工厂。每次调用 {@link #open()} 返回一个新连接，
 * 调用方负责用 try-with-resources 关闭。
 *
 * 不做连接池：教学项目并发量低、操作短、用最直白的方式。
 */
public final class Db {
    private final AppConfig config;

    public Db(AppConfig config) {
        this.config = config;
    }

    public Connection open() throws SQLException {
        return DriverManager.getConnection(
                config.dbUrl(),
                config.dbUser(),
                config.dbPassword());
    }
}
