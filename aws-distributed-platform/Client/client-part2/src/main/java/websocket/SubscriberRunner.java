package websocket;

import javax.websocket.ContainerProvider;
import javax.websocket.WebSocketContainer;
import java.net.URI;
import metrics.MetricsCollector;

public class SubscriberRunner implements Runnable {

  private final String baseUrl;
  private final int resortId;

  public SubscriberRunner(String baseUrl, int resortId) {
    this.baseUrl = baseUrl;
    this.resortId = resortId;
  }

  @Override
  public void run() {
    while(true) {
      try {
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        String endpoint = baseUrl.replace("http://", "ws://") + "/leaderboard/" + resortId;

        long startConnection = System.currentTimeMillis();
        container.connectToServer(new LeaderboardSubscriber(resortId), URI.create(endpoint));
        long connectionTime = System.currentTimeMillis() - startConnection;
        MetricsCollector.connectionTimes.add(connectionTime);

        while (true) {
          // stay connected until the socket closes
          Thread.sleep(Long.MAX_VALUE);
        }
      } catch(Exception e) {

        MetricsCollector.reconnects.incrementAndGet();

        try {
          Thread.sleep(1000);
        } catch(Exception ignored) {
        }
      }
    }
  }
}