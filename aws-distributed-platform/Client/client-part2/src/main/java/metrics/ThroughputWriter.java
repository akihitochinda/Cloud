package metrics;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Map;

public class ThroughputWriter {

  public static void writeThroughputCsv(String fileName) throws Exception {
    try (PrintWriter writer = new PrintWriter(new FileWriter(fileName))) {
      writer.println("bucket,swipes");
      MetricsCollector.throughputBuckets
          .entrySet()
          .stream()
          .sorted(Map.Entry.comparingByKey())
          .forEach(entry -> writer.println(entry.getKey() + "," + entry.getValue().get()));
    }
  }
}