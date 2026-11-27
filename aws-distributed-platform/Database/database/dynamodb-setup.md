# Install AWS CLI on Consumer EC2
 - `curl -fsSL 'https://awscli.amazonaws.com/v2/install.sh' | bash`
 - `aws --version`

# Attach the DynamoDB Role to Consumer and Ingest(s) EC2
1. Open the EC2 Console. Navigate to EC2 > Instances and select the checkbox next to your Consumer (Ingest) EC2 instance.
2. Modify the IAM role. Click Actions > Security > Modify IAM role.
3. Apply the role. Select 'LabInstanceProfile' from the dropdown menu and click Update IAM role.

# Verify service
 - `aws sts get-caller-identity`

# Create table on Consumer EC2
1. Copy monitoring script `create-tables.sh` in monitoring folder to `consumer-v3` Project
   - `scp -i your-key.pem create-tables.sh ec2-user@YOUR_EC2_CONSUMER_IP:/consumer/` (From your local machine)
   - `cd /consumer/` (From EC2)
   - `chmod +x create-tables.sh`
   - `./create-tables.sh` 
2. Partition
   1. SkierId partition
      - SwipeEvents
      - SkierStats
      - SkierLiftStats
   2. ResortDay partition
      - SkierResortDayStats
      - LeaderboardSnapshot
   3. Aggregate partition (analytics)
      - ActivityWindow
      - TrafficMetrics

# Verify tables
`aws dynamodb list-tables`

# Verify rows and queries
 - `aws dynamodb scan --table-name TABLE_NAME --limit 5`
 - `aws dynamodb query --table-name TABLE_NAME --key-condition-expression "SkierId = :id" --expression-attribute-values '{":id":{"S":"SPECIFIC_SKIER_ID"}}'`
 - `aws dynamodb query --table-name TABLE_NAME --key-condition-expression "ResortDay = :rd" --expression-attribute-values '{":rd":{"S":"SPECIFIC_RESORT_ID#SPECIFIC_DAY_ID"}}'`
 - `aws dynamodb query --table-name TABLE_NAME --key-condition-expression "MinuteBucket = :bucket" --expression-attribute-values '{":bucket":{"S":"yyyy-MM-ddTHH:mm"}}'`
 - `aws dynamodb query --table-name TABLE_NAME --key-condition-expression "TimeBucket = :bucket" --expression-attribute-values '{":bucket":{"S":"yyyy-MM-ddTHH:mm:SS"}}'`

# Test
1. Request Statistics:
 - `curl "http://YOUR_EC2_INGEST_IP:8080/skitrak-server/skiers/stats"`

2. Core queries:
 - `curl "http://YOUR_EC2_INGEST_IP:8080/skitrak-server/skiers/SPECIFIC_SKIER_ID/stats"`
 - `curl "http://YOUR_EC2_INGEST_IP:8080/skitrak-server/skiers/leaderboards/SPECIFIC_RESORT_ID/SPECIFIC_DAY_ID"`
 - `curl "http://YOUR_EC2_INGEST_IP:8080/skitrak-server/skiers/SPECIFIC_SKIER_ID/resorts"`
 - `curl "http://YOUR_EC2_INGEST_IP:8080/skitrak-server/skiers/active?start=yyyy-MM-ddTHH:mm&end=yyyy-MM-ddTHH:mm"`

3. Analytics queries:
 - `curl "http://YOUR_EC2_INGEST_IP:8080/skitrak-server/skiers/analytics/swipes?bucket=yyyy-MM-ddTHH:mm:SS"`
 - `curl "http://YOUR_EC2_INGEST_IP:8080/skitrak-server/skiers/analytics/top-skiers?limit=TOP_N"`
 - `curl "http://YOUR_EC2_INGEST_IP:8080/skitrak-server/skiers/analytics/top-resorts?limit=TOP_N"`
 - `curl "http://YOUR_EC2_INGEST_IP:8080/skitrak-server/skiers/SPECIFIC_SKIER_ID/participation"`