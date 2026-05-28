# integrated-edu — 一键启动三个 Docker DB
# 用法（PowerShell）: powershell -ExecutionPolicy Bypass -File scripts\start-dbs.ps1
# 已经存在同名容器时会跳过；想重置请先 docker rm -f oracle-xe mysql sqlserver

$ErrorActionPreference = "Stop"

function Ensure-Container($name, $runCmd) {
    $existing = docker ps -a --format "{{.Names}}" | Select-String -SimpleMatch $name
    if ($existing) {
        Write-Host "[skip] 容器 $name 已存在；如需重置请 docker rm -f $name"
        docker start $name | Out-Null
    } else {
        Write-Host "[run] $runCmd"
        Invoke-Expression $runCmd
    }
}

Ensure-Container "oracle-xe" "docker run -d --name oracle-xe -p 1521:1521 -e ORACLE_PASSWORD=oracle123 gvenzl/oracle-free:slim"
Ensure-Container "mysql"     "docker run -d --name mysql -p 3306:3306 -e MYSQL_ROOT_PASSWORD=123456 -e MYSQL_DATABASE=college_c_edu mysql:8"
Ensure-Container "sqlserver" "docker run -d --name sqlserver -p 1433:1433 -e ACCEPT_EULA=Y -e MSSQL_SA_PASSWORD=Sql@123456 mcr.microsoft.com/mssql/server:2022-latest"

Write-Host ""
Write-Host "等待 DB ready 后再执行各院 sql/01_schema.sql + 02_seed.sql。"
Write-Host "查看就绪状态：docker logs -f oracle-xe   (DATABASE IS READY TO USE!)"
Write-Host "                docker logs mysql        (ready for connections)"
Write-Host "                docker logs sqlserver    (SQL Server is now ready)"
