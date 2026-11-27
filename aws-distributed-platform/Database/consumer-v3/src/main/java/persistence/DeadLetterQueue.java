package persistence;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import model.SwipeEvent;

public class DeadLetterQueue {

  private static final BlockingQueue<
      List<SwipeEvent>> QUEUE = new LinkedBlockingQueue<>();

  public static void enqueue(List<SwipeEvent> batch) {
    QUEUE.offer(batch);
  }

  public static List<SwipeEvent> take() throws InterruptedException {
    return QUEUE.take();
  }

  public static int size() {
    return QUEUE.size();
  }
}