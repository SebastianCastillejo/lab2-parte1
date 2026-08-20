package co.eci.snake.concurrency;

public final class PauseControl {

  private boolean paused = true;
  private int running = 0;
  private int parked = 0;

  public synchronized void register() {
    running++;
  }

  public synchronized void unregister() {
    running--;
    notifyAll();
  }

  public synchronized void awaitIfPaused() throws InterruptedException {
    while (paused) {
      parked++;
      notifyAll();
      try {
        wait();
      } finally {
        parked--;
      }
    }
  }

  public synchronized void pause() {
    paused = true;
  }

  public synchronized void awaitQuiescent() throws InterruptedException {
    while (parked < running) {
      wait();
    }
  }

  public synchronized void resume() {
    paused = false;
    notifyAll();
  }

  public synchronized boolean isPaused() {
    return paused;
  }
}
