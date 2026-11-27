package persistence;

import java.util.ArrayList;
import java.util.List;
import model.SwipeEvent;

public class BatchWriter implements Runnable {
  private final int batchSize;
  private final long flushIntervalMs;
  private final SwipeEventsDao swipeEventsDao = new SwipeEventsDao(DynamoDbProvider.getClient());
  private final SkierStatsDao skierStatsDao = new SkierStatsDao(DynamoDbProvider.getClient());
  private final SkierResortDayStatsDao skierResortDayStatsDao = new SkierResortDayStatsDao(DynamoDbProvider.getClient());
  private final SkierLiftStatsDao skierLiftStatsDao = new SkierLiftStatsDao(DynamoDbProvider.getClient());
  private final ActivityWindowDao activityWindowDao = new ActivityWindowDao(DynamoDbProvider.getClient());
  private final TrafficMetricsDao trafficMetricsDao = new TrafficMetricsDao(DynamoDbProvider.getClient());
  private final CircuitBreaker circuitBreaker = new CircuitBreaker();

  public BatchWriter(int batchSize, long flushIntervalMs) {
    this.batchSize = batchSize;
    this.flushIntervalMs = flushIntervalMs;
  }

  @Override
  public void run() {
    List<SwipeEvent> batch = new ArrayList<>(batchSize);
    long lastFlushTime = System.currentTimeMillis();

    while (true) {
      try {
        SwipeEvent event = PersistenceBuffer.poll(flushIntervalMs);
        if (event != null) batch.add(event);

        long flushTime = System.currentTimeMillis();
        boolean sizeReached = batch.size() >= batchSize;
        boolean intervalReached = !batch.isEmpty() && (flushTime - lastFlushTime) >= flushIntervalMs;

        if (sizeReached || intervalReached) {
          writeBatch(batch);
          // System.out.println("Persisted batch size=" + batch.size());
          batch.clear();
          lastFlushTime = flushTime;
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return;
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  private void writeBatch(List<SwipeEvent> batch) throws InterruptedException {
    if (!circuitBreaker.allowRequest()) {
      DeadLetterQueue.enqueue(new ArrayList<>(batch));
      return;
    }
    try {
      swipeEventsDao.batchSave(batch);
      for (SwipeEvent e : batch) {
        skierStatsDao.updateStats(e.getSkierID(), e.getVertical());
        skierResortDayStatsDao.updateStats(e.getSkierID(), e.getResortID(), e.getDayID(), e.getVertical());
        skierLiftStatsDao.updateLiftCount(e.getSkierID(), e.getLiftID());
        String minuteBucket = e.getTimestamp().substring(0, 16);
        activityWindowDao.recordActivity(minuteBucket, e.getSkierID());
        String secondBucket = e.getTimestamp().substring(0, 19);
        trafficMetricsDao.incrementSecond(secondBucket);
      }
      circuitBreaker.recordSuccess();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw e;
    } catch (Exception e) {
      circuitBreaker.recordFailure();
      DeadLetterQueue.enqueue(new ArrayList<>(batch));
      DynamoDbWriteMetrics.recordDynamoDbFailure(e);
      e.printStackTrace();
    }
  }
}