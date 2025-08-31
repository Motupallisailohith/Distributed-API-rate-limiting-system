@echo off
echo ========================================
echo   STARTING NODE 2 - GATEWAY B
echo   HTTP: 8092  UDP: 7002  Algorithm: %1
echo ========================================
java -cp target\classes com.Motupallisailohith.ratelimit.SimpleMain --algorithm=%1 --http.port=8092 --udp.port=7002 --config=config/demo-nodes.yml

