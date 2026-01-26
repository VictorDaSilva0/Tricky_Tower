package Main;

import java.awt.*;
import java.util.Random;

public class PowerUpManager {

    PlayManager pm;
    public boolean active = false;
    public String currentEvent = "";
    public int eventTimer = 0;
    public final int EVENT_DURATION = 300; // 5 seconds (at 60 FPS)
    public int cooldownTimer = 900; // 15 seconds between events
    Random rand = new Random();

    // Event Types
    public static final int EVt_NONE = 0;
    public static final int EVT_WIND = 1;
    public static final int EVT_HEAVY = 2;
    public static final int EVT_REVERSE = 3;

    int currentEventType = EVt_NONE;

    public PowerUpManager(PlayManager pm) {
        this.pm = pm;
    }

    public void update() {
        if (active) {
            applyEffect();
            eventTimer--;
            if (eventTimer <= 0) {
                active = false;
                currentEvent = "";
                currentEventType = EVt_NONE;
                cooldownTimer = 0;
            }
        }
    }

    // Triggered by Player Input
    public void castMagic(int playerID, int type) {
        if (active)
            return; // Cannot cast if event active

        active = true;
        eventTimer = EVENT_DURATION;
        currentEventType = type;

        // Visuals
        String name = "";
        switch (type) {
            case EVT_WIND:
                name = "TEMPÊTE !";
                break;
            case EVT_HEAVY:
                name = "BLOCS LOURDS !";
                break;
            case EVT_REVERSE:
                name = "CONFUSION !";
                break;
        }
        currentEvent = name;
        pm.effectManager.addFloatingText(pm.left_x + 100, pm.top_y + 200, name, Color.MAGENTA);
    }

    private void applyEffect() {
        if (pm.currentMino != null && pm.currentMino.active) {
            switch (currentEventType) {
                case EVT_WIND:
                    // Push sideways
                    float force = (rand.nextBoolean()) ? 300.0f : -300.0f;
                    pm.currentMino.body.applyForce(new org.jbox2d.common.Vec2(force, 0),
                            pm.currentMino.body.getWorldCenter());
                    break;
                case EVT_HEAVY:
                    // Push down strongly
                    pm.currentMino.body.applyForce(new org.jbox2d.common.Vec2(0, 300.0f),
                            pm.currentMino.body.getWorldCenter());
                    break;
                // Reverse is handled in PlayManager
            }
        }
    }

    public boolean isReverseActive() {
        return active && currentEventType == EVT_REVERSE;
    }

    public void draw(Graphics2D g2) {
        if (active) {
            g2.setColor(Color.MAGENTA);
            g2.setFont(new Font("Arial", Font.BOLD, 25));
            int w = g2.getFontMetrics().stringWidth(currentEvent);
            g2.drawString(currentEvent, pm.left_x + (pm.WIDTH / 2) - (w / 2), pm.top_y + 100);

            // Timer bar
            g2.fillRect(pm.left_x + (pm.WIDTH / 2) - 50, pm.top_y + 110,
                    (int) ((double) eventTimer / EVENT_DURATION * 100), 10);
        }
    }
}
