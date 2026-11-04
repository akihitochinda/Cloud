package rabbitmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

// Required: pool and reuse channels — do NOT open one per publish.
public class ChannelPool {

  private final BlockingQueue<Channel> pool;
  private Connection connection;
  private final int poolSize;

  public ChannelPool(int poolSize) {
    this.poolSize = poolSize;
    this.pool = new ArrayBlockingQueue<>(poolSize);
  }

  /* create one connection, pre-create channels */
  public void init() throws Exception {
    connection = RabbitMQManager.getConnection();

    for (int i = 0; i < poolSize; i++) {
      Channel channel = connection.createChannel();
      channel.confirmSelect(); // publisher confirms
      pool.offer(channel);
    }
  }

  /* take from pool */
  public Channel borrowChannel() throws Exception {
    if (connection == null || !connection.isOpen()) {
      reconnect();
    }
    return pool.take();
  }

  /* return to pool */
  public void returnChannel(Channel channel) {
    if (channel != null && channel.isOpen()) {
      pool.offer(channel);
    }
  }

  private synchronized void reconnect() throws Exception {
    pool.clear();
    connection = RabbitMQManager.getConnection();
    for (int i = 0; i < poolSize; i++) {
      Channel channel = connection.createChannel();
      channel.confirmSelect(); // publisher confirms
      pool.offer(channel);
    }
  }
}