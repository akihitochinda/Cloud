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
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import leaderboard.Aggregator;
import leaderboard.LeaderboardEntry;
import leaderboard.LeaderboardStore;
import model.SwipeEvent;
import persistence.BatchWriter;
import persistence.DeadLetterQueueWorker;
import persistence.DynamoDbWriteMetrics;
import persistence.LeaderboardSnapshotBuffer;
import persistence.LeaderboardSnapshotWriter;
import persistence.PersistenceBuffer;
import persistence.SnapshotBatch;

public class RabbitConsumer {
  private static final int NUM_RESORT_QUEUES = 10;
  private static final int BATCH_SIZE = 1000;
  private static final int FLUSH_INTERVAL_MS = 1000;
  private static final long WRITE_METRICS_REPORT_INTERVAL_MS = 60_000;
  private static final long DEFAULT_TEST_DURATION_MS = 30L * 60 * 1000; // a documented duration (e.g., 30 minutes)
  private static final Gson gson = new Gson();
  private static final Set<String> processedEvents = ConcurrentHashMap.newKeySet();
  private static final ConcurrentHashMap<String,String> previousLeaderboards = new ConcurrentHashMap<>();
  private static final ConcurrentHashMap<String, Long> lastPublishTimes = new ConcurrentHashMap<>();

  // Make consumer threads configurable (one channel per queue) and scale them with load
  private static int getConsumerCount() {
    long count = getPositiveEnvironmentValue("CONSUMER_COUNT", 1L);
    if (count > NUM_RESORT_QUEUES) {
      throw new IllegalArgumentException(
          "CONSUMER_COUNT must not exceed " + NUM_RESORT_QUEUES);
    }
    return (int) count;
  }

  private static int getPrefetchCount() {
    long prefetchCount = getPositiveEnvironmentValue("RABBITMQ_PREFETCH", 1L);
    if (prefetchCount > Integer.MAX_VALUE) {
      throw new IllegalArgumentException("RABBITMQ_PREFETCH exceeds the supported integer range");
    }
    return (int) prefetchCount;
  }

  private static TestMode getTestMode() {
    String configured = System.getenv("TEST_MODE");
    if (configured == null || configured.isBlank()) {
      return TestMode.COUNT;
    }
    try {
      return TestMode.valueOf(configured.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("TEST_MODE must be COUNT or DURATION", e);
    }
  }

  private static long getTestDurationMs() {
    return getPositiveEnvironmentValue("TEST_DURATION_MS", DEFAULT_TEST_DURATION_MS);
  }

  private static long getPositiveEnvironmentValue(String name, long defaultValue) {
    String configured = System.getenv(name);
    if (configured == null || configured.isBlank()) {
      return defaultValue;
    }
    try {
      long value = Long.parseLong(configured);
      if (value < 1) {
        throw new IllegalArgumentException(name + " must be at least 1");
      }
      return value;
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(name + " must be a positive integer", e);
    }
  }

  private enum TestMode {
    COUNT,
    DURATION
  }

  public static void main(String[] args) throws Exception {

    int consumerCount = getConsumerCount();
    int prefetchCount = getPrefetchCount();
    TestMode testMode = getTestMode();
    long testDurationMs = getTestDurationMs();

    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("172.31.36.28"); // RabbitMQ EC2 Private IPv4 addresses
    Connection connection = factory.newConnection();
    System.out.println(
        "Waiting for messages... consumerCount=" + consumerCount
            + ", prefetch=" + prefetchCount
            + ", resortQueues=" + NUM_RESORT_QUEUES);
    System.out.println("Test mode=" + testMode);
    if (testMode == TestMode.DURATION) {
      System.out.println("Duration measurement window=" + testDurationMs + " ms");
    }

    Function<Channel, DeliverCallback> callbackFactory =
        workerChannel -> (consumerTag, delivery) -> {
          String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
          // System.out.println("Received: " + message);

          // Event entry
          SwipeEvent event = gson.fromJson(message, SwipeEvent.class);

          // Idempotent aggregation: dedupe by eventId
          if (!processedEvents.add(event.getEventId())) {
            workerChannel.basicAck(
                delivery.getEnvelope()
                    .getDeliveryTag(),
                false);
            return;
          }

          long total = Aggregator.process(event);
          // System.out.println("Skier " + event.getSkierID() + " total vertical = " + total);
          LeaderboardStore.update(event, total);

          // DynamoDB
          PersistenceBuffer.enqueue(event);

          long publishTime = System.currentTimeMillis();
          long lastPublishTime = lastPublishTimes.getOrDefault(event.getResortID() + ":" + event.getDayID(), 0L);

          if (publishTime - lastPublishTime > 1000) { // 1 update/sec
            List<LeaderboardEntry> top100 = LeaderboardStore.getTopN(event.getResortID(), event.getDayID(), 100);
            List<LeaderboardEntry> top5 = top100.subList(0, Math.min(5, top100.size()));
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
              workerChannel.basicPublish(
                  "leaderboard.fanout",
                  "",
                  null,
                  leaderboardJson.getBytes());
              previousLeaderboards.put(event.getResortID() + ":" + event.getDayID(), leaderboardState);
            }
            // DynamoDB
            LeaderboardSnapshotBuffer.enqueue(new SnapshotBatch(event.getResortID() + ":" + event.getDayID(), top100));
            lastPublishTimes.put(event.getResortID() + ":" + event.getDayID(), publishTime);
          }

          workerChannel.basicAck(
              delivery.getEnvelope()
                  .getDeliveryTag(),
              false);
        };

    // DynamoDB
    new Thread(new BatchWriter(BATCH_SIZE, FLUSH_INTERVAL_MS)).start();
    new Thread(new LeaderboardSnapshotWriter()).start();

    // Circuit Breaker
    new Thread(new DeadLetterQueueWorker()).start();

    // Each consumer handles a subset of resorts
    for (int worker = 0; worker < consumerCount; worker++) {
      Channel workerChannel = connection.createChannel();
      workerChannel.basicQos(prefetchCount);
      workerChannel.exchangeDeclare(
          "leaderboard.fanout",
          "fanout",
          true);
      DeliverCallback callback = callbackFactory.apply(workerChannel);

      // Per-resort ordering: consume each resort's queue so that one resort's swipes are applied in order
      // preserve per-resort order via the per-resort queues
      for (int resortId = worker + 1; resortId <= NUM_RESORT_QUEUES; resortId += consumerCount) {
        String queue = "resort." + resortId;
        workerChannel.basicConsume(
            queue,
            false, // manual ack
            callback,
            consumerTag -> {});
      }
    }

    // DynamoDB Metrics
    Thread metricsReporter = startDynamoDbMetricsReporter(testMode);

    if (testMode == TestMode.DURATION) {
      long testStartNanos = System.nanoTime();
      while ((System.nanoTime() - testStartNanos) / 1_000_000 < testDurationMs) {
        Thread.sleep(1000);
      }
      long elapsedMs = (System.nanoTime() - testStartNanos) / 1_000_000;
      metricsReporter.interrupt();
      metricsReporter.join();
      printDynamoDbWriteMetrics("window-end", elapsedMs, testMode);
    }

    while (true) {
      Thread.sleep(1000);
    }
  }

  private static Thread startDynamoDbMetricsReporter(TestMode testMode) {
    long reporterStartNanos = System.nanoTime();
    Thread reporter =
        new Thread(
            () -> {
              printDynamoDbWriteMetrics("start", 0, testMode);
              while (true) {
                try {
                  Thread.sleep(WRITE_METRICS_REPORT_INTERVAL_MS);
                } catch (InterruptedException e) {
                  Thread.currentThread().interrupt();
                  return;
                }
                long elapsedMs =
                    (System.nanoTime() - reporterStartNanos) / 1_000_000;
                printDynamoDbWriteMetrics("progress", elapsedMs, testMode);
              }
            },
            "dynamodb-write-metrics-reporter");
    reporter.setDaemon(true);
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  reporter.interrupt();
                  try {
                    reporter.join();
                  } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                  }
                  if (testMode == TestMode.COUNT) {
                    long elapsedMs =
                        (System.nanoTime() - reporterStartNanos) / 1_000_000;
                    printDynamoDbWriteMetrics("shutdown", elapsedMs, testMode);
                  }
                },
                "dynamodb-write-metrics-shutdown"));
    reporter.start();
    return reporter;
  }

  private static void printDynamoDbWriteMetrics(String phase, long elapsedMs, TestMode testMode) {
    System.out.printf(
        Locale.ROOT,
        "DYNAMODB_WRITE_METRICS mode=%s phase=%s scope=process-cumulative elapsedMs=%d "
            + "unprocessedItems=%d retryAttempts=%d "
            + "batchWriteRetries=%d systemErrors=%d throttleErrors=%d%n",
        testMode,
        phase,
        elapsedMs,
        DynamoDbWriteMetrics.getUnprocessedItems(),
        DynamoDbWriteMetrics.getRetryAttempts(),
        DynamoDbWriteMetrics.getBatchWriteRetries(),
        DynamoDbWriteMetrics.getSystemErrors(),
        DynamoDbWriteMetrics.getThrottleErrors());
  }
}