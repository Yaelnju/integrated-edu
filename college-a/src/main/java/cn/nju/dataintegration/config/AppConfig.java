package cn.nju.dataintegration.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

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
        return props.getProperty("db.user", "sa");
    }

    public String dbPassword() {
        return props.getProperty("db.password", "");
    }

    public int collegeGuiPort() {
        return Integer.parseInt(props.getProperty("college.a.gui.port", "9002"));
    }

    public int collegeXmlPort() {
        return Integer.parseInt(props.getProperty("college.a.xml.port", "9102"));
    }

    public int integrationPort() {
        return Integer.parseInt(props.getProperty("integration.server.port", "9200"));
    }

}
