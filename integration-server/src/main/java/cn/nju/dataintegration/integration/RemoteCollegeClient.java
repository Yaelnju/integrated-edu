package cn.nju.dataintegration.integration;

import cn.nju.dataintegration.net.XmlFrameProtocol;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/** 连接各学院 XML(91xx) 或 GUI(90xx) 端口的 TCP 客户端。 */
public final class RemoteCollegeClient {
    private final String host;
    private final int port;

    public RemoteCollegeClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public String fetchXml(String command) throws IOException {
        try (Socket s = new Socket(host, port)) {
            var w = new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8);
            w.write(command.endsWith("\n") ? command : command + "\n");
            w.flush();
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
            return XmlFrameProtocol.readOkXmlBody(br);
        }
    }

    public void enrollXmlWithName(String sno, String cno, String courseName) throws IOException {
        enrollXml(sno, cno, courseName);
    }

    public void enrollXml(String sno, String cno) throws IOException {
        enrollXml(sno, cno, null);
    }

    private void enrollXml(String sno, String cno, String courseName) throws IOException {
        try (Socket s = new Socket(host, port)) {
            var w = new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8);
            String cmd = (courseName != null && !courseName.isBlank())
                    ? "ENROLL|" + sno + "|" + cno + "|" + courseName + "\n"
                    : "ENROLL|" + sno + "|" + cno + "\n";
            w.write(cmd);
            w.flush();
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
            String line = br.readLine();
            if (line == null || line.startsWith("ERR|")) {
                throw new IOException(line == null ? "空响应" : line.substring(4));
            }
            if (!"OK".equals(line)) {
                throw new IOException("选课失败: " + line);
            }
        }
    }

    public void pickGui(String sno, String cno) throws IOException {
        try (Socket s = new Socket(host, port)) {
            var w = new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8);
            w.write("PICK|" + sno + "|" + cno + "\n");
            w.flush();
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
            String body = XmlFrameProtocol.readOkXmlBody(br);
            if (!body.contains("OK")) {
                throw new IOException("本院选课失败: " + body);
            }
        }
    }

    public void dropGui(String sno, String cno) throws IOException {
        try (Socket s = new Socket(host, port)) {
            var w = new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8);
            w.write("DROP|" + sno + "|" + cno + "\n");
            w.flush();
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
            XmlFrameProtocol.readOkXmlBody(br);
        }
    }
}
