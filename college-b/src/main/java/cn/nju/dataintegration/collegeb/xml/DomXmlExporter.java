package cn.nju.dataintegration.collegeb.xml;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import java.io.IOException;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/**
 * 把院系 B 数据库表导出为 XML 字符串。
 * 元素名由 SQL 列名决定（Oracle 默认大写），跟 xsd/college-b/ 下的 XSD 一一对应。
 * 不做校验、不做业务过滤 —— 校验交 {@link XmlSchemaValidator}，过滤交业务层。
 */
public final class DomXmlExporter {

    public String exportStudents(Connection c) throws SQLException, IOException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT STU_NO, STU_NAME, SEX, MAJOR, PWD FROM STUDENT ORDER BY STU_NO");
             ResultSet rs = ps.executeQuery()) {
            return resultSetToXml(rs, "Students", "student");
        }
    }

    public String exportCourses(Connection c) throws SQLException, IOException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT CRS_NO, CRS_NAME, PERIODS, CREDIT, TEACHER, LOCATION, SHARED " +
                "FROM COURSE ORDER BY CRS_NO");
             ResultSet rs = ps.executeQuery()) {
            return resultSetToXml(rs, "Courses", "course");
        }
    }

    public String exportChoices(Connection c) throws SQLException, IOException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT STU_NO, CRS_NO, SCORE FROM ENROLLMENT ORDER BY STU_NO, CRS_NO");
             ResultSet rs = ps.executeQuery()) {
            return resultSetToXml(rs, "Choices", "choice");
        }
    }

    private String resultSetToXml(ResultSet rs, String rootName, String rowName)
            throws SQLException, IOException {
        Document doc = DocumentHelper.createDocument();
        Element root = doc.addElement(rootName);
        ResultSetMetaData md = rs.getMetaData();
        int cols = md.getColumnCount();
        while (rs.next()) {
            Element row = root.addElement(rowName);
            for (int i = 1; i <= cols; i++) {
                String col = md.getColumnLabel(i);
                Object v = rs.getObject(i);
                row.addElement(col).setText(v == null ? "" : String.valueOf(v));
            }
        }
        OutputFormat format = OutputFormat.createPrettyPrint();
        format.setEncoding("UTF-8");
        StringWriter sw = new StringWriter();
        XMLWriter xw = new XMLWriter(sw, format);
        try {
            xw.write(doc);
        } finally {
            xw.close();
        }
        return sw.toString();
    }
}
