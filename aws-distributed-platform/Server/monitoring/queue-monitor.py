import csv
import time
import requests
from requests.auth import HTTPBasicAuth

RABBITMQ_HOST = "YOUR_RABBITMQ_PUBLIC_IP"
USERNAME = "guest"
PASSWORD = "guest"

OUTPUT_FILE = "rabbitmq-report.csv"

with open(OUTPUT_FILE, "w", newline="") as f:

    writer = csv.writer(f)

    writer.writerow([
        "timestamp",
        "queue",
        "depth",
        "ready",
        "unacked",
        "publish_rate",
        "consumer_rate"
    ])

    while True:

        response = requests.get(
            f"http://{RABBITMQ_HOST}:15672/api/queues",
            auth=HTTPBasicAuth(
                USERNAME,
                PASSWORD)
        )

        queues = response.json()

        timestamp = int(time.time())

        for q in queues:

            writer.writerow([
                timestamp,
                q["name"],
                q.get("messages", 0),
                q.get("messages_ready", 0),
                q.get("messages_unacknowledged", 0),
                q.get(
                    "message_stats",
                    {}
                ).get(
                    "publish_details",
                    {}
                ).get(
                    "rate",
                    0
                ),
                q.get(
                    "message_stats",
                    {}
                ).get(
                    "deliver_get_details",
                    {}
                ).get(
                    "rate",
                    0
                )
            ])

        f.flush()

        time.sleep(5)