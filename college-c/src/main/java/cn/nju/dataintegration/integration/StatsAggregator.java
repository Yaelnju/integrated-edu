package cn.nju.dataintegration.integration;

import org.dom4j.Document;
import org.dom4j.io.SAXReader;

import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicInteger;

public final class StatsAggregator {
    private final XsltTransformer xslt = new XsltTransformer();

    public String buildAllCollegesReport(String jdbcUrl, String user, String pwd) throws Exception {
        int cStu = 0;
        int cCourse = 0;
        int cChoice = 0;
        try (Connection c = DriverManager.getConnection(jdbcUrl, user, pwd);
             Statement st = c.createStatement()) {
            try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM student")) {
                rs.next();
                cStu = rs.getInt(1);
            }
            try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM course")) {
                rs.next();
                cCourse = rs.getInt(1);
            }
            try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM sc")) {
                rs.next();
                cChoice = rs.getInt(1);
            }
        }
        Counts a = readExportCounts("integration-samples/college_a_export.xml");
        Counts b = readExportCounts("integration-samples/college_b_export.xml");
        StringBuilder sb = new StringBuilder();
        sb.append("===== 集成教务统计（学生 / 课程 / 选课条数）=====\n");
        sb.append("院系 C（MySQL 在线库）: 学生=").append(cStu)
                .append(", 课程=").append(cCourse)
                .append(", 选课=").append(cChoice).append('\n');
        sb.append("院系 A（示例统一 XML）: 学生=").append(a.students)
                .append(", 课程=").append(a.courses)
                .append(", 选课=").append(a.choices).append('\n');
        sb.append("院系 B（示例统一 XML）: 学生=").append(b.students)
                .append(", 课程=").append(b.courses)
                .append(", 选课=").append(b.choices).append('\n');
        int ts = cStu + a.students + b.students;
        int tc = cCourse + a.courses + b.courses;
        int tch = cChoice + a.choices + b.choices;
        sb.append("---- 合计 ----\n");
        sb.append("学生总数: ").append(ts).append('\n');
        sb.append("课程总数: ").append(tc).append('\n');
        sb.append("选课记录总数: ").append(tch).append('\n');
        sb.append("\n说明: A/B 当前使用 resources 下示例导出文件；联调时可替换为同学1/2真实导出。\n");
        return sb.toString();
    }

    public String demoXslStudentRoundTrip(String jdbcUrl, String user, String pwd) throws Exception {
        String nativeXml;
        try (Connection c = DriverManager.getConnection(jdbcUrl, user, pwd);
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT Sno,Snm,Sex,Sde,Pwd FROM student ORDER BY Sno LIMIT 3")) {
            StringBuilder sb = new StringBuilder();
            sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<Students>\n");
            while (rs.next()) {
                sb.append("<student><Sno>").append(rs.getString(1)).append("</Sno>");
                sb.append("<Snm>").append(rs.getString(2)).append("</Snm>");
                sb.append("<Sex>").append(rs.getString(3)).append("</Sex>");
                sb.append("<Sde>").append(rs.getString(4)).append("</Sde>");
                sb.append("<Pwd>").append(rs.getString(5)).append("</Pwd></student>\n");
            }
            sb.append("</Students>");
            nativeXml = sb.toString();
        }
        try (InputStream x1 = resource("xsl/integration/studentC_to_unified.xsl");
             InputStream x2 = resource("xsl/integration/unified_to_studentC.xsl")) {
            String unified = xslt.transform(nativeXml, new StreamSource(x1));
            String back = xslt.transform(unified, new StreamSource(x2));
            return "----- XSL 学生 C→统一→C 演示 -----\n" + back;
        }
    }

    private static InputStream resource(String path) {
        InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
        if (in == null) {
            throw new IllegalStateException("缺少资源: " + path);
        }
        return in;
    }

    private Counts readExportCounts(String classpathResource) throws Exception {
        SAXReader reader = new SAXReader();
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(classpathResource)) {
            if (in == null) {
                return new Counts(0, 0, 0);
            }
            Document doc = reader.read(in);
            AtomicInteger st = new AtomicInteger();
            AtomicInteger cl = new AtomicInteger();
            AtomicInteger ch = new AtomicInteger();
            doc.selectNodes("//Students/student").forEach(n -> st.incrementAndGet());
            doc.selectNodes("//Classes/class").forEach(n -> cl.incrementAndGet());
            doc.selectNodes("//Choices/choice").forEach(n -> ch.incrementAndGet());
            return new Counts(st.get(), cl.get(), ch.get());
        }
    }

    private static final class Counts {
        final int students;
        final int courses;
        final int choices;

        Counts(int students, int courses, int choices) {
            this.students = students;
            this.courses = courses;
            this.choices = choices;
        }
    }
}
