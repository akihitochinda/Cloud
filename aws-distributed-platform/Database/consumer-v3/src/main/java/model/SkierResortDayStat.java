package model;

public class SkierResortDayStat {
  private final String resortId;
  private final String dayId;
  private final int runs;
  private final int vertical;

  public SkierResortDayStat(String resortId, String dayId, int runs, int vertical) {
    this.resortId = resortId;
    this.dayId = dayId;
    this.runs = runs;
    this.vertical = vertical;
  }

  public String getResortId() {
    return resortId;
  }

  public String getDayId() {
    return dayId;
  }

  public int getRuns() {
    return runs;
  }

  public int getVertical() {
    return vertical;
  }
}