package persistence;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class LeaderboardSnapshotBuffer {
  private static final BlockingQueue<SnapshotBatch> queue = new LinkedBlockingQueue<>();

  public static void enqueue(SnapshotBatch batch) {
    queue.offer(batch);
  }

  public static SnapshotBatch take() throws InterruptedException {
    return queue.take();
  }
}
