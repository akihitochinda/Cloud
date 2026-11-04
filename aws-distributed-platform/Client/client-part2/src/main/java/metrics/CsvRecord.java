package metrics;

public class CsvRecord {

  private final long requestStartTime;
  private final long responseTime;
  private final long latency;

  private final int statusCode;
  private final int resortId;

  public CsvRecord(long requestStartTime, long responseTime, long latency,
      int statusCode, int resortId) {

    this.requestStartTime = requestStartTime;
    this.responseTime = responseTime;
    this.latency = latency;
    this.statusCode = statusCode;
    this.resortId = resortId;
  }

  public long getRequestStartTime() {
    return requestStartTime;
  }

  public long getResponseTime() {
    return responseTime;
  }

  public long getLatency() {
    return latency;
  }

  public int getStatusCode() {
    return statusCode;
  }

  public int getResortId() {
    return resortId;
  }
}