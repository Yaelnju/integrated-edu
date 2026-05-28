package cn.nju.dataintegration.integration;

import cn.nju.dataintegration.config.AppConfig;

public final class IntegrationApplication {
    public static void main(String[] args) throws Exception {
        AppConfig config = new AppConfig();
        IntegrationTcpServer server = new IntegrationTcpServer(config);
        server.run();
    }
}
