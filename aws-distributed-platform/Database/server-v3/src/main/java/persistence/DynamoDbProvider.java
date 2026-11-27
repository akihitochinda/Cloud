package persistence;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public class DynamoDbProvider {

  private static final DynamoDbClient client = DynamoDbClient.builder()
      .region(Region.US_WEST_2)
      .build();

  public static DynamoDbClient getClient() {
    return client;
  }
}
