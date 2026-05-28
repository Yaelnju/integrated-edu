package cn.nju.dataintegration.integration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CrossEnrollServiceTest {

    @Test
    void detectStudentCollege_usesStudentIdPrefix() {
        assertEquals("A", CrossEnrollService.detectStudentCollege("A20240001"));
        assertEquals("B", CrossEnrollService.detectStudentCollege("B240000001"));
        assertEquals("C", CrossEnrollService.detectStudentCollege("C202401"));
    }

    @Test
    void detectStudentCollege_rejectsUnknownPrefix() {
        assertThrows(IllegalArgumentException.class,
                () -> CrossEnrollService.detectStudentCollege("X20240001"));
    }

    @Test
    void detectCourseCollege_matchesCurrentCourseIdRules() {
        assertEquals("A", CrossEnrollService.detectCourseCollege("AC2024001"));
        assertEquals("B", CrossEnrollService.detectCourseCollege("B0001"));
        assertEquals("C", CrossEnrollService.detectCourseCollege("C001"));
    }

    @Test
    void detectCourseCollege_rejectsUnknownCourseId() {
        assertThrows(IllegalArgumentException.class,
                () -> CrossEnrollService.detectCourseCollege("UNKNOWN"));
    }
}
