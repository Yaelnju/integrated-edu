package cn.nju.dataintegration.collegec.xml;

import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.IOException;
import java.io.StringReader;

public final class XmlSchemaValidator {
    private final SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

    public void validate(String xml, StreamSource xsd) throws SAXException, IOException {
        Schema schema = factory.newSchema(xsd);
        Validator v = schema.newValidator();
        v.validate(new StreamSource(new StringReader(xml)));
    }
}
