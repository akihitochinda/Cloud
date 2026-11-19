package leaderboard;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import model.SwipeEvent;

public class LeaderboardStore {

  private static final ConcurrentHashMap<String, ConcurrentHashMap<String, Long>>
      resortLeaderboards = new ConcurrentHashMap<>(); // key: resort:day

  public static void update(SwipeEvent event, long totalVertical) {

    String resortKey = event.getResortID() + ":" + event.getDayID();
    resortLeaderboards.computeIfAbsent(
            resortKey,
            k -> new ConcurrentHashMap<>())
            .put(event.getSkierID(), totalVertical);
  }

  public static List<LeaderboardEntry> getTop5(String resortID, String dayID) {

    String key = resortID + ":" + dayID;
    var board = resortLeaderboards.get(key);
    if (board == null) return Collections.emptyList();

    return board.entrySet()
        .stream()
        .sorted(
            (a, b) ->
                Long.compare(
                    b.getValue(),
                    a.getValue()))
        .limit(5)
        .map(e ->
            new LeaderboardEntry(
                e.getKey(),
                e.getValue()))
        .collect(Collectors.toList());
  }

  public static ConcurrentHashMap<String, Long> getLeaderboard(String resortID, String dayID) {
    String key = resortID + ":" + dayID;
    return resortLeaderboards.getOrDefault(key, new ConcurrentHashMap<>());
  }
}