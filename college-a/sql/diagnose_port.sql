-- 在 SSMS 中执行：查看 SQL Server 实际监听的 TCP 端口
EXEC xp_readerrorlog 0, 1, N'Server is listening on';

-- 当前会话使用的地址（若为共享内存则 tcp 列为 NULL）
SELECT local_net_address, local_tcp_port
FROM sys.dm_exec_connections
WHERE session_id = @@SPID;
