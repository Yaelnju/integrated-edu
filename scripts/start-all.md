# 启动顺序（4 窗口）

> 前提：`scripts\start-dbs.ps1` 三容器全 ready，`scripts\load-schemas.ps1` 已执行成功（每院 50/10/250）。
> JDK ≥ 11，本地实测 JDK 17 OK。Maven 3.9+。

## 1. 每个新窗口先设环境

```powershell
$env:JAVA_HOME = "C:\Users\YaelCheng\scoop\apps\temurin17-jdk\current"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
cd C:\Users\YaelCheng\Programming\DataIntegration\integrated-edu
```

## 2. 按顺序起 4 个窗口

| 顺序 | 窗口 | 命令 | 监听端口 | 输出关键字 |
|---|---|---|---|---|
| ① | 集成服务器 | `mvn -q -pl integration-server exec:java` | 9200 | `[Integration] 监听 9200` |
| ② | College A | `mvn -q -pl college-a exec:java` | 9002 / 9102 | `[CollegeA] 已连接 SQL Server 库: CollegeA_DB` |
| ③ | College B | `mvn -q -pl college-b exec:java` | 9001 / 9101 | `[CollegeB] 已连接 Oracle 库` |
| ④ | College C | `mvn -q -pl college-c exec:java` | 9000 / 9100 | `[CollegeC] 已连接 MySQL 库` |

集成服务器必须 **先** 起；A/B/C 之间不分先后。
A/B/C 任一窗口都会弹出 Swing 登录界面（学生用 `A20240001` / `B240000001` / `C202401`，管理员 `admin`，密码均 `123456`）。

## 3. 验证联调

按 `docs\报告.md` §7.2 表跑 TC1–TC6，每条用例截 2 张图（GUI + DB 表查询），存到 `docs\screenshots\TC{n}_{a|b}.png`。

## 4. 常见故障排查

### 4.1 端口已占
`Address already in use: bind` →
```powershell
netstat -ano | findstr "9200 9002 9102 9001 9101 9000 9100"
taskkill /F /PID <pid>
```
或重启 Docker Desktop / 杀掉残留 `java.exe`。

### 4.2 Docker 还没 ready
- Oracle 起得最慢，首次 90s 才能登录，`docker logs oracle-xe` 出现 `DATABASE IS READY TO USE!` 才能 `load-schemas.ps1`。
- SQL Server 镜像首次拉 1.6 GB，国内可能 ≥10 min。

### 4.3 JDK 版本不对
`Fatal error compiling: 无效的标记: --release` →
父 pom 用 `<release>11</release>`，必须 JDK ≥ 11。先 `java -version` 确认；若 `JAVA_HOME` 仍指 JDK 8，按 §1 重设。

### 4.4 集成服务器 9200 拒绝连接
B/C GUI 点"跨院选课 / 集成统计" 弹 `集成服务不可用: Connection refused` →
确认窗口①还在跑（没被 Ctrl+C），`Test-NetConnection 127.0.0.1 -Port 9200` 应 True。

### 4.5 DB 客户端连不上
- SQL Server：`sqlcmd ... -C` 必须有 `-C`（trust server cert）。`-S localhost` 在容器里指向 SQL Server 自身。
- Oracle：必须用 `@FREEPDB1` service name（不是 SID）。`collegeb/collegeb123` 是 `load-schemas.ps1` 创建的应用账号。
- MySQL：seed 脚本不再 `USE college_c_edu`，load-schemas.ps1 在 `mysql ... college_c_edu < ...` 时已选库。

### 4.6 跨院 FK 报错
跨院选课后 `Cannot add or update a child row: a foreign key constraint fails (Sno → student)` 表示 `ensureStudentForCross` 未生效。
确认是 **集成服务器（窗口①）→ A 的 XML 端口 9102** 路径上的 `ENROLL`，A 的 `XmlTcpServer.serveEnroll` 已在 F9 时调 `ensureStudentForCross`；B/C 同理。
若仍报错：查看 A/B/C 窗口里的具体 SQL 异常，多半是 schema 没建好（重跑 `load-schemas.ps1`）。

## 5. 优雅关闭

每窗口 `Ctrl+C` 即可。再起一次时若提示 `[skip] 容器 X 已存在`，那是 `start-dbs.ps1` 的预期行为，无需处理。重置 DB：

```powershell
docker rm -f oracle-xe mysql sqlserver
.\scripts\start-dbs.ps1
.\scripts\load-schemas.ps1
```
