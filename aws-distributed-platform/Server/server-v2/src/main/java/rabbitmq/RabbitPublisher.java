package rabbitmq;

import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.MessageProperties;
import model.SwipeEvent;

public class RabbitPublisher {

  private static final String EXCHANGE = "skitrak.exchange";
  private static final Gson gson = new Gson();
  private final ChannelPool pool;

  public RabbitPublisher(ChannelPool pool) {
    this.pool = pool;
  }

  public void publish(SwipeEvent event) throws Exception {
    Channel channel = null;
    try {
      channel = pool.borrowChannel();
      String json = gson.toJson(event);
      String routingKey = "resort." + event.getResortID();
      channel.basicPublish(
          EXCHANGE,
          routingKey,
          MessageProperties.PERSISTENT_TEXT_PLAIN,
          json.getBytes());
      channel.waitForConfirms(); // publisher confirms
    } finally {
      if (channel != null) {
        pool.returnChannel(channel);
      }
    }
  }
}