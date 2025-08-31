@echo off
echo ========================================
echo   STARTING NODE 1 - GATEWAY A
echo   HTTP: 8091  UDP: 7001  Algorithm: %1
echo ========================================
java -cp target\classes com.Motupallisailohith.ratelimit.SimpleMain --algorithm=%1 --http.port=8091 --udp.port=7001 --config=config/demo-nodes.yml

