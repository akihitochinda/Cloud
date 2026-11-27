package rabbitmq;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class RabbitMQManager {

  private static Connection connection;

  private RabbitMQManager() {}

  public static synchronized Connection getConnection() throws Exception {

    if (connection == null || !connection.isOpen()) {
      ConnectionFactory factory = new ConnectionFactory();
      factory.setHost("172.31.36.28"); // RabbitMQ EC2 Private IPv4 addresses
      connection = factory.newConnection();
      System.out.println("RabbitMQ connection established");
    }
    return connection;
  }

  public static synchronized void close() throws Exception {

    if (connection != null && connection.isOpen()) {
      connection.close();
    }
  }
}