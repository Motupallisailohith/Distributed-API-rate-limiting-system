@echo off
echo ========================================
echo   STARTING NODE 3 - GATEWAY C
echo   HTTP: 8093  UDP: 7003  Algorithm: %1
echo ========================================
java -cp target\classes com.Motupallisailohith.ratelimit.SimpleMain --algorithm=%1 --http.port=8093 --udp.port=7003 --config=config/demo-nodes.yml

