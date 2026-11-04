package rabbitmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import java.util.HashMap;
import java.util.Map;

public class RabbitSetup {

  public static void initialize() throws Exception {

    Connection conn = RabbitMQManager.getConnection();
    Channel channel = conn.createChannel();
    Map<String, Object> args = new HashMap<>();

    args.put("x-message-ttl", 60000);
    args.put("x-max-length", 100000);

    channel.exchangeDeclare(
        "skitrak.exchange",
        "topic",
        true);

    for (int i = 1; i <= 10; i++) {
      String queue = "resort." + i;
      channel.queueDeclare(
          queue,
          true,
          false,
          false,
          args);

      channel.queueBind(
          queue,
          "skitrak.exchange",
          queue);
    }

    channel.close();
  }
}