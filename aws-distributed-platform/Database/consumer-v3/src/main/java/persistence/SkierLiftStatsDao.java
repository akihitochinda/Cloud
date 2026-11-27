package persistence;

import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

public class SkierLiftStatsDao {
  private static final String TABLE_NAME = "SkierLiftStats";
  private final DynamoDbClient client;

  public SkierLiftStatsDao(DynamoDbClient client) {
    this.client = client;
  }

  public void updateLiftCount(String skierId, String liftId) {
    Map<String, AttributeValue> key = new HashMap<>();
    key.put("SkierId", AttributeValue.builder()
            .s(skierId)
            .build());
    key.put("LiftId", AttributeValue.builder()
            .s(liftId)
            .build());

    Map<String, String> names = new HashMap<>();
    names.put("#count", "Count");

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