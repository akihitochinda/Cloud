package generator;

import model.LiftRide;
import model.SwipeEvent;

import java.util.Random;
import java.util.concurrent.BlockingQueue;

public class SwipeGenerator implements Runnable {

  private final BlockingQueue<SwipeEvent> queue;
  private final int totalEvents;
  private final int seasonId;
  private final int dayId;
  private final int maxResortId;

  public SwipeGenerator(
      BlockingQueue<SwipeEvent> queue,
      int totalEvents,
      int seasonId,
      int dayId,
      int maxResortId) {

    this.queue = queue;
    this.totalEvents = totalEvents;
    this.seasonId = seasonId;
    this.dayId = dayId;
    this.maxResortId = maxResortId;
  }

  @Override
  public void run() {

    Random random = new Random();

    try {

      for (int i = 0; i < totalEvents; i++) {

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

        queue.put(event);
      }

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}