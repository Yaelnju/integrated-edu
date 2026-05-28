package cn.nju.dataintegration.integration;

import cn.nju.dataintegration.config.AppConfig;
import cn.nju.dataintegration.net.XmlFrameProtocol;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public final class IntegrationTcpServer implements Runnable {
    private final AppConfig config;
    private final StatsAggregator stats;
    private final CrossEnrollService cross;

    public IntegrationTcpServer(AppConfig config) {
        this.config = config;
        this.stats = new StatsAggregator(config);
        this.cross = new CrossEnrollService(config);
    }

    @Override
    public void run() {
        try (ServerSocket ss = new ServerSocket(config.integrationPort())) {
            System.out.println("[IntegrationTcpServer] listening on " + config.integrationPort());
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
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            String line = br.readLine();
            if (line == null) {
                return;
            }
            if (line.startsWith("STATS_ALL")) {
                XmlFrameProtocol.writeOkXml(socket, stats.buildAllCollegesReport());
            } else if (line.startsWith("CROSS_ENROLL|")) {
                String[] p = line.split("\\|", -1);
                if (p.length < 4) {
                    XmlFrameProtocol.writeErr(socket, "CROSS_ENROLL|sno|cno|target");
                    return;
                }
                String msg = cross.crossEnroll(p[1], p[2], p[3]);
                XmlFrameProtocol.writeOkXml(socket, msg);
            } else if (line.startsWith("INTEGRATED_DROP|")) {
                String[] p = line.split("\\|", -1);
                if (p.length < 3) {
                    XmlFrameProtocol.writeErr(socket, "INTEGRATED_DROP|sno|cno");
                    return;
                }
                XmlFrameProtocol.writeOkXml(socket, cross.integratedDrop(p[1], p[2]));
            } else {
                XmlFrameProtocol.writeErr(socket, "未知命令: " + line);
            }
        } catch (Exception e) {
            try {
                XmlFrameProtocol.writeErr(socket, e.getMessage());
            } catch (IOException ignored) {
            }
        }
    }

}
