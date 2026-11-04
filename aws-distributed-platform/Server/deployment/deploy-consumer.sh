#!/bin/bash

CONSUMER_IP=$1
KEY=$2

if [ $# -ne 2 ]; then
    echo "Usage (Run the script from consumer project):"
    echo "./deploy-consumer.sh YOUR_EC2_CONSUMER_IP your-key.pem"
    exit 1
fi

echo "[INFO] Deploying consumer to $CONSUMER_IP"

mvn clean package || {
    echo "[ERROR] Maven build failed"
    exit 1
}

scp -i "$KEY" target/skitrak-consumer-1.0-SNAPSHOT.jar ec2-user@"$CONSUMER_IP":/tmp/ || {
    echo "[ERROR] SCP upload failed"
    exit 1
}

ssh -i "$KEY" ec2-user@"$CONSUMER_IP" << EOF

mkdir -p ~/consumer

sudo cp /tmp/skitrak-consumer-1.0-SNAPSHOT.jar ~/consumer/skitrak-consumer.jar

# sudo chown -R ec2-user:ec2-user /home/ec2-user/consumer

sudo systemctl daemon-reload

sudo systemctl enable consumer

sudo systemctl restart consumer

sleep 5

sudo systemctl is-active consumer

EOF

if [ $? -eq 0 ]; then
    echo "[SUCCESS] Consumer deployed successfully"
else
    echo "[FAILURE] Consumer is not running"

    ssh -i "$KEY" ec2-user@"$CONSUMER_IP" "sudo journalctl -u consumer -n 50 --no-pager"

    exit 1
fi