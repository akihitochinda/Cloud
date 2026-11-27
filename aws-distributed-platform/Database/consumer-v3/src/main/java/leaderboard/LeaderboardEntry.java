package leaderboard;

public class LeaderboardEntry {

  private final String skierID;
  private final long vertical;

  public LeaderboardEntry(String skierID, long vertical) {
    this.skierID = skierID;
    this.vertical = vertical;
  }

  public String getSkierID() { return skierID; }

  public long getVertical() { return vertical; }

  @Override
  public String toString() { return skierID + " -> " + vertical; }
}