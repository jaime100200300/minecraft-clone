import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import javax.imageio.ImageIO;

class MenuPanel extends JPanel {
    public MenuPanel(Runnable onPlay) {
        setLayout(null);
        setBackground(new Color(30, 30, 30));

        setPreferredSize(new Dimension(800, 600)); // FIX

        JButton playButton = new JButton("Play");
        playButton.setBounds(350, 250, 100, 40);
        playButton.addActionListener(e -> onPlay.run());
        add(playButton);

        JButton quitButton = new JButton("Quit");
        quitButton.setBounds(350, 300, 100, 40);
        quitButton.addActionListener(e -> System.exit(0));
        add(quitButton);
    }
}



public class Main extends JPanel implements KeyListener, MouseMotionListener {
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;

    private static final int MAP_X = 32;
    private static final int MAP_Y = 16;
    private static final int MAP_Z = 32;
    private byte[][][] world = new byte[MAP_X][MAP_Y][MAP_Z];
    private double px = 8.0;
    private double pz = 8.0;
    private double py = 10.0;

    private double yaw = 0.0, pitch = 0.0;
    private boolean[] keys = new boolean[256];

    private double vy = 0.0;
    private boolean onGround = false;
    // time for day-night cycle (radians)
    private double time = 0.0;
    private final double gravity = 0.01;
    private final double jumpStrength = 0.25;
    private final double playerHeight = 1.7;
    private final double playerRadius = 0.3;

    public Main() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setFocusable(true);
        addKeyListener(this);
        addMouseMotionListener(this);

        setCursor(Toolkit.getDefaultToolkit().createCustomCursor(
                new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB), new Point(0, 0), "blank"));

        for (int x = 0; x < MAP_X; x++) {
            for (int z = 0; z < MAP_Z; z++) {
                world[x][0][z] = 1;
                int height = (int)(Math.sin(x * 0.4) * Math.cos(z * 0.4) * 2) + 2;
                for (int y = 1; y <= height && y < MAP_Y; y++) {
                    world[x][y][z] = (byte)(y == height ? 2 : 3);
                }
            }
        }

        py = getTerrainHeight((int)px, (int)pz) + 1.0;

        new Thread(() -> {
            while (true) {
                updateInputs();
                repaint();
                try { Thread.sleep(16); } catch (InterruptedException e) {}
            }
        }).start();
    }

    private void updateInputs() {
        double speed = 0.12;

        double forwardX = -Math.sin(yaw);
        double forwardZ = Math.cos(yaw);
        double rightX = Math.cos(yaw);
        double rightZ = Math.sin(yaw);

        double nx = px;
        double nz = pz;

        if (keys[KeyEvent.VK_W]) { nx += forwardX * speed; nz += forwardZ * speed; }
        if (keys[KeyEvent.VK_S]) { nx -= forwardX * speed; nz -= forwardZ * speed; }
        if (keys[KeyEvent.VK_A]) { nx -= rightX * speed;   nz -= rightZ * speed; }
        if (keys[KeyEvent.VK_D]) { nx += rightX * speed;   nz += rightZ * speed; }

        if (!isColliding(nx, py, pz)) px = nx;
        if (!isColliding(px, py, nz)) pz = nz;

        vy -= gravity;
        double newY = py + vy;

        if (vy < 0 && isColliding(px, newY, pz)) {
            vy = 0;
            onGround = true;
        } else {
            py = newY;
            onGround = false;
        }

        if (keys[KeyEvent.VK_SPACE] && onGround) {
            vy = jumpStrength;
            onGround = false;
        }

        // advance time (slow)
        time += 0.002;
        if (time > Math.PI * 2) time -= Math.PI * 2;
    }

    private int getTerrainHeight(int x, int z) {
        for (int y = MAP_Y - 1; y >= 0; y--) {
            if (world[x][y][z] != 0) return y;
        }
        return 0;
    }

    private boolean isColliding(double x, double y, double z) {
        double[][] checks = {
            {x - playerRadius, y, z - playerRadius},
            {x + playerRadius, y, z - playerRadius},
            {x - playerRadius, y, z + playerRadius},
            {x + playerRadius, y, z + playerRadius},
            {x - playerRadius, y + playerHeight, z - playerRadius},
            {x + playerRadius, y + playerHeight, z - playerRadius},
            {x - playerRadius, y + playerHeight, z + playerRadius},
            {x + playerRadius, y + playerHeight, z + playerRadius},
        };

        for (double[] c : checks) {
            int bx = (int)Math.floor(c[0]);
            int by = (int)Math.floor(c[1]);
            int bz = (int)Math.floor(c[2]);

            if (bx < 0 || bx >= MAP_X ||
                by < 0 || by >= MAP_Y ||
                bz < 0 || bz >= MAP_Z) return true;

            if (world[bx][by][bz] != 0) return true;
        }

        return false;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);

        // day-night cycle: time ranges 0..2PI, use sin to get [0..1]
        double dayFactor = 0.5 * (Math.sin(time) + 1.0); // 0 = night, 1 = day
        Color daySky = new Color(135, 206, 235);
        Color nightSky = new Color(20, 24, 82);
        Color sky = lerpColor(nightSky, daySky, dayFactor);
        g2d.setColor(sky);
        g2d.fillRect(0, 0, WIDTH, HEIGHT / 2);

        // ground tint darker at night
        Color dayGround = new Color(70, 70, 70);
        Color nightGround = new Color(20, 20, 30);
        Color ground = lerpColor(nightGround, dayGround, dayFactor);
        g2d.setColor(ground);
        g2d.fillRect(0, HEIGHT / 2, WIDTH, HEIGHT / 2);

        double cosYaw = Math.cos(-yaw), sinYaw = Math.sin(-yaw);
        double cosPitch = Math.cos(-pitch), sinPitch = Math.sin(-pitch);

        double fov = 400.0;

        class Face {
            Polygon poly;
            Color color;
            double depth;
        }

        List<Face> faces = new ArrayList<>();

        for (int x = 0; x < MAP_X; x++) {
            for (int y = 0; y < MAP_Y; y++) {
                for (int z = 0; z < MAP_Z; z++) {
                    int id = world[x][y][z];
                    if (id == 0) continue;

                    boolean leftVisible   = (x == 0 || world[x-1][y][z] == 0);
                    boolean rightVisible  = (x == MAP_X-1 || world[x+1][y][z] == 0);
                    boolean bottomVisible = (y == 0 || world[x][y-1][z] == 0);
                    boolean topVisible    = (y == MAP_Y-1 || world[x][y+1][z] == 0);
                    boolean frontVisible  = (z == 0 || world[x][y][z-1] == 0);
                    boolean backVisible   = (z == MAP_Z-1 || world[x][y][z+1] == 0);

                    if (!leftVisible && !rightVisible && !bottomVisible && !topVisible && !frontVisible && !backVisible)
                        continue;

                    Color base = getBlockColor(id);

                    double[][] v = new double[8][3];
                    int index = 0;
                    for (int dx = 0; dx <= 1; dx++) {
                        for (int dy = 0; dy <= 1; dy++) {
                            for (int dz = 0; dz <= 1; dz++) {
                                double tx = (x + dx) - px;
                                double ty = (y + dy) - (py + playerHeight);
                                double tz = (z + dz) - pz;

                                double rx = tx * cosYaw - tz * sinYaw;
                                double rz = tx * sinYaw + tz * cosYaw;

                                double ry = ty * cosPitch - rz * sinPitch;
                                double fz = ty * sinPitch + rz * cosPitch;

                                v[index][0] = rx;
                                v[index][1] = ry;
                                v[index][2] = fz;
                                index++;
                            }
                        }
                    }

                    java.util.function.BiConsumer<int[], Color> addFace = (indices, color) -> {
                        double avgZ = 0;
                        int[] xs = new int[4];
                        int[] ys = new int[4];
                        for (int i = 0; i < 4; i++) {
                            double[] p = v[indices[i]];
                            if (p[2] <= 0.05) return;
                            int sx = (int)(WIDTH / 2 + (p[0] / p[2]) * fov);
                            int sy = (int)(HEIGHT / 2 - (p[1] / p[2]) * fov);
                            xs[i] = sx;
                            ys[i] = sy;
                            avgZ += p[2];
                        }
                        avgZ /= 4.0;

                        Polygon poly = new Polygon(xs, ys, 4);
                        Face face = new Face();
                        face.poly = poly;
                        face.color = color;
                        face.depth = avgZ;
                        faces.add(face);
                    };

                    if (topVisible)
                        addFace.accept(new int[]{2,3,7,6}, base);
                    if (bottomVisible)
                        addFace.accept(new int[]{0,1,5,4}, getShadedColor(base, 0.6f));
                    if (leftVisible)
                        addFace.accept(new int[]{0,1,3,2}, getShadedColor(base, 0.8f));
                    if (rightVisible)
                        addFace.accept(new int[]{4,5,7,6}, getShadedColor(base, 0.8f));
                    if (frontVisible)
                        addFace.accept(new int[]{0,2,6,4}, getShadedColor(base, 0.7f));
                    if (backVisible)
                        addFace.accept(new int[]{1,3,7,5}, getShadedColor(base, 0.7f));
                }
            }
        }

        faces.sort((a, b) -> Double.compare(b.depth, a.depth));

        for (Face f : faces) {
            g2d.setColor(f.color);
            g2d.fillPolygon(f.poly);
        }

        g2d.setColor(Color.WHITE);
        g2d.drawLine(WIDTH/2 - 8, HEIGHT/2, WIDTH/2 + 8, HEIGHT/2);
        g2d.drawLine(WIDTH/2, HEIGHT/2 - 8, WIDTH/2, HEIGHT/2 + 8);
    }

    private Color getBlockColor(int id) {
        return switch (id) {
            case 1 -> new Color(110, 110, 110);
            case 2 -> new Color(34, 139, 34);
            case 3 -> new Color(139, 69, 19);
            default -> Color.MAGENTA;
        };
    }

    public static Image loadEmbeddedIcon() {
        try {
            return ImageIO.read(new ByteArrayInputStream(IconData.ICON));
        } catch (Exception e) {
            return null;
        }
    }

    private Color getShadedColor(Color base, float factor) {
        return new Color(
                Math.min(255, (int)(base.getRed() * factor)),
                Math.min(255, (int)(base.getGreen() * factor)),
                Math.min(255, (int)(base.getBlue() * factor))
        );
    }

    private Color lerpColor(Color a, Color b, double t) {
        t = Math.max(0.0, Math.min(1.0, t));
        int r = (int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * t);
        int g = (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t);
        int bl = (int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t);
        return new Color(Math.min(255, Math.max(0, r)), Math.min(255, Math.max(0, g)), Math.min(255, Math.max(0, bl)));
    }

    public void keyPressed(KeyEvent e) { if (e.getKeyCode() < 256) keys[e.getKeyCode()] = true; }
    public void keyReleased(KeyEvent e) { if (e.getKeyCode() < 256) keys[e.getKeyCode()] = false; }
    public void keyTyped(KeyEvent e) {}

    public void mouseMoved(MouseEvent e) {
        int centerX = WIDTH / 2;
        int centerY = HEIGHT / 2;
        if (e.getX() == centerX && e.getY() == centerY) return;

        double sensitivity = 0.005;
        yaw -= (e.getX() - centerX) * sensitivity;
        pitch += (e.getY() - centerY) * sensitivity;

        pitch = Math.max(-Math.PI / 2.5, Math.min(Math.PI / 2.5, pitch));

        try {
            Robot robot = new Robot();
            Point windowLocation = this.getLocationOnScreen();
            robot.mouseMove(windowLocation.x + centerX, windowLocation.y + centerY);
        } catch (Exception ex) {}
    }
    public void mouseDragged(MouseEvent e) { mouseMoved(e); }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Minecraft Clone");

        // Create menu
        MenuPanel menu = new MenuPanel(() -> {
            frame.getContentPane().removeAll();
            Main gamePanel = new Main();
            frame.add(gamePanel);
            frame.revalidate();
            frame.repaint();
            gamePanel.requestFocusInWindow();
        });

        frame.add(menu);

        Image icon = loadEmbeddedIcon();
        if (icon != null) frame.setIconImage(icon);

        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setVisible(true);
    }

}
