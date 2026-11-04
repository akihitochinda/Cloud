import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import rabbitmq.ChannelPool;
import rabbitmq.RabbitPublisher;
import model.SwipeEvent;

public class PoolTest {

  public static void main(String[] args) throws Exception {

    ChannelPool pool = new ChannelPool(20);
    pool.init();
    RabbitPublisher publisher = new RabbitPublisher(pool);
    ExecutorService exec = Executors.newFixedThreadPool(50);

    for (int i = 0; i < 1000; i++) {
      exec.submit(() -> {
        try {
          SwipeEvent event =
              new SwipeEvent(
                  UUID.randomUUID().toString(),
                  "1",
                  "1",
                  "100",
                  "5",
                  200,
                  217,
                  Instant.now().toString(),
                  "server-1"
              );
          publisher.publish(event);
        } catch (Exception e) {
          e.printStackTrace();
        }
      });
    }
    exec.shutdown();

    if (!exec.awaitTermination(1, TimeUnit.MINUTES)) {
      exec.shutdownNow();
    }
  }
}