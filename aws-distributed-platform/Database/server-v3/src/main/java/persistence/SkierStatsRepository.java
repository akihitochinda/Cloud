package persistence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

public class SkierStatsRepository {

  private final DynamoDbClient client = DynamoDbProvider.getClient();

  public Map<String, Object> getStats(String skierId) {
    Map<String, AttributeValue> key = new HashMap<>();
    key.put("SkierId", AttributeValue.builder()
        .s(skierId)
        .build());

    GetItemResponse response = client.getItem(GetItemRequest.builder()
        .tableName("SkierStats")
        .key(key)
        .build());

    Map<String, AttributeValue> item = response.item();

    if (item == null || item.isEmpty()) {
      return null;
    }

    Map<String, Object> result = new HashMap<>();
    result.put("totalRuns", Long.parseLong(item.get("TotalRuns").n()));
    result.put("totalVertical", Long.parseLong(item.get("TotalVertical").n()));
    return result;
  }

  public List<Map<String, Object>> getTopSkiers(int limit) {
    ScanResponse response = client.scan(ScanRequest.builder()
        .tableName("SkierStats")
        .build());

    List<Map<String, Object>> result = new ArrayList<>();
    List<Map<String, AttributeValue>> items = new ArrayList<>(response.items());
    items.sort(
        (a, b) ->
            Long.compare(
                Long.parseLong(
                    b.get("TotalVertical").n()),
                Long.parseLong(
                    a.get("TotalVertical").n())));

    int rank = 1;

    for (int i = 0; i < Math.min(limit, items.size()); i++) {
      Map<String, AttributeValue> item = items.get(i);
      Map<String, Object> skier = new HashMap<>();

      skier.put("rank", rank++);
      skier.put("skierId", item.get("SkierId").s());
      skier.put("totalVertical", Long.parseLong(item.get("TotalVertical").n()));
      skier.put("totalRuns", Long.parseLong(item.get("TotalRuns").n()));
      result.add(skier);
    }
    return result;
  }
}