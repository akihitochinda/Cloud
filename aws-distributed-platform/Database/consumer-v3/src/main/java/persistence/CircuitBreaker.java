package persistence;

public class CircuitBreaker {
  private static final int FAILURE_THRESHOLD = 5;
  private static final long OPEN_TIMEOUT_MS = 30000;
  private volatile CircuitState state = CircuitState.CLOSED;
  private int failureCount = 0;
  private long openedAt = 0L;

  public synchronized boolean allowRequest() {
    if (state == CircuitState.CLOSED) {
      return true;
    }
    if (state == CircuitState.OPEN) {
      long elapsed = System.currentTimeMillis() - openedAt;
      if (elapsed >= OPEN_TIMEOUT_MS) {
        state = CircuitState.HALF_OPEN;
        return true;
      }
      return false;
    }
    return true;
  }

  public synchronized void recordSuccess() {
    failureCount = 0;
    state = CircuitState.CLOSED;
  }

  public synchronized void recordFailure() {
    failureCount++;
    if (failureCount >= FAILURE_THRESHOLD) {
      state = CircuitState.OPEN;
      openedAt = System.currentTimeMillis();
    }
  }

  public CircuitState getState() {
    return state;
  }

  public int getFailureCount() {
    return failureCount;
  }
}