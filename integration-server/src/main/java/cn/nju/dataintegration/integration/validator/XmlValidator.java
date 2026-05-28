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
 * 调用方在各院 XML 经 XSL 变换为规范格式后调用本类做合法性校验。
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

    private InputStream openResource(String path) {
        InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
        if (in == null) {
            throw new IllegalStateException("缺少 XSD 资源: " + path);
        }
        return in;
    }
}
