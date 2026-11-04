package sender;

import com.google.gson.Gson;
import java.util.concurrent.atomic.AtomicInteger;
import metrics.MetricsCollector;
import model.SwipeEvent;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.BlockingQueue;

public class HttpSender implements Runnable {

  private final BlockingQueue<SwipeEvent> queue;
  private final HttpClient client;
  private final String baseUrl;
  private final Gson gson = new Gson();

  public HttpSender(
      BlockingQueue<SwipeEvent> queue, HttpClient client, String baseUrl) {
    this.queue = queue;
    this.client = client;
    this.baseUrl = baseUrl;
  }

  @Override
  public void run() {
    while (!Thread.currentThread().isInterrupted()) {
      try {
        SwipeEvent event = queue.take();
        sendSwipe(event);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }

  private void sendSwipe(SwipeEvent event) {

    long startTime = System.currentTimeMillis();
    int statusCode = -1;
    String url =
        String.format(
            "%s/skiers/%d/seasons/%d/days/%d/skiers/%d",
            baseUrl,
            event.getResortID(),
            event.getSeasonID(),
            event.getDayID(),
            event.getSkierID());
    try {
      String json = gson.toJson(event.getLiftRide());

      boolean success = false;

      for (int retry = 0; retry < 5 && !success; retry++) {
        try {
          HttpRequest request =
              HttpRequest.newBuilder()
                  .uri(URI.create(url))
                  .header(
                      "Content-Type",
                      "application/json")
                  .POST(
                      HttpRequest.BodyPublishers
                          .ofString(json))
                  .build();

          HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
          statusCode = response.statusCode();

          if (statusCode == 201) {
            long responseTime = System.currentTimeMillis();
            MetricsCollector.success.incrementAndGet();
            MetricsCollector.latencies.add(responseTime - startTime);
            MetricsCollector.resortCounts.computeIfAbsent(event.getResortID(), k -> new AtomicInteger()).incrementAndGet();
            success = true;
          }
        } catch (Exception ignored) {
          Thread.sleep((long)Math.pow(2, retry + 1) * 100);
        }
      }

      if (!success) {
        MetricsCollector.failed.incrementAndGet();
      }
    } catch (Exception e) {
      MetricsCollector.failed.incrementAndGet();
    }
  }
}