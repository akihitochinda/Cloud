package servlet;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import model.LiftRide;
import model.ResponseMsg;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;

import java.util.logging.Logger;
import java.util.logging.Level;
import model.SwipeEvent;
import persistence.ActivityWindowRepository;
import persistence.LeaderboardSnapshotRepository;
import persistence.SkierLiftStatsRepository;
import persistence.SkierResortDayStatsRepository;
import persistence.SkierStatsRepository;
import persistence.TrafficMetricsRepository;
import rabbitmq.ChannelPool;
import rabbitmq.LiftVerticalLookup;
import rabbitmq.RabbitMQManager;
import rabbitmq.RabbitPublisher;
import rabbitmq.RabbitSetup;
import util.RequestStats;
import websocket.LeaderboardUpdateConsumer;

public class SkierServlet extends HttpServlet {
  private static final Logger logger = Logger.getLogger(SkierServlet.class.getName());
  private Gson gson = new Gson();
  private RabbitPublisher publisher;
  private Thread leaderboardThread;
  private final SkierStatsRepository statsRepo = new SkierStatsRepository();
  private final SkierLiftStatsRepository liftRepo = new SkierLiftStatsRepository();
  private final SkierResortDayStatsRepository resortRepo = new SkierResortDayStatsRepository();
  private final LeaderboardSnapshotRepository leaderboardRepo = new LeaderboardSnapshotRepository();
  private final ActivityWindowRepository activityRepo = new ActivityWindowRepository();
  private final TrafficMetricsRepository trafficRepo = new TrafficMetricsRepository();

  // RabbitMQ
  @Override
  public void init() {
    try {
      RabbitSetup.initialize();
      ChannelPool pool = new ChannelPool(20);
      pool.init();
      leaderboardThread = new Thread(new LeaderboardUpdateConsumer());
      leaderboardThread.start(); // get the update from the consumer
      publisher = new RabbitPublisher(pool);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void destroy() {
    try {
      if (leaderboardThread != null) {
        leaderboardThread.interrupt();
      }
      RabbitMQManager.close();
    } catch (Exception e) {
      logger.warning("Failed closing RabbitMQ");
    }
    super.destroy();
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse res)
      throws ServletException, IOException {

    logger.info("GET request received: " + req.getPathInfo());

    res.setContentType("application/json");
    res.setCharacterEncoding("UTF-8");

    String urlPath = req.getPathInfo();
    RequestStats.incrementGet();

    // Special endpoint for statistics
    if ("/stats".equals(urlPath)) {
      res.setContentType("application/json");
      res.setStatus(HttpServletResponse.SC_OK);
      res.getWriter().write(gson.toJson(new ResponseMsg(RequestStats.getStats())));
      return;
    }

    // Dynamo DB for statistics
    // Skier's own stats — skierID → total runs, total vertical, favorite lift, per resort/day. Target < 200 ms.
    if (urlPath.matches("^/\\d+/stats$")) {
      String skierId = urlPath.split("/")[1];
      Map<String,Object> response = new HashMap<>();
      response.put("skierId", skierId);
      response.putAll(statsRepo.getStats(skierId));
      response.put("favoriteLift", liftRepo.getFavoriteLift(skierId));
      response.put("resortDays", resortRepo.getResortDays(skierId));
      res.setStatus(HttpServletResponse.SC_OK);
      res.getWriter().write(gson.toJson(response));
      return;
    }

    // Resort leaderboard for a day — resortID, dayID → ordered top-N by vertical. Target < 100 ms for top 100.
    if (urlPath.matches("^/leaderboards/\\d+/\\d+$")) {
      String[] parts = urlPath.split("/");
      String resortId = parts[2];
      String dayId = parts[3];
      Map<String,Object> response = new HashMap<>();
      response.put("resortId", resortId);
      response.put("dayId", dayId);
      response.put("leaderboard", leaderboardRepo.getLeaderboard(resortId, dayId));
      res.setStatus(HttpServletResponse.SC_OK);
      res.getWriter().write(gson.toJson(response));
      return;
    }

    // Rooms/resorts a skier visited — skierID → resorts with last activity. Target < 50 ms.
    if (urlPath.matches("^/\\d+/resorts$")) {
      String skierId = urlPath.split("/")[1];
      Map<String,Object> response = new HashMap<>();
      response.put("skierId", skierId);
      response.put("resorts", resortRepo.getVisitedResorts(skierId));
      res.setStatus(HttpServletResponse.SC_OK);
      res.getWriter().write(gson.toJson(response));
      return;
    }

    // Active skiers in a time window — startTime, endTime → unique skier count. Target < 500 ms.
    if (urlPath.equals("/active")) {
      String start = req.getParameter("start");
      String end = req.getParameter("end");
      Map<String,Object> response = new HashMap<>();
      response.put("start", start);
      response.put("end", end);
      response.put("activeSkiers", activityRepo.getActiveSkiers(start, end));
      res.setStatus(HttpServletResponse.SC_OK);
      res.getWriter().write(gson.toJson(response));
      return;
    }

    // Dynamo DB for analytics
    // Swipes per second/minute
    if (urlPath.equals("/analytics/swipes")) {
      String bucket = req.getParameter("bucket");
      Map<String,Object> response = new HashMap<>();
      response.put("bucket", bucket);
      response.put("swipes", trafficRepo.getSwipesPerSecond(bucket));
      res.setStatus(HttpServletResponse.SC_OK);
      res.getWriter().write(gson.toJson(response));
      return;
    }

    // Most active skiers (top N by vertical)
    if (urlPath.equals("/analytics/top-skiers")) {
      int limit = Integer.parseInt(req.getParameter("limit"));
      Map<String,Object> response = new HashMap<>();
      response.put("limit", limit);
      response.put("topSkiers", statsRepo.getTopSkiers(limit));
      res.setStatus(HttpServletResponse.SC_OK);
      res.getWriter().write(gson.toJson(response));
      return;
    }

    // Most active resorts (top N)
    if (urlPath.equals("/analytics/top-resorts")) {
      int limit = Integer.parseInt(req.getParameter("limit"));
      Map<String,Object> response = new HashMap<>();
      response.put("limit", limit);
      response.put("topResorts", resortRepo.getTopResorts(limit));
      res.setStatus(HttpServletResponse.SC_OK);
      res.getWriter().write(gson.toJson(response));
      return;
    }

    // Skier participation patterns
    if (urlPath.matches("^/\\d+/participation$")) {
      String skierId = urlPath.split("/")[1];
      Map<String,Object> stats = statsRepo.getStats(skierId);
      Map<String,Object> response = new HashMap<>();
      response.put("skierId", skierId);
      response.putAll(resortRepo.getParticipationPattern(skierId, stats));
      res.setStatus(HttpServletResponse.SC_OK);
      res.getWriter().write(gson.toJson(response));
      return;
    }

    // Check we have a URL
    if (urlPath == null || urlPath.isEmpty()) {
      res.setStatus(HttpServletResponse.SC_NOT_FOUND);
      res.getWriter().write(gson.toJson(new ResponseMsg("missing parameters")));
      return;
    }

    String[] urlParts = urlPath.split("/");

    if (!isUrlValid(urlParts)) {
      logger.warning("Invalid URL: " + req.getPathInfo());
      res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      res.getWriter().write(gson.toJson(new ResponseMsg("invalid url")));
    } else {
      logger.info("Valid GET request processed for: " + req.getPathInfo());
      res.setStatus(HttpServletResponse.SC_OK);

      // Extract parameters
      String resortID = urlParts[1];
      String seasonID = urlParts[3];
      String dayID = urlParts[5];
      String skierID = urlParts[7];

      // Create response
      ResponseMsg response = new ResponseMsg(
          String.format("GET request processed for skier %s at resort %s, season %s, day %s",
              skierID, resortID, seasonID, dayID)
      );

      res.getWriter().write(gson.toJson(response));
    }
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse res)
      throws ServletException, IOException {

    res.setContentType("application/json");
    res.setCharacterEncoding("UTF-8");

    String urlPath = req.getPathInfo();

    logger.info("POST request received. Path: " + urlPath);

    // Check we have a URL
    if (urlPath == null || urlPath.isEmpty()) {
      logger.warning("POST failed: missing URL parameters");
      res.setStatus(HttpServletResponse.SC_NOT_FOUND);
      res.getWriter().write(gson.toJson(new ResponseMsg("missing parameters")));
      return;
    }

    String[] urlParts = urlPath.split("/");

    if (!isUrlValid(urlParts)) {
      logger.warning("POST failed: invalid URL -> " + urlPath);
      res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      res.getWriter().write(gson.toJson(new ResponseMsg("invalid url")));
      return;
    }

    // Parse JSON body
    try {
      StringBuilder jsonBody = new StringBuilder();
      BufferedReader reader = req.getReader();
      String line;
      while ((line = reader.readLine()) != null) {
        jsonBody.append(line);
      }

      logger.info("POST body: " + jsonBody);

      LiftRide liftRide = gson.fromJson(jsonBody.toString(), LiftRide.class);

      if (liftRide == null || !isLiftRideValid(liftRide)) {
        logger.warning(
            "POST failed: invalid LiftRide object. Body="
                + jsonBody);
        res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        res.getWriter().write(gson.toJson(new ResponseMsg("invalid JSON body")));
        return;
      }

      logger.info(
          "POST success: skier="
              + urlParts[7]
              + ", time="
              + liftRide.getTime()
              + ", lift="
              + liftRide.getLiftID());

      // Extract URL parameters
      String resortID = urlParts[1];
      String seasonID = urlParts[3];
      String dayID = urlParts[5];
      String skierID = urlParts[7];

      // Process the lift ride (in real app, save to database)
      res.setStatus(HttpServletResponse.SC_CREATED);
      ResponseMsg response = new ResponseMsg(
          String.format("Lift ride recorded: skier %s, resort %s, season %s, day %s, time %d, lift %d",
              skierID, resortID, seasonID, dayID, liftRide.getTime(), liftRide.getLiftID())
      );

      // RabbitMQ
      int vertical = LiftVerticalLookup.getVertical(liftRide.getLiftID());
      SwipeEvent event =
          new SwipeEvent(
              UUID.randomUUID().toString(),
              resortID,
              dayID,
              skierID,
              String.valueOf(liftRide.getLiftID()),
              vertical,
              liftRide.getTime(),
              Instant.now().toString(),
              "server-1"
          );

      try {
        publisher.publish(event);
      } catch (Exception e) {
        logger.log(
            Level.SEVERE,
            "RabbitMQ publish failed",
            e);
        res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        res.getWriter().write(gson.toJson(new ResponseMsg("publish failed")));

        return;
      }

      res.setStatus(HttpServletResponse.SC_CREATED);
      res.getWriter().write(gson.toJson(response));

    } catch (JsonSyntaxException e) {
      logger.log(
          Level.WARNING,
          "POST failed: invalid JSON format",
          e);
      res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      res.getWriter().write(gson.toJson(new ResponseMsg("invalid JSON format")));
    }
  }

  private boolean isUrlValid(String[] urlParts) {
    // Expected URL: /skiers/{resortID}/seasons/{seasonID}/days/{dayID}/skiers/{skierID}
    // urlParts = ["", "1", "seasons", "2019", "days", "1", "skiers", "123"]

    if (urlParts.length != 8) {
      return false;
    }

    try {
      // Check structure
      if (!urlParts[2].equals("seasons") ||
          !urlParts[4].equals("days") ||
          !urlParts[6].equals("skiers")) {
        return false;
      }

      // Validate numeric parameters
      Integer.parseInt(urlParts[1]); // resortID
      Integer.parseInt(urlParts[3]); // seasonID
      Integer.parseInt(urlParts[5]); // dayID
      Integer.parseInt(urlParts[7]); // skierID

      return true;

    } catch (NumberFormatException e) {
      return false;
    }
  }

  private boolean isLiftRideValid(LiftRide liftRide) {
    // Basic validation
    return liftRide.getTime() > 0 &&
        liftRide.getTime() <= 360 && // Max 6 hours (360 minutes)
        liftRide.getLiftID() > 0 &&
        liftRide.getLiftID() <= 40;
  }
}