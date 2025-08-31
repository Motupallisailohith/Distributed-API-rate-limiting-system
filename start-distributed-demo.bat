@echo off
set ALGORITHM=%1
if "%ALGORITHM%"=="" set ALGORITHM=tokenbucket

echo ========================================
echo   DISTRIBUTED RATE LIMITER DEMO
echo   Starting 3 nodes with %ALGORITHM% algorithm
echo ========================================
echo.
echo Node 1: http://localhost:8091 (UDP: 7001)
echo Node 2: http://localhost:8092 (UDP: 7002) 
echo Node 3: http://localhost:8093 (UDP: 7003)
echo Frontend: http://localhost:3000
echo.
echo Starting nodes in 3 seconds...
timeout /t 3 /nobreak > nul

echo Starting Node 1...
start "Node 1 - Gateway A" cmd /k "run-node1.bat %ALGORITHM%"
timeout /t 2 /nobreak > nul

echo Starting Node 2...
start "Node 2 - Gateway B" cmd /k "run-node2.bat %ALGORITHM%"
timeout /t 2 /nobreak > nul

echo Starting Node 3...
start "Node 3 - Gateway C" cmd /k "run-node3.bat %ALGORITHM%"

echo.
echo ========================================
echo   ALL NODES STARTED!
echo ========================================
echo.
echo Test the distributed system:
echo   curl -Method POST -Uri http://localhost:8091/api/data -Headers @{"Authorization"="Bearer demo-token"}
echo   curl -Method POST -Uri http://localhost:8092/api/data -Headers @{"Authorization"="Bearer demo-token"}  
echo   curl -Method POST -Uri http://localhost:8093/api/data -Headers @{"Authorization"="Bearer demo-token"}
echo.
echo Switch algorithms on any node:
echo   curl -Method POST -Uri http://localhost:8091/api/algorithm -Body '{"algorithm":"leakybucket"}' -ContentType "application/json"
echo.
echo View UDP metrics:
echo   curl http://localhost:8091/api/metrics
echo   curl http://localhost:8092/api/metrics
echo   curl http://localhost:8093/api/metrics
echo.
pause

