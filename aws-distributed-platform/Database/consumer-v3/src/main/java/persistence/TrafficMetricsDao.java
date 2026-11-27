package persistence;

import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

public class TrafficMetricsDao {
  private static final String TABLE_NAME = "TrafficMetrics";
  private final DynamoDbClient client;

  public TrafficMetricsDao(DynamoDbClient client) {
    this.client = client;
  }

  public void incrementSecond(String secondBucket) {
    Map<String, AttributeValue> key = new HashMap<>();
    key.put("TimeBucket", AttributeValue.builder()
            .s(secondBucket)
            .build());

    Map<String,String> names = new HashMap<>();
    names.put("#count", "SwipeCount");

    Map<String, AttributeValue> values = new HashMap<>();
    values.put(":one", AttributeValue.builder()
            .n("1")
            .build());

    client.updateItem(UpdateItemRequest.builder()
            .tableName(TABLE_NAME)
            .key(key)
            .updateExpression("ADD #count :one")
            .expressionAttributeNames(names)
            .expressionAttributeValues(values)
            .build());
  }
}