package persistence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

public class LeaderboardSnapshotRepository {
  private final DynamoDbClient client = DynamoDbProvider.getClient();

  public List<Map<String, Object>> getLeaderboard(String resortId, String dayId) {
    String resortDay = resortId + ":" + dayId;

    Map<String, AttributeValue> values = new HashMap<>();
    values.put(":rd", AttributeValue.builder()
            .s(resortDay)
            .build());

    QueryResponse response = client.query(QueryRequest.builder()
                .tableName("LeaderboardSnapshot")
                .keyConditionExpression("ResortDay = :rd")
                .expressionAttributeValues(values)
                .build());

    List<Map<String, Object>> result = new ArrayList<>();

    for (Map<String, AttributeValue> item : response.items()) {
      Map<String, Object> row = new HashMap<>();
      row.put("rank", Integer.parseInt(item.get("Rank").n()));
      row.put("skierId", item.get("SkierId").s());
      row.put("vertical", Integer.parseInt(item.get("Vertical").n()));
      result.add(row);
    }
    return result;
  }
}
