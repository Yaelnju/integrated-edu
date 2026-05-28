package cn.nju.dataintegration.collegec.gui;

import cn.nju.dataintegration.config.AppConfig;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.GridLayout;

public final class LoginFrame extends JFrame {
    private final AppConfig config;
    private final JTextField user = new JTextField(16);
    private final JPasswordField pwd = new JPasswordField(16);

    public LoginFrame(AppConfig config) {
        this.config = config;
        setTitle("院系 C 教务系统 — 登录");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel form = new JPanel(new GridLayout(0, 1, 6, 6));
        form.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        form.add(labeled("账号（学号或 admin）", user));
        form.add(labeled("密码", pwd));
        JButton login = new JButton("登录");
        login.addActionListener(e -> doLogin());
        JPanel south = new JPanel();
        south.add(login);
        add(form, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(null);
    }

    private JPanel labeled(String title, JTextField field) {
        JPanel p = new JPanel(new BorderLayout(4, 4));
        p.add(new JLabel(title), BorderLayout.NORTH);
        p.add(field, BorderLayout.CENTER);
        return p;
    }

    private void doLogin() {
        String u = user.getText().trim();
        String p = new String(pwd.getPassword());
        try {
            CollegeCClient client = new CollegeCClient(config);
            String body = client.call("LOGIN|" + u + "|" + p);
            String[] parts = body.split("\\|", 2);
            if (parts.length < 2) {
                throw new IllegalStateException("登录响应异常");
            }
            MainFrame mf = new MainFrame(config, parts[0], parts[1]);
            mf.setVisible(true);
            dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "登录失败", JOptionPane.ERROR_MESSAGE);
        }
    }
}
