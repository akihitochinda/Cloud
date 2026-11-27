package persistence;

import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

public class SkierResortDayStatsDao {
  private static final String TABLE_NAME = "SkierResortDayStats";
  private final DynamoDbClient client;

  public SkierResortDayStatsDao(DynamoDbClient client) {
    this.client = client;
  }

  public void updateStats(String skierId, String resortId, String dayId, int vertical) {
    String resortDay = resortId + "#" + dayId;

    Map<String, AttributeValue> key = new HashMap<>();
    key.put("SkierId", AttributeValue.builder()
            .s(skierId)
            .build());
    key.put("ResortDay", AttributeValue.builder()
            .s(resortDay)
            .build());

    Map<String, String> names = new HashMap<>();
    names.put("#runs", "Runs");
    names.put("#vertical", "Vertical");

    Map<String, AttributeValue> values = new HashMap<>();
    values.put(":one", AttributeValue.builder()
            .n("1")
            .build());
    values.put(":vertical", AttributeValue.builder()
            .n(String.valueOf(vertical))
            .build());

    client.updateItem(UpdateItemRequest.builder()
            .tableName(TABLE_NAME)
            .key(key)
            .updateExpression("ADD #runs :one, #vertical :vertical")
            .expressionAttributeNames(names)
            .expressionAttributeValues(values)
            .build());
  }
}