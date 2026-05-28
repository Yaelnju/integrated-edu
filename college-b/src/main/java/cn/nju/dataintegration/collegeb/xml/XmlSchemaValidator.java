package cn.nju.dataintegration.collegeb.xml;

import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.IOException;
import java.io.StringReader;

/**
 * 用 JAXP 校验 XML 字符串是否符合给定 XSD。
 * 调用方负责把 XSD 包成 {@link StreamSource}（通常用 classpath 资源流）。
 * 校验失败抛 {@link SAXException}，由上层翻译成 ERR 响应。
 */
public final class XmlSchemaValidator {
    private final SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

    public void validate(String xml, StreamSource xsd) throws SAXException, IOException {
        Schema schema = factory.newSchema(xsd);
        Validator v = schema.newValidator();
        v.validate(new StreamSource(new StringReader(xml)));
    }
}
