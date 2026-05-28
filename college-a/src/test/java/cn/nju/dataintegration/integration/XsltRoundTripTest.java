package cn.nju.dataintegration.integration;

import org.junit.jupiter.api.Test;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 院系 A 本地格式 (Students/StuID/StuName/Gender/Dept、Courses/CourseID 等)
 * 经 {@code studentA_to_unified.xsl} / {@code classA_to_unified.xsl} 应得到规范格式
 * (Students/student/id/name/sex/major、Classes/class/id/name/time/score 等)。
 *
 * 集成服务器跑的 XSL 主资源在 integration-server 模块，本测试为避免跨模块依赖，
 * 同名 XSL 拷贝到 college-a/src/test/resources 当作测试 fixture；
 * 两份 XSL 必须保持逐字一致。
 */
public class XsltRoundTripTest {

    private String transform(String xml, String xslResource) throws Exception {
        try (InputStream xsl = getClass().getClassLoader().getResourceAsStream(xslResource)) {
            if (xsl == null) {
                throw new IllegalStateException("缺少测试 fixture: " + xslResource);
            }
            Transformer t = TransformerFactory.newInstance().newTransformer(new StreamSource(xsl));
            StringWriter out = new StringWriter();
            t.transform(new StreamSource(new StringReader(xml)), new StreamResult(out));
            return out.toString();
        }
    }

    @Test
    void studentA_to_unified_mapsFields() throws Exception {
        String nativeXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<Students>\n"
                + "  <student>\n"
                + "    <StuID>A20240001</StuID>\n"
                + "    <StuName>赵小明</StuName>\n"
                + "    <Gender>M</Gender>\n"
                + "    <Dept>计算机</Dept>\n"
                + "  </student>\n"
                + "</Students>\n";
        String unified = transform(nativeXml, "xsl/integration/studentA_to_unified.xsl");
        assertTrue(unified.contains("<id>A20240001</id>"), "id 应映射自 StuID");
        assertTrue(unified.contains("<name>赵小明</name>"), "name 应映射自 StuName");
        assertTrue(unified.contains("<sex>M</sex>"), "sex 应映射自 Gender");
        assertTrue(unified.contains("<major>计算机</major>"), "major 应映射自 Dept");
    }

    @Test
    void classA_to_unified_computesTimeFromCredit() throws Exception {
        // A 院本地 schema 没有"课时"字段，按 classA_to_unified.xsl 约定 time = credit * 16
        String nativeXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<Courses>\n"
                + "  <course>\n"
                + "    <CourseID>AC01</CourseID>\n"
                + "    <CourseName>数据结构</CourseName>\n"
                + "    <Credit>3</Credit>\n"
                + "    <Teacher>王老师</Teacher>\n"
                + "    <IsShared>1</IsShared>\n"
                + "  </course>\n"
                + "</Courses>\n";
        String unified = transform(nativeXml, "xsl/integration/classA_to_unified.xsl");
        assertTrue(unified.contains("<id>AC01</id>"));
        assertTrue(unified.contains("<name>数据结构</name>"));
        assertTrue(unified.contains("<time>48</time>"), "time 应为 Credit*16=48");
        assertTrue(unified.contains("<score>3</score>"));
        assertTrue(unified.contains("<teacher>王老师</teacher>"));
        assertTrue(unified.contains("<share>1</share>"));
    }
}
