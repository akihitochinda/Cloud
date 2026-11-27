#!/bin/bash

OUTPUT=system-report.csv

IFACE=$(ip route | awk '/default/ {print $5}')

echo "timestamp,cpu_percent,memory_mb,rx_bytes,tx_bytes" > $OUTPUT

while true
do

CPU=$(top -bn1 | grep "Cpu(s)" | awk '{print $2+$4}')
MEM=$(free -m | awk '/Mem:/ {print $3}')

RX=$(cat /sys/class/net/$IFACE/statistics/rx_bytes)
TX=$(cat /sys/class/net/$IFACE/statistics/tx_bytes)

echo "$(date +%s),$CPU,$MEM,$RX,$TX" >> $OUTPUT

sleep 5

done