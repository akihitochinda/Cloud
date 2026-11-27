package persistence;

import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

public class TrafficMetricsRepository {
  private final DynamoDbClient client = DynamoDbProvider.getClient();

  public int getSwipesPerSecond(String bucket) {
    Map<String, AttributeValue> key = new HashMap<>();
    key.put("TimeBucket", AttributeValue.builder()
            .s(bucket)
            .build());

    GetItemResponse response = client.getItem(GetItemRequest.builder()
                .tableName("TrafficMetrics")
                .key(key)
                .build());

    Map<String, AttributeValue> item = response.item();

    if (item == null || item.isEmpty()) {
      return 0;
    }
    return Integer.parseInt(item.get("SwipeCount").n());
  }
}
