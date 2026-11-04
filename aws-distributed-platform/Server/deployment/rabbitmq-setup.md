# Install via docker
sudo dnf install docker -y
sudo systemctl enable docker
sudo systemctl start docker
sudo usermod -aG docker ec2-user

# Build container
sudo docker run -d \
  --hostname rabbit \
  --name rabbitmq \
  -p 5672:5672 \
  -p 15672:15672 \
  rabbitmq:management

# Verify service
sudo docker ps

# Open
http://YOUR_EC2_RABBITMQ_IP:15672

# Start
sudo docker start rabbitmq

# Restart
sudo docker restart rabbitmq

# Remove for rebuild
sudo docker rm -f rabbitmq