package cn.nju.dataintegration.integration;

import cn.nju.dataintegration.config.AppConfig;
import cn.nju.dataintegration.integration.validator.XmlValidator;
import org.dom4j.Document;
import org.dom4j.io.SAXReader;

import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;
import java.io.StringReader;

public final class StatsAggregator {
    private final AppConfig config;
    private final XsltTransformer xslt = new XsltTransformer();
    private final XmlValidator validator = new XmlValidator();
    private final SAXReader reader = new SAXReader();

    public StatsAggregator(AppConfig config) {
        this.config = config;
    }

    public String buildAllCollegesReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("===== 集成教务统计（学生 / 课程 / 选课条数）=====\n");
        int ts = 0, tc = 0, tch = 0;
        for (String col : new String[]{"A", "B", "C"}) {
            try {
                Counts c = fetchCounts(col);
                sb.append("院系 ").append(col).append(": 学生=").append(c.students)
                        .append(", 课程=").append(c.courses)
                        .append(", 选课=").append(c.choices).append('\n');
                ts += c.students;
                tc += c.courses;
                tch += c.choices;
            } catch (Exception e) {
                sb.append("院系 ").append(col).append(": 不可用 (").append(e.getMessage()).append(")\n");
            }
        }
        sb.append("---- 合计（在线可达学院）----\n");
        sb.append("学生总数: ").append(ts).append('\n');
        sb.append("课程总数: ").append(tc).append('\n');
        sb.append("选课记录总数: ").append(tch).append('\n');
        return sb.toString();
    }

    private Counts fetchCounts(String college) throws Exception {
        RemoteCollegeClient client = clientFor(college);
        Document st = reader.read(new StringReader(fetchUnifiedXml(client, college, "GET_STUDENTS")));
        Document co = reader.read(new StringReader(fetchUnifiedXml(client, college, "GET_COURSES")));
        Document ch = reader.read(new StringReader(fetchUnifiedXml(client, college, "GET_CHOICES")));
        int students = st.getRootElement().elements().size();
        int courses = co.getRootElement().elements().size();
        int choices = ch.getRootElement().elements().size();
        return new Counts(students, courses, choices);
    }

    private String fetchUnifiedXml(RemoteCollegeClient client, String college, String command) throws Exception {
        String nativeXml = client.fetchXml(command);
        String xsl = xslPathFor(college, command);
        try (InputStream xslIn = resource(xsl)) {
            String unified = xslt.transform(nativeXml, new StreamSource(xslIn));
            validateUnified(command, unified);
            return unified;
        }
    }

    private void validateUnified(String command, String xml) throws Exception {
        if ("GET_STUDENTS".equals(command)) {
            validator.validateUnifiedStudents(xml);
        } else if ("GET_COURSES".equals(command)) {
            validator.validateUnifiedClasses(xml);
        } else if ("GET_CHOICES".equals(command)) {
            validator.validateUnifiedChoices(xml);
        }
    }

    private String xslPathFor(String college, String command) {
        String suffix;
        if ("GET_STUDENTS".equals(command)) {
            suffix = "student";
        } else if ("GET_COURSES".equals(command)) {
            suffix = "class";
        } else if ("GET_CHOICES".equals(command)) {
            suffix = "choice";
        } else {
            throw new IllegalArgumentException("unknown command: " + command);
        }
        if ("A".equals(college)) {
            return "xsl/integration/" + suffix + "A_to_unified.xsl";
        }
        if ("B".equals(college)) {
            return "xsl/integration/" + suffix + "B_to_unified.xsl";
        }
        if ("C".equals(college)) {
            return "xsl/integration/" + suffix + "C_to_unified.xsl";
        }
        throw new IllegalArgumentException("unknown college: " + college);
    }

    private InputStream resource(String path) {
        InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
        if (in == null) {
            throw new IllegalStateException("missing resource: " + path);
        }
        return in;
    }

    private RemoteCollegeClient clientFor(String college) {
        if ("A".equals(college)) {
            return new RemoteCollegeClient("127.0.0.1", config.collegeXmlPort());
        }
        if ("B".equals(college)) {
            return new RemoteCollegeClient(config.collegeBXmlHost(), config.collegeBXmlPort());
        }
        if ("C".equals(college)) {
            return new RemoteCollegeClient(config.collegeCXmlHost(), config.collegeCXmlPort());
        }
        throw new IllegalArgumentException(college);
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
