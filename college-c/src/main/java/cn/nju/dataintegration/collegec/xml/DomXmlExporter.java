package cn.nju.dataintegration.collegec.xml;

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

public final class DomXmlExporter {

    public String exportStudents(Connection c) throws SQLException, IOException {
        try (PreparedStatement ps = c.prepareStatement("SELECT Sno,Snm,Sex,Sde,Pwd FROM student ORDER BY Sno");
             ResultSet rs = ps.executeQuery()) {
            return resultSetToXml(rs, "Students", "student");
        }
    }

    public String exportCourses(Connection c) throws SQLException, IOException {
        try (PreparedStatement ps = c.prepareStatement("SELECT Cno,Cnm,Ctm,Cpt,Tec,Pla,Share FROM course ORDER BY Cno");
             ResultSet rs = ps.executeQuery()) {
            return resultSetToXml(rs, "Courses", "course");
        }
    }

    public String exportChoices(Connection c) throws SQLException, IOException {
        try (PreparedStatement ps = c.prepareStatement("SELECT Sno,Cno,Grd FROM sc ORDER BY Sno,Cno");
             ResultSet rs = ps.executeQuery()) {
            return resultSetToXml(rs, "Choices", "choice");
        }
    }

    private String resultSetToXml(ResultSet rs, String rootName, String rowName) throws SQLException, IOException {
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
