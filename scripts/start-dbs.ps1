# integrated-edu - start three Docker DB containers
# Usage (PowerShell): powershell -ExecutionPolicy Bypass -File scripts\start-dbs.ps1
# Existing containers are started again. To reset, run:
# docker rm -f oracle-xe mysql sqlserver

$ErrorActionPreference = "Stop"

function Ensure-Container($name, [string[]] $runArgs) {
    $existing = docker ps -a --format "{{.Names}}" | Where-Object { $_ -eq $name }
    if ($existing) {
        Write-Host "[skip] container $name already exists; run 'docker rm -f $name' to reset it"
        docker start $name | Out-Null
    } else {
        Write-Host "[run] docker $($runArgs -join ' ')"
        & docker @runArgs
    }
}

Ensure-Container "oracle-xe" @("run", "-d", "--name", "oracle-xe", "-p", "1521:1521", "-e", "ORACLE_PASSWORD=oracle123", "gvenzl/oracle-free:slim")
Ensure-Container "mysql"     @("run", "-d", "--name", "mysql", "-p", "3306:3306", "-e", "MYSQL_ROOT_PASSWORD=123456", "-e", "MYSQL_DATABASE=college_c_edu", "mysql:8")
Ensure-Container "sqlserver" @("run", "-d", "--name", "sqlserver", "-p", "1433:1433", "-e", "ACCEPT_EULA=Y", "-e", "MSSQL_SA_PASSWORD=Sql@123456", "mcr.microsoft.com/mssql/server:2022-latest")

Write-Host ""
Write-Host "Wait until each DB is ready, then run each school's sql/01_schema.sql and 02_seed.sql."
Write-Host 'Readiness checks: docker logs -f oracle-xe   (DATABASE IS READY TO USE!)'
Write-Host '                docker logs mysql        (ready for connections)'
Write-Host '                docker logs sqlserver    (SQL Server is now ready)'
