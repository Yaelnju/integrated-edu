package cn.nju.dataintegration.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public final class AppConfig {
    private final Properties props = new Properties();

    public AppConfig() {
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("application.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public String dbUrl() {
        return props.getProperty("db.url");
    }

    public String dbUser() {
        return props.getProperty("db.user", "root");
    }

    public String dbPassword() {
        return props.getProperty("db.password", "");
    }

    public int collegeGuiPort() {
        return Integer.parseInt(props.getProperty("college.c.gui.port", "9000"));
    }

    public int collegeXmlPort() {
        return Integer.parseInt(props.getProperty("college.c.xml.port", "9100"));
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
