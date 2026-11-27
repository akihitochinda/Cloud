package model;

import java.util.List;

public class SkierStats {
  private String skierId;
  private long totalRuns;
  private long totalVertical;
  private String favoriteLift;
  private List<SkierResortDayStat> resortDays;

  public SkierStats(String skierId, long totalRuns, long totalVertical, String favoriteLift, List<SkierResortDayStat> resortDays) {
    this.skierId = skierId;
    this.totalRuns = totalRuns;
    this.totalVertical = totalVertical;
    this.favoriteLift = favoriteLift;
    this.resortDays = resortDays;
  }

  public String getSkierId() {
    return skierId;
  }

  public void setSkierId(String skierId) {
    this.skierId = skierId;
  }

  public long getTotalRuns() {
    return totalRuns;
  }

  public void setTotalRuns(long totalRuns) {
    this.totalRuns = totalRuns;
  }

  public long getTotalVertical() {
    return totalVertical;
  }

  public void setTotalVertical(long totalVertical) {
    this.totalVertical = totalVertical;
  }

  public String getFavoriteLift() {
    return favoriteLift;
  }

  public void setFavoriteLift(String favoriteLift) {
    this.favoriteLift = favoriteLift;
  }

  public List<SkierResortDayStat> getResortDays() {
    return resortDays;
  }

  public void setResortDays(List<SkierResortDayStat> resortDays) {
    this.resortDays = resortDays;
  }
}