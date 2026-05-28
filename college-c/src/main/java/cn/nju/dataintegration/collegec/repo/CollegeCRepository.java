package cn.nju.dataintegration.collegec.repo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class CollegeCRepository {
    public enum Role {
        STUDENT, ADMIN
    }

    public Optional<Role> login(Connection c, String user, String pwd) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT 1 FROM student WHERE Sno=? AND Pwd=?")) {
            ps.setString(1, user);
            ps.setString(2, pwd);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(Role.STUDENT);
                }
            }
        }
        try (PreparedStatement ps = c.prepareStatement("SELECT 1 FROM account WHERE acc=? AND passwd=?")) {
            ps.setString(1, user);
            ps.setString(2, pwd);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(Role.ADMIN);
                }
            }
        }
        return Optional.empty();
    }

    public List<String[]> listCourses(Connection c) throws SQLException {
        List<String[]> rows = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT Cno,Cnm,Ctm,Cpt,Tec,Pla,Share FROM course WHERE Cno LIKE 'C%' ORDER BY Cno");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rows.add(new String[]{
                        rs.getString("Cno"),
                        rs.getString("Cnm"),
                        String.valueOf(rs.getInt("Ctm")),
                        String.valueOf(rs.getInt("Cpt")),
                        rs.getString("Tec"),
                        rs.getString("Pla"),
                        rs.getString("Share")
                });
            }
        }
        return rows;
    }

    public List<String[]> mySelections(Connection c, String sno) throws SQLException {
        List<String[]> rows = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT sc.Sno, sc.Cno, sc.Grd, co.Cnm FROM sc JOIN course co ON co.Cno=sc.Cno WHERE sc.Sno=? ORDER BY sc.Cno")) {
            ps.setString(1, sno);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new String[]{
                            rs.getString("Sno"),
                            rs.getString("Cno"),
                            String.valueOf(rs.getInt("Grd")),
                            rs.getString("Cnm")
                    });
                }
            }
        }
        return rows;
    }

    public int countSelections(Connection c, String sno) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM sc WHERE Sno=?")) {
            ps.setString(1, sno);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /**
     * 跨院选课写回前置：若 sno 不在本院 student 表中，先插一条占位记录避免 FK 失败。
     * Snm VARCHAR(10) 限制下用 sno 占位（≤9 字符）。
     */
    public void ensureStudentForCross(Connection c, String sno) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT 1 FROM student WHERE Sno=?")) {
            ps.setString(1, sno);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return;
                }
            }
        }
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO student(Sno, Snm, Sex, Sde, Pwd) VALUES(?, ?, 'M', '外院', '000000')")) {
            ps.setString(1, sno);
            ps.setString(2, sno);
            ps.executeUpdate();
        }
    }

    /**
     * 跨院写回前置：Cno CHAR(4) 只能存 4 字符，长于 4 字符的外院课号（如 B 院 5 字符）直接跳过。
     * 跳过后 pickCourse 的 FK 仍会失败，由 CrossEnrollService 的 try-catch 忽略源院写回失败。
     */
    public void ensureCourseForCross(Connection c, String cno, String courseName) throws SQLException {
        if (cno.length() > 4) return;
        try (PreparedStatement ps = c.prepareStatement("SELECT 1 FROM course WHERE Cno=?")) {
            ps.setString(1, cno);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return;
            }
        }
        String name = (courseName != null && !courseName.isBlank()) ? courseName : cno;
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO course(Cno,Cnm,Ctm,Cpt,Tec,Pla,Share) VALUES(?,?,0,1,?,?,0)")) {
            ps.setString(1, cno);
            ps.setString(2, name.length() > 20 ? name.substring(0, 20) : name);
            ps.setString(3, "外院");
            ps.setString(4, "外院");
            ps.executeUpdate();
        }
    }

    public boolean pickCourse(Connection c, String sno, String cno) throws SQLException {
        if (countSelections(c, sno) >= 5) {
            return false;
        }
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO sc(Sno,Cno,Grd) VALUES(?,?,0)")) {
            ps.setString(1, sno);
            ps.setString(2, cno);
            try {
                return ps.executeUpdate() == 1;
            } catch (SQLException ex) {
                if ("23000".equals(ex.getSQLState())) {
                    return false;
                }
                throw ex;
            }
        }
    }

    public boolean dropCourse(Connection c, String sno, String cno) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM sc WHERE Sno=? AND Cno=?")) {
            ps.setString(1, sno);
            ps.setString(2, cno);
            return ps.executeUpdate() == 1;
        }
    }

    public int[] localAdminStats(Connection c) throws SQLException {
        int students = 0;
        int courses = 0;
        int choices = 0;
        try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM student");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            students = rs.getInt(1);
        }
        try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM course");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            courses = rs.getInt(1);
        }
        try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM sc");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            choices = rs.getInt(1);
        }
        return new int[]{students, courses, choices};
    }
}
