package Effects;

import java.awt.*;

public class Particle {
    public double x, y;
    public double vx, vy;
    public int life;
    public int maxLife;
    public Color color;
    public int size;

    public Particle(double x, double y, double vx, double vy, int life, int size, Color color) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.maxLife = life;
        this.life = life;
        this.size = size;
        this.color = color;
    }

    public boolean update() {
        x += vx;
        y += vy;
        life--;

        // Simple gravity
        vy += 0.2;

        return life > 0;
    }

    public void draw(Graphics2D g2) {
        float alpha = (float) life / maxLife;
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g2.setColor(color);
        g2.fillOval((int) x, (int) y, size, size);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
    }
}
