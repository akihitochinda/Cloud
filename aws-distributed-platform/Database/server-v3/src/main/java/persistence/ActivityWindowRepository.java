package persistence;

import java.util.HashMap;
import java.util.Map;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

public class ActivityWindowRepository {
  private final DynamoDbClient client = DynamoDbProvider.getClient();

  public int getActiveSkiers(String startTime, String endTime) {
    Set<String> uniqueSkiers = new HashSet<>();
    LocalDateTime start = LocalDateTime.parse(startTime);
    LocalDateTime end = LocalDateTime.parse(endTime);

    while (!start.isAfter(end)) {
      String minuteBucket = start.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));

      Map<String, AttributeValue> values = new HashMap<>();
      values.put(":bucket", AttributeValue.builder()
              .s(minuteBucket)
              .build());

      QueryResponse response = client.query(QueryRequest.builder()
                  .tableName("ActivityWindow")
                  .keyConditionExpression("MinuteBucket = :bucket")
                  .expressionAttributeValues(values)
                  .build());

      for (Map<String, AttributeValue> item : response.items()) {
        uniqueSkiers.add(item.get("SkierId").s());
      }
      start = start.plusMinutes(1);
    }
    return uniqueSkiers.size();
  }
}