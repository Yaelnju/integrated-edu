-- ==========================================================
-- College B (Oracle) 数据库 - 化学学院
-- 课本参考：表 3-6 ~ 表 3-9
-- 在 collegeb 用户下执行（FREEPDB1 pluggable database）

-- ==========================================================

-- 清理旧表（CASCADE 自动解除外键约束，第一次执行会报"不存在"，可忽略）
DROP TABLE ENROLLMENT CASCADE CONSTRAINTS;
DROP TABLE ACCOUNT    CASCADE CONSTRAINTS;
DROP TABLE COURSE     CASCADE CONSTRAINTS;
DROP TABLE STUDENT    CASCADE CONSTRAINTS;

-- ==========================================================
-- 学生表 (课本 表 3-7)
-- ==========================================================
CREATE TABLE STUDENT (
    STU_NO    VARCHAR2(9)        NOT NULL,             -- 学号
    STU_NAME  VARCHAR2(10 CHAR)  NOT NULL,             -- 姓名 (10 个中文字符)
    SEX       CHAR(1)            NOT NULL,             -- 性别 M/F
    MAJOR     VARCHAR2(16 CHAR)  NOT NULL,             -- 专业 (16 个中文字符)
    PWD       VARCHAR2(6)        NOT NULL,             -- 密码 (ASCII)
    CONSTRAINT pk_student  PRIMARY KEY (STU_NO),
    CONSTRAINT chk_stu_sex CHECK (SEX IN ('M','F'))
);

-- ==========================================================
-- 课程表 (课本 表 3-8)
-- ==========================================================
CREATE TABLE COURSE (
    CRS_NO    VARCHAR2(5)        NOT NULL,             -- 编号
    CRS_NAME  VARCHAR2(16 CHAR)  NOT NULL,             -- 名称 (16 个中文字符)
    PERIODS   NUMBER(3)          NOT NULL,             -- 课时
    CREDIT    NUMBER(1)          NOT NULL,             -- 学分
    TEACHER   VARCHAR2(10 CHAR)  NOT NULL,             -- 老师
    LOCATION  VARCHAR2(20 CHAR)  NOT NULL,             -- 地点
    SHARED    CHAR(1)            DEFAULT '0' NOT NULL, -- 共享 0=否 1=是
    CONSTRAINT pk_course      PRIMARY KEY (CRS_NO),
    CONSTRAINT chk_crs_credit CHECK (CREDIT BETWEEN 1 AND 5),
    CONSTRAINT chk_crs_shared CHECK (SHARED IN ('0','1'))
);

-- ==========================================================
-- 选课表 (课本 表 3-9)
-- 课本 PK 只有 CRS_NO 是错的（会导致一门课只能被一人选），改为复合主键
-- ==========================================================
CREATE TABLE ENROLLMENT (
    CRS_NO  VARCHAR2(5) NOT NULL,                      -- 课程编号
    STU_NO  VARCHAR2(9) NOT NULL,                      -- 学号
    SCORE   NUMBER(3)   DEFAULT 0 NOT NULL,            -- 得分
    CONSTRAINT pk_enrollment    PRIMARY KEY (CRS_NO, STU_NO),
    CONSTRAINT fk_enroll_course FOREIGN KEY (CRS_NO) REFERENCES COURSE(CRS_NO),
    CONSTRAINT fk_enroll_stu    FOREIGN KEY (STU_NO) REFERENCES STUDENT(STU_NO),
    CONSTRAINT chk_score_range  CHECK (SCORE BETWEEN 0 AND 100)
);

-- ==========================================================
-- 账户表 (课本 表 3-6)
-- ACC_NO 是登录账号；STU_NO 是课本"客体"FK
-- 学生账户：STU_NO 指向学生记录；管理员账户：STU_NO 为 NULL
-- ==========================================================
CREATE TABLE ACCOUNT (
    ACC_NO    VARCHAR2(12) NOT NULL,                   -- 账户名
    ACC_PWD   VARCHAR2(12) NOT NULL,                   -- 密码
    ACC_LEVEL NUMBER(2)    DEFAULT 2 NOT NULL,         -- 级别 1=管理员 2=学生
    STU_NO    VARCHAR2(9)  NULL,                       -- 客体（关联学生）
    CONSTRAINT pk_account     PRIMARY KEY (ACC_NO),
    CONSTRAINT fk_account_stu FOREIGN KEY (STU_NO) REFERENCES STUDENT(STU_NO),
    CONSTRAINT chk_acc_level  CHECK (ACC_LEVEL IN (1,2))
);

-- ==========================================================
-- 索引：跨院选课时频繁查 SHARED='1'；登录后查学生选课
-- ==========================================================
CREATE INDEX idx_course_shared  ON COURSE(SHARED);
CREATE INDEX idx_enrollment_stu ON ENROLLMENT(STU_NO);
CREATE INDEX idx_account_stu    ON ACCOUNT(STU_NO);

COMMIT;
