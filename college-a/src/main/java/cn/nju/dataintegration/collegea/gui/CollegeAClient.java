package cn.nju.dataintegration.collegea.gui;

import cn.nju.dataintegration.config.AppConfig;
import cn.nju.dataintegration.net.XmlFrameProtocol;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Swing GUI 用的轻量 TCP 客户端：每次 call() 建一条新连接、发一行命令、读 OK+body。
 */
public final class CollegeAClient {
    private final String host;
    private final int port;

    public CollegeAClient(AppConfig config) {
        this("127.0.0.1", config.collegeGuiPort());
    }

    public CollegeAClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public String call(String commandLine) throws IOException {
        try (Socket s = new Socket(host, port)) {
            var w = new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8);
            w.write(commandLine);
            if (!commandLine.endsWith("\n")) {
                w.write('\n');
            }
            w.flush();
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
            return XmlFrameProtocol.readOkXmlBody(br);
        }
    }
}
