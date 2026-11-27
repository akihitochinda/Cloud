package persistence;

import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

public class LeaderboardSnapshotDao {
  private static final String TABLE = "LeaderboardSnapshot";
  private final DynamoDbClient client;

  public LeaderboardSnapshotDao(DynamoDbClient client) {
    this.client = client;
  }

  public void saveSnapshotRow(String resortDay, int rank, String skierId, long vertical) {
    Map<String, AttributeValue> item = new HashMap<>();
    item.put("ResortDay", AttributeValue.builder()
            .s(resortDay)
            .build());
    item.put("Rank", AttributeValue.builder()
            .n(String.valueOf(rank))
            .build());
    item.put("SkierId", AttributeValue.builder()
            .s(skierId)
            .build());
    item.put("Vertical", AttributeValue.builder()
            .n(String.valueOf(vertical))
            .build());

    client.putItem(PutItemRequest.builder()
            .tableName(TABLE)
            .item(item)
            .build());
  }
}