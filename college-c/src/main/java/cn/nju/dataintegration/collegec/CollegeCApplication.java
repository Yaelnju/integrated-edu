package cn.nju.dataintegration.collegec;

import cn.nju.dataintegration.collegec.gui.LoginFrame;
import cn.nju.dataintegration.collegec.net.CollegeCTcpServer;
import cn.nju.dataintegration.collegec.net.XmlTcpServer;
import cn.nju.dataintegration.config.AppConfig;
import cn.nju.dataintegration.db.Db;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.sql.Connection;

public final class CollegeCApplication {
    public static void main(String[] args) {
        try {
            AppConfig config = new AppConfig();
            Db db = new Db(config);
            try (Connection c = db.open()) {
                c.createStatement().execute("SELECT 1");
            }
            // 集成服务器由 college-a 模块独占启动（端口 9200）；这里不再启动避免端口冲突
            new Thread(new CollegeCTcpServer(config, db), "college-c-tcp").start();
            new Thread(new XmlTcpServer(config, db), "college-c-xml").start();
            Thread.sleep(300);
            SwingUtilities.invokeLater(() -> new LoginFrame(config).setVisible(true));
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "启动失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }
}
