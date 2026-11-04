#!/bin/bash

OUTPUT=system-report.csv

echo "timestamp,cpu_percent,memory_mb" > $OUTPUT

while true
do

CPU=$(top -bn1 | grep "Cpu(s)" | awk '{print $2+$4}')

MEM=$(free -m | awk '/Mem:/ {print $3}')

echo "$(date +%s),$CPU,$MEM" >> $OUTPUT

sleep 5

done