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
        String sys = System.getProperty("db.url");
        if (sys != null && !sys.isBlank()) {
            return sys;
        }
        return props.getProperty("db.url");
    }

    public String dbUser() {
        String sys = System.getProperty("db.user");
        if (sys != null) {
            return sys;
        }
        return props.getProperty("db.user", "sa");
    }

    public String dbPassword() {
        String sys = System.getProperty("db.password");
        if (sys != null) {
            return sys;
        }
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

    public String collegeBXmlHost() {
        return props.getProperty("college.b.xml.host", "127.0.0.1");
    }

    public int collegeBXmlPort() {
        return Integer.parseInt(props.getProperty("college.b.xml.port", "9101"));
    }

    public String collegeCXmlHost() {
        return props.getProperty("college.c.xml.host", "127.0.0.1");
    }

    public int collegeCXmlPort() {
        return Integer.parseInt(props.getProperty("college.c.xml.port", "9100"));
    }

    public int collegeBGuiPort() {
        return Integer.parseInt(props.getProperty("college.b.gui.port", "9001"));
    }

    public int collegeCGuiPort() {
        return Integer.parseInt(props.getProperty("college.c.gui.port", "9000"));
    }
}
