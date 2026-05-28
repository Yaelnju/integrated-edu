# integrated-edu — 三院异构教务系统（XML 数据集成）

南京大学《数据集成》课程作业，三人团队合并提交版本。三个学院各自使用不同 DBMS
（SQL Server / Oracle / MySQL），通过中央集成服务器以 XML + XSD + XSL 完成跨院选课、跨院退课和集成统计。

## 一、模块结构

```
integrated-edu/                 ← Maven 父工程 (packaging=pom)
├── pom.xml                     父 pom：统一 dependency / plugin 版本
├── README.md                   本文件
├── docs/
│   ├── 报告.md                  作业报告
│   └── screenshots/            联调截图（用户驱动）
├── scripts/
│   ├── start-dbs.ps1           起 3 个 Docker DB
│   ├── load-schemas.ps1        灌 schema + seed
│   └── start-all.md            4 窗口启动顺序 + 故障排查
├── integration-server/         集成服务器（独立模块，端口 9200，无 DBMS 依赖）
├── college-a/                  Student 1 — SQL Server
├── college-b/                  Student 2 — Oracle
└── college-c/                  Student 3 — MySQL
```

`mvn -q -pl <module> exec:java` 单跑某模块；`mvn -q install` 编译全部；`mvn -q test` 一次跑全 3 套 11 用例单元测试。

## 二、端口分配

| 模块 | 业务 TCP | XML TCP | DB |
|---|---|---|---|
| integration-server | — | 9200 | — |
| College A | 9002 | 9102 | SQL Server `localhost:1433/CollegeA_DB` |
| College B | 9001 | 9101 | Oracle `localhost:1521/FREEPDB1` |
| College C | 9000 | 9100 | MySQL `localhost:3306/college_c_edu` |

集成服务器命令（端口 9200）：
- `STATS_ALL` — 汇总三院学生/课程/选课数
- `CROSS_ENROLL|sno|cno|target` — 跨院选课
- `INTEGRATED_DROP|sno|cno` — 集成环境退选（源院 + 开课院都删）
- `RECORD_DROP|sno|cno` — 退课审计日志（兼容老接口）

## 三、快速启动（本机 Docker）

### 3.1 起三个 DB

```powershell
# Oracle (College B)
docker run -d --name oracle-xe -p 1521:1521 -e ORACLE_PASSWORD=oracle123 gvenzl/oracle-free:slim

# MySQL (College C)
docker run -d --name mysql -p 3306:3306 -e MYSQL_ROOT_PASSWORD=123456 -e MYSQL_DATABASE=college_c_edu mysql:8

# SQL Server (College A)
docker run -d --name sqlserver -p 1433:1433 -e ACCEPT_EULA=Y -e MSSQL_SA_PASSWORD=Sql@123456 mcr.microsoft.com/mssql/server:2022-latest
```

等待三个容器 ready：

```powershell
docker logs oracle-xe   # 看到 "DATABASE IS READY TO USE!"
docker logs mysql       # 看到 "ready for connections" (在 3306)
docker logs sqlserver   # 看到 "SQL Server is now ready for client connections"
```

### 3.2 建表 + 灌数据

```powershell
powershell -ExecutionPolicy Bypass -File scripts\load-schemas.ps1
```

脚本走 `docker exec` 调三套客户端（sqlcmd / sqlplus / mysql），一键灌 3 院 schema + seed，期望每院 50 学生 / 10 课程 / 250 选课。

手工方式亦可：
- College A：SSMS 或 sqlcmd 连 `sa` 跑两份脚本，库名 `CollegeA_DB`
- College B：先建用户 `collegeb / collegeb123` 并授权，再以该账号执行
- College C：以 root 连接，DB 已自动建好

### 3.3 4 窗口启动应用

```powershell
# 终端 1 — 集成服务器（必须先起）
mvn -q -pl integration-server exec:java

# 终端 2 — College A
mvn -q -pl college-a exec:java

# 终端 3 — College B
mvn -q -pl college-b exec:java

# 终端 4 — College C
mvn -q -pl college-c exec:java
```

详细顺序、JDK 设置、5 类故障排查见 `scripts\start-all.md`。

**启动顺序**：integration-server 必须先于 A/B/C（端口 9200 就绪后，跨院调用才不会被拒）。

### 3.4 默认账号

每院 `sql/02_seed.sql` 都种了 `admin / 123456` 管理员账号，外加 50 个学生 `123456`：

- College A：`A20240001` ~ `A20240050`
- College B：`B240000001` ~ `B240000050`
- College C：`C202401` ~ `C202450`（或类似格式，见各院 README）

## 四、跨院流程示意

### 跨院选课

```
B 学生 ─→ B GUI ─→ B 业务 TCP (9001) ─CROSS_ENROLL|sno|cno|A─→ 集成服务器 (9200)
                                                         ↓
                              GET_COURSES + XSD校验 + XSL变换
                                                         ↓
                            ENROLL|sno|cno → A 的 XML 服务 (9102)
                                                         ↓
                                       A's repo.pickCourse + ensureStudentForCross
```

### 跨院退课

```
B 学生 ─→ B GUI 退课按钮 ─→ B 业务 TCP DROP ─→ 本地删除 ✓
                                              ↓
                                    INTEGRATED_DROP|sno|cno
                                              ↓
                              集成服务器：detectStudentCollege + detectCourseCollege
                                              ↓
                                    源院和开课院都执行 dropGui
```

### 集成统计

```
任意 GUI 集成统计按钮 → 本院 TCP STATS_ALL → 集成服务器
                                          ↓
                          连 A/B/C XML 端口 GET_STUDENTS/COURSES/CHOICES
                                          ↓
                          各院 XML → XSL 变换 → 规范格式 → XSD 校验
                                          ↓
                                  返回汇总文本
```

## 五、技术栈

- Java 11（已用 JDK 17 验证编译）
- Maven 3.9+ 多模块聚合
- DOM4J 2.1.4 + jaxen 2.0
- JAXP（XSD 校验 / XSLT 1.0）
- 纯 TCP socket（无 HTTP、无 Spring）；行命令 + `<XMLBEGIN>/<XMLEND>` 帧
- Swing GUI
- JUnit 5（college-c 有 XsltRoundTripTest）
- JDBC 驱动：mssql-jdbc 12.8 / ojdbc11 23.5 / mysql-connector-j 8.3

## 六、团队分工

| 同学 | 模块 | 主要工作 |
|---|---|---|
| Student 1 | college-a + integration-server | SQL Server schema/seed/repo/GUI；**集成服务器主框架**（CrossEnrollService / StatsAggregator / XsltTransformer / RemoteCollegeClient）；A/B/C 三向 XSL 全套；报告 Mermaid 流程图骨架 |
| Student 2 | college-b + integration-server/XmlValidator | Oracle schema/seed/repo/GUI；XSD 校验组件 `XmlValidator`（集成服务器 StatsAggregator 调用）；B↔规范格式 XSL |
| Student 3 | college-c | MySQL schema/seed/repo/GUI；首版 `XmlFrameProtocol`（A/B 复用）；JUnit XSL 往返测试；首版规范格式 XSD |
| 全员 | 合并联调 | Maven 4 模块聚合；跨模块接口对接；集成服务器拆分独立部署；联调用例 |

## 七、合并阶段的接口对齐变更（来自 `integrated-edu` 初始 commit）

合并三人独立工程时做了以下接口拉齐，详细见各文件 git blame：

1. **F1** `college-c/CollegeCApplication.java`：注释掉 C 自启的 `IntegrationTcpServer`，集成服务器由 A 独占（避免端口 9200 冲突）。
2. **F2** `college-c/CollegeCTcpServer.java`：新增 `handleCrossEnroll`；C 学生现在也能跨院选课。
3. **F2** `college-c/MainFrame.java`：新增"跨院选课"输入框 + JComboBox（目标 A/B）+ 按钮。
4. **F5** `college-b/CollegeBTcpServer.java` 和 `college-c/CollegeCTcpServer.java`：本地 DROP 通知集成服务器的命令从 `RECORD_DROP` 升级为 `INTEGRATED_DROP`，与 A 对齐，自动触发跨院退课。
5. **F9** `college-b/CollegeBRepository.java` 和 `college-c/CollegeCRepository.java`：新增 `ensureStudentForCross(sno)`，跨院学生写回前自动建占位学生记录，避免 FK 失败。
6. **Maven** 父 pom 用 `<dependencyManagement>` 统一 dom4j / jaxen / JUnit 版本；子 pom 改用 `<parent>` 继承。
7. **Split-IS** 集成服务器从 college-a 内嵌拆出，独立成第 4 个 Maven 模块 `integration-server/`，无 JDBC 依赖；删除 college-c 残留的 integration 死代码。A 离线不再影响集成功能。

## 八、已知限制

- JDK 必须 ≥ 11。Windows 默认 JAVA_HOME 若指向 JDK 8 会报 `--release` 错；切到 17/21：
  `set JAVA_HOME=C:\path\to\jdk17` 后再 `mvn ...`
- 集成服务器需要 A 模块在线才能跑；A 离线时 B/C 的"跨院选课"和"集成统计"按钮会弹出"集成服务不可用"。
- SQL Server Docker 镜像（mcr.microsoft.com/mssql/server）国内可能拉取较慢，必要时换 SQL Server Express 本机安装。
- 应用启动顺序：A 必须先于 B/C；否则 B/C 的初始化不会失败（集成功能延迟可用即可）。

## 九、报告

报告在 `docs/报告.md`，包含数据集成关键流程图（Mermaid）+ XSD/XSL 设计说明 +
联调截图。导出 PDF 可用 Typora 或 Pandoc。
