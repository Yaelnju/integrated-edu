package cn.nju.dataintegration.collegec.net;

import cn.nju.dataintegration.collegec.repo.CollegeCRepository;
import cn.nju.dataintegration.collegec.xml.DomXmlExporter;
import cn.nju.dataintegration.xml.XmlSchemaValidator;
import cn.nju.dataintegration.config.AppConfig;
import cn.nju.dataintegration.db.Db;
import cn.nju.dataintegration.net.XmlFrameProtocol;

import javax.xml.transform.stream.StreamSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.Objects;

/**
 * 院系 C 的 XMLServer：供集成服务器拉取/校验本院 XML，并接收跨院选课写回。
 */
public final class XmlTcpServer implements Runnable {
    private final AppConfig config;
    private final Db db;
    private final DomXmlExporter exporter = new DomXmlExporter();
    private final XmlSchemaValidator validator = new XmlSchemaValidator();
    private final CollegeCRepository repo = new CollegeCRepository();

    public XmlTcpServer(AppConfig config, Db db) {
        this.config = config;
        this.db = db;
    }

    @Override
    public void run() {
        try (ServerSocket ss = new ServerSocket(config.collegeXmlPort())) {
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
            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            String cmd = br.readLine();
            if (cmd == null) {
                return;
            }
            try (Connection c = db.open()) {
                String xml;
                if ("GET_STUDENTS".equals(cmd)) {
                    xml = exporter.exportStudents(c);
                } else if ("GET_COURSES".equals(cmd)) {
                    xml = exporter.exportCourses(c);
                } else if ("GET_CHOICES".equals(cmd)) {
                    xml = exporter.exportChoices(c);
                } else if (cmd.startsWith("ENROLL|")) {
                    serveEnroll(socket, c, cmd);
                    return;
                } else {
                    xml = null;
                }
                if (xml == null) {
                    XmlFrameProtocol.writeErr(socket, "未知命令");
                    return;
                }
                validate(cmd, xml);
                XmlFrameProtocol.writeOkXml(socket, xml);
            }
        } catch (Exception e) {
            try {
                XmlFrameProtocol.writeErr(socket, e.getMessage());
            } catch (IOException ignored) {
            }
        }
    }

    private void validate(String cmd, String xml) throws Exception {
        String resource;
        if ("GET_STUDENTS".equals(cmd)) {
            resource = "xsd/college-c/studentC.xsd";
        } else if ("GET_COURSES".equals(cmd)) {
            resource = "xsd/college-c/classC.xsd";
        } else if ("GET_CHOICES".equals(cmd)) {
            resource = "xsd/college-c/choiceC.xsd";
        } else {
            resource = null;
        }
        Objects.requireNonNull(resource);
        try (java.io.InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("缺少资源: " + resource);
            }
            validator.validate(xml, new StreamSource(in));
        }
    }

    private void serveEnroll(Socket socket, Connection c, String cmd) throws Exception {
        String[] parts = cmd.split("\\|", -1);
        if (parts.length != 3 || parts[1].isEmpty() || parts[2].isEmpty()) {
            XmlFrameProtocol.writeErr(socket, "ENROLL 参数错误，应为 ENROLL|sno|cno");
            return;
        }
        String sno = parts[1];
        String cno = parts[2];
        String courseName = parts.length > 3 ? parts[3] : null;
        repo.ensureStudentForCross(c, sno);
        repo.ensureCourseForCross(c, cno, courseName);
        boolean ok = repo.pickCourse(c, sno, cno);
        if (ok) {
            writeOk(socket);
        } else {
            XmlFrameProtocol.writeErr(socket, "选课失败（已选过 / 超过 5 门 / 学生或课程不存在）");
        }
    }

    private void writeOk(Socket socket) throws IOException {
        Writer w = new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8);
        w.write("OK\n");
        w.flush();
    }
}
