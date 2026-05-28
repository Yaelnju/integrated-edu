package cn.nju.dataintegration.collegea.gui;

import cn.nju.dataintegration.config.AppConfig;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

/**
 * 院系 A 主界面。布局沿用 Student 3 的设计，字段标签换 B 中文（专业 / 课程编号 / 课时 / 学分 等）。
 * 学生角色可见跨院选课区（目标院系下拉框 A/C）。
 */
public final class MainFrame extends JFrame {
    private final String role;
    private final String userId;
    private final CollegeAClient client;
    private final DefaultTableModel courseModel = new DefaultTableModel(
            new Object[]{"课程编号", "课程名称", "学分", "教师", "共享"}, 0);
    private final DefaultTableModel selModel = new DefaultTableModel(
            new Object[]{"学号", "课程编号", "得分", "课程名称"}, 0);
    private final JTextField pickField = new JTextField(6);
    private final JTextField dropField = new JTextField(6);
    private final JTextField crossCnoField = new JTextField(6);
    private final JComboBox<String> crossTargetBox = new JComboBox<>(new String[]{"B", "C"});

    public MainFrame(AppConfig config, String role, String userId) {
        this.role = role;
        this.userId = userId;
        this.client = new CollegeAClient(config);
        setTitle("院系 A 计算机学院教务系统 — " + role + " — " + userId);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));

        JTable courseTable = new JTable(courseModel);
        JTable selTable = new JTable(selModel);

        JPanel center = new JPanel(new BorderLayout(8, 8));
        center.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        center.add(new JLabel("课程列表（本院）"), BorderLayout.NORTH);
        center.add(new JScrollPane(courseTable), BorderLayout.CENTER);

        JPanel east = new JPanel(new BorderLayout(8, 8));
        east.add(new JLabel("我的选课"), BorderLayout.NORTH);
        east.add(new JScrollPane(selTable), BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton refresh = new JButton("刷新");
        refresh.addActionListener(e -> refreshAll());
        actions.add(refresh);

        if ("STUDENT".equals(role)) {
            actions.add(new JLabel("选课号"));
            actions.add(pickField);
            JButton pick = new JButton("本院选课");
            pick.addActionListener(e -> doPick());
            actions.add(pick);

            actions.add(new JLabel("退课号"));
            actions.add(dropField);
            JButton drop = new JButton("退课");
            drop.addActionListener(e -> doDrop());
            actions.add(drop);

            actions.add(new JLabel("跨院课号"));
            actions.add(crossCnoField);
            actions.add(new JLabel("目标"));
            actions.add(crossTargetBox);
            JButton cross = new JButton("跨院选课");
            cross.addActionListener(e -> doCrossEnroll());
            actions.add(cross);
        }
        if ("ADMIN".equals(role)) {
            JButton localStats = new JButton("本院统计");
            localStats.addActionListener(e -> showLocalStats());
            actions.add(localStats);
        }
        JButton intStats = new JButton("集成统计(全院)");
        intStats.addActionListener(e -> showIntegratedStats());
        actions.add(intStats);

        add(center, BorderLayout.CENTER);
        add(east, BorderLayout.EAST);
        add(actions, BorderLayout.SOUTH);
        setSize(1000, 540);
        setLocationRelativeTo(null);
        refreshAll();
    }

    private void refreshAll() {
        try {
            String courses = client.call("LIST_COURSES");
            courseModel.setRowCount(0);
            for (String line : courses.split("\n")) {
                if (line.isBlank()) {
                    continue;
                }
                String[] r = line.split("\\|", -1);
                if (r.length >= 5) {
                    courseModel.addRow(new Object[]{r[0], r[1], r[2], r[3], r[4]});
                }
            }
            if ("STUDENT".equals(role)) {
                String mine = client.call("MY_SC|" + userId);
                selModel.setRowCount(0);
                for (String line : mine.split("\n")) {
                    if (line.isBlank()) {
                        continue;
                    }
                    String[] r = line.split("\\|", -1);
                    if (r.length >= 4) {
                        selModel.addRow(new Object[]{r[0], r[1], r[2], r[3]});
                    }
                }
            } else {
                selModel.setRowCount(0);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "刷新失败", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void doPick() {
        try {
            String cno = pickField.getText().trim();
            String res = client.call("PICK|" + userId + "|" + cno);
            JOptionPane.showMessageDialog(this,
                    res.contains("OK") ? "选课成功或已选" : "选课失败（已满 5 门或重复）",
                    "结果", JOptionPane.INFORMATION_MESSAGE);
            refreshAll();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "选课失败", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void doDrop() {
        try {
            String cno = dropField.getText().trim();
            String res = client.call("DROP|" + userId + "|" + cno);
            JOptionPane.showMessageDialog(this,
                    res.contains("OK") ? "退课成功（已通知集成服务器登记）" : "退课失败",
                    "结果", JOptionPane.INFORMATION_MESSAGE);
            refreshAll();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "退课失败", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void doCrossEnroll() {
        try {
            String cno = crossCnoField.getText().trim();
            String target = (String) crossTargetBox.getSelectedItem();
            String res = client.call("CROSS_ENROLL|" + userId + "|" + cno + "|" + target);
            JOptionPane.showMessageDialog(this, res, "跨院选课结果", JOptionPane.INFORMATION_MESSAGE);
            refreshAll();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "跨院选课失败", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showLocalStats() {
        try {
            String body = client.call("STATS_LOCAL");
            String[] p = body.split("\\|");
            JOptionPane.showMessageDialog(this,
                    "学生: " + p[0] + "\n课程: " + p[1] + "\n选课记录: " + p[2],
                    "本院统计", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "统计失败", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showIntegratedStats() {
        try {
            String body = client.call("INTEGRATED_STATS");
            JOptionPane.showMessageDialog(this, body, "集成统计", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "集成统计失败", JOptionPane.ERROR_MESSAGE);
        }
    }
}
