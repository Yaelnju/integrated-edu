USE college_c_edu;

INSERT INTO account (acc, passwd) VALUES ('admin', '123456');

INSERT INTO course (Cno, Cnm, Ctm, Cpt, Tec, Pla, Share) VALUES
('C001', '数据结构', 48, 4, '王立新', '仙I-303', '1'),
('C002', '数据库系统', 48, 3, '李敏', '仙II-109', '1'),
('C003', '操作系统', 40, 3, '赵刚', '仙I-205', '0'),
('C004', '计算机网络', 36, 2, '周洋', '仙II-401', '1'),
('C005', '软件工程', 32, 2, '钱程', '仙I-411', '0'),
('C006', '离散数学', 48, 3, '孙悦', '仙II-210', '1'),
('C007', '概率统计', 40, 3, '吴迪', '仙I-317', '0'),
('C008', '人工智能导论', 32, 2, '郑华', '仙II-305', '1'),
('C009', 'Web程序设计', 40, 2, '冯岚', '仙I-502', '0'),
('C010', 'XML与数据集成', 36, 2, '何宁', '仙II-118', '1');

DROP PROCEDURE IF EXISTS seed_c_demo;
DELIMITER $$
CREATE PROCEDURE seed_c_demo()
BEGIN
  DECLARE i INT DEFAULT 1;
  DECLARE j INT;
  WHILE i <= 50 DO
    INSERT INTO student (Sno, Snm, Sex, Sde, Pwd) VALUES (
      CONCAT('C20240', LPAD(i, 3, '0')),
      CONCAT('学生', i),
      IF(i MOD 2 = 1, 'M', 'F'),
      'CS',
      '000000'
    );
    SET j = 1;
    WHILE j <= 5 DO
      INSERT INTO sc (Sno, Cno, Grd) VALUES (
        CONCAT('C20240', LPAD(i, 3, '0')),
        CONCAT('C', LPAD(MOD(i + j - 2, 10) + 1, 3, '0')),
        60 + MOD(i + j, 40)
      );
      SET j = j + 1;
    END WHILE;
    SET i = i + 1;
  END WHILE;
END$$
DELIMITER ;

CALL seed_c_demo();

DROP PROCEDURE IF EXISTS seed_c_demo;
