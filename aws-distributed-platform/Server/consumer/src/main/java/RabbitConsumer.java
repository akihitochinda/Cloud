import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import leaderboard.Aggregator;
import leaderboard.LeaderboardEntry;
import leaderboard.LeaderboardStore;
import model.SwipeEvent;

public class RabbitConsumer {
  private static final int NUM_RESORT_QUEUES = 10;
  private static final Gson gson = new Gson();
  private static final AtomicLong swipesProcessed = new AtomicLong();
  private static final Set<String> processedEvents = ConcurrentHashMap.newKeySet();
  private static final ConcurrentHashMap<String,String> previousLeaderboards = new ConcurrentHashMap<>();
  private static final ConcurrentHashMap<String, Long> lastPublishTimes = new ConcurrentHashMap<>();

  public static void main(String[] args) throws Exception {

    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("172.31.36.28"); // RabbitMQ EC2 Private IPv4 addresses
    Connection connection = factory.newConnection();
    Channel channel = connection.createChannel();
    channel.basicQos(1);
    System.out.println("Waiting for messages...");

    // Consumers publish leaderboard updates to a fanout exchange;
    channel.exchangeDeclare(
        "leaderboard.fanout",
        "fanout",
        true);

    DeliverCallback callback =
        (consumerTag, delivery) -> {
          String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
          // System.out.println("Received: " + message);

          // Event entry
          SwipeEvent event = gson.fromJson(message, SwipeEvent.class);

          // Idempotent aggregation: dedupe by eventId
          if (!processedEvents.add(event.getEventId())) {
            channel.basicAck(
                delivery.getEnvelope()
                    .getDeliveryTag(),
                false);
            return;
          }

          long total = Aggregator.process(event);
          // System.out.println("Skier " + event.getSkierID() + " total vertical = " + total);
          LeaderboardStore.update(event, total);

          long publishTime = System.currentTimeMillis();
          long lastPublishTime = lastPublishTimes.getOrDefault(event.getResortID() + ":" + event.getDayID(), 0L);

          if (publishTime - lastPublishTime > 1000) { // 1 update/sec
            var top5 = LeaderboardStore.getTop5(event.getResortID(), event.getDayID());
            /* System.out.println("Top 5");
            for (LeaderboardEntry entry : top5) {
              System.out.println("Skier " + entry.getSkierID() + " -> " + entry.getVertical());
            } */

            // Leaderboard information
            Map<String,Object> payload = new HashMap<>();

            payload.put("resortID", event.getResortID());
            payload.put("serverTimestamp", Instant.now().toString());

            List<Map<String, Object>> top = new ArrayList<>();

            int rank = 1;
            for (LeaderboardEntry entry : top5) {
              Map<String, Object> skier = new HashMap<>();

              skier.put("rank", rank++);
              skier.put("skierID", entry.getSkierID());
              skier.put("vertical", entry.getVertical());
              top.add(skier);
            }

            payload.put("top", top);

            String leaderboardState = gson.toJson(top5);
            String previousLeaderboardState = previousLeaderboards.get(event.getResortID() + ":" + event.getDayID());

            if (!leaderboardState.equals(previousLeaderboardState)) { // if state updated
              // Publish via leaderboard fanout
              // every WebSocket server binds a queue and pushes to its local subscribers.
              String leaderboardJson = gson.toJson(payload);
              channel.basicPublish(
                  "leaderboard.fanout",
                  "",
                  null,
                  leaderboardJson.getBytes());
              previousLeaderboards.put(event.getResortID() + ":" + event.getDayID(), leaderboardState);
            }
            lastPublishTimes.put(event.getResortID() + ":" + event.getDayID(), publishTime);
          }

          swipesProcessed.incrementAndGet(); // AtomicLong swipesProcessed;

          channel.basicAck(
              delivery.getEnvelope()
                  .getDeliveryTag(),
              false);
        };

    // Each consumer handles a subset of resorts
    for (int i = 1; i <= NUM_RESORT_QUEUES; i++) {
      // Per-resort ordering: consume each resort's queue so that one resort's swipes are applied in order
      // preserve per-resort order via the per-resort queues
      String queue = "resort." + i;

      channel.basicConsume( // manual ack
          queue,
          false,
          callback,
          consumerTag -> {});
    }

    while (true) {
      Thread.sleep(1000);
    }
  }
}