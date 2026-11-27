package persistence;

import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

public class SkierLiftStatsRepository {
  private final DynamoDbClient client = DynamoDbProvider.getClient();

  public String getFavoriteLift(String skierId) {
    Map<String, AttributeValue> values = new HashMap<>();
    values.put(":id", AttributeValue.builder()
            .s(skierId)
            .build());

    QueryResponse response = client.query(QueryRequest.builder()
                .tableName("SkierLiftStats")
                .keyConditionExpression("SkierId = :id")
                .expressionAttributeValues(values)
                .build());

    String favoriteLift = null;
    long maxCount = -1;

    for (Map<String, AttributeValue> item : response.items()) {
      long count = Long.parseLong(item.get("Count").n());
      if (count > maxCount) {
        maxCount = count;
        favoriteLift = item.get("LiftId").s();
      }
    }
    return favoriteLift;
  }
}