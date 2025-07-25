#!/usr/bin/env bash
#
# Fires 120 requests over ~1 minute, alternating between Node A (8080) and Node B (8081).
# No floating-point math in shell expansions – just a literal 0.5s sleep.

API_KEY="test-key"
TOTAL=120
SLEEP_SEC="0.5"
NODE1=8080
NODE2=8081

echo "Starting rate-limit test: $TOTAL requests (0.5s apart)..."

for i in $(seq 1 $TOTAL); do
  # Alternate target port
  if (( i % 2 == 0 )); then
    PORT=$NODE1
  else
    PORT=$NODE2
  fi

  STATUS=$(curl -s -o /dev/null -w '%{http_code}' \
    -H "X-Api-Key: $API_KEY" \
    http://localhost:$PORT/api/data)

  echo "Req#$i → Node:$PORT → HTTP $STATUS"
  # Sleep half a second – this works in Git-bash & Linux
  sleep $SLEEP_SEC
done
