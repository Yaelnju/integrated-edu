package cn.nju.dataintegration.integration.validator;

import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class XmlValidatorTest {

    private final XmlValidator validator = new XmlValidator();

    // 规范格式 Students ------------------------------------------------------

    @Test
    void unifiedStudents_valid_passes() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<Students>\n"
                + "  <student>\n"
                + "    <id>B240000001</id>\n"
                + "    <name>张三</name>\n"
                + "    <sex>M</sex>\n"
                + "    <major>化学</major>\n"
                + "  </student>\n"
                + "</Students>";
        assertDoesNotThrow(() -> validator.validateUnifiedStudents(xml));
    }

    @Test
    void unifiedStudents_missingName_fails() {
        // 缺少必填 <name> 元素，按 formatStudent.xsd 应抛 SAXException
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<Students>\n"
                + "  <student>\n"
                + "    <id>B240000001</id>\n"
                + "    <sex>M</sex>\n"
                + "    <major>化学</major>\n"
                + "  </student>\n"
                + "</Students>";
        assertThrows(SAXException.class, () -> validator.validateUnifiedStudents(xml));
    }

    @Test
    void unifiedStudents_wrongRoot_fails() {
        // 根元素拼错（Studets）应抛 SAXException
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<Studets><student><id>x</id><name>x</name><sex>M</sex><major>x</major></student></Studets>";
        assertThrows(SAXException.class, () -> validator.validateUnifiedStudents(xml));
    }

    // 规范格式 Classes -------------------------------------------------------

    @Test
    void unifiedClasses_valid_passes() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<Classes>\n"
                + "  <class>\n"
                + "    <id>00000B0001</id>\n"
                + "    <name>大学化学</name>\n"
                + "    <time>48</time>\n"
                + "    <score>3</score>\n"
                + "    <teacher>李教授</teacher>\n"
                + "    <location>仙林化学楼-201</location>\n"
                + "    <share>1</share>\n"
                + "  </class>\n"
                + "</Classes>";
        assertDoesNotThrow(() -> validator.validateUnifiedClasses(xml));
    }

    @Test
    void unifiedClasses_timeNotInt_fails() {
        // <time> 必须 xs:int，给非数字应抛 SAXException
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<Classes>\n"
                + "  <class>\n"
                + "    <id>00000B0001</id>\n"
                + "    <name>大学化学</name>\n"
                + "    <time>四十八</time>\n"
                + "    <score>3</score>\n"
                + "    <teacher>李</teacher>\n"
                + "    <location>仙林</location>\n"
                + "  </class>\n"
                + "</Classes>";
        assertThrows(SAXException.class, () -> validator.validateUnifiedClasses(xml));
    }

    // 规范格式 Choices -------------------------------------------------------

    @Test
    void unifiedChoices_valid_passes() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<Choices>\n"
                + "  <choice><sid>B240000001</sid><cid>00000B0001</cid><score>85</score></choice>\n"
                + "  <choice><sid>B240000002</sid><cid>00000B0002</cid><score>0</score></choice>\n"
                + "</Choices>";
        assertDoesNotThrow(() -> validator.validateUnifiedChoices(xml));
    }

    @Test
    void unifiedChoices_emptyCollection_passes() {
        // minOccurs="0"：空 Choices 也合法
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Choices/>";
        assertDoesNotThrow(() -> validator.validateUnifiedChoices(xml));
    }
}
