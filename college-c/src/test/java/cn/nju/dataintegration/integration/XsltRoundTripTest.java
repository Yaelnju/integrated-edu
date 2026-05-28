package cn.nju.dataintegration.integration;

import org.junit.jupiter.api.Test;

import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class XsltRoundTripTest {
    @Test
    void studentC_to_unified_contains_id() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<Students>\n"
                + "  <student>\n"
                + "    <Sno>C20240001</Sno>\n"
                + "    <Snm>学生1</Snm>\n"
                + "    <Sex>M</Sex>\n"
                + "    <Sde>CS</Sde>\n"
                + "  </student>\n"
                + "</Students>\n";
        XsltTransformer t = new XsltTransformer();
        try (InputStream xsl = getClass().getClassLoader().getResourceAsStream("xsl/integration/studentC_to_unified.xsl")) {
            String out = t.transform(xml, new StreamSource(xsl));
            assertTrue(out.contains("<id>C20240001</id>"));
            assertTrue(out.contains("<name>学生1</name>"));
        }
    }

    @Test
    void class_roundtrip_basic() throws Exception {
        String nativeXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<Courses>\n"
                + "  <course>\n"
                + "    <Cno>C001</Cno>\n"
                + "    <Cnm>数据结构</Cnm>\n"
                + "    <Ctm>48</Ctm>\n"
                + "    <Cpt>4</Cpt>\n"
                + "    <Tec>王老师</Tec>\n"
                + "    <Pla>仙I-101</Pla>\n"
                + "    <Share>1</Share>\n"
                + "  </course>\n"
                + "</Courses>\n";
        XsltTransformer t = new XsltTransformer();
        try (InputStream x1 = getClass().getClassLoader().getResourceAsStream("xsl/integration/classC_to_unified.xsl");
             InputStream x2 = getClass().getClassLoader().getResourceAsStream("xsl/integration/unified_to_classC.xsl")) {
            String unified = t.transform(nativeXml, new StreamSource(x1));
            assertTrue(unified.contains("<id>00000C001</id>"));
            String back = t.transform(unified, new StreamSource(x2));
            assertTrue(back.contains("<Cno>C001</Cno>"));
        }
    }
}
