# 学院 A — SQL Server 说明

## 表结构（唯一标准）

与 `sql/college_a_schema.sql`、Python `college_a/db.py`、Java `CollegeARepository` **完全一致**：

| 表 | 主要字段 |
|----|----------|
| `Student` | `StuID`, `StuName`, `Gender`, `BirthDate`, `Dept` |
| `Course` | `CourseID`, `CourseName`, `Credit`, `Teacher`, `IsShared` |
| `Enrollment` | `StuID`, `CourseID`, `SelectDate`, `Grade`, `IsCross`, `HomeCollege` |
| `SysUser` | `UserName`, `Password`, `Role` |

**不要使用**旧版 `Stu_num` / `SC` / `Account` 字段脚本。

若 SSMS 里已有错误结构的表，请先执行 `01_schema.sql`（会 DROP 重建）。

## 初始化数据（二选一）

### 方式 A：Python（推荐，Windows 身份验证）

```powershell
cd "d:\2026-spring\课程\数据集成\HW3"
$env:COLLEGE_A_USE_SQLITE="0"
$env:COLLEGE_A_CONN="DRIVER={ODBC Driver 17 for SQL Server};SERVER=localhost;DATABASE=CollegeA_DB;Trusted_Connection=yes;TrustServerCertificate=yes;"
python scripts\seed_college_a.py
```

### 方式 B：SSMS 执行 SQL

在已执行 `01_schema.sql` 之后：

```text
02_seed.sql
```

## 验证

```sql
USE CollegeA_DB;
SELECT COUNT(*) FROM Student;    -- 50
SELECT COUNT(*) FROM Course;     -- 10
SELECT COUNT(*) FROM Enrollment; -- 250
```


## 启动 Java GUI

```powershell
cd DataIntegration-collegeA
mvn -q compile exec:java "-Dexec.mainClass=cn.nju.dataintegration.tools.DbConnectionTest"
mvn -q compile exec:java
```

登录：`admin` / `admin123` 或学生 `A20240001` / `123456`
