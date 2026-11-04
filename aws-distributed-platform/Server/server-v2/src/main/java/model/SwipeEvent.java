package model;

public class SwipeEvent {

  private final String eventId;
  private final String resortID;
  private final String dayID;
  private final String skierID;
  private final String liftID;
  private final int vertical;
  private final int time;
  private final String timestamp;
  private final String serverId;

  public SwipeEvent(
      String eventId,
      String resortID,
      String dayID,
      String skierID,
      String liftID,
      int vertical,
      int time,
      String timestamp,
      String serverId) {

    this.eventId = eventId;
    this.resortID = resortID;
    this.dayID = dayID;
    this.skierID = skierID;
    this.liftID = liftID;
    this.vertical = vertical;
    this.time = time;
    this.timestamp = timestamp;
    this.serverId = serverId;
  }

  public String getEventId() { return eventId; }

  public String getResortID() { return resortID; }

  public String getDayID()  { return dayID; }

  public String getSkierID()  { return skierID; }

  public String getLiftID()  { return liftID; }

  public int getVertical()  { return vertical; }

  public int getTime()  { return time; }

  public String getTimestamp() { return timestamp; }

  public String getServerId() { return serverId; }
}