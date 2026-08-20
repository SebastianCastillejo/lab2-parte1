package co.eci.snake.core;

import java.util.ArrayDeque;
import java.util.Deque;

public final class Snake {
  private final int id;
  private final Deque<Position> body = new ArrayDeque<>();
  private volatile Direction direction;
  private int maxLength = 5;
  private volatile boolean alive = true;
  private long deathMillis = 0;

  private Snake(int id, Position start, Direction dir) {
    this.id = id;
    body.addFirst(start);
    this.direction = dir;
  }

  public static Snake of(int id, int x, int y, Direction dir) {
    return new Snake(id, new Position(x, y), dir);
  }

  public int id() {
    return id;
  }

  public Direction direction() {
    return direction;
  }

  public boolean isAlive() {
    return alive;
  }

  public long deathMillis() {
    return deathMillis;
  }

  public void turn(Direction dir) {
    if ((direction == Direction.UP && dir == Direction.DOWN) ||
        (direction == Direction.DOWN && dir == Direction.UP) ||
        (direction == Direction.LEFT && dir == Direction.RIGHT) ||
        (direction == Direction.RIGHT && dir == Direction.LEFT)) {
      return;
    }
    this.direction = dir;
  }

  public synchronized Position head() {
    return body.peekFirst();
  }

  public synchronized int length() {
    return body.size();
  }

  public synchronized Deque<Position> snapshot() {
    return new ArrayDeque<>(body);
  }

  public synchronized boolean hitsSelf(Position next) {
    if (body.size() <= 1) return false;
    Position tail = body.peekLast();
    for (Position p : body) {
      if (p.equals(next) && !p.equals(tail)) return true;
    }
    return false;
  }

  public synchronized void advance(Position newHead, boolean grow) {
    if (!alive) return;
    body.addFirst(newHead);
    if (grow) maxLength++;
    while (body.size() > maxLength) body.removeLast();
  }

  public synchronized void die() {
    if (!alive) return;
    alive = false;
    deathMillis = System.currentTimeMillis();
  }
}
