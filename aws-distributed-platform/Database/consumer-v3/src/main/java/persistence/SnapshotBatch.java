package persistence;

import java.util.List;
import leaderboard.LeaderboardEntry;

public class SnapshotBatch {
  private final String resortDay;
  private final List<LeaderboardEntry> top100;

  public SnapshotBatch(String resortDay, List<LeaderboardEntry> top100) {
    this.resortDay = resortDay;
    this.top100 = top100;
  }

  public String getResortDay() {
    return resortDay;
  }

  public List<LeaderboardEntry> getTop100() {
    return top100;
  }
}