package cn.nju.dataintegration.integration;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.io.StringWriter;

public final class XsltTransformer {
    private final TransformerFactory tf = TransformerFactory.newInstance();

    public String transform(String xml, StreamSource xsl) throws TransformerException {
        Transformer t = tf.newTransformer(xsl);
        StringWriter out = new StringWriter();
        t.transform(new StreamSource(new StringReader(xml)), new StreamResult(out));
        return out.toString();
    }
}
