package cn.nju.dataintegration.collegea;

import cn.nju.dataintegration.collegea.gui.LoginFrame;
import cn.nju.dataintegration.collegea.net.CollegeATcpServer;
import cn.nju.dataintegration.collegea.net.XmlTcpServer;
import cn.nju.dataintegration.config.AppConfig;
import cn.nju.dataintegration.db.Db;
import cn.nju.dataintegration.integration.IntegrationTcpServer;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.sql.Connection;

/**
 * 学院 A 主入口：集成服务器(9200) + 业务(9002) + XML(9102) + Swing 登录。
 */
public final class CollegeAApplication {

    public static void main(String[] args) {
        try {
            AppConfig config = new AppConfig();
            Db db = new Db(config);
            try (Connection c = db.open();
                 var rs = c.createStatement().executeQuery(
                         "SELECT DB_NAME(), (SELECT COUNT(*) FROM Student), (SELECT COUNT(*) FROM Course)")) {
                if (!rs.next()) {
                    throw new IllegalStateException("数据库无响应");
                }
                System.out.println("[CollegeA] 已连接 SQL Server 库: " + rs.getString(1)
                        + " | 学生=" + rs.getInt(2) + " 课程=" + rs.getInt(3));
            }
            new Thread(new IntegrationTcpServer(config), "integration-tcp").start();
            new Thread(new CollegeATcpServer(config, db), "college-a-tcp").start();
            new Thread(new XmlTcpServer(config, db), "college-a-xml").start();
            Thread.sleep(300);
            SwingUtilities.invokeLater(() -> new LoginFrame(config).setVisible(true));
        } catch (Exception e) {
            e.printStackTrace();
            String hint = e.getMessage() + "\n\n请确认:\n"
                    + "1. SQL Server 已启动且已执行 sql/01_schema.sql、02_seed.sql\n"
                    + "2. application.properties 中 db.url / sa 密码正确\n"
                    + "3. 运行 DbConnectionTest 单独测连接\n详见 sql/README.md";
            JOptionPane.showMessageDialog(null, hint, "SQL Server 连接失败", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }
}
