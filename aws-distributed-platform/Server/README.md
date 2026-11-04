### Run instructions for Performance Analysis (Run the client (ingest + subscribe) against the new pipeline.)

#### JAR Deployment in IntelliJ (From Assignment2\consumer Project)

1. Download `Assignment2` from Git Repository URL
2. Change factory.setHost("172.31.x.x"); RabbitMQ EC2 Private IPv4 address with YOUR_EC2_RABBITMQ_PRIVATE_IP in RabbitConsumer class
3. Deployment
   1. Copy JAR deployment script `deploy-consumer.sh` in deployment folder to `consumer` Project
   2. Place your-key.pem file in the same project & Start Consumer EC2 instance
   3. `chmod +x deploy-consumer.sh`
   4. `./deploy-consumer.sh YOUR_EC2_CONSUMER_IP your-key.pem` 

**Expected Result:** [SUCCESS] Consumer deployed successfully

4. (Optional) Replace consumer.service on EC2 with `consumer.service` in deployment folder
   1. `ssh -i your-key.pem ec2-user@YOUR_EC2_CONSUMER_IP`
   2. `sudo nano /etc/systemd/system/consumer.service` (Edit & Write & Save)
   3. `sudo systemctl daemon-reload`

#### WAR Deployment in IntelliJ (From Assignment2\server-v2 Project)

1. Change factory.setHost("172.31.x.x"); RabbitMQ EC2 Private IPv4 address with YOUR_EC2_RABBITMQ_PRIVATE_IP in RabbitMQManager class
2. Deployment
   1. Copy WAR deployment script `deploy-war.sh` in deployment folder to `server-v2` project
   2. Place your-key.pem file in the same project & Start Ingest EC2 instance
   3. `chmod +x deploy-war.sh`
   4. `./deploy-war.sh YOUR_EC2_INGEST_IP your-key.pem`

**Expected Result:** [SUCCESS] WAR deployed successfully

#### Performance Analysis

1. Start RabbitMQ (From Assignment2\server-v2 Project)
   1. Start RabbitMQ EC2 instance
   2. `ssh -i your-key.pem ec2-user@YOUR_EC2_RABBITMQ_IP`
   3. `sudo docker start rabbitmq` (Refer to rabbitmq-setup.md in deployment folder for setups)
   4. Open http://YOUR_EC2_RABBITMQ_IP:15672
   
   **Expected Result:** RabbitMQ login page appears.

   5. Start monitoring script (From your local machine)
      1. Navigate to monitoring script `queue-monitor.py` in monitoring folder
      2. Change RABBITMQ_HOST RabbitMQ EC2 Public IPv4 address with YOUR_RABBITMQ_PUBLIC_IP in queue-monitor.py
      3. `python3 queue-monitor.py`  # macOS
      OR
      `python queue-monitor.py`  # Windows

   6. Start monitoring script (From EC2)
      - Copy monitoring script `system-monitor.sh` in monitoring folder to `server-v2` Project
      - `scp -i your-key.pem system-monitor.sh ec2-user@YOUR_EC2_RABBITMQ_IP:/tmp/` (From your local machine)
      - `cd /tmp/` (From EC2)
      - `chmod +x system-monitor.sh`
      - `nohup ./system-monitor.sh &`
   
   **Expected Result:** Monitoring script starts on RabbitMQ EC2
      - (Optional) `tail -f system-report.csv` (You should see new rows with new timestamps increasing every 5 seconds)

2. Start Consumer (From Assignment2\consumer Project)
   1. Start Consumer EC2 instance
   2. `ssh -i your-key.pem ec2-user@YOUR_EC2_CONSUMER_IP`
   3. (Optional) Restart service to refresh the consumer state (every time you run load test)
      - `sudo systemctl restart consumer` (You should see consumers connection on RabbitMQ console)
      - `sudo systemctl is-active consumer` (Verify it is active)
      - `sudo journalctl -u consumer -n 50 --no-pager` (Verify it is working)
   4. Start monitoring script (From EC2)
      - Copy monitoring script `system-monitor.sh` in monitoring folder to `consumer` Project
      - `scp -i your-key.pem system-monitor.sh ec2-user@YOUR_EC2_CONSUMER_IP:/tmp/` (From your local machine)
      - `cd /tmp/` (From EC2)
      - `chmod +x system-monitor.sh`
      - `nohup ./system-monitor.sh &`
   
   **Expected Result:** Monitoring script starts on Consumer EC2
      - (Optional) `tail -f system-report.csv` (You should see new rows with new timestamps increasing every 5 seconds)

3. Start Ingest Server(s) (From Assignment2\server-v2 Project)
   1. Start Ingest EC2 instance(s)
   2. `ssh -i your-key.pem ec2-user@YOUR_EC2_INGEST_IP`
   3. (Optional) Restart service to reconnect to RabbitMQ (if already started)
      - `/opt/tomcat9/bin/shutdown.sh`
      - `/pkill -f org.apache.catalina.startup.Bootstrap`
      - `/opt/tomcat9/bin/startup.sh` (You should see channels connection on RabbitMQ console)
   4. Start monitoring script (From EC2)
      - `scp -i your-key.pem system-monitor.sh ec2-user@YOUR_EC2_INGEST_IP:/tmp/` (From your local machine)
      - `cd /tmp/` (From EC2)
      - `chmod +x system-monitor.sh`
      - `nohup ./system-monitor.sh &`
   
   **Expected Result:** Monitoring script starts on Ingest EC2
      - (Optional) `tail -f system-report.csv` (You should see new rows with new timestamps increasing every 5 seconds)

4. Run Load Test (From Assignment1\client-part1 Project)
   1. Add Application Configuration
      1. **Run** → **Edit Configurations**
      2. Click **+** → **Application**
         - **Name:** `Main`
         - **Main class** → **Browse**→ **Main** (skitrak-client)
         - **Program arguments:** `http://YOUR_ALB_DNS/skitrak-server`
      3. Click **OK**

   2. Click **Run** button (green triangle)
   3. IntelliJ should:
      - Start Main console
      - Start sending swipes via RabbitMQ console
      - Start Performance Analysis (printed on completion)
      - Start Statistical Analysis (printed on completion)

   Expected Result: Your rabbitmq-report.csv report stores in Assignment2\monitoring folder and system-report.csv in YOUR_EC2_IP/tmp folder

   4. Stop monitoring
      - `pkill -f system-monitor.sh` (From EC2)
      - ctrl + c queue-monitor.py (From your local machine)

5. Export the system-report.csv from EC2 to local folder (tmp folder content will be deleted once restart EC2)
   1. `scp -i your-key.pem ec2-user@YOUR_EC2_IP:/tmp/system-report.csv ./Assignment2/monitoring/` (From your local machine)