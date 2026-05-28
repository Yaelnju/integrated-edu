# 院系 A — 教务系统（SQL Server）+ 集成服务器（XML 集成路线1）

南京大学《数据集成》课程作业三人组的 **Student 1（院系 A + 集成服务器）** 实现。
与院系 B/C 异构对接，使用 XML + XSD + XSL 完成跨院选课与集成统计。

## 一、模块定位

| 谁 | 院系 | DBMS | 业务端口 | XML 端口 | 状态 |
|---|---|---|---|---|---|
| **Student 1（本仓库）** | **A** | **SQL Server** | **9002** | **9102** | **已完成** |
| Student 2 | B | Oracle | 9001 | 9101 | 已完成 |
| Student 3 | C | MySQL | 9000 | 9100 | 已完成 |
| Student 1（兼） | 集成服务器 | — | **9200** | — | 已完成 |

## 二、技术栈

- Java 11（编译/运行需 ≥ JDK 11）
- Maven 3.9+
- SQL Server + `mssql-jdbc` 12.8
- DOM4J 2.1.4 + jaxen 2.0 + JAXP（XSD 校验 / XSLT）
- Swing GUI + 纯 TCP socket（`XmlFrameProtocol`）
- JUnit 5

## 三、快速启动

### 1. 安装 SQL Server 并建库

务必使用 **真实 SQL Server**（Express/Developer 皆可），安装时启用 **混合模式** 并设置 `sa` 密码。

详细步骤见 **[sql/README.md](sql/README.md)**，核心流程：

1. 在 SSMS 中依次执行：
   - `sql/01_schema.sql`（建 Student / Course / SC / Account）
   - `sql/02_seed.sql`（50 学生 + 10 课程 + 250 选课）
2. 复制 `src/main/resources/application.properties.example` 为 `application.properties`
3. 填写本机 SQL Server 连接信息

可选：测试连接

```powershell
mvn -q compile exec:java -Dexec.mainClass=cn.nju.dataintegration.tools.DbConnectionTest
```

### 2. 编译 & 运行

```powershell
cd DataIntegration-collegeA
mvn -q compile
mvn -q exec:java
```

将同时启动：

1. **集成服务器** `9200`（`CROSS_ENROLL` / `STATS_ALL` / `INTEGRATED_DROP`）
2. **院系 A 业务** `9002`
3. **院系 A XML** `9102`
4. **Swing 登录窗**

默认账号（来自 `sql/02_seed.sql`）：

| 账号 | 密码 | 角色 |
|---|---|---|
| `admin` | `123456` | 管理员 |
| `A20240001` ~ `A20240050` | `123456` | 学生 |

### 3. 仅启动集成服务器

```powershell
mvn -q exec:java -Dexec.mainClass=cn.nju.dataintegration.integration.IntegrationApplication
```

### 4. 跑测试

```powershell
mvn -q test
```

## 四、目录结构

```
college-a/
├── pom.xml
├── sql/
│   ├── 01_schema.sql        SQL Server DDL
│   └── 02_seed.sql          50 学生 / 10 课程 / 250 选课
└── src/main/
	├── java/cn/nju/dataintegration/
	│   ├── collegea/         (CollegeAApplication, GUI, TCP, Repo)
	│   ├── config/           (AppConfig)
	│   ├── db/               (Db)
	│   └── tools/            (DbConnectionTest)
	└── resources/
		├── application.properties
		├── application.properties.example
		└── xsd/college-a/    本院 schema
```

## 五、给 B/C 的对接说明

### 5.1 A 的 XML 服务端口（9102）

命令用 `|` 分隔，XML 用 `<XMLBEGIN>…<XMLEND>` 帧住。协议工具：
`common` 模块中的 `cn.nju.dataintegration.net.XmlFrameProtocol`（三院共用）。

| 命令 | 返回 | 你什么时候用 |
|---|---|---|
| `GET_STUDENTS\n` | `OK\n<XMLBEGIN>…<XMLEND>` | 集成统计 / 跨院校验 |
| `GET_COURSES\n` | 同上 | 共享课拉取 |
| `GET_CHOICES\n` | 同上 | 统计汇总 |
| `ENROLL\|sno\|cno\n` | `OK\n` 或 `ERR\|msg\n` | 跨院选课写回 |

### 5.2 集成服务器端口（9200）

集成服务器由本仓库启动，B/C 需要按以下命令接入：

| 命令 | 说明 |
|---|---|
| `STATS_ALL` | 三院学生/课程/选课汇总 |
| `CROSS_ENROLL\|sno\|cno\|target` | 跨院选课（目标院 A/B/C） |
| `INTEGRATED_DROP\|sno\|cno` | 集成环境退选（删源院+开课院） |
| `RECORD_DROP\|sno\|cno` | 退课审计日志（兼容 B 院通知） |

### 5.3 异构差异（关键字段）

| 概念 | A（SQL Server） | B（Oracle） | C（MySQL） |
|---|---|---|---|
| 学号字段 | `Student.Stu_num` | `STUDENT.STU_NO` | `student.Sno` |
| 课号 | 9 位 | 5 位 | 4 位 |
| 共享标志 | `Cou_share` | `SHARED` | `Share` |

集成层通过 `integration-server/src/main/resources/xsl/integration/` 下的 XSL 映射到统一格式。

## 六、三院联调顺序

```text
终端1: integration-server → mvn -q -pl integration-server exec:java
终端2: college-a          → mvn -q -pl college-a exec:java
终端3: college-b          → mvn -q -pl college-b exec:java
终端4: college-c          → mvn -q -pl college-c exec:java
```

