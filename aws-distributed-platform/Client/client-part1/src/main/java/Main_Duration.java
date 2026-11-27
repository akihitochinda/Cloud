import generator.SwipeGenerator;
import java.util.ArrayList;
import java.util.List;
import metrics.MetricsCollector;
import metrics.StatsUtil;
import model.SwipeEvent;
import sender.HttpSender;

import java.net.http.HttpClient;
import java.util.concurrent.*;
import websocket.SubscriberRunner;

public class Main_Duration {

  private static final long TEST_DURATION_MS = 30L * 60 * 1000; // a documented duration (e.g., 30 minutes)
  private static final int WARMUP_THREADS = 32;
  private static final int WARMUP_REQUESTS_PER_THREAD = 1000;
  private static final int MAIN_THREADS = 200;
  private static final int SEASON_ID = 2026;
  private static final int DAY_ID = 1;
  private static final int MAX_RESORT_ID = 10;

  public static void main(String[] args) throws Exception {

    if (args.length != 1) {
      System.out.println("Usage: java Main_Duration <baseUrl>");
      System.out.println("Example: java Main_Duration http://YOUR_EC2_IP:8080/skitrak-server");
      return;
    }
    String baseUrl = args[0];

    // Connection establishment
    for (int resort = 1; resort <= MAX_RESORT_ID; resort++) {
      new Thread(new SubscriberRunner(baseUrl, resort)).start();
    }

    Thread.sleep(5000);

    BlockingQueue<SwipeEvent> queue = new LinkedBlockingQueue<>(100000);
    HttpClient client = HttpClient.newHttpClient();

    // Warmup phase
    int totalWarmupEvents = WARMUP_THREADS * WARMUP_REQUESTS_PER_THREAD;
    System.out.println("Starting warmup...");

    ExecutorService warmupPool = Executors.newFixedThreadPool(WARMUP_THREADS);
    Thread warmupGenerator = new Thread(new SwipeGenerator(queue, totalWarmupEvents, SEASON_ID, DAY_ID, MAX_RESORT_ID));

    long warmupStart = System.currentTimeMillis();
    warmupGenerator.start();

    for(int i = 0; i < WARMUP_THREADS; i++) {
      warmupPool.submit(new HttpSender(queue, client, baseUrl));
    }

    warmupGenerator.join();

    while(
        MetricsCollector.success.get() +
            MetricsCollector.failed.get() < totalWarmupEvents) {
      Thread.sleep(500);
    }

    long warmupTime = System.currentTimeMillis() - warmupStart;
    double warmupThroughput = totalWarmupEvents / (warmupTime/1000.0);

    warmupPool.shutdownNow();
    queue.clear();

    System.out.println("===== WARMUP RESULTS =====");
    System.out.println("Requests: " + totalWarmupEvents);
    System.out.println("Runtime(ms): " + warmupTime + " ms");
    System.out.println("Throughput = " + String.format("%.2f", warmupThroughput) + " req/sec");

    MetricsCollector.reset();

    // Main phase
    System.out.println("Starting " + (TEST_DURATION_MS / 60000) + "-minute duration test...");

    ExecutorService senderPool = Executors.newFixedThreadPool(MAIN_THREADS);
    SwipeGenerator durationGenerator = new SwipeGenerator(queue, TEST_DURATION_MS, SEASON_ID, DAY_ID, MAX_RESORT_ID);
    Thread generator = new Thread(durationGenerator);
    
    long startTime = System.currentTimeMillis();
    generator.start();

    for(int i = 0; i < MAIN_THREADS; i++) {
      senderPool.submit(new HttpSender(queue, client, baseUrl));
    }

    generator.join();
    int generatedSwipes = durationGenerator.getGeneratedEvents();

    while(
        MetricsCollector.success.get() +
            MetricsCollector.failed.get() < generatedSwipes) {
      Thread.sleep(500);
    }

    senderPool.shutdownNow();

    long endTime = System.currentTimeMillis();
    long runtime = endTime-startTime;
    double throughput = MetricsCollector.success.get() / (runtime/1000.0);
    List<Long> connections = new ArrayList<>(MetricsCollector.connectionTimes);
    List<Long> latencyData = new ArrayList<>(MetricsCollector.latencies);
    List<Long> pushLatencyData = new ArrayList<>(MetricsCollector.pushLatencies);

    System.out.println();
    System.out.println("===== DURATION TEST RESULTS =====");
    System.out.println("Generation duration(ms): " + TEST_DURATION_MS);
    System.out.println("Generated: " + generatedSwipes);
    System.out.println("Success: " + MetricsCollector.success.get());
    System.out.println("Failed: " + MetricsCollector.failed.get());
    System.out.println("Runtime including queue drain(ms): " + runtime);
    System.out.println("Throughput: " + String.format("%.2f", throughput) + " req/sec");
    System.out.println("Throughput Per Resort");
    MetricsCollector.resortCounts.forEach((resort, count) -> {
          double resortThroughput = count.get() / (runtime / 1000.0);
          System.out.println("Resort " + resort + ": " + String.format("%.2f", resortThroughput) + " req/sec");
        });

    System.out.println();
    System.out.println("===== WEBSOCKET =====");
    System.out.println("Connections: " + MetricsCollector.websocketConnections.get());
    System.out.println("Reconnects: " + MetricsCollector.reconnects.get());
    System.out.println("Total Leaderboard Updates: " + MetricsCollector.leaderboardMessages.get());
    System.out.println("Leaderboard Updates Per Subscriber:");
    MetricsCollector.subscriberUpdates.forEach(
        (resortId, count) ->
            System.out.println("Resort " + resortId + " Subscriber: " + count.get() + " messages"));
    System.out.println(
        String.format(
            "Connection Establishment Time Mean(ms): %.2f",
            StatsUtil.mean(connections)));

    System.out.println();
    System.out.println("===== LATENCY STATISTICAL ANALYSIS =====");
    if (!latencyData.isEmpty()) {
      System.out.println(String.format("Mean Latency(ms): %.2f", StatsUtil.mean(latencyData)));
      System.out.println("Median Latency(ms): " + StatsUtil.median(latencyData));
      System.out.println("P95 Latency(ms): " + StatsUtil.percentile(latencyData, 0.95));
      System.out.println("P99 Latency(ms): " + StatsUtil.percentile(latencyData, 0.99));
      System.out.println("Min Latency(ms): " + StatsUtil.min(latencyData));
      System.out.println("Max Latency(ms): " + StatsUtil.max(latencyData));
    }
    if (!pushLatencyData.isEmpty()) {
      System.out.println(String.format("Mean Push Latency(ms): %.2f", StatsUtil.mean(pushLatencyData)));
      System.out.println("Median Push Latency(ms): " + StatsUtil.median(pushLatencyData));
      System.out.println("P95 Push Latency(ms): " + StatsUtil.percentile(pushLatencyData, 0.95));
      System.out.println("P99 Push Latency(ms): " + StatsUtil.percentile(pushLatencyData, 0.99));
    }

    System.exit(0);
  }
}
