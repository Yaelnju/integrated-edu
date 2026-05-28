-- 学院 A（SQL Server）— 与 sql/college_a_schema.sql、Python college_a/db.py 一致
IF DB_ID(N'CollegeA_DB') IS NULL
    CREATE DATABASE CollegeA_DB;
GO
USE CollegeA_DB;
GO

IF OBJECT_ID(N'dbo.Enrollment', N'U') IS NOT NULL DROP TABLE dbo.Enrollment;
IF OBJECT_ID(N'dbo.SysUser', N'U') IS NOT NULL DROP TABLE dbo.SysUser;
IF OBJECT_ID(N'dbo.Course', N'U') IS NOT NULL DROP TABLE dbo.Course;
IF OBJECT_ID(N'dbo.Student', N'U') IS NOT NULL DROP TABLE dbo.Student;
GO

CREATE TABLE dbo.Student (
    StuID       VARCHAR(20)  NOT NULL PRIMARY KEY,
    StuName     NVARCHAR(50) NOT NULL,
    Gender      CHAR(1)      NOT NULL CHECK (Gender IN ('M','F')),
    BirthDate   DATE         NULL,
    Dept        NVARCHAR(50) NOT NULL
);

CREATE TABLE dbo.Course (
    CourseID    VARCHAR(20)  NOT NULL PRIMARY KEY,
    CourseName  NVARCHAR(80) NOT NULL,
    Credit      INT          NOT NULL,
    Teacher     NVARCHAR(50) NOT NULL,
    IsShared    BIT          NOT NULL DEFAULT 0
);

CREATE TABLE dbo.Enrollment (
    StuID       VARCHAR(20) NOT NULL,
    CourseID    VARCHAR(20) NOT NULL,
    SelectDate  DATE        NOT NULL DEFAULT CAST(GETDATE() AS DATE),
    Grade       DECIMAL(5,2) NULL,
    IsCross     BIT         NOT NULL DEFAULT 0,
    HomeCollege CHAR(1)     NULL,
    PRIMARY KEY (StuID, CourseID),
    FOREIGN KEY (StuID) REFERENCES dbo.Student(StuID)
);

CREATE TABLE dbo.SysUser (
    UserName VARCHAR(30) NOT NULL PRIMARY KEY,
    Password VARCHAR(64) NOT NULL,
    Role     VARCHAR(20) NOT NULL
);
GO

INSERT INTO dbo.SysUser(UserName, Password, Role) VALUES
('admin', 'admin123', 'admin'),
('teacher', 'teacher123', 'teacher');
GO
