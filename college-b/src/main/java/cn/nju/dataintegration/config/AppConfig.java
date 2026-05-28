package cn.nju.dataintegration.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * 从 classpath 下的 application.properties 加载配置。
 * 集中管理 JDBC 连接信息、端口、外部资源路径等。
 */
public final class AppConfig {
    private final Properties props = new Properties();

    public AppConfig() {
        try (InputStream in = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("application.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException e) {
            throw new IllegalStateException("failed to load application.properties", e);
        }
    }

    public String dbUrl() {
        return props.getProperty("db.url");
    }

    public String dbUser() {
        return props.getProperty("db.user", "collegeb");
    }

    public String dbPassword() {
        return props.getProperty("db.password", "");
    }

    public int collegeGuiPort() {
        return Integer.parseInt(props.getProperty("college.b.gui.port", "9001"));
    }

    public int collegeXmlPort() {
        return Integer.parseInt(props.getProperty("college.b.xml.port", "9101"));
    }

    public int integrationPort() {
        return Integer.parseInt(props.getProperty("integration.server.port", "9200"));
    }

    public Path projectRelativeOrNull(String key) {
        String p = props.getProperty(key);
        if (p == null || p.isBlank()) {
            return null;
        }
        return Paths.get(p);
    }
}
