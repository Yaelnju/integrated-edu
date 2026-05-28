package cn.nju.dataintegration.integration;

import org.junit.jupiter.api.Test;

import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class XsltMappingTest {
    private final XsltTransformer transformer = new XsltTransformer();

    private String transform(String xml, String resource) throws Exception {
        try (InputStream xsl = getClass().getClassLoader().getResourceAsStream(resource)) {
            if (xsl == null) {
                throw new IllegalStateException("missing test XSL: " + resource);
            }
            return transformer.transform(xml, new StreamSource(xsl));
        }
    }

    @Test
    void studentA_to_unified_mapsFields() throws Exception {
        String nativeXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<Students><student>"
                + "<StuID>A20240001</StuID><StuName>赵小明</StuName><Gender>M</Gender><Dept>计算机</Dept>"
                + "</student></Students>";

        String unified = transform(nativeXml, "xsl/integration/studentA_to_unified.xsl");

        assertTrue(unified.contains("<id>A20240001</id>"));
        assertTrue(unified.contains("<name>赵小明</name>"));
        assertTrue(unified.contains("<sex>M</sex>"));
        assertTrue(unified.contains("<major>计算机</major>"));
    }

    @Test
    void classA_to_unified_computesTimeFromCredit() throws Exception {
        String nativeXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<Courses><course>"
                + "<CourseID>AC01</CourseID><CourseName>数据结构</CourseName><Credit>3</Credit>"
                + "<Teacher>王老师</Teacher><IsShared>1</IsShared>"
                + "</course></Courses>";

        String unified = transform(nativeXml, "xsl/integration/classA_to_unified.xsl");

        assertTrue(unified.contains("<id>AC01</id>"));
        assertTrue(unified.contains("<time>48</time>"));
        assertTrue(unified.contains("<score>3</score>"));
        assertTrue(unified.contains("<share>1</share>"));
    }

    @Test
    void studentB_to_unified_mapsFields() throws Exception {
        String nativeXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<Students><student>"
                + "<STU_NO>B240000001</STU_NO><STU_NAME>李同学</STU_NAME><SEX>F</SEX><MAJOR>化学</MAJOR>"
                + "</student></Students>";

        String unified = transform(nativeXml, "xsl/integration/studentB_to_unified.xsl");

        assertTrue(unified.contains("<id>B240000001</id>"));
        assertTrue(unified.contains("<name>李同学</name>"));
        assertTrue(unified.contains("<sex>F</sex>"));
        assertTrue(unified.contains("<major>化学</major>"));
    }

    @Test
    void classC_roundtrip_preservesCourseId() throws Exception {
        String nativeXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<Courses><course>"
                + "<Cno>C001</Cno><Cnm>数据结构</Cnm><Ctm>48</Ctm><Cpt>4</Cpt>"
                + "<Tec>王老师</Tec><Pla>仙I-101</Pla><Share>1</Share>"
                + "</course></Courses>";

        String unified = transform(nativeXml, "xsl/integration/classC_to_unified.xsl");
        String back = transform(unified, "xsl/integration/unified_to_classC.xsl");

        assertTrue(unified.contains("<id>00000C001</id>"));
        assertTrue(back.contains("<Cno>C001</Cno>"));
    }
}
