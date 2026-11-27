package websocket;

import com.google.gson.Gson;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

@ServerEndpoint("/leaderboard/{resortID}")
public class LeaderboardEndpoint {

  private static final Gson gson = new Gson();

  // resortID -> sessions subscribed to that resort
  private static final Map<String, Set<Session>> subscribers = new ConcurrentHashMap<>();
  private static final ScheduledExecutorService pusher =
      Executors.newSingleThreadScheduledExecutor();

  static {
    // Dummy: every 500ms, push a synthetic top-N to each resort's subscribers.
    pusher.scheduleAtFixedRate(LeaderboardEndpoint::pushAll, 500, 500, TimeUnit.MILLISECONDS);
  }

  @OnOpen
  public void onOpen(Session session, @PathParam("resortID") String resortID) {
    subscribers.computeIfAbsent(resortID, k -> ConcurrentHashMap.newKeySet()).add(session);
  }

  @OnClose
  public void onClose(Session session, @PathParam("resortID") String resortID) {
    Optional.ofNullable(subscribers.get(resortID)).ifPresent(s -> s.remove(session));
  }

  @OnError
  public void onError(Session session, Throwable t) {
    // log, then clean up the session
    t.printStackTrace();
    subscribers.values().forEach(set -> set.remove(session));
  }

  private static void pushAll() {
    for (Map.Entry<String, Set<Session>> e : subscribers.entrySet()) {
      // String json = syntheticLeaderboard(e.getKey()); // canned/random top-N + serverTimestamp
      String json = LeaderboardCache.get(e.getKey()); // key: resort
      if (json == null) continue; // skip if no json
      if (!LeaderboardCache.isUpdated(e.getKey())) continue; // skip if no resort update
      for (Session s : e.getValue()) {
        if (s.isOpen()) s.getAsyncRemote().sendText(json);
      }
      LeaderboardCache.clearUpdated(e.getKey()); // clear resort cache
    }
  }

  private static String syntheticLeaderboard(String resortID) {
    Map<String, Object> payload = new HashMap<>();

    payload.put("resortID", resortID);
    payload.put("serverTimestamp", Instant.now().toString());

    List<Map<String, Object>> top = new ArrayList<>();

    for (int i = 1; i <= 5; i++) {
      Map<String, Object> skier = new HashMap<>();

      skier.put("rank", i);
      skier.put("skierID", ThreadLocalRandom.current().nextInt(1, 100000));
      skier.put("vertical", ThreadLocalRandom.current().nextInt(1000, 50000));
      top.add(skier);
    }

    payload.put("top", top);

    return gson.toJson(payload);
  }
}