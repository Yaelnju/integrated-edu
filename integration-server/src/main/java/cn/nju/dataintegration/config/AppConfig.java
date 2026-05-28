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

    public int integrationPort() {
        return Integer.parseInt(props.getProperty("integration.server.port", "9200"));
    }

    public String collegeAXmlHost() { return props.getProperty("college.a.xml.host", "127.0.0.1"); }
    public int    collegeAXmlPort() { return Integer.parseInt(props.getProperty("college.a.xml.port", "9102")); }
    public String collegeAGuiHost() { return props.getProperty("college.a.gui.host", "127.0.0.1"); }
    public int    collegeAGuiPort() { return Integer.parseInt(props.getProperty("college.a.gui.port", "9002")); }

    public String collegeBXmlHost() { return props.getProperty("college.b.xml.host", "127.0.0.1"); }
    public int    collegeBXmlPort() { return Integer.parseInt(props.getProperty("college.b.xml.port", "9101")); }
    public String collegeBGuiHost() { return props.getProperty("college.b.gui.host", "127.0.0.1"); }
    public int    collegeBGuiPort() { return Integer.parseInt(props.getProperty("college.b.gui.port", "9001")); }

    public String collegeCXmlHost() { return props.getProperty("college.c.xml.host", "127.0.0.1"); }
    public int    collegeCXmlPort() { return Integer.parseInt(props.getProperty("college.c.xml.port", "9100")); }
    public String collegeCGuiHost() { return props.getProperty("college.c.gui.host", "127.0.0.1"); }
    public int    collegeCGuiPort() { return Integer.parseInt(props.getProperty("college.c.gui.port", "9000")); }
}
