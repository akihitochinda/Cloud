Load balancer: cs6650-assignment2-alb
Scheme: Internet-facing
IP type: IPv4
Listeners: HTTP 80

Target Group: cs6650-ingest-tg
Protocol: HTTP
Port: 8080
Zone: us-west-2b (usw2-az2)

Health Check Path:
/skitrak-server/health

Health Check Configration:
Interval = 30
Timeout = 5
Healthy = 2
Unhealthy = 3

Idle Timeout = 120 seconds

Stickiness = Enabled