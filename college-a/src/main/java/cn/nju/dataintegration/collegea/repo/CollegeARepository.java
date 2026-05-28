package cn.nju.dataintegration.collegea.repo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** 字段与 dbo.Student / Course / Enrollment / SysUser 一致。 */
public final class CollegeARepository {

    public enum Role {
        STUDENT, ADMIN
    }

    public Optional<Role> login(Connection c, String user, String pwd) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT Role FROM SysUser WHERE UserName=? AND Password=?")) {
            ps.setString(1, user);
            ps.setString(2, pwd);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String role = rs.getString(1);
                    if ("admin".equalsIgnoreCase(role) || "teacher".equalsIgnoreCase(role)) {
                        return Optional.of(Role.ADMIN);
                    }
                    return Optional.of(Role.STUDENT);
                }
            }
        }
        return Optional.empty();
    }

    public List<String[]> listCourses(Connection c) throws SQLException {
        List<String[]> rows = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT CourseID, CourseName, Credit, Teacher, IsShared FROM Course WHERE CourseID LIKE 'A%' ORDER BY CourseID");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rows.add(new String[]{
                        rs.getString(1), rs.getString(2),
                        String.valueOf(rs.getInt(3)), rs.getString(4),
                        rs.getInt(5) == 1 ? "1" : "0"
                });
            }
        }
        return rows;
    }

    public List<String[]> mySelections(Connection c, String sno) throws SQLException {
        List<String[]> rows = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT e.StuID, e.CourseID, e.Grade, c.CourseName " +
                        "FROM Enrollment e LEFT JOIN Course c ON c.CourseID = e.CourseID " +
                        "WHERE e.StuID=? ORDER BY e.CourseID")) {
            ps.setString(1, sno);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new String[]{
                            rs.getString(1), rs.getString(2),
                            rs.getBigDecimal(3) == null ? "0" : rs.getBigDecimal(3).toPlainString(),
                            rs.getString(4) == null ? "(外院课程)" : rs.getString(4)
                    });
                }
            }
        }
        return rows;
    }

    public int countSelections(Connection c, String sno) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM Enrollment WHERE StuID=?")) {
            ps.setString(1, sno);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public void ensureStudentForCross(Connection c, String sno) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT 1 FROM Student WHERE StuID=?")) {
            ps.setString(1, sno);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return;
                }
            }
        }
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO Student(StuID,StuName,Gender,BirthDate,Dept) VALUES (?,?,?,?,?)")) {
            ps.setString(1, sno);
            ps.setString(2, "外院-" + sno);
            ps.setString(3, "M");
            ps.setNull(4, java.sql.Types.DATE);
            ps.setString(5, "外院选修");
            ps.executeUpdate();
        }
    }

    /** 跨院写回前置：CourseID VARCHAR(9) 可容纳任意学院课号。 */
    public void ensureCourseForCross(Connection c, String cno) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT 1 FROM Course WHERE CourseID=?")) {
            ps.setString(1, cno);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return;
            }
        }
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO Course(CourseID,CourseName,Credit,Teacher,IsShared) VALUES(?,N'外院课程',1,N'外院',0)")) {
            ps.setString(1, cno);
            ps.executeUpdate();
        }
    }

    public boolean pickCourse(Connection c, String sno, String cno) throws SQLException {
        if (countSelections(c, sno) >= 5) {
            return false;
        }
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO Enrollment(StuID, CourseID, SelectDate, IsCross, HomeCollege) VALUES(?,?,CAST(GETDATE() AS DATE),0,'A')")) {
            ps.setString(1, sno);
            ps.setString(2, cno);
            try {
                return ps.executeUpdate() == 1;
            } catch (SQLException ex) {
                if ("23000".equals(ex.getSQLState()) || ex.getErrorCode() == 2627) {
                    return false;
                }
                throw ex;
            }
        }
    }

    public boolean dropCourse(Connection c, String sno, String cno) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "DELETE FROM Enrollment WHERE StuID=? AND CourseID=?")) {
            ps.setString(1, sno);
            ps.setString(2, cno);
            return ps.executeUpdate() == 1;
        }
    }

    public int[] localAdminStats(Connection c) throws SQLException {
        int[] t = new int[3];
        try (var ps = c.prepareStatement("SELECT COUNT(*) FROM Student");
             var rs = ps.executeQuery()) {
            rs.next();
            t[0] = rs.getInt(1);
        }
        try (var ps = c.prepareStatement("SELECT COUNT(*) FROM Course");
             var rs = ps.executeQuery()) {
            rs.next();
            t[1] = rs.getInt(1);
        }
        try (var ps = c.prepareStatement("SELECT COUNT(*) FROM Enrollment");
             var rs = ps.executeQuery()) {
            rs.next();
            t[2] = rs.getInt(1);
        }
        return t;
    }
}
