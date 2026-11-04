package websocket;

import com.google.gson.Gson;
import java.util.concurrent.atomic.AtomicInteger;
import metrics.MetricsCollector;

import javax.websocket.*;
import java.time.Instant;
import model.LeaderboardMessage;

@ClientEndpoint
public class
LeaderboardSubscriber {

  private final int resortId;
  private final Gson gson = new Gson();

  public LeaderboardSubscriber(int resortId) {
    this.resortId = resortId;
  }

  @OnOpen
  public void onOpen(Session session) {
    System.out.println("Connected to resort " + resortId);
    MetricsCollector.websocketConnections.incrementAndGet();
  }

  @OnMessage
  public void onMessage(String message) {
    MetricsCollector.leaderboardMessages.incrementAndGet();
    MetricsCollector.subscriberUpdates.computeIfAbsent(resortId, k -> new AtomicInteger()).incrementAndGet();
    try {
      LeaderboardMessage leaderboard = gson.fromJson(message, LeaderboardMessage.class);
      long receiveTime = System.currentTimeMillis();
      long serverTime = Instant.parse(leaderboard.getServerTimestamp()).toEpochMilli();
      MetricsCollector.pushLatencies.add(receiveTime - serverTime);
    } catch (Exception ignored) {
    }
  }

  @OnClose
  public void onClose(Session session, CloseReason reason) {
    System.out.println("Disconnected from resort " + resortId);
  }

  @OnError
  public void onError(Session session, Throwable throwable) {
    throwable.printStackTrace();
  }
}