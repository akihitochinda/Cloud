package persistence;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import model.SwipeEvent;

public class PersistenceBuffer {
  private static final BlockingQueue<SwipeEvent> queue = new LinkedBlockingQueue<>();

  public static void enqueue(SwipeEvent event) {
    queue.offer(event);
  }

  public static SwipeEvent take() throws InterruptedException {
    return queue.take();
  }

  public static SwipeEvent poll(long timeoutMs) throws InterruptedException {
    return queue.poll(timeoutMs, TimeUnit.MILLISECONDS);
  }
}
