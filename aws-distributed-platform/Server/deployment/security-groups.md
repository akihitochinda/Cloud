Security Group: cs6650-alb-sg
Inbound: HTTP 80
Source: 0.0.0.0/0

Security Group: cs6650-rabbitmq-sg
Inbound: Custom TCP (RabbitMQ) 5672
Source: cs6650-lab1-sg, cs6650-consumer-sg
Inbound: Custom TCP (RabbitMQ Management) 15672, SSH 22
Source: My IP

Security Group: cs6650-consumer-sg
Inbound: SSH 22
Source: My IP

Security Group: cs6650-lab1-sg
Inbound: SSH 22, HTTP 8080
Source: My IP
Inbound: HTTP 8080
Source: cs6650-alb-sg