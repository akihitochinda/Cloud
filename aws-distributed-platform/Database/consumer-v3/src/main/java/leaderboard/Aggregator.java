package leaderboard;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import model.SwipeEvent;

public class Aggregator {

  private static final ConcurrentHashMap<String, AtomicLong>
      skierVertical = new ConcurrentHashMap<>(); // key: resort:day:skier

  public static long process(SwipeEvent event) {

    String key = event.getResortID() + ":" + event.getDayID() + ":" + event.getSkierID();

    return skierVertical
        .computeIfAbsent(
            key,
            k -> new AtomicLong())
        .addAndGet(event.getVertical());
  }

  public static long getTotal(String resortId, String dayId, String skierId) {
    AtomicLong value = skierVertical.get(resortId + ":" + dayId + ":" + skierId);
    return value == null ? 0 : value.get();
  }
}