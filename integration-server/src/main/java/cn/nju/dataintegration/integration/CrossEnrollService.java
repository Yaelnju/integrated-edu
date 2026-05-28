package cn.nju.dataintegration.integration;

import cn.nju.dataintegration.config.AppConfig;
import cn.nju.dataintegration.integration.validator.XmlValidator;
import org.dom4j.Document;
import org.dom4j.io.SAXReader;

import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.Locale;

public final class CrossEnrollService {
    private final AppConfig config;
    private final XsltTransformer xslt = new XsltTransformer();
    private final XmlValidator validator = new XmlValidator();
    private final SAXReader reader = new SAXReader();

    public CrossEnrollService(AppConfig config) {
        this.config = config;
    }

    public String crossEnroll(String sno, String cno, String targetCollege) {
        try {
            String source = detectStudentCollege(sno);
            String target = targetCollege.trim().toUpperCase(Locale.ROOT);
            if (!isSharedOnCollege(target, cno)) {
                return "目标学院 " + target + " 不存在共享课程 " + cno;
            }
            enrollOnCollege(source, sno, cno);
            enrollOnCollege(target, sno, cno);
            return "跨院选课成功：学生 " + sno + " 已选 " + target + " 院课程 " + cno
                    + "（源院 " + source + " 与目标院均已记录）";
        } catch (Exception e) {
            return "跨院选课失败: " + e.getMessage();
        }
    }

    public String integratedDrop(String sno, String cno) {
        try {
            String source = detectStudentCollege(sno);
            String owner = detectCourseCollege(cno);
            dropOnCollege(source, sno, cno);
            if (!owner.equals(source)) {
                dropOnCollege(owner, sno, cno);
            }
            return "退选成功：已从 " + source + (owner.equals(source) ? "" : " 与 " + owner) + " 删除记录";
        } catch (Exception e) {
            return "退选失败: " + e.getMessage();
        }
    }

    private boolean isSharedOnCollege(String college, String cno) throws Exception {
        String xml = fetchUnifiedCoursesXml(college);
        String targetId = normalizeCourseIdForUnified(college, cno);
        Document doc = reader.read(new StringReader(xml));
        for (Object n : doc.selectNodes("//Classes/class")) {
            var el = (org.dom4j.Element) n;
            String shared = el.elementText("share");
            if (targetId.equals(el.elementText("id")) && isSharedFlag(shared)) {
                return true;
            }
        }
        return false;
    }

    private String fetchUnifiedCoursesXml(String college) throws Exception {
        String nativeXml = fetchCoursesXml(college);
        String xsl = xslPathForCourses(college);
        try (InputStream xslIn = resource(xsl)) {
            String unified = xslt.transform(nativeXml, new StreamSource(xslIn));
            validator.validateUnifiedClasses(unified);
            return unified;
        }
    }

    private String normalizeCourseIdForUnified(String college, String cno) {
        String raw = cno == null ? "" : cno.trim();
        if ("B".equals(college) && raw.length() == 5) {
            return "0000" + raw;
        }
        if ("C".equals(college) && raw.length() == 4) {
            return "00000" + raw;
        }
        return raw;
    }

    private boolean isSharedFlag(String shared) {
        return "1".equals(shared) || "true".equalsIgnoreCase(shared);
    }

    private String xslPathForCourses(String college) {
        if ("A".equals(college)) {
            return "xsl/integration/classA_to_unified.xsl";
        }
        if ("B".equals(college)) {
            return "xsl/integration/classB_to_unified.xsl";
        }
        if ("C".equals(college)) {
            return "xsl/integration/classC_to_unified.xsl";
        }
        throw new IllegalArgumentException("未知学院: " + college);
    }

    private InputStream resource(String path) {
        InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
        if (in == null) {
            throw new IllegalStateException("missing resource: " + path);
        }
        return in;
    }

    private String fetchCoursesXml(String college) throws Exception {
        if ("A".equals(college)) {
            return new RemoteCollegeClient("127.0.0.1", config.collegeXmlPort()).fetchXml("GET_COURSES");
        }
        if ("B".equals(college)) {
            return new RemoteCollegeClient(config.collegeBXmlHost(), config.collegeBXmlPort()).fetchXml("GET_COURSES");
        }
        if ("C".equals(college)) {
            return new RemoteCollegeClient(config.collegeCXmlHost(), config.collegeCXmlPort()).fetchXml("GET_COURSES");
        }
        throw new IllegalArgumentException("未知学院: " + college);
    }

    private void enrollOnCollege(String college, String sno, String cno) throws Exception {
        if ("A".equals(college)) {
            new RemoteCollegeClient("127.0.0.1", config.collegeXmlPort()).enrollXml(sno, cno);
        } else if ("B".equals(college)) {
            new RemoteCollegeClient(config.collegeBXmlHost(), config.collegeBXmlPort()).enrollXml(sno, cno);
        } else if ("C".equals(college)) {
            new RemoteCollegeClient(config.collegeCXmlHost(), config.collegeCGuiPort()).pickGui(sno, cno);
        } else {
            throw new IllegalArgumentException("未知学院: " + college);
        }
    }

    private void dropOnCollege(String college, String sno, String cno) {
        try {
            if ("A".equals(college)) {
                new RemoteCollegeClient("127.0.0.1", config.collegeGuiPort()).dropGui(sno, cno);
            } else if ("B".equals(college)) {
                new RemoteCollegeClient(config.collegeBXmlHost(), config.collegeBGuiPort()).dropGui(sno, cno);
            } else if ("C".equals(college)) {
                new RemoteCollegeClient(config.collegeCXmlHost(), config.collegeCGuiPort()).dropGui(sno, cno);
            }
        } catch (IOException ignored) {
            // 可能已由本院 GUI 删除
        }
    }

    static String detectStudentCollege(String sno) {
        if (sno.startsWith("A")) {
            return "A";
        }
        if (sno.startsWith("B")) {
            return "B";
        }
        if (sno.startsWith("C")) {
            return "C";
        }
        throw new IllegalArgumentException("无法识别学号所属学院: " + sno);
    }

    static String detectCourseCollege(String cno) {
        if (cno.startsWith("AC") || cno.startsWith("A2024") || (cno.length() == 9 && cno.contains("A"))) {
            return "A";
        }
        if (cno.length() == 5 && cno.charAt(0) == 'B') {
            return "B";
        }
        if (cno.length() == 4) {
            return "C";
        }
        throw new IllegalArgumentException("无法识别课程所属学院: " + cno);
    }
}
