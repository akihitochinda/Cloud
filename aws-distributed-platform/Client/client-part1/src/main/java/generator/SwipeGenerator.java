package generator;

import model.LiftRide;
import model.SwipeEvent;

import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class SwipeGenerator implements Runnable {

  private final BlockingQueue<SwipeEvent> queue;
  private final int totalEvents;
  private final int seasonId;
  private final int dayId;
  private final int maxResortId;
  private final long durationMs;
  private final AtomicInteger generatedEvents = new AtomicInteger();

  public SwipeGenerator(
      BlockingQueue<SwipeEvent> queue,
      int totalEvents,
      int seasonId,
      int dayId,
      int maxResortId) {
    this(queue, totalEvents, 0, seasonId, dayId, maxResortId);
  }

  public SwipeGenerator(
      BlockingQueue<SwipeEvent> queue,
      long durationMs,
      int seasonId,
      int dayId,
      int maxResortId) {
    this(queue, 0, durationMs, seasonId, dayId, maxResortId);
    if (durationMs < 1) {
      throw new IllegalArgumentException("durationMs must be at least 1");
    }
  }

  private SwipeGenerator(
      BlockingQueue<SwipeEvent> queue,
      int totalEvents,
      long durationMs,
      int seasonId,
      int dayId,
      int maxResortId) {
    this.queue = queue;
    this.totalEvents = totalEvents;
    this.durationMs = durationMs;
    this.seasonId = seasonId;
    this.dayId = dayId;
    this.maxResortId = maxResortId;
  }

  public int getGeneratedEvents() {
    return generatedEvents.get();
  }

  @Override
  public void run() {

    Random random = new Random();
    long startNanos = System.nanoTime();

    try {

      while (durationMs > 0
          ? (System.nanoTime() - startNanos) / 1_000_000 < durationMs
          : generatedEvents.get() < totalEvents) {

        LiftRide ride =
            new LiftRide(
                random.nextInt(360) + 1,
                random.nextInt(40) + 1);

        SwipeEvent event =
            new SwipeEvent(
                random.nextInt(100000) + 1,
                random.nextInt(maxResortId) + 1,
                seasonId,
                dayId,
                ride);

        if (durationMs > 0) {
          long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;
          long remainingMs = durationMs - elapsedMs;
          if (remainingMs <= 0 || !queue.offer(event, remainingMs, TimeUnit.MILLISECONDS)) {
            break;
          }
        } else {
          queue.put(event);
        }
        generatedEvents.incrementAndGet();
      }

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}