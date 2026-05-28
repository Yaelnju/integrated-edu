package cn.nju.dataintegration.integration.validator;

import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;

/**
 * 集成服务器侧的 XSD 校验模块（Student 2 负责）。
 * 调用方：Student 1 的 IntegrationTcpServer 在拉取到任一院系 XML、或把规范化（unified）XML
 * 喂给目标院系前调用本类做合法性校验。
 *
 * 资源约定（所有 XSD 都打包进 college-b 模块的 classpath）：
 *   - 规范格式（unified）：xsd/integration/formatStudent.xsd、formatClass.xsd、formatChoice.xsd
 *   - 院系 B 原生格式：     xsd/college-b/studentB.xsd、classB.xsd、choiceB.xsd、accountB.xsd
 *   - 院系 A / C 的本地 XSD 由各自模块提供，本类的 {@link #validate(String, String)} 直接接 classpath 路径，
 *     如果将来集成服务器需要校验 A/C 的本地 XML，把对应 XSD 资源放到 classpath 即可，无需改本类。
 */
public final class XmlValidator {
    private final SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

    /** 用 classpath 上的 XSD 资源校验给定 XML 字符串；失败抛 SAXException，由调用方翻译成错误响应。 */
    public void validate(String xml, String xsdClasspathResource) throws SAXException, IOException {
        try (InputStream in = openResource(xsdClasspathResource)) {
            Schema schema = factory.newSchema(new StreamSource(in));
            Validator v = schema.newValidator();
            v.validate(new StreamSource(new StringReader(xml)));
        }
    }

    // 规范格式 ----------------------------------------------------------------
    public void validateUnifiedStudents(String xml) throws SAXException, IOException {
        validate(xml, "xsd/integration/formatStudent.xsd");
    }

    public void validateUnifiedClasses(String xml) throws SAXException, IOException {
        validate(xml, "xsd/integration/formatClass.xsd");
    }

    public void validateUnifiedChoices(String xml) throws SAXException, IOException {
        validate(xml, "xsd/integration/formatChoice.xsd");
    }

    // 院系 B 原生格式 ----------------------------------------------------------
    public void validateCollegeBStudents(String xml) throws SAXException, IOException {
        validate(xml, "xsd/college-b/studentB.xsd");
    }

    public void validateCollegeBClasses(String xml) throws SAXException, IOException {
        validate(xml, "xsd/college-b/classB.xsd");
    }

    public void validateCollegeBChoices(String xml) throws SAXException, IOException {
        validate(xml, "xsd/college-b/choiceB.xsd");
    }

    public void validateCollegeBAccounts(String xml) throws SAXException, IOException {
        validate(xml, "xsd/college-b/accountB.xsd");
    }

    private InputStream openResource(String path) {
        InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
        if (in == null) {
            throw new IllegalStateException("缺少 XSD 资源: " + path);
        }
        return in;
    }
}
