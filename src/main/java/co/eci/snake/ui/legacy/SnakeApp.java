package co.eci.snake.ui.legacy;

import co.eci.snake.concurrency.PauseControl;
import co.eci.snake.concurrency.SnakeRunner;
import co.eci.snake.core.Board;
import co.eci.snake.core.Direction;
import co.eci.snake.core.Position;
import co.eci.snake.core.Snake;
import co.eci.snake.core.engine.GameClock;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class SnakeApp extends JFrame {

  private final Board board;
  private final GamePanel gamePanel;
  private final JButton actionButton;
  private final JLabel statsLabel;
  private final GameClock clock;
  private final PauseControl pauseControl = new PauseControl();
  private final ExecutorService exec = Executors.newVirtualThreadPerTaskExecutor();
  private final java.util.List<Snake> snakes = new java.util.ArrayList<>();
  private boolean started = false;
  private boolean pausing = false;

  public SnakeApp() {
    super("The Snake Race");
    this.board = new Board(35, 28);

    int N = Integer.getInteger("snakes", 2);
    for (int i = 0; i < N; i++) {
      int x = 2 + (i * 3) % board.width();
      int y = 2 + (i * 2) % board.height();
      var dir = Direction.values()[i % Direction.values().length];
      snakes.add(Snake.of(i, x, y, dir));
    }

    this.gamePanel = new GamePanel(board, () -> snakes);
    this.actionButton = new JButton("Iniciar");
    this.statsLabel = new JLabel(" Presione Iniciar para comenzar");
    statsLabel.setFont(statsLabel.getFont().deriveFont(Font.PLAIN, 13f));

    JPanel south = new JPanel(new BorderLayout(8, 0));
    south.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
    south.add(actionButton, BorderLayout.WEST);
    south.add(statsLabel, BorderLayout.CENTER);

    setLayout(new BorderLayout());
    add(gamePanel, BorderLayout.CENTER);
    add(south, BorderLayout.SOUTH);

    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    pack();
    setLocationRelativeTo(null);

    this.clock = new GameClock(60, () -> SwingUtilities.invokeLater(gamePanel::repaint));

    for (Snake s : snakes) {
      pauseControl.register();
      exec.submit(new SnakeRunner(s, board, pauseControl));
    }

    actionButton.addActionListener((ActionEvent e) -> onAction());

    gamePanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("SPACE"), "pause");
    gamePanel.getActionMap().put("pause", new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        onAction();
      }
    });

    var player = snakes.get(0);
    InputMap im = gamePanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
    ActionMap am = gamePanel.getActionMap();
    im.put(KeyStroke.getKeyStroke("LEFT"), "left");
    im.put(KeyStroke.getKeyStroke("RIGHT"), "right");
    im.put(KeyStroke.getKeyStroke("UP"), "up");
    im.put(KeyStroke.getKeyStroke("DOWN"), "down");
    am.put("left", new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        player.turn(Direction.LEFT);
      }
    });
    am.put("right", new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        player.turn(Direction.RIGHT);
      }
    });
    am.put("up", new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        player.turn(Direction.UP);
      }
    });
    am.put("down", new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        player.turn(Direction.DOWN);
      }
    });

    if (snakes.size() > 1) {
      var p2 = snakes.get(1);
      im.put(KeyStroke.getKeyStroke('A'), "p2-left");
      im.put(KeyStroke.getKeyStroke('D'), "p2-right");
      im.put(KeyStroke.getKeyStroke('W'), "p2-up");
      im.put(KeyStroke.getKeyStroke('S'), "p2-down");
      am.put("p2-left", new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
          p2.turn(Direction.LEFT);
        }
      });
      am.put("p2-right", new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
          p2.turn(Direction.RIGHT);
        }
      });
      am.put("p2-up", new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
          p2.turn(Direction.UP);
        }
      });
      am.put("p2-down", new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
          p2.turn(Direction.DOWN);
        }
      });
    }

    setVisible(true);
  }

  private void onAction() {
    if (!started) {
      iniciar();
    } else if (pausing) {
      return;
    } else if (pauseControl.isPaused()) {
      reanudar();
    } else {
      pausar();
    }
  }

  private void iniciar() {
    started = true;
    clock.start();
    pauseControl.resume();
    actionButton.setText("Pausar");
    statsLabel.setText(" En juego");
    gamePanel.setPauseInfo(null);
  }

  private void pausar() {
    pausing = true;
    clock.pause();
    pauseControl.pause();
    actionButton.setEnabled(false);
    statsLabel.setText(" Esperando a que las serpientes se detengan...");
    exec.submit(() -> {
      try {
        pauseControl.awaitQuiescent();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        SwingUtilities.invokeLater(() -> {
          actionButton.setEnabled(true);
          pausing = false;
        });
        return;
      }
      SwingUtilities.invokeLater(() -> {
        String info = armarEstadisticas();
        statsLabel.setText(" " + info);
        gamePanel.setPauseInfo(info);
        gamePanel.repaint();
        actionButton.setText("Reanudar");
        actionButton.setEnabled(true);
        pausing = false;
      });
    });
  }

  private void reanudar() {
    gamePanel.setPauseInfo(null);
    statsLabel.setText(" En juego");
    actionButton.setText("Pausar");
    pauseControl.resume();
    clock.resume();
    gamePanel.repaint();
  }

  private String armarEstadisticas() {
    Snake masLarga = null;
    Snake peor = null;
    for (Snake s : snakes) {
      if (s.isAlive()) {
        if (masLarga == null || s.length() > masLarga.length()) {
          masLarga = s;
        }
      } else {
        if (peor == null || s.deathMillis() < peor.deathMillis()) {
          peor = s;
        }
      }
    }

    String viva = (masLarga == null)
        ? "ninguna viva"
        : "#" + masLarga.id() + " (largo " + masLarga.length() + ")";
    String muerta = (peor == null)
        ? "todavia no muere ninguna"
        : "#" + peor.id() + " (largo " + peor.length() + ")";
    return "Viva mas larga: " + viva + "   |   Peor (primera en morir): " + muerta;
  }

  public static final class GamePanel extends JPanel {
    private final Board board;
    private final Supplier snakesSupplier;
    private final int cell = 20;
    private volatile String pauseInfo;

    @FunctionalInterface
    public interface Supplier {
      List<Snake> get();
    }

    public GamePanel(Board board, Supplier snakesSupplier) {
      this.board = board;
      this.snakesSupplier = snakesSupplier;
      setPreferredSize(new Dimension(board.width() * cell + 1, board.height() * cell + 40));
      setBackground(Color.WHITE);
    }

    public void setPauseInfo(String info) {
      this.pauseInfo = info;
    }

    @Override
    protected void paintComponent(Graphics g) {
      super.paintComponent(g);
      var g2 = (Graphics2D) g.create();
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

      g2.setColor(new Color(220, 220, 220));
      for (int x = 0; x <= board.width(); x++)
        g2.drawLine(x * cell, 0, x * cell, board.height() * cell);
      for (int y = 0; y <= board.height(); y++)
        g2.drawLine(0, y * cell, board.width() * cell, y * cell);

      // Obstáculos
      g2.setColor(new Color(255, 102, 0));
      for (var p : board.obstacles()) {
        int x = p.x() * cell, y = p.y() * cell;
        g2.setColor(new Color(255, 102, 0));
        g2.fillRect(x + 2, y + 2, cell - 4, cell - 4);
        g2.setColor(Color.RED);
        g2.drawLine(x + 4, y + 4, x + cell - 6, y + 4);
        g2.drawLine(x + 4, y + 8, x + cell - 6, y + 8);
        g2.drawLine(x + 4, y + 12, x + cell - 6, y + 12);
      }

      // Ratones
      g2.setColor(Color.BLACK);
      for (var p : board.mice()) {
        int x = p.x() * cell, y = p.y() * cell;
        g2.setColor(Color.BLACK);
        g2.fillOval(x + 4, y + 4, cell - 8, cell - 8);
        g2.setColor(Color.WHITE);
        g2.fillOval(x + 8, y + 8, cell - 16, cell - 16);
      }

      // Teleports (flechas rojas)
      Map<Position, Position> tp = board.teleports();
      g2.setColor(Color.RED);
      for (var entry : tp.entrySet()) {
        Position from = entry.getKey();
        int x = from.x() * cell, y = from.y() * cell;
        int[] xs = { x + 4, x + cell - 4, x + cell - 10, x + cell - 10, x + 4 };
        int[] ys = { y + cell / 2, y + cell / 2, y + 4, y + cell - 4, y + cell / 2 };
        g2.fillPolygon(xs, ys, xs.length);
      }

      // Turbo (rayos)
      g2.setColor(Color.BLACK);
      for (var p : board.turbo()) {
        int x = p.x() * cell, y = p.y() * cell;
        int[] xs = { x + 8, x + 12, x + 10, x + 14, x + 6, x + 10 };
        int[] ys = { y + 2, y + 2, y + 8, y + 8, y + 16, y + 10 };
        g2.fillPolygon(xs, ys, xs.length);
      }

      // Serpientes
      var snakes = snakesSupplier.get();
      int idx = 0;
      for (Snake s : snakes) {
        var body = s.snapshot().toArray(new Position[0]);
        for (int i = 0; i < body.length; i++) {
          var p = body[i];
          if (!s.isAlive()) {
            g2.setColor(new Color(140, 140, 140));
          } else {
            Color base = Color.getHSBColor((idx * 0.13f) % 1f, 0.75f, 0.72f);
            int shade = Math.max(0, 40 - i * 4);
            g2.setColor(new Color(
                Math.min(255, base.getRed() + shade),
                Math.min(255, base.getGreen() + shade),
                Math.min(255, base.getBlue() + shade)));
          }
          g2.fillRect(p.x() * cell + 2, p.y() * cell + 2, cell - 4, cell - 4);
        }
        if (body.length > 0) {
          var head = body[0];
          g2.setColor(Color.WHITE);
          g2.setFont(g2.getFont().deriveFont(Font.BOLD, 10f));
          g2.drawString(String.valueOf(s.id()), head.x() * cell + 6, head.y() * cell + 14);
        }
        idx++;
      }

      String info = pauseInfo;
      if (info != null) {
        int barY = board.height() * cell;
        g2.setColor(new Color(40, 40, 40, 210));
        g2.fillRect(0, barY, getWidth(), 40);
        g2.setColor(Color.WHITE);
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 12f));
        g2.drawString(info, 8, barY + 24);
      }

      g2.dispose();
    }
  }

  public static void launch() {
    SwingUtilities.invokeLater(SnakeApp::new);
  }
}
