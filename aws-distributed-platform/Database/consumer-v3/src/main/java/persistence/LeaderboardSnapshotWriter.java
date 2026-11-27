package persistence;

import java.util.List;
import leaderboard.LeaderboardEntry;

public class LeaderboardSnapshotWriter implements Runnable {
  private final LeaderboardSnapshotDao dao;

  public LeaderboardSnapshotWriter() {
    this.dao = new LeaderboardSnapshotDao(DynamoDbProvider.getClient());
  }

  @Override
  public void run() {
    while (true) {
      try {
        persistSnapshots();
        Thread.sleep(1000);
      } catch (Exception e) {
        DynamoDbWriteMetrics.recordDynamoDbFailure(e);
        e.printStackTrace();
      }
    }
  }

  private void persistSnapshots() throws InterruptedException {
    SnapshotBatch batch = LeaderboardSnapshotBuffer.take();
    String resortDay = batch.getResortDay();
    List<LeaderboardEntry> entries = batch.getTop100();

    for (int i = 0; i < entries.size(); i++) {
      LeaderboardEntry entry = entries.get(i);

      dao.saveSnapshotRow(
          resortDay,
          i + 1,
          entry.getSkierID(),
          entry.getVertical());
    }
  }
}