#!/bin/bash
pkill -f order-service || true
sleep 2
nohup java -jar /home/ec2-user/order-service.jar > /home/ec2-user/app.log 2>&1 &
echo "Started PID: $!"
