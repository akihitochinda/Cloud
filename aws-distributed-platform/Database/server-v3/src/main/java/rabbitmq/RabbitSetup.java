package rabbitmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import java.util.HashMap;
import java.util.Map;

public class RabbitSetup {
  private static final long DEFAULT_TEST_DURATION_MS = 30L * 60 * 1000; // a documented duration (e.g., 30 minutes)
  private static final long QUEUE_DRAIN_GRACE_MS = 30L * 60 * 1000; // 30-minute drain grace

  public static void initialize() throws Exception {

    int queueTtlMs = getQueueTtlMs();
    Connection conn = RabbitMQManager.getConnection();
    Channel channel = conn.createChannel();
    Map<String, Object> args = new HashMap<>();

    args.put("x-message-ttl", queueTtlMs);
    args.put("x-max-length", 100000);
    System.out.println("Rabbit resort queue TTL=" + queueTtlMs + " ms");

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

  private static int getQueueTtlMs() {
    long configuredDurationMs = getPositiveLongEnvironmentValue(
        "TEST_DURATION_MS",
        DEFAULT_TEST_DURATION_MS);
    long queueTtlMs;
    try {
      queueTtlMs = Math.addExact(configuredDurationMs, QUEUE_DRAIN_GRACE_MS);
    } catch (ArithmeticException e) {
      throw new IllegalArgumentException("TEST_DURATION_MS is too large to calculate queue TTL", e);
    }
    if (queueTtlMs > Integer.MAX_VALUE) {
      throw new IllegalArgumentException("TEST_DURATION_MS is too large for RabbitMQ queue TTL");
    }
    return (int) queueTtlMs;
  }

  private static long getPositiveLongEnvironmentValue(String name, long defaultValue) {
    String configured = System.getenv(name);
    if (configured == null || configured.isBlank()) {
      return defaultValue;
    }
    try {
      long value = Long.parseLong(configured);
      if (value < 1) {
        throw new IllegalArgumentException(name + " must be at least 1");
      }
      return value;
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(name + " must be a positive integer", e);
    }
  }
}