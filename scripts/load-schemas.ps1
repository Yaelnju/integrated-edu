# integrated-edu — 一键灌三院 schema + seed
# 前提：scripts\start-dbs.ps1 已起 oracle-xe / mysql / sqlserver 三个容器，且全部 ready。
# 用法：powershell -ExecutionPolicy Bypass -File scripts\load-schemas.ps1
#
# 三种 DB 客户端在各自容器里的调用方式：
#   SQL Server (2022)   /opt/mssql-tools18/bin/sqlcmd -C -S localhost -U sa -P '<pwd>' -i <file>
#   Oracle (23ai free)  sqlplus system/<pwd>@FREEPDB1  @<file>
#   MySQL (8)           mysql -uroot -p<pwd> < <file>

$ErrorActionPreference = "Stop"

function Run-In-Container {
    param([string]$container, [string]$cmd, [string]$label)
    Write-Host ""
    Write-Host "[$label] $cmd"
    docker exec $container bash -lc $cmd
    if ($LASTEXITCODE -ne 0) {
        throw "[$label] 失败 (exit=$LASTEXITCODE)"
    }
}

function Load-SqlServer {
    Write-Host "============================================="
    Write-Host "College A — SQL Server (CollegeA_DB)"
    Write-Host "============================================="
    docker cp ..\college-a\sql\01_schema.sql sqlserver:/tmp/a1.sql
    docker cp ..\college-a\sql\02_seed.sql   sqlserver:/tmp/a2.sql
    $tool = "/opt/mssql-tools18/bin/sqlcmd"
    Run-In-Container "sqlserver" "$tool -C -S localhost -U sa -P 'Sql@123456' -i /tmp/a1.sql" "A schema"
    Run-In-Container "sqlserver" "$tool -C -S localhost -U sa -P 'Sql@123456' -i /tmp/a2.sql" "A seed"
}

function Load-Oracle {
    Write-Host "============================================="
    Write-Host "College B — Oracle (FREEPDB1, user collegeb)"
    Write-Host "============================================="
    # 先用 system 账号创建 collegeb 用户（若已存在则跳过）
    $createUser = @"
ALTER SESSION SET CONTAINER = FREEPDB1;
DECLARE u NUMBER;
BEGIN
  SELECT COUNT(*) INTO u FROM dba_users WHERE username='COLLEGEB';
  IF u = 0 THEN
    EXECUTE IMMEDIATE 'CREATE USER collegeb IDENTIFIED BY collegeb123';
    EXECUTE IMMEDIATE 'GRANT CONNECT, RESOURCE, UNLIMITED TABLESPACE TO collegeb';
  END IF;
END;
/
EXIT;
"@
    $createUser | docker exec -i oracle-xe bash -lc 'sqlplus -S system/oracle123@FREEPDB1'
    if ($LASTEXITCODE -ne 0) { throw "[B 建用户] 失败" }

    docker cp ..\college-b\sql\01_schema.sql oracle-xe:/tmp/b1.sql
    docker cp ..\college-b\sql\02_seed.sql   oracle-xe:/tmp/b2.sql
    Run-In-Container "oracle-xe" "echo 'EXIT' | sqlplus -S collegeb/collegeb123@FREEPDB1 @/tmp/b1.sql" "B schema"
    Run-In-Container "oracle-xe" "echo 'EXIT' | sqlplus -S collegeb/collegeb123@FREEPDB1 @/tmp/b2.sql" "B seed"
}

function Load-MySql {
    Write-Host "============================================="
    Write-Host "College C — MySQL (college_c_edu)"
    Write-Host "============================================="
    docker cp ..\college-c\sql\01_schema.sql mysql:/tmp/c1.sql
    docker cp ..\college-c\sql\02_seed.sql   mysql:/tmp/c2.sql
    Run-In-Container "mysql" "mysql -uroot -p123456 < /tmp/c1.sql" "C schema"
    Run-In-Container "mysql" "mysql -uroot -p123456 college_c_edu < /tmp/c2.sql" "C seed"
}

# ---- 执行 ----
Load-SqlServer
Load-Oracle
Load-MySql

Write-Host ""
Write-Host "============================================="
Write-Host "全部完成。期望每院 50 学生 / 10 课程 / 250 选课。"
Write-Host "下一步：见 scripts\start-all.md 启动 4 个 Java 进程。"
Write-Host "============================================="
