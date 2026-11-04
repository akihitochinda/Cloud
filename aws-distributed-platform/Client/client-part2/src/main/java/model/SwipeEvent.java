package model;

public class SwipeEvent {

  private final int skierID;
  private final int resortID;
  private final int seasonID;
  private final int dayID;

  private final LiftRide liftRide;

  public SwipeEvent(
      int skierID,
      int resortID,
      int seasonID,
      int dayID,
      LiftRide liftRide) {

    this.skierID = skierID;
    this.resortID = resortID;
    this.seasonID = seasonID;
    this.dayID = dayID;
    this.liftRide = liftRide;
  }

  public int getSkierID() {
    return skierID;
  }

  public int getResortID() {
    return resortID;
  }

  public int getSeasonID() {
    return seasonID;
  }

  public int getDayID() {
    return dayID;
  }

  public LiftRide getLiftRide() {
    return liftRide;
  }
}