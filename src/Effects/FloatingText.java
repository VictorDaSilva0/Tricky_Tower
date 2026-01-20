package Effects;

import java.awt.*;

public class FloatingText {
    public double x, y;
    public String text;
    public int life;
    public int maxLife;
    public Color color;

    public FloatingText(double x, double y, String text, Color color) {
        this.x = x;
        this.y = y;
        this.text = text;
        this.color = color;
        this.maxLife = 60; // 1 second roughly
        this.life = maxLife;
    }

    public boolean update() {
        y -= 1.0; // Float upwards
        life--;
        return life > 0;
    }

    public void draw(Graphics2D g2) {
        float alpha = (float) life / maxLife;
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g2.setColor(color);
        g2.setFont(new Font("Arial", Font.BOLD, 14));
        g2.drawString(text, (int) x, (int) y);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
    }
}
