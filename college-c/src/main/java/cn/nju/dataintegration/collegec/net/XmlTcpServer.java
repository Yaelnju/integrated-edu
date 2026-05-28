package cn.nju.dataintegration.collegec.net;

import cn.nju.dataintegration.collegec.xml.DomXmlExporter;
import cn.nju.dataintegration.collegec.xml.XmlSchemaValidator;
import cn.nju.dataintegration.config.AppConfig;
import cn.nju.dataintegration.db.Db;
import cn.nju.dataintegration.net.XmlFrameProtocol;

import javax.xml.transform.stream.StreamSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.Objects;

/**
 * 院系 C 的 XMLServer：供集成服务器拉取/校验本院 XML。
 */
public final class XmlTcpServer implements Runnable {
    private final AppConfig config;
    private final Db db;
    private final DomXmlExporter exporter = new DomXmlExporter();
    private final XmlSchemaValidator validator = new XmlSchemaValidator();

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
}
