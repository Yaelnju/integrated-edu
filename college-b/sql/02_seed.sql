-- ==========================================================
-- College B (化学学院) 种子数据
-- 50 学生 + 10 课程 + 250 选课 + 51 账户(1管理员+50学生)
--
-- 全部用纯 SQL（无 PL/SQL 块、无 `/` 终止符），DBeaver 兼容性最好
-- 可重复执行（先清空再插入）
-- ==========================================================

-- 按 FK 反向顺序清空
DELETE FROM ENROLLMENT;
DELETE FROM ACCOUNT;
DELETE FROM STUDENT;
DELETE FROM COURSE;

-- ==========================================================
-- 10 门化学学院课程：前 5 门共享(SHARED='1')，后 5 门不共享
-- ==========================================================
INSERT ALL
    INTO COURSE VALUES ('B0001', '大学化学',     64, 4, '周墨涵', '化学楼-101', '1')
    INTO COURSE VALUES ('B0002', '化学实验基础', 48, 2, '李清华', '化学楼-102', '1')
    INTO COURSE VALUES ('B0003', '有机化学',     64, 4, '陈学文', '化学楼-103', '1')
    INTO COURSE VALUES ('B0004', '有机化学实验', 48, 2, '何明远', '化学楼-104', '1')
    INTO COURSE VALUES ('B0005', '物理化学',     64, 4, '张博文', '化学楼-105', '1')
    INTO COURSE VALUES ('B0006', '物理化学实验', 48, 2, '王雅婷', '化学楼-201', '0')
    INTO COURSE VALUES ('B0007', '仪器分析',     48, 3, '赵子轩', '化学楼-202', '0')
    INTO COURSE VALUES ('B0008', '结构化学',     48, 3, '林晓琳', '化学楼-203', '0')
    INTO COURSE VALUES ('B0009', '高分子导论',   32, 2, '吴志远', '化学楼-204', '0')
    INTO COURSE VALUES ('B0010', '普通物理',     64, 4, '孙宇阳', '化学楼-205', '0')
SELECT * FROM dual;

-- ==========================================================
-- 50 个学生
-- 学号 B24000001..B24000050；姓名来自用户提供的随机名单
-- 性别根据名字推断；专业分布：1-20 化学 / 21-35 应用化学 / 36-45 化学生物学 / 46-50 高分子材料
-- ==========================================================
INSERT ALL
    INTO STUDENT VALUES ('B24000001', '袁涵芳', 'F', '化学',       '123456')
    INTO STUDENT VALUES ('B24000002', '徐欣',   'F', '化学',       '123456')
    INTO STUDENT VALUES ('B24000003', '蒋磊阳', 'M', '化学',       '123456')
    INTO STUDENT VALUES ('B24000004', '彭文煜', 'M', '化学',       '123456')
    INTO STUDENT VALUES ('B24000005', '程焱军', 'M', '化学',       '123456')
    INTO STUDENT VALUES ('B24000006', '龚泽晨', 'M', '化学',       '123456')
    INTO STUDENT VALUES ('B24000007', '杜颖欣', 'F', '化学',       '123456')
    INTO STUDENT VALUES ('B24000008', '蒋晖曦', 'M', '化学',       '123456')
    INTO STUDENT VALUES ('B24000009', '方芳慧', 'F', '化学',       '123456')
    INTO STUDENT VALUES ('B24000010', '赵勇熠', 'M', '化学',       '123456')
    INTO STUDENT VALUES ('B24000011', '覃泽杰', 'M', '化学',       '123456')
    INTO STUDENT VALUES ('B24000012', '江刚明', 'M', '化学',       '123456')
    INTO STUDENT VALUES ('B24000013', '龙刚熠', 'M', '化学',       '123456')
    INTO STUDENT VALUES ('B24000014', '贾彤萱', 'F', '化学',       '123456')
    INTO STUDENT VALUES ('B24000015', '徐萱',   'F', '化学',       '123456')
    INTO STUDENT VALUES ('B24000016', '陈蕾悦', 'F', '化学',       '123456')
    INTO STUDENT VALUES ('B24000017', '周晴嫣', 'F', '化学',       '123456')
    INTO STUDENT VALUES ('B24000018', '史峰斌', 'M', '化学',       '123456')
    INTO STUDENT VALUES ('B24000019', '孟兰',   'F', '化学',       '123456')
    INTO STUDENT VALUES ('B24000020', '潘昊磊', 'M', '化学',       '123456')
    INTO STUDENT VALUES ('B24000021', '黎玲丽', 'F', '应用化学',   '123456')
    INTO STUDENT VALUES ('B24000022', '孟秀琳', 'F', '应用化学',   '123456')
    INTO STUDENT VALUES ('B24000023', '潘晴',   'F', '应用化学',   '123456')
    INTO STUDENT VALUES ('B24000024', '何蕾妍', 'F', '应用化学',   '123456')
    INTO STUDENT VALUES ('B24000025', '孙炜伟', 'M', '应用化学',   '123456')
    INTO STUDENT VALUES ('B24000026', '吕宇',   'M', '应用化学',   '123456')
    INTO STUDENT VALUES ('B24000027', '白勇轩', 'M', '应用化学',   '123456')
    INTO STUDENT VALUES ('B24000028', '范梅欣', 'F', '应用化学',   '123456')
    INTO STUDENT VALUES ('B24000029', '熊雅',   'F', '应用化学',   '123456')
    INTO STUDENT VALUES ('B24000030', '覃丽珊', 'F', '应用化学',   '123456')
    INTO STUDENT VALUES ('B24000031', '蔡睿熠', 'M', '应用化学',   '123456')
    INTO STUDENT VALUES ('B24000032', '赵鑫强', 'M', '应用化学',   '123456')
    INTO STUDENT VALUES ('B24000033', '于薇娜', 'F', '应用化学',   '123456')
    INTO STUDENT VALUES ('B24000034', '覃龙',   'M', '应用化学',   '123456')
    INTO STUDENT VALUES ('B24000035', '龙桂丽', 'F', '应用化学',   '123456')
    INTO STUDENT VALUES ('B24000036', '钟昊亮', 'M', '化学生物学', '123456')
    INTO STUDENT VALUES ('B24000037', '胡芳瑶', 'F', '化学生物学', '123456')
    INTO STUDENT VALUES ('B24000038', '梁霞萱', 'F', '化学生物学', '123456')
    INTO STUDENT VALUES ('B24000039', '谢秀瑶', 'F', '化学生物学', '123456')
    INTO STUDENT VALUES ('B24000040', '邹宇磊', 'M', '化学生物学', '123456')
    INTO STUDENT VALUES ('B24000041', '田萱雅', 'F', '化学生物学', '123456')
    INTO STUDENT VALUES ('B24000042', '田怡欣', 'F', '化学生物学', '123456')
    INTO STUDENT VALUES ('B24000043', '孙琪洁', 'F', '化学生物学', '123456')
    INTO STUDENT VALUES ('B24000044', '孙菊琪', 'F', '化学生物学', '123456')
    INTO STUDENT VALUES ('B24000045', '宋晴莉', 'F', '化学生物学', '123456')
    INTO STUDENT VALUES ('B24000046', '龙峰亮', 'M', '高分子材料', '123456')
    INTO STUDENT VALUES ('B24000047', '韦静丽', 'F', '高分子材料', '123456')
    INTO STUDENT VALUES ('B24000048', '龙晨',   'M', '高分子材料', '123456')
    INTO STUDENT VALUES ('B24000049', '方晨磊', 'M', '高分子材料', '123456')
    INTO STUDENT VALUES ('B24000050', '董雅晴', 'F', '高分子材料', '123456')
SELECT * FROM dual;

-- ==========================================================
-- 250 条选课：每个学生选 5 门（轮转分配）
-- 学生 s 选课 = 课程 ((s-1+c) mod 10)+1，c ∈ 0..4
-- 得分 = 60 + MOD(s*7 + c*13, 41) → 60..100，确定性可复现
-- 用 CONNECT BY 生成 50×5 笛卡尔积，避免 PL/SQL
-- ==========================================================
INSERT INTO ENROLLMENT (CRS_NO, STU_NO, SCORE)
WITH
    student_seq   AS (SELECT LEVEL AS s     FROM dual CONNECT BY LEVEL <= 50),
    course_offset AS (SELECT LEVEL - 1 AS c FROM dual CONNECT BY LEVEL <= 5)
SELECT
    'B'    || LPAD(MOD(s - 1 + c, 10) + 1, 4, '0'),
    'B240' || LPAD(s, 5, '0'),
    60 + MOD(s * 7 + c * 13, 41)
FROM student_seq CROSS JOIN course_offset;

-- ==========================================================
-- 51 个账户：1 管理员 + 50 学生
-- 学生账户 ACC_NO = STU_NO，密码统一 '123456'，level=2
-- 管理员账户 admin/admin123，level=1，STU_NO=NULL
-- ==========================================================
INSERT ALL
    INTO ACCOUNT VALUES ('admin',     'admin123', 1, NULL)
    INTO ACCOUNT VALUES ('B24000001', '123456',   2, 'B24000001')
    INTO ACCOUNT VALUES ('B24000002', '123456',   2, 'B24000002')
    INTO ACCOUNT VALUES ('B24000003', '123456',   2, 'B24000003')
    INTO ACCOUNT VALUES ('B24000004', '123456',   2, 'B24000004')
    INTO ACCOUNT VALUES ('B24000005', '123456',   2, 'B24000005')
    INTO ACCOUNT VALUES ('B24000006', '123456',   2, 'B24000006')
    INTO ACCOUNT VALUES ('B24000007', '123456',   2, 'B24000007')
    INTO ACCOUNT VALUES ('B24000008', '123456',   2, 'B24000008')
    INTO ACCOUNT VALUES ('B24000009', '123456',   2, 'B24000009')
    INTO ACCOUNT VALUES ('B24000010', '123456',   2, 'B24000010')
    INTO ACCOUNT VALUES ('B24000011', '123456',   2, 'B24000011')
    INTO ACCOUNT VALUES ('B24000012', '123456',   2, 'B24000012')
    INTO ACCOUNT VALUES ('B24000013', '123456',   2, 'B24000013')
    INTO ACCOUNT VALUES ('B24000014', '123456',   2, 'B24000014')
    INTO ACCOUNT VALUES ('B24000015', '123456',   2, 'B24000015')
    INTO ACCOUNT VALUES ('B24000016', '123456',   2, 'B24000016')
    INTO ACCOUNT VALUES ('B24000017', '123456',   2, 'B24000017')
    INTO ACCOUNT VALUES ('B24000018', '123456',   2, 'B24000018')
    INTO ACCOUNT VALUES ('B24000019', '123456',   2, 'B24000019')
    INTO ACCOUNT VALUES ('B24000020', '123456',   2, 'B24000020')
    INTO ACCOUNT VALUES ('B24000021', '123456',   2, 'B24000021')
    INTO ACCOUNT VALUES ('B24000022', '123456',   2, 'B24000022')
    INTO ACCOUNT VALUES ('B24000023', '123456',   2, 'B24000023')
    INTO ACCOUNT VALUES ('B24000024', '123456',   2, 'B24000024')
    INTO ACCOUNT VALUES ('B24000025', '123456',   2, 'B24000025')
    INTO ACCOUNT VALUES ('B24000026', '123456',   2, 'B24000026')
    INTO ACCOUNT VALUES ('B24000027', '123456',   2, 'B24000027')
    INTO ACCOUNT VALUES ('B24000028', '123456',   2, 'B24000028')
    INTO ACCOUNT VALUES ('B24000029', '123456',   2, 'B24000029')
    INTO ACCOUNT VALUES ('B24000030', '123456',   2, 'B24000030')
    INTO ACCOUNT VALUES ('B24000031', '123456',   2, 'B24000031')
    INTO ACCOUNT VALUES ('B24000032', '123456',   2, 'B24000032')
    INTO ACCOUNT VALUES ('B24000033', '123456',   2, 'B24000033')
    INTO ACCOUNT VALUES ('B24000034', '123456',   2, 'B24000034')
    INTO ACCOUNT VALUES ('B24000035', '123456',   2, 'B24000035')
    INTO ACCOUNT VALUES ('B24000036', '123456',   2, 'B24000036')
    INTO ACCOUNT VALUES ('B24000037', '123456',   2, 'B24000037')
    INTO ACCOUNT VALUES ('B24000038', '123456',   2, 'B24000038')
    INTO ACCOUNT VALUES ('B24000039', '123456',   2, 'B24000039')
    INTO ACCOUNT VALUES ('B24000040', '123456',   2, 'B24000040')
    INTO ACCOUNT VALUES ('B24000041', '123456',   2, 'B24000041')
    INTO ACCOUNT VALUES ('B24000042', '123456',   2, 'B24000042')
    INTO ACCOUNT VALUES ('B24000043', '123456',   2, 'B24000043')
    INTO ACCOUNT VALUES ('B24000044', '123456',   2, 'B24000044')
    INTO ACCOUNT VALUES ('B24000045', '123456',   2, 'B24000045')
    INTO ACCOUNT VALUES ('B24000046', '123456',   2, 'B24000046')
    INTO ACCOUNT VALUES ('B24000047', '123456',   2, 'B24000047')
    INTO ACCOUNT VALUES ('B24000048', '123456',   2, 'B24000048')
    INTO ACCOUNT VALUES ('B24000049', '123456',   2, 'B24000049')
    INTO ACCOUNT VALUES ('B24000050', '123456',   2, 'B24000050')
SELECT * FROM dual;

COMMIT;

-- ==========================================================
-- 验证：跑完应当看到 50 / 10 / 250 / 51
-- ==========================================================
SELECT 'STUDENT'    AS T, COUNT(*) AS CNT FROM STUDENT
UNION ALL SELECT 'COURSE',     COUNT(*) FROM COURSE
UNION ALL SELECT 'ENROLLMENT', COUNT(*) FROM ENROLLMENT
UNION ALL SELECT 'ACCOUNT',    COUNT(*) FROM ACCOUNT;
