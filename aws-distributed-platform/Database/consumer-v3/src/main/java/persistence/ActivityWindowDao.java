package persistence;

import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

public class ActivityWindowDao {
  private static final String TABLE_NAME = "ActivityWindow";
  private final DynamoDbClient client;

  public ActivityWindowDao(DynamoDbClient client) {
    this.client = client;
  }

  public void recordActivity(String minuteBucket, String skierId) {
    Map<String, AttributeValue> item = new HashMap<>();
    item.put("MinuteBucket", AttributeValue.builder()
            .s(minuteBucket)
            .build());
    item.put("SkierId", AttributeValue.builder()
            .s(skierId)
            .build());

    client.putItem(PutItemRequest.builder()
            .tableName(TABLE_NAME)
            .item(item)
            .build());
  }
}