package cn.nju.dataintegration.integration;

import cn.nju.dataintegration.config.AppConfig;

public final class IntegrationApplication {
    public static void main(String[] args) throws InterruptedException {
        AppConfig config = new AppConfig();
        new Thread(new IntegrationTcpServer(config), "integration-server").start();
        System.out.println("集成服务器已监听端口 " + config.integrationPort() + "，Ctrl+C 结束。");
        Thread.sleep(Long.MAX_VALUE);
    }
}
