-- 与 01_schema.sql 字段一致；也可用 python scripts/seed_college_a.py 自动灌数
USE CollegeA_DB;
GO

DELETE FROM dbo.Enrollment;
DELETE FROM dbo.SysUser WHERE UserName NOT IN ('admin','teacher');
DELETE FROM dbo.Student;
DELETE FROM dbo.Course;
GO

INSERT INTO dbo.Course (CourseID, CourseName, Credit, Teacher, IsShared) VALUES
('AC01', N'数据结构',     4, N'王教授', 1),
('AC02', N'数据库原理',   3, N'李教授', 1),
('AC03', N'操作系统',     4, N'张教授', 1),
('AC04', N'计算机网络',   3, N'刘教授', 1),
('AC05', N'软件工程',     3, N'陈教授', 1),
('AC06', N'编译原理',     3, N'赵教授', 0),
('AC07', N'人工智能导论', 2, N'周教授', 0),
('AC08', N'离散数学',     4, N'吴教授', 0),
('AC09', N'算法设计',     3, N'郑教授', 0),
('AC10', N'云计算基础',   2, N'孙教授', 0);
GO

DECLARE @i INT = 1;
WHILE @i <= 50
BEGIN
    DECLARE @sid VARCHAR(20) = 'A2024' + RIGHT('0000' + CAST(@i AS VARCHAR), 4);
    INSERT INTO dbo.Student (StuID, StuName, Gender, BirthDate, Dept)
    VALUES (
        @sid,
        N'A院学生' + RIGHT('00' + CAST(@i AS VARCHAR), 2),
        CASE WHEN @i % 2 = 0 THEN 'F' ELSE 'M' END,
        DATEADD(day, @i * 10, '2004-01-01'),
        CASE (@i % 3) WHEN 0 THEN N'软件工程' WHEN 1 THEN N'计算机科学' ELSE N'信息安全' END
    );
    INSERT INTO dbo.SysUser (UserName, Password, Role) VALUES (@sid, '123456', 'student');
    SET @i = @i + 1;
END;
GO

DECLARE @s INT = 1;
WHILE @s <= 50
BEGIN
    DECLARE @sno VARCHAR(20) = 'A2024' + RIGHT('0000' + CAST(@s AS VARCHAR), 4);
    INSERT INTO dbo.Enrollment (StuID, CourseID, SelectDate, Grade, IsCross, HomeCollege)
    SELECT @sno, CourseID, CAST(GETDATE() AS DATE), NULL, 0, 'A'
    FROM (
        SELECT CourseID, ROW_NUMBER() OVER (ORDER BY CourseID) AS rn FROM dbo.Course
    ) t
    WHERE rn IN (
        ((@s - 1) % 10) + 1,
        ((@s) % 10) + 1,
        ((@s + 1) % 10) + 1,
        ((@s + 2) % 10) + 1,
        ((@s + 3) % 10) + 1
    );
    SET @s = @s + 1;
END;
GO
