# 院系 B — 化学学院教务系统（Oracle）+ 集成校验模块

南京大学《数据集成》课程作业三人组的 **Student 2（院系 B）** 实现。基于 Java 11 + Oracle XE 23ai +
DOM4J + 纯 TCP socket。和院系 A / C 异构对接，由集成服务器中转跨院选课与统计请求。

## 一、模块定位

| 谁 | 院系 | DBMS | 业务端口 | XML 端口 | 状态 |
|---|---|---|---|---|---|
| Student 1 | A | SQL Server | 9002 | 9102 | 待实现 |
| **Student 2（本仓库）** | **B = 化学学院** | **Oracle 23ai** | **9001** | **9101** | **已完成** |
| Student 3 | C | MySQL | 9000 | 9100 | 已完成（团队参考实现） |
| Student 1（兼） | 集成服务器 | — | 9200 | — | 框架待 Student 1 落地，本仓库提供 `XmlValidator` 校验模块 |

学生专业：化学 / 应用化学 / 化学生物学 / 高分子材料。课程：大学化学、有机化学、物理化学、仪器分析、结构化学、高分子导论 等。

## 二、技术栈

- Java 11（编译/运行均需 ≥ JDK 11；本机用 Temurin 17 验证通过）
- Maven 3.9+
- Oracle Database 23ai Free（推荐 Docker 镜像 `gvenzl/oracle-free:slim`）
- `ojdbc11` 23.5
- DOM4J 2.1.4 + jaxen 2.0 + JAXP（XSD 校验 / XSLT）
- JUnit 5

## 三、快速启动

### 1. 起一个 Oracle

```bash
docker run -d --name oracle-xe -p 1521:1521 \
  -e ORACLE_PASSWORD=oracle123 \
  gvenzl/oracle-free:slim
# 等待镜像 ready（首次启动约 1~2 分钟）
docker logs -f oracle-xe   # 看到 "DATABASE IS READY TO USE!" 即可
```

### 2. 建用户 + 建表 + 灌数据

用 `system / oracle123` 连到 `FREEPDB1`，先建本院用户：

```sql
ALTER SESSION SET CONTAINER = FREEPDB1;
CREATE USER collegeb IDENTIFIED BY collegeb123 QUOTA UNLIMITED ON USERS;
GRANT CONNECT, RESOURCE TO collegeb;
```

然后以 `collegeb / collegeb123` 登入，依次执行：

```
sql/01_schema.sql   -- 建 STUDENT / COURSE / ENROLLMENT / ACCOUNT 四张表
sql/02_seed.sql     -- 50 学生 + 10 课程 + 250 条选课
```

> 字段命名（`STU_NO` / `CRS_NO` / `PERIODS` …）跟课本 表 3-6~3-9 对齐，跟院系 C 故意不同 —— 异构是这门课的考点。

### 3. 编译 & 运行

```bash
# 编译
mvn -q compile

# 启动院系 B（自动起 9001 业务服务、9101 XML 服务、Swing 登录窗）
mvn -q exec:java
```

默认账号（来自 `sql/02_seed.sql`）：

| 账号 | 密码 | 角色 |
|---|---|---|
| `admin` | `123456` | 管理员 |
| `B240000001` ~ `B240000050` | `123456` | 学生 |

### 4. 跑测试

```bash
mvn -q test
```

## 四、目录结构

```
college-b/
├── pom.xml
├── sql/
│   ├── 01_schema.sql        Oracle DDL
│   └── 02_seed.sql          50 学生 / 10 课程 / 250 选课
└── src/main/
    ├── java/cn/nju/dataintegration/
    │   ├── collegeb/
    │   │   ├── CollegeBApplication.java     主入口
    │   │   ├── gui/   (LoginFrame, MainFrame, CollegeBClient)
    │   │   ├── net/   (CollegeBTcpServer:9001, XmlTcpServer:9101)
    │   │   ├── repo/  (CollegeBRepository — JDBC)
    │   │   └── xml/   (DomXmlExporter, XmlSchemaValidator)
    │   ├── integration/validator/
    │   │   └── XmlValidator.java            ← 给集成服务器用的 XSD 校验模块
    │   ├── config/   (AppConfig)
    │   ├── db/       (Db)
    │   └── net/      (CollegeBTcpServer / XmlTcpServer)
    └── resources/
        ├── application.properties
        └── xsd/college-b/    studentB.xsd / classB.xsd / choiceB.xsd
```

---

## 五、给 Student 1（院系 A + 集成服务器）的对接说明

### 5.1 集成服务器需要从 B 拉什么 / 写什么

B 的 **XML 服务端口在 9101**，命令行用 `|` 分隔，XML 用 `<XMLBEGIN>…<XMLEND>` 帧住。
所有协议细节走 `common` 模块里的 `cn.nju.dataintegration.net.XmlFrameProtocol`。

| 命令 | 返回 | 你（集成服务器）什么时候用 |
|---|---|---|
| `GET_STUDENTS\n` | `OK\n<XMLBEGIN>…<XMLEND>` | 汇总全院学生 / 跨院选课校验 |
| `GET_COURSES\n` | 同上 | 给 B 学生展示其它院的共享课，或反过来给 A 学生展示 B 的课 |
| `GET_CHOICES\n` | 同上 | 集成统计：`INTEGRATED_STATS` 汇总 |
| `ENROLL\|sno\|cno\n` | `OK\n` 或 `ERR\|msg\n` | 跨院选课**写回**：A/C 学生选到 B 的共享课时调这个 |

> B 端 `GET_*` 在响应前会先用本地 XSD（`xsd/college-b/*.xsd`）校验；
> 如果 B 的数据有问题（罕见，主要是脏数据/字段超长），会回 `ERR|...`。

### 5.2 B 主动调集成服务器的两个出站请求

集成服务器需要在 9200 端口监听并实现：

| B 发起 | 命令 | 期望响应 | 触发时机 |
|---|---|---|---|
| `CollegeBTcpServer#handleCrossEnroll` | `CROSS_ENROLL\|sno\|cno\|target\n` | `OK\n<XMLBEGIN>结果文本<XMLEND>` 或 `ERR\|msg\n` | B 学生在 GUI 点"跨院选课"，target = `A` 或 `C` |
| `CollegeBTcpServer#notifyIntegrationDrop` | `RECORD_DROP\|sno\|cno\n` | 任意单行（B 不校验，读一行就关闭） | B 学生退课，集成服务器需要登记 |
| `CollegeBTcpServer#handleIntegratedStats` | `STATS_ALL\n` | `OK\n<XMLBEGIN>多行统计文本<XMLEND>` | 任意角色点"集成统计(全院)" |

> 9200 不可达时 B 不会崩溃 —— `INTEGRATED_STATS` / `CROSS_ENROLL` 会把 "集成服务不可用" 透传给 GUI；`RECORD_DROP` 失败则静默（本地退课已生效）。

### 5.3 XSD 校验模块（你可以直接调）

`cn.nju.dataintegration.integration.validator.XmlValidator` 提供：

```java
XmlValidator v = new XmlValidator();
v.validateUnifiedStudents(xml);   // 规范格式 Students
v.validateUnifiedClasses(xml);    // 规范格式 Classes
v.validateUnifiedChoices(xml);    // 规范格式 Choices
v.validateCollegeBStudents(xml);  // B 本地格式
v.validate(xml, "xsd/college-a/studentA.xsd"); // 也支持随便指 classpath XSD 路径
```

校验失败抛 `org.xml.sax.SAXException`，你按需翻译成 `ERR|...` 即可。

### 5.4 跨院选课流程（B → 集成 → A/C）

```
B GUI 选共享课
  → CollegeBTcpServer (9001)  CROSS_ENROLL|sno|cno|target
  → 集成服务器 (9200)
       1. 连目标院 XML 端口 GET_COURSES，确认 cno 存在 + shared=1
       2. 拉取学生 XML，本地 / 目标各做 XSD 校验
       3. 走 XSL 变换：B 本地 → 规范 → 目标本地（或省略中间步直接调 ENROLL）
       4. 连目标院 XML 端口 ENROLL|sno|cno 写回
       5. 把结果文本回给 B
```

---

## 六、给 Student 3（院系 C）的对接说明

C 院已经做完了，对 B 来说你的实现就是"参考蓝本"。这里只列 **跟 C 故意不一样、对接时容易踩的地方**。

### 6.1 字段命名

| 概念 | C（MySQL） | B（Oracle） |
|---|---|---|
| 学号 | `student.sno` | `STUDENT.STU_NO` |
| 学号长度 | 9 | 9（`B24000001` 前缀） |
| 课号 | `course.cno` | `COURSE.CRS_NO` |
| **课号长度** | **9** | **5**（课本 表 3-8） |
| 共享标志 | `share` 0/1 | `SHARED` CHAR(1) `'0'/'1'` |
| 选课表 PK | `(sno,cno)` 复合 | `(STU_NO,CRS_NO)` 复合 |

### 6.2 课号长度差异 — 跨院映射的关键

B 的课号是 5 位（如 `C0001` 表示有机化学）。规范格式（你那边的 `formatClass.xsd`）`id` 要求 9 位。

`integration-server/src/main/resources/xsl/integration/classB_to_unified.xsl` 用 `concat('0000', CRS_NO)` 把 B 的 5 位课号
左补 4 个零变成 9 位；反向 `unified_to_classB.xsl` 用 `substring($id9, string-length($id9) - 4)` 恢复。

**注意**：如果集成服务器拿到一条课号不是 `0000?????` 开头的课要写回 B（ENROLL），那不是 B 的课，
应该拒绝或直接路由到正确的目标院。

### 6.3 共享课语义

跟你一致：`SHARED = '1'` 表示对外开放。B 的种子数据里前 3 门标记为共享。

### 6.4 性别字段

B 的 `STUDENT.SEX` 是 `CHAR(1)`，取值 `M / F`，与课本一致（C 院用的是 `男 / 女`？请你看下你 `formatStudent.xsd`）。
如果规范格式预期中文，需要在 XSL 里加映射。当前我假设规范格式直接透传 `M/F`，跟你 `formatStudent.xsd`
的 `sex: xs:string` 一致。

### 6.5 复用情况

以下文件我 **逐字复制了你的实现**，没有改动：
- `cn.nju.dataintegration.net.XmlFrameProtocol`
- 规范格式 XSD 统一由 `integration-server/src/main/resources/xsd/integration/` 维护

如果你后续修了上述任何一个，请同步告诉我。

---

## 七、已知限制 / TODO

- 集成服务器（9200）由 Student 1 负责，本仓库**不**启动它；测试跨院选课需等 9200 就位。
- `application.properties` 里的密码 `collegeb123` 是教学用，部署到任何非本机环境都该改。
- `mvn exec:java` 退出后会有 Oracle JDBC 的 daemon 线程残留，已通过 `cleanupDaemonThreads=false`
  消掉 15 秒等待报红，但 JVM 真正退出可能晚几百毫秒，正常现象。
- 默认 JDK 必须 ≥ 11；本机如果是 JDK 8 会报 `无效的标记: --release`。临时切换：
  ```bash
  set JAVA_HOME=C:\Users\YaelCheng\scoop\apps\temurin17-jdk\current   # Windows cmd
  ```
