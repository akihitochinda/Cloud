package persistence;

import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

public class SkierStatsDao {
  private static final String TABLE ="SkierStats";
  private final DynamoDbClient client;

  public SkierStatsDao(DynamoDbClient client) {
    this.client = client;
  }

  public void updateStats(String skierId, int vertical) {
    Map<String, AttributeValue> key = new HashMap<>();
    key.put("SkierId", AttributeValue.builder()
            .s(skierId)
            .build());

    Map<String, String> names = new HashMap<>();
    names.put("#runs", "TotalRuns");
    names.put("#vertical", "TotalVertical");

    Map<String, AttributeValue> values = new HashMap<>();
    values.put(":one", AttributeValue.builder()
            .n("1")
            .build());
    values.put(":vertical", AttributeValue.builder()
            .n(String.valueOf(vertical))
            .build());

    client.updateItem(UpdateItemRequest.builder()
            .tableName(TABLE)
            .key(key)
            .updateExpression("ADD #runs :one, #vertical :vertical")
            .expressionAttributeNames(names)
            .expressionAttributeValues(values)
            .build());
  }
}