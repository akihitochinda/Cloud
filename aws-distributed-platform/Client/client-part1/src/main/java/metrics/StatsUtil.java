package metrics;

import java.util.*;

public class StatsUtil {

  public static double mean(List<Long> values) {

    return values.stream()
        .mapToLong(Long::longValue)
        .average()
        .orElse(0);
  }

  public static long percentile(List<Long> values, double percentile) {
    Collections.sort(values);
    int index = (int)Math.ceil(percentile * values.size()) - 1;

    return values.get(index);
  }

  public static long median(List<Long> values) {
    Collections.sort(values);

    return values.get(values.size() / 2);
  }

  public static long min(List<Long> values) {
    return Collections.min(values);
  }

  public static long max(List<Long> values) {
    return Collections.max(values);
  }
}