package cn.nju.dataintegration.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public final class XmlFrameProtocol {
    public static final String BEGIN = "<XMLBEGIN>\n";
    public static final String END = "\n<XMLEND>\n";

    private XmlFrameProtocol() {
    }

    public static void writeOkXml(Socket socket, String xml) throws IOException {
        Writer w = new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8);
        w.write("OK\n");
        w.write(BEGIN);
        w.write(xml);
        w.write(END);
        w.flush();
    }

    public static void writeErr(Socket socket, String msg) throws IOException {
        Writer w = new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8);
        w.write("ERR|");
        w.write(msg == null ? "" : msg.replace('\n', ' '));
        w.write("\n");
        w.flush();
    }

    public static String readLineCommand(Socket socket) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        return br.readLine();
    }

    public static String readOkXmlBody(BufferedReader br) throws IOException {
        String line = br.readLine();
        if (line == null) {
            throw new IOException("空响应");
        }
        if (line.startsWith("ERR|")) {
            throw new IOException(line.substring(4));
        }
        if (!"OK".equals(line)) {
            throw new IOException("unexpected: " + line);
        }
        line = br.readLine();
        if (!"<XMLBEGIN>".equals(line)) {
            throw new IOException("expected XMLBEGIN, got: " + line);
        }
        StringBuilder sb = new StringBuilder();
        while (true) {
            line = br.readLine();
            if (line == null) {
                break;
            }
            if ("<XMLEND>".equals(line)) {
                break;
            }
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(line);
        }
        return sb.toString();
    }
}
