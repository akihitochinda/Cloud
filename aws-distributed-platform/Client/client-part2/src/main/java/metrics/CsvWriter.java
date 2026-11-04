package metrics;

import java.io.FileWriter;
import java.io.PrintWriter;

public class CsvWriter {

  public static void writeCsv(String fileName) throws Exception {
    try (
        PrintWriter writer = new PrintWriter(new FileWriter(fileName))) {
      writer.println("timestamp,requestType,latency,statusCode,resortID");

      for (CsvRecord record : MetricsCollector.csvRecords) {
        writer.println(record.getRequestStartTime() + ",SWIPE," + record.getLatency() + "," + record.getStatusCode() + "," + record.getResortId());
      }
    }
  }
}