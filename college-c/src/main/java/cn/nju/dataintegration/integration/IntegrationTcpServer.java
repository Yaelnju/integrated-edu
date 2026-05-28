package cn.nju.dataintegration.integration;

import cn.nju.dataintegration.config.AppConfig;
import cn.nju.dataintegration.net.XmlFrameProtocol;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

public final class IntegrationTcpServer implements Runnable {
    private final AppConfig config;
    private final StatsAggregator stats = new StatsAggregator();

    public IntegrationTcpServer(AppConfig config) {
        this.config = config;
    }

    @Override
    public void run() {
        try (ServerSocket ss = new ServerSocket(config.integrationPort())) {
            while (!Thread.currentThread().isInterrupted()) {
                Socket s = ss.accept();
                new Thread(() -> handle(s), "integration").start();
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
            if (line.startsWith("STATS_ALL")) {
                String report = stats.buildAllCollegesReport(config.dbUrl(), config.dbUser(), config.dbPassword());
                XmlFrameProtocol.writeOkXml(socket, report);
            } else if (line.startsWith("DEMO_XSL_STUDENT")) {
                String report = stats.demoXslStudentRoundTrip(config.dbUrl(), config.dbUser(), config.dbPassword());
                XmlFrameProtocol.writeOkXml(socket, report);
            } else if (line.startsWith("RECORD_DROP|")) {
                appendDropLog(line);
                try (var w = new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8)) {
                    w.write("OK\n");
                    w.flush();
                }
            } else {
                XmlFrameProtocol.writeErr(socket, "未知命令");
            }
        } catch (Exception e) {
            try {
                XmlFrameProtocol.writeErr(socket, e.getMessage());
            } catch (IOException ignored) {
            }
        }
    }

    private void appendDropLog(String line) throws IOException {
        Path p = Path.of(System.getProperty("java.io.tmpdir"), "integration-drop-audit.log");
        String row = Instant.now() + " " + line + System.lineSeparator();
        Files.writeString(p, row, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }
}
