package persistence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

public class SkierResortDayStatsRepository {
  private final DynamoDbClient client = DynamoDbProvider.getClient();

  public List<Map<String,Object>> getResortDays(String skierId) {
    Map<String, AttributeValue> values = new HashMap<>();
    values.put(":id", AttributeValue.builder()
            .s(skierId)
            .build());

    QueryResponse response = client.query(QueryRequest.builder()
                .tableName("SkierResortDayStats")
                .keyConditionExpression("SkierId = :id")
                .expressionAttributeValues(values)
                .build());

    List<Map<String,Object>> result = new ArrayList<>();

    for (Map<String, AttributeValue> item : response.items()) {
      String[] parts = item.get("ResortDay")
              .s()
              .split("#");

      Map<String,Object> row = new HashMap<>();
      row.put("resortId", parts[0]);
      row.put("dayId", parts[1]);
      row.put("runs", Integer.parseInt(item.get("Runs").n()));
      row.put("vertical", Integer.parseInt(item.get("Vertical").n()));
      result.add(row);
    }
    return result;
  }

  public List<Map<String,Object>> getVisitedResorts(String skierId) {
    Map<String, AttributeValue> values = new HashMap<>();
    values.put(":id", AttributeValue.builder()
            .s(skierId)
            .build());

    QueryResponse response = client.query(QueryRequest.builder()
                .tableName("SkierResortDayStats")
                .keyConditionExpression("SkierId = :id")
                .expressionAttributeValues(values)
                .build());

    Map<String,String> resorts = new HashMap<>();

    for (Map<String, AttributeValue> item : response.items()) {
      String[] parts = item.get("ResortDay")
              .s()
              .split("#");

      String resortId = parts[0];
      String dayId = parts[1];

      resorts.put(resortId, dayId);
    }

    List<Map<String,Object>> result = new ArrayList<>();

    for (Map.Entry<String,String> entry : resorts.entrySet()) {
      Map<String,Object> resort = new HashMap<>();
      resort.put("resortId", entry.getKey());
      resort.put("lastDay", entry.getValue());
      result.add(resort);
    }
    return result;
  }

  public List<Map<String,Object>> getTopResorts(int limit) {
    ScanResponse response = client.scan(ScanRequest.builder()
                .tableName("SkierResortDayStats")
                .build());

    Map<String, Long> resortTotals = new HashMap<>();

    for (Map<String, AttributeValue> item : response.items()) {
      String[] parts = item.get("ResortDay")
              .s()
              .split("#");

      String resortId = parts[0];
      long vertical = Long.parseLong(item.get("Vertical").n());

      resortTotals.merge(
          resortId,
          vertical,
          Long::sum);
    }

    List<Map.Entry<String, Long>> sorted = new ArrayList<>(resortTotals.entrySet());
    sorted.sort(
        (a, b) ->
            Long.compare(
                b.getValue(),
                a.getValue()));

    List<Map<String,Object>> result = new ArrayList<>();

    int rank = 1;

    for (int i = 0; i < Math.min(limit, sorted.size()); i++) {
      Map.Entry<String, Long> entry = sorted.get(i);
      Map<String,Object> resort = new HashMap<>();
      resort.put("rank", rank++);
      resort.put("resortId", entry.getKey());
      resort.put("totalVertical", entry.getValue());
      result.add(resort);
    }
    return result;
  }

  public Map<String,Object> getParticipationPattern(String skierId, Map<String,Object> stats) {
    List<Map<String,Object>> resortDays = getResortDays(skierId);

    long totalRuns = ((Number) stats.get("totalRuns")).longValue();
    long totalVertical = ((Number) stats.get("totalVertical")).longValue();
    int daysActive = resortDays.size();

    Map<String,Object> result = new HashMap<>();
    result.put("daysActive", daysActive);
    result.put("avgRunsPerDay", daysActive == 0 ? 0 : (double) totalRuns / daysActive);
    result.put("avgVerticalPerDay", daysActive == 0 ? 0 : (double) totalVertical / daysActive);
    return result;
  }
}