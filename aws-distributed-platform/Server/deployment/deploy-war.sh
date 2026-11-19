#!/bin/bash

INGEST_IP=$1
KEY=$2

if [ $# -ne 2 ]; then
    echo "Usage (Run the script from server-v2 project):"
    echo "./deploy-war.sh YOUR_EC2_INGEST_IP your-key.pem"
    exit 1
fi

echo "[INFO] Deploying WAR to $INGEST_IP"

mvn clean package || {
    echo "[ERROR] Maven build failed"
    exit 1
}

scp -i "$KEY" target/skitrak-server-1.0-SNAPSHOT.war ec2-user@"$INGEST_IP":/tmp/ || {
    echo "[ERROR] SCP upload failed"
    exit 1
}

ssh -i "$KEY" ec2-user@"$INGEST_IP" << EOF

sudo rm -rf /opt/tomcat9/webapps/skitrak-server*

sudo cp /tmp/skitrak-server-1.0-SNAPSHOT.war /opt/tomcat9/webapps/skitrak-server.war

# sudo chown tomcat:tomcat /opt/tomcat9/webapps/skitrak-server.war

/opt/tomcat9/bin/shutdown.sh

sleep 10

pkill -f org.apache.catalina.startup.Bootstrap || true

/opt/tomcat9/bin/startup.sh

sleep 10

curl -f http://"$INGEST_IP":8080/skitrak-server/health

EOF

if [ $? -eq 0 ]; then
    echo "[SUCCESS] WAR deployed successfully"
else
    echo "[FAILURE] Tomcat service failed to start"

    exit 1
fi