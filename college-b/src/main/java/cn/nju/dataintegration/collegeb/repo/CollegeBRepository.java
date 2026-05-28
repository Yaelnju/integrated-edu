package cn.nju.dataintegration.collegeb.repo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 院系 B 业务数据访问层。所有方法接收 {@link Connection} 由调用方控制生命周期；
 * 本类不缓存连接、不开事务，单条 SQL 即一次方法调用。
 */
public final class CollegeBRepository {

    public enum Role {
        STUDENT, ADMIN
    }

    /**
     * 登录：查 ACCOUNT 表（学生和管理员统一在这里），靠 ACC_LEVEL 区分角色。
     */
    public Optional<Role> login(Connection c, String user, String pwd) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT ACC_LEVEL FROM ACCOUNT WHERE ACC_NO=? AND ACC_PWD=?")) {
            ps.setString(1, user);
            ps.setString(2, pwd);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int level = rs.getInt(1);
                    return Optional.of(level == 1 ? Role.ADMIN : Role.STUDENT);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * 列出所有课程（共享与不共享都返回，由调用方按需过滤）。
     */
    public List<String[]> listCourses(Connection c) throws SQLException {
        List<String[]> rows = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT CRS_NO, CRS_NAME, PERIODS, CREDIT, TEACHER, LOCATION, SHARED " +
                "FROM COURSE WHERE SUBSTR(CRS_NO,1,1)='B' ORDER BY CRS_NO");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rows.add(new String[]{
                        rs.getString("CRS_NO"),
                        rs.getString("CRS_NAME"),
                        String.valueOf(rs.getInt("PERIODS")),
                        String.valueOf(rs.getInt("CREDIT")),
                        rs.getString("TEACHER"),
                        rs.getString("LOCATION"),
                        rs.getString("SHARED")
                });
            }
        }
        return rows;
    }

    /**
     * 学生本人的选课记录（带课程名，方便 GUI 直接显示）。
     */
    public List<String[]> mySelections(Connection c, String sno) throws SQLException {
        List<String[]> rows = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT e.STU_NO, e.CRS_NO, e.SCORE, " +
                "NVL(co.CRS_NAME, e.CRS_NO) AS CRS_NAME " +
                "FROM ENROLLMENT e LEFT JOIN COURSE co ON co.CRS_NO = e.CRS_NO " +
                "WHERE e.STU_NO=? ORDER BY e.CRS_NO")) {
            ps.setString(1, sno);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new String[]{
                            rs.getString("STU_NO"),
                            rs.getString("CRS_NO"),
                            String.valueOf(rs.getInt("SCORE")),
                            rs.getString("CRS_NAME")
                    });
                }
            }
        }
        return rows;
    }

    public int countSelections(Connection c, String sno) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT COUNT(*) FROM ENROLLMENT WHERE STU_NO=?")) {
            ps.setString(1, sno);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /**
     * 跨院选课写回前置：若 sno 在本院 STUDENT 表中不存在，先插一条占位记录，
     * 避免 ENROLLMENT 的外键约束失败。STU_NAME 受 VARCHAR2(10 CHAR) 限制，
     * 直接用 sno 作占位名（≤9 字符）。
     */
    public void ensureStudentForCross(Connection c, String sno) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT 1 FROM STUDENT WHERE STU_NO=?")) {
            ps.setString(1, sno);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return;
                }
            }
        }
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO STUDENT(STU_NO, STU_NAME, SEX, MAJOR, PWD) VALUES(?, ?, 'M', '外院选修', '000000')")) {
            ps.setString(1, sno);
            ps.setString(2, sno);
            ps.executeUpdate();
        }
    }

    /**
     * 跨院写回前置：若 cno 在本院 COURSE 表不存在，插一条占位记录避免 FK 失败。
     * CRS_NO VARCHAR2(5) 最长 5 字节，可容纳 A 院 4 字符课号（AC01 等）和 C 院 4 字符课号。
     */
    public void ensureCourseForCross(Connection c, String cno, String courseName) throws SQLException {
        String name = (courseName != null && !courseName.isBlank()) ? courseName : cno;
        String trunc = name.length() > 16 ? name.substring(0, 16) : name;
        try (PreparedStatement sel = c.prepareStatement("SELECT CRS_NAME FROM COURSE WHERE CRS_NO=?")) {
            sel.setString(1, cno);
            try (ResultSet rs = sel.executeQuery()) {
                if (rs.next()) {
                    String existing = rs.getString(1);
                    if (existing == null || existing.isBlank() || existing.contains("外院") || existing.equals(cno)) {
                        try (PreparedStatement up = c.prepareStatement(
                                "UPDATE COURSE SET CRS_NAME=? WHERE CRS_NO=?")) {
                            up.setString(1, trunc);
                            up.setString(2, cno);
                            up.executeUpdate();
                        }
                    }
                    return;
                }
            }
        }
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO COURSE(CRS_NO,CRS_NAME,PERIODS,CREDIT,TEACHER,LOCATION,SHARED) " +
                "VALUES(?,?,0,1,'外院','外院','0')")) {
            ps.setString(1, cno);
            ps.setString(2, trunc);
            ps.executeUpdate();
        }
    }

    /** 集成写回专用：无 5 门上限，重复选当成成功。 */
    public void enrollForCross(Connection c, String sno, String cno) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO ENROLLMENT(CRS_NO, STU_NO, SCORE) VALUES(?, ?, 0)")) {
            ps.setString(1, cno);
            ps.setString(2, sno);
            try {
                ps.executeUpdate();
            } catch (SQLException ex) {
                if (!"23000".equals(ex.getSQLState())) throw ex;
            }
        }
    }

    /**
     * 选课：上限 5 门；命中复合主键唯一约束（已选过同一门）也算失败。
     */
    public boolean pickCourse(Connection c, String sno, String cno) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO ENROLLMENT(CRS_NO, STU_NO, SCORE) VALUES(?, ?, 0)")) {
            ps.setString(1, cno);
            ps.setString(2, sno);
            try {
                return ps.executeUpdate() == 1;
            } catch (SQLException ex) {
                // SQLState 23000 = 完整性约束冲突 (Oracle ORA-00001 唯一键 / ORA-02291 外键)
                if ("23000".equals(ex.getSQLState())) {
                    return false;
                }
                throw ex;
            }
        }
    }

    public boolean dropCourse(Connection c, String sno, String cno) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "DELETE FROM ENROLLMENT WHERE STU_NO=? AND CRS_NO=?")) {
            ps.setString(1, sno);
            ps.setString(2, cno);
            return ps.executeUpdate() == 1;
        }
    }

    /**
     * 管理员统计：返回 [学生数, 课程数, 选课数]。
     */
    public int[] localAdminStats(Connection c) throws SQLException {
        int students;
        int courses;
        int enrollments;
        try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM STUDENT");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            students = rs.getInt(1);
        }
        try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM COURSE");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            courses = rs.getInt(1);
        }
        try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM ENROLLMENT");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            enrollments = rs.getInt(1);
        }
        return new int[]{students, courses, enrollments};
    }
}
