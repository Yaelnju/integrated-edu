package cn.nju.dataintegration.collegec.net;

import cn.nju.dataintegration.collegec.repo.CollegeCRepository;
import cn.nju.dataintegration.config.AppConfig;
import cn.nju.dataintegration.db.Db;
import cn.nju.dataintegration.net.XmlFrameProtocol;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.StringJoiner;

public final class CollegeCTcpServer implements Runnable {
    private final AppConfig config;
    private final Db db;
    private final CollegeCRepository repo = new CollegeCRepository();

    public CollegeCTcpServer(AppConfig config, Db db) {
        this.config = config;
        this.db = db;
    }

    @Override
    public void run() {
        try (ServerSocket ss = new ServerSocket(config.collegeGuiPort())) {
            while (!Thread.currentThread().isInterrupted()) {
                Socket s = ss.accept();
                new Thread(() -> handle(s), "college-c-client").start();
            }
        } catch (IOException e) {
            if (!Thread.currentThread().isInterrupted()) {
                e.printStackTrace();
            }
        }
    }

    private void handle(Socket socket) {
        try (socket) {
            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            String line = br.readLine();
            if (line == null) {
                return;
            }
            String[] p = line.split("\\|");
            String cmd = p[0];
            try (Connection c = db.open()) {
                if ("LOGIN".equals(cmd)) {
                    handleLogin(socket, c, p);
                } else if ("LIST_COURSES".equals(cmd)) {
                    handleListCourses(socket, c);
                } else if ("MY_SC".equals(cmd)) {
                    handleMySc(socket, c, p);
                } else if ("PICK".equals(cmd)) {
                    handlePick(socket, c, p);
                } else if ("DROP".equals(cmd)) {
                    handleDrop(socket, c, p);
                } else if ("STATS_LOCAL".equals(cmd)) {
                    handleStatsLocal(socket, c);
                } else if ("INTEGRATED_STATS".equals(cmd)) {
                    handleIntegratedStats(socket);
                } else if ("CROSS_ENROLL".equals(cmd)) {
                    handleCrossEnroll(socket, p);
                } else {
                    XmlFrameProtocol.writeErr(socket, "未知命令");
                }
            }
        } catch (Exception e) {
            try {
                XmlFrameProtocol.writeErr(socket, e.getMessage());
            } catch (IOException ignored) {
            }
        }
    }

    private void handleLogin(Socket socket, Connection c, String[] p) throws IOException {
        if (p.length < 3) {
            XmlFrameProtocol.writeErr(socket, "参数不足");
            return;
        }
        try {
            var role = repo.login(c, p[1], p[2]);
            if (role.isEmpty()) {
                XmlFrameProtocol.writeErr(socket, "账号或密码错误");
                return;
            }
            XmlFrameProtocol.writeOkXml(socket, role.get().name() + "|" + p[1]);
        } catch (Exception e) {
            XmlFrameProtocol.writeErr(socket, e.getMessage());
        }
    }

    private void handleListCourses(Socket socket, Connection c) throws Exception {
        var rows = repo.listCourses(c);
        StringJoiner j = new StringJoiner("\n");
        for (String[] r : rows) {
            j.add(String.join("|", r));
        }
        XmlFrameProtocol.writeOkXml(socket, j.toString());
    }

    private void handleMySc(Socket socket, Connection c, String[] p) throws Exception {
        if (p.length < 2) {
            XmlFrameProtocol.writeErr(socket, "参数不足");
            return;
        }
        String sno = p[1];
        var rows = repo.mySelections(c, sno);
        StringJoiner j = new StringJoiner("\n");
        for (String[] r : rows) {
            j.add(String.join("|", r));
        }
        XmlFrameProtocol.writeOkXml(socket, j.toString());
    }

    private void handlePick(Socket socket, Connection c, String[] p) throws Exception {
        if (p.length < 3) {
            XmlFrameProtocol.writeErr(socket, "参数不足");
            return;
        }
        // 跨院选课时集成服务器会用外院 sno 走 PICK；若不存在先建占位学生避免 FK 报错
        repo.ensureStudentForCross(c, p[1]);
        boolean ok = repo.pickCourse(c, p[1], p[2]);
        XmlFrameProtocol.writeOkXml(socket, ok ? "OK" : "FAIL");
    }

    private void handleDrop(Socket socket, Connection c, String[] p) throws Exception {
        if (p.length < 3) {
            XmlFrameProtocol.writeErr(socket, "参数不足");
            return;
        }
        boolean ok = repo.dropCourse(c, p[1], p[2]);
        if (ok) {
            notifyIntegrationDrop(p[1], p[2]);
        }
        XmlFrameProtocol.writeOkXml(socket, ok ? "OK" : "FAIL");
    }

    private void notifyIntegrationDrop(String sno, String cno) {
        try (Socket s = new Socket("127.0.0.1", config.integrationPort())) {
            var w = new java.io.OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8);
            // 走 INTEGRATED_DROP：集成服务器从源院 + 开课院都删，满足作业要求 #4 跨院退选
            w.write("INTEGRATED_DROP|");
            w.write(sno);
            w.write("|");
            w.write(cno);
            w.write("\n");
            w.flush();
            BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
            br.readLine();
        } catch (IOException ignored) {
        }
    }

    private void handleStatsLocal(Socket socket, Connection c) throws Exception {
        int[] t = repo.localAdminStats(c);
        XmlFrameProtocol.writeOkXml(socket, t[0] + "|" + t[1] + "|" + t[2]);
    }

    /**
     * 跨院选课：CROSS_ENROLL|sno|cno|target  转发给集成服务器（端口 9200）。
     */
    private void handleCrossEnroll(Socket socket, String[] p) throws IOException {
        if (p.length < 4) {
            XmlFrameProtocol.writeErr(socket, "CROSS_ENROLL 参数不足，应为 CROSS_ENROLL|sno|cno|target");
            return;
        }
        String sno = p[1], cno = p[2], target = p[3];
        try (Socket s = new Socket("127.0.0.1", config.integrationPort())) {
            var w = new java.io.OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8);
            w.write("CROSS_ENROLL|" + sno + "|" + cno + "|" + target + "\n");
            w.flush();
            BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
            String body = XmlFrameProtocol.readOkXmlBody(br);
            XmlFrameProtocol.writeOkXml(socket, body);
        } catch (Exception e) {
            XmlFrameProtocol.writeErr(socket, "集成服务不可用: " + e.getMessage());
        }
    }

    private void handleIntegratedStats(Socket socket) throws IOException {
        try (Socket s = new Socket("127.0.0.1", config.integrationPort())) {
            var w = new java.io.OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8);
            w.write("STATS_ALL\n");
            w.flush();
            BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
            String body = XmlFrameProtocol.readOkXmlBody(br);
            XmlFrameProtocol.writeOkXml(socket, body);
        } catch (Exception e) {
            XmlFrameProtocol.writeErr(socket, "集成服务不可用: " + e.getMessage());
        }
    }
}
