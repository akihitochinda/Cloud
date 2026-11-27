package websocket;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import rabbitmq.RabbitMQManager;

public class LeaderboardUpdateConsumer implements Runnable {

  private final Gson gson = new Gson();

  @Override
  public void run() {
    try {
      Connection connection = RabbitMQManager.getConnection();
      Channel channel = connection.createChannel();

      // Consumers publish leaderboard updates to a fanout exchange;
      channel.exchangeDeclare(
          "leaderboard.fanout",
          "fanout",
          true);

      String queue = channel.queueDeclare().getQueue();

      // every WebSocket server binds a queue and pushes to its local subscribers.
      channel.queueBind(
          queue,
          "leaderboard.fanout",
          "");

      channel.basicConsume(
          queue,
          true,
          (tag, delivery) -> {
            String json = new String(delivery.getBody());
            JsonObject obj = gson.fromJson(json, JsonObject.class);
            String resortID = obj.get("resortID").getAsString();
            LeaderboardCache.put(resortID, json);
          },
          tag -> {});
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
}