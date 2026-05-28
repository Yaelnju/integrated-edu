package cn.nju.dataintegration.collegeb;

import cn.nju.dataintegration.collegeb.gui.LoginFrame;
import cn.nju.dataintegration.collegeb.net.CollegeBTcpServer;
import cn.nju.dataintegration.collegeb.net.XmlTcpServer;
import cn.nju.dataintegration.config.AppConfig;
import cn.nju.dataintegration.db.Db;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.sql.Connection;

/**
 * 院系 B 主入口。启动顺序：
 *   1) 探活 Oracle 连接
 *   2) 业务 TCP 服务（9001）
 *   3) XML TCP 服务（9101）—— 供集成服务器拉数据 / 写回 ENROLL
 *   4) Swing 登录窗
 * 注：集成服务器（9200）由 Student 1 负责启动；本入口不再启动它。
 */
public final class CollegeBApplication {

    public static void main(String[] args) {
        try {
            AppConfig config = new AppConfig();
            Db db = new Db(config);
            try (Connection c = db.open()) {
                c.createStatement().execute("SELECT 1 FROM DUAL");
            }
            new Thread(new CollegeBTcpServer(config, db), "college-b-tcp").start();
            new Thread(new XmlTcpServer(config, db), "college-b-xml").start();
            Thread.sleep(300);
            SwingUtilities.invokeLater(() -> new LoginFrame(config).setVisible(true));
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "启动失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }
}
