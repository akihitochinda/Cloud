package persistence;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

public final class DynamoDbWriteMetrics {
  private static final AtomicLong unprocessedItems = new AtomicLong();
  private static final AtomicLong retryAttempts = new AtomicLong();
  private static final AtomicLong batchWriteRetries = new AtomicLong();
  private static final AtomicLong systemErrors = new AtomicLong();
  private static final AtomicLong throttleErrors = new AtomicLong();

  private DynamoDbWriteMetrics() {}

  public static void recordUnprocessedItems(int count) {
    unprocessedItems.addAndGet(count);
  }

  public static void recordRetry() {
    retryAttempts.incrementAndGet();
  }

  public static void recordBatchWriteRetry() {
    batchWriteRetries.incrementAndGet();
  }

  public static void recordSystemError() {
    systemErrors.incrementAndGet();
  }

  public static void recordThrottleError() {
    throttleErrors.incrementAndGet();
  }

  public static void recordDynamoDbFailure(Exception failure) {
    if (!(failure instanceof DynamoDbException)) {
      return;
    }

    DynamoDbException dynamoDbFailure = (DynamoDbException) failure;
    String errorCode = dynamoDbFailure.awsErrorDetails() == null
        ? ""
        : dynamoDbFailure.awsErrorDetails().errorCode();
    String normalizedCode = errorCode == null ? "" : errorCode.toLowerCase(Locale.ROOT);

    if (dynamoDbFailure.statusCode() >= 500) {
      recordSystemError();
    }
    if (dynamoDbFailure.statusCode() == 429
        || normalizedCode.contains("throttl")
        || normalizedCode.contains("provisionedthroughputexceeded")
        || normalizedCode.contains("requestlimitexceeded")) {
      recordThrottleError();
    }
  }

  public static long getUnprocessedItems() {
    return unprocessedItems.get();
  }

  public static long getRetryAttempts() {
    return retryAttempts.get();
  }

  public static long getBatchWriteRetries() {
    return batchWriteRetries.get();
  }

  public static long getSystemErrors() {
    return systemErrors.get();
  }

  public static long getThrottleErrors() {
    return throttleErrors.get();
  }
}
