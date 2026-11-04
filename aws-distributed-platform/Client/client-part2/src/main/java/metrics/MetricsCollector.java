package metrics;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class MetricsCollector {

  // Ingest metrics
  public static final AtomicInteger success = new AtomicInteger();
  public static final AtomicInteger failed = new AtomicInteger();
  public static final ConcurrentLinkedQueue<Long> latencies = new ConcurrentLinkedQueue<>();
  public static final ConcurrentHashMap<Integer, AtomicInteger> resortCounts = new ConcurrentHashMap<>();

  // WebSocket metrics
  public static AtomicInteger websocketConnections = new AtomicInteger();
  public static AtomicInteger reconnects = new AtomicInteger();
  public static AtomicInteger leaderboardMessages = new AtomicInteger();
  public static final ConcurrentHashMap<Integer, AtomicInteger> subscriberUpdates = new ConcurrentHashMap<>();
  public static ConcurrentLinkedQueue<Long> pushLatencies = new ConcurrentLinkedQueue<>();

  // WebSocket connection times
  public static final ConcurrentLinkedQueue<Long> connectionTimes = new ConcurrentLinkedQueue<>();

  // Test results and analysis
  public static final ConcurrentLinkedQueue<CsvRecord> csvRecords = new ConcurrentLinkedQueue<>();
  public static final ConcurrentHashMap<Long, AtomicInteger> throughputBuckets = new ConcurrentHashMap<>();
  public static volatile long testStartTime;

  public static void reset() {
    success.set(0);
    failed.set(0);
    latencies.clear();
    resortCounts.clear();

    // websocketConnections.set(0);
    // reconnects.set(0);
    leaderboardMessages.set(0);
    subscriberUpdates.clear();
    pushLatencies.clear();

    // connectionTimes.clear();

    csvRecords.clear();
    throughputBuckets.clear();
  }
}