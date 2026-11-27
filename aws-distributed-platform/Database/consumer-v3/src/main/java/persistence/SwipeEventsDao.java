package persistence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import model.SwipeEvent;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BatchWriteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.BatchWriteItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutRequest;
import software.amazon.awssdk.services.dynamodb.model.WriteRequest;

public class SwipeEventsDao {
  private static final String TABLE_NAME = "SwipeEvents";
  private static final int MAX_BATCH_WRITE_ATTEMPTS = 5;
  private static final long INITIAL_RETRY_BACKOFF_MS = 50;
  private static final long MAX_RETRY_BACKOFF_MS = 1000;
  private final DynamoDbClient dynamoDb;

  public SwipeEventsDao(DynamoDbClient dynamoDb) {
    this.dynamoDb = dynamoDb;
  }

  public void batchSave(List<SwipeEvent> events) throws InterruptedException {
    if (events == null || events.isEmpty()) {
      return;
    }

    List<WriteRequest> requests = new ArrayList<>();

    for (SwipeEvent event : events) {
      Map<String, AttributeValue> item = new HashMap<>();
      item.put("SkierId", AttributeValue.builder()
              .s(event.getSkierID())
              .build());
      item.put("EventTimestamp", AttributeValue.builder()
              .s(event.getTimestamp())
              .build());
      item.put("EventId", AttributeValue.builder()
              .s(event.getEventId())
              .build());
      item.put("ResortId", AttributeValue.builder()
              .s(event.getResortID())
              .build());
      item.put("DayId", AttributeValue.builder()
              .s(event.getDayID())
              .build());
      item.put("LiftId", AttributeValue.builder()
              .s(event.getLiftID())
              .build());
      item.put("Vertical", AttributeValue.builder()
              .n(String.valueOf(event.getVertical()))
              .build());
      item.put("Time", AttributeValue.builder()
              .n(String.valueOf(event.getTime()))
              .build());
      item.put("ServerId", AttributeValue.builder()
              .s(event.getServerId())
              .build());

      requests.add(WriteRequest.builder()
              .putRequest(PutRequest.builder()
                  .item(item)
                  .build())
              .build());
    }

    for (int i = 0; i < requests.size(); i += 25) {
      int end = Math.min(i + 25, requests.size());
      List<WriteRequest> pending = new ArrayList<>(requests.subList(i, end));
      int attempts = 0;

      while (!pending.isEmpty()) {
        Map<String, List<WriteRequest>> requestItems = new HashMap<>();
        requestItems.put(TABLE_NAME, pending);

        BatchWriteItemRequest request = BatchWriteItemRequest.builder()
            .requestItems(requestItems)
            .build();
        BatchWriteItemResponse response = dynamoDb.batchWriteItem(request);
        List<WriteRequest> unprocessed = response.unprocessedItems().get(TABLE_NAME);
        if (unprocessed == null || unprocessed.isEmpty()) {
          break;
        }

        DynamoDbWriteMetrics.recordUnprocessedItems(unprocessed.size());
        if (attempts + 1 >= MAX_BATCH_WRITE_ATTEMPTS) {
          throw new IllegalStateException(
              "DynamoDB left " + unprocessed.size()
                  + " SwipeEvents writes unprocessed after "
                  + MAX_BATCH_WRITE_ATTEMPTS + " attempts");
        }

        attempts++;
        Thread.sleep(getRetryBackoffMs(attempts));
        DynamoDbWriteMetrics.recordRetry();
        DynamoDbWriteMetrics.recordBatchWriteRetry();
        pending = new ArrayList<>(unprocessed);
      }
    }
  }

  private static long getRetryBackoffMs(int retryNumber) {
    long exponentialDelayMs =
        Math.min(MAX_RETRY_BACKOFF_MS, INITIAL_RETRY_BACKOFF_MS * (1L << (retryNumber - 1)));
    long minimumDelayMs = exponentialDelayMs / 2;
    return ThreadLocalRandom.current().nextLong(minimumDelayMs, exponentialDelayMs + 1);
  }
}