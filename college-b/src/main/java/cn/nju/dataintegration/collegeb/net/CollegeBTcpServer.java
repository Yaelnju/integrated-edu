package cn.nju.dataintegration.collegeb.net;

import cn.nju.dataintegration.collegeb.repo.CollegeBRepository;
import cn.nju.dataintegration.config.AppConfig;
import cn.nju.dataintegration.db.Db;
import cn.nju.dataintegration.net.XmlFrameProtocol;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.StringJoiner;

/**
 * 院系 B 业务服务器（默认端口 9001）。
 * 面向 B 自己的 Swing GUI；命令清单见 CLAUDE.md：
 *   LOGIN / LIST_COURSES / MY_SC / PICK / DROP / STATS_LOCAL / INTEGRATED_STATS / CROSS_ENROLL
 *
 * INTEGRATED_STATS / CROSS_ENROLL 转发给集成服务器（9200）。集成服务器未启动时回 ERR，
 * GUI 侧负责把消息呈现给用户。
 */
public final class CollegeBTcpServer implements Runnable {
    private final AppConfig config;
    private final Db db;
    private final CollegeBRepository repo = new CollegeBRepository();

    public CollegeBTcpServer(AppConfig config, Db db) {
        this.config = config;
        this.db = db;
    }

    @Override
    public void run() {
        int port = config.collegeGuiPort();
        try (ServerSocket ss = new ServerSocket(port)) {
            System.out.println("[CollegeBTcpServer] listening on " + port);
            while (!Thread.currentThread().isInterrupted()) {
                Socket s = ss.accept();
                new Thread(() -> handle(s), "college-b-client").start();
            }
        } catch (IOException e) {
            if (!Thread.currentThread().isInterrupted()) {
                e.printStackTrace();
            }
        }
    }

    private void handle(Socket socket) {
        try (socket) {
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            String line = br.readLine();
            if (line == null) {
                return;
            }
            String[] p = line.split("\\|", -1);
            String cmd = p[0];
            try (Connection c = db.open()) {
                switch (cmd) {
                    case "LOGIN":            handleLogin(socket, c, p); break;
                    case "LIST_COURSES":     handleListCourses(socket, c); break;
                    case "MY_SC":            handleMySc(socket, c, p); break;
                    case "PICK":             handlePick(socket, c, p); break;
                    case "DROP":             handleDrop(socket, c, p); break;
                    case "STATS_LOCAL":      handleStatsLocal(socket, c); break;
                    case "INTEGRATED_STATS": handleIntegratedStats(socket); break;
                    case "CROSS_ENROLL":     handleCrossEnroll(socket, p); break;
                    default:                 XmlFrameProtocol.writeErr(socket, "未知命令: " + cmd);
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
        var rows = repo.mySelections(c, p[1]);
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
            var w = new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8);
            // 走 INTEGRATED_DROP：集成服务器从源院 + 开课院都删，满足作业要求 #4 跨院退选
            w.write("INTEGRATED_DROP|" + sno + "|" + cno + "\n");
            w.flush();
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
            br.readLine();
        } catch (IOException ignored) {
            // 集成服务器不可用时静默：本地退课已生效
        }
    }

    private void handleStatsLocal(Socket socket, Connection c) throws Exception {
        int[] t = repo.localAdminStats(c);
        XmlFrameProtocol.writeOkXml(socket, t[0] + "|" + t[1] + "|" + t[2]);
    }

    private void handleIntegratedStats(Socket socket) throws IOException {
        try (Socket s = new Socket("127.0.0.1", config.integrationPort())) {
            var w = new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8);
            w.write("STATS_ALL\n");
            w.flush();
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
            String body = XmlFrameProtocol.readOkXmlBody(br);
            XmlFrameProtocol.writeOkXml(socket, body);
        } catch (Exception e) {
            XmlFrameProtocol.writeErr(socket, "集成服务不可用: " + e.getMessage());
        }
    }

    /**
     * 跨院选课：CROSS_ENROLL|sno|cno|target  转发给集成服务器。
     * 集成服务器协议（约定，待 Student 1 实现）：同样命令格式，返回 OK + body 帧。
     */
    private void handleCrossEnroll(Socket socket, String[] p) throws IOException {
        if (p.length < 4) {
            XmlFrameProtocol.writeErr(socket, "CROSS_ENROLL 参数不足，应为 CROSS_ENROLL|sno|cno|target");
            return;
        }
        String sno = p[1], cno = p[2], target = p[3];
        try (Socket s = new Socket("127.0.0.1", config.integrationPort())) {
            var w = new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8);
            w.write("CROSS_ENROLL|" + sno + "|" + cno + "|" + target + "\n");
            w.flush();
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
            String body = XmlFrameProtocol.readOkXmlBody(br);
            XmlFrameProtocol.writeOkXml(socket, body);
        } catch (Exception e) {
            XmlFrameProtocol.writeErr(socket, "集成服务不可用: " + e.getMessage());
        }
    }
}
