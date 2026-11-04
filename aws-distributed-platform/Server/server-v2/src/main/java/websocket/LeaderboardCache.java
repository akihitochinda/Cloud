package websocket;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class LeaderboardCache {

  private static final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();
  private static final Set<String> isUpdated = ConcurrentHashMap.newKeySet();

  public static void put(String resortID, String leaderboardJson) {
    cache.put(resortID, leaderboardJson);
    isUpdated.add(resortID);
  }

  public static String get(String resortID) {
    return cache.get(resortID);
  }

  public static boolean isUpdated(String resortID) {
    return isUpdated.contains(resortID);
  }

  public static void clearUpdated(String resortID) {
    isUpdated.remove(resortID);
  }
}