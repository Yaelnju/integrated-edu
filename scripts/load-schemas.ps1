# integrated-edu - load schema and seed data for three colleges
# Prerequisite: scripts\start-dbs.ps1 has started oracle-xe / mysql / sqlserver.
# Usage: powershell -ExecutionPolicy Bypass -File scripts\load-schemas.ps1
#
# DB clients inside containers:
#   SQL Server (2022)   /opt/mssql-tools18/bin/sqlcmd -C -S localhost -U sa -P '<pwd>' -i <file>
#   Oracle (23ai free)  sqlplus system/<pwd>@FREEPDB1  @<file>
#   MySQL (8)           mysql -uroot -p<pwd> < <file>

$ErrorActionPreference = "Stop"
$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path

function Run-In-Container {
    param([string]$container, [string]$cmd, [string]$label)
    Write-Host ""
    Write-Host "[$label] $cmd"
    docker exec $container bash -lc $cmd
    if ($LASTEXITCODE -ne 0) {
        throw "[$label] failed (exit=$LASTEXITCODE)"
    }
}

function Load-SqlServer {
    Write-Host "============================================="
    Write-Host "College A - SQL Server (CollegeA_DB)"
    Write-Host "============================================="
    docker cp (Join-Path $RepoRoot "college-a\sql\01_schema.sql") sqlserver:/tmp/a1.sql
    docker cp (Join-Path $RepoRoot "college-a\sql\02_seed.sql")   sqlserver:/tmp/a2.sql
    $tool = "/opt/mssql-tools18/bin/sqlcmd"
    Run-In-Container "sqlserver" "$tool -C -S localhost -U sa -P 'Sql@123456' -i /tmp/a1.sql" "A schema"
    Run-In-Container "sqlserver" "$tool -C -S localhost -U sa -P 'Sql@123456' -i /tmp/a2.sql" "A seed"
}

function Load-Oracle {
    Write-Host "============================================="
    Write-Host "College B - Oracle (FREEPDB1, user collegeb)"
    Write-Host "============================================="
    # Create collegeb user if it does not exist.
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
    if ($LASTEXITCODE -ne 0) { throw "[B create user] failed" }

    docker cp (Join-Path $RepoRoot "college-b\sql\01_schema.sql") oracle-xe:/tmp/b1.sql
    docker cp (Join-Path $RepoRoot "college-b\sql\02_seed.sql")   oracle-xe:/tmp/b2.sql
    Run-In-Container "oracle-xe" "echo 'EXIT' | sqlplus -S collegeb/collegeb123@FREEPDB1 @/tmp/b1.sql" "B schema"
    Run-In-Container "oracle-xe" "echo 'EXIT' | sqlplus -S collegeb/collegeb123@FREEPDB1 @/tmp/b2.sql" "B seed"
}

function Load-MySql {
    Write-Host "============================================="
    Write-Host "College C - MySQL (college_c_edu)"
    Write-Host "============================================="
    docker cp (Join-Path $RepoRoot "college-c\sql\01_schema.sql") mysql:/tmp/c1.sql
    docker cp (Join-Path $RepoRoot "college-c\sql\02_seed.sql")   mysql:/tmp/c2.sql
    Run-In-Container "mysql" "mysql --default-character-set=utf8mb4 -uroot -p123456 < /tmp/c1.sql" "C schema"
    Run-In-Container "mysql" "mysql --default-character-set=utf8mb4 -uroot -p123456 college_c_edu < /tmp/c2.sql" "C seed"
}

# ---- run ----
Load-SqlServer
Load-Oracle
Load-MySql

Write-Host ""
Write-Host "============================================="
Write-Host "Done. Expected per college: 50 students / 10 courses / 250 enrollments."
Write-Host "Next: see scripts\start-all.md to start the 4 Java processes."
Write-Host "============================================="
