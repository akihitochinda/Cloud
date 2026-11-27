package persistence;

import java.util.List;
import model.SwipeEvent;

public class DeadLetterQueueWorker implements Runnable {
  private final SwipeEventsDao dao = new SwipeEventsDao(DynamoDbProvider.getClient());

  @Override
  public void run() {
    while (true) {
      try {
        List<SwipeEvent> batch = DeadLetterQueue.take();
        int retry = 0;
        while (retry < 5) {
          try {
            dao.batchSave(batch);
            break;
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
          } catch (Exception e) {
            retry++;
            DynamoDbWriteMetrics.recordDynamoDbFailure(e);
            if (retry < 5) {
              DynamoDbWriteMetrics.recordRetry();
              DynamoDbWriteMetrics.recordBatchWriteRetry();
            }
            if (retry == 5) {
              System.err.println(
                  "Dead-letter batch exhausted retries; batch size=" + batch.size());
              e.printStackTrace();
              break;
            }
            long delay = (long) Math.pow(2, retry) * 1000L;
            Thread.sleep(delay);
          }
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return;
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
}