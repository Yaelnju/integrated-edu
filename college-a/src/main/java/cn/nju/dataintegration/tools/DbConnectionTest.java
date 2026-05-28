package cn.nju.dataintegration.tools;

import cn.nju.dataintegration.config.AppConfig;
import cn.nju.dataintegration.db.Db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * 启动前测试 SQL Server 连接与种子数据是否就绪。
 * mvn -q compile exec:java -Dexec.mainClass=cn.nju.dataintegration.tools.DbConnectionTest
 */
public final class DbConnectionTest {

    public static void main(String[] args) {
        AppConfig config = new AppConfig();
        System.out.println("JDBC URL: " + config.dbUrl());
        System.out.println("User: " + config.dbUser());
        try {
            Db db = new Db(config);
            try (Connection c = db.open();
                 Statement st = c.createStatement()) {
                try (ResultSet rs = st.executeQuery("SELECT @@VERSION")) {
                    rs.next();
                    System.out.println("SQL Server 连接成功。");
                    System.out.println(rs.getString(1).split("\n")[0]);
                }
                int stu = scalar(st, "SELECT COUNT(*) FROM Student");
                int cou = scalar(st, "SELECT COUNT(*) FROM Course");
                int sc = scalar(st, "SELECT COUNT(*) FROM Enrollment");
                System.out.println("Student: " + stu + ", Course: " + cou + ", Enrollment: " + sc);
                if (stu < 50 || cou < 10 || sc < 250) {
                    System.out.println("警告: 数据量不足，请在 SSMS 中执行 sql/01_schema.sql 与 sql/02_seed.sql");
                } else {
                    System.out.println("数据检查通过，可运行 CollegeAApplication。");
                }
            }
        } catch (Exception e) {
            System.err.println("连接失败: " + e.getMessage());
            System.err.println();
            System.err.println("请检查:");
            System.err.println("  1. SQL Server 服务已启动，TCP 已启用");
            System.err.println("  2. application.properties 中 db.url / db.user / db.password");
            System.err.println("  3. 已执行 sql/01_schema.sql 与 sql/02_seed.sql");
            System.err.println("详见 sql/README.md");
            System.exit(1);
        }
    }

    private static int scalar(Statement st, String sql) throws Exception {
        try (ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        }
    }
}
