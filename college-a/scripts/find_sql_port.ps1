# 查看本机 SQL Server 实际 TCP 监听端口
$proc = Get-Process sqlservr -ErrorAction SilentlyContinue | Select-Object -First 1
if (-not $proc) {
    Write-Host "未找到 sqlservr 进程，请先启动 SQL Server (MSSQLSERVER) 服务"
    exit 1
}
Write-Host "SQL Server 进程 PID: $($proc.Id)"
Get-NetTCPConnection -State Listen -OwningProcess $proc.Id |
    Select-Object LocalAddress, LocalPort |
    Format-Table -AutoSize
Write-Host ""
Write-Host "将 application.properties 中 db.url 的端口改为上表中的 LocalPort"
Write-Host "示例: jdbc:sqlserver://localhost:13685;databaseName=CollegeA_DB;..."
