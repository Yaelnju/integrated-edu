package cn.nju.dataintegration.collegea.net;

import cn.nju.dataintegration.collegea.repo.CollegeARepository;
import cn.nju.dataintegration.collegea.xml.DomXmlExporter;
import cn.nju.dataintegration.config.AppConfig;
import cn.nju.dataintegration.xml.XmlSchemaValidator;
import cn.nju.dataintegration.db.Db;
import cn.nju.dataintegration.net.XmlFrameProtocol;

import javax.xml.transform.stream.StreamSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;

/**
 * 院系 A 的 XMLServer（默认端口 9102）。
 * 供集成服务器拉取/校验本院 XML，并接收跨院学生选课写回（ENROLL）。
 *
 * 命令：
 *   GET_STUDENTS / GET_COURSES / GET_CHOICES → OK + XMLBEGIN/XMLEND 帧
 *   ENROLL|sno|cno                          → OK 或 ERR
 */
public final class XmlTcpServer implements Runnable {
    private final AppConfig config;
    private final Db db;
    private final DomXmlExporter exporter = new DomXmlExporter();
    private final XmlSchemaValidator validator = new XmlSchemaValidator();
    private final CollegeARepository repo = new CollegeARepository();

    public XmlTcpServer(AppConfig config, Db db) {
        this.config = config;
        this.db = db;
    }

    @Override
    public void run() {
        int port = config.collegeXmlPort();
        try (ServerSocket ss = new ServerSocket(port)) {
            System.out.println("[XmlTcpServer] listening on " + port);
            while (!Thread.currentThread().isInterrupted()) {
                Socket s = ss.accept();
                new Thread(() -> handle(s), "xml-server").start();
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
            String cmd = br.readLine();
            if (cmd == null) {
                return;
            }
            try (Connection c = db.open()) {
                if ("GET_STUDENTS".equals(cmd)) {
                    serveXml(socket, exporter.exportStudents(c), "xsd/college-a/studentA.xsd");
                } else if ("GET_COURSES".equals(cmd)) {
                    serveXml(socket, exporter.exportCourses(c), "xsd/college-a/classA.xsd");
                } else if ("GET_CHOICES".equals(cmd)) {
                    serveXml(socket, exporter.exportChoices(c), "xsd/college-a/choiceA.xsd");
                } else if (cmd.startsWith("ENROLL|")) {
                    serveEnroll(socket, c, cmd);
                } else {
                    XmlFrameProtocol.writeErr(socket, "未知命令: " + cmd);
                }
            }
        } catch (Exception e) {
            try {
                XmlFrameProtocol.writeErr(socket, e.getMessage());
            } catch (IOException ignored) {
            }
        }
    }

    private void serveXml(Socket socket, String xml, String xsdResource) throws Exception {
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(xsdResource)) {
            if (in == null) {
                throw new IllegalStateException("缺少 XSD 资源: " + xsdResource);
            }
            validator.validate(xml, new StreamSource(in));
        }
        XmlFrameProtocol.writeOkXml(socket, xml);
    }

    private void serveEnroll(Socket socket, Connection c, String cmd) throws Exception {
        String[] parts = cmd.split("\\|", -1);
        if (parts.length != 3 || parts[1].isEmpty() || parts[2].isEmpty()) {
            XmlFrameProtocol.writeErr(socket, "ENROLL 参数错误，应为 ENROLL|sno|cno");
            return;
        }
        String sno = parts[1];
        String cno = parts[2];
        repo.ensureStudentForCross(c, sno);
        boolean ok = repo.pickCourse(c, sno, cno);
        if (ok) {
            writeOk(socket);
        } else {
            XmlFrameProtocol.writeErr(socket, "选课失败（已选过 / 超过 5 门 / 学生或课程不存在）");
        }
    }

    /** ENROLL 成功时只回单行 OK\n，不带 XML payload。 */
    private void writeOk(Socket socket) throws IOException {
        Writer w = new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8);
        w.write("OK\n");
        w.flush();
    }
}
