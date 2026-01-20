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
                cooldownTimer = 900; // Reset Cooldown
            }
        } else {
            cooldownTimer--;
            if (cooldownTimer <= 0) {
                activateRandomEvent();
            }
        }
    }

    private void activateRandomEvent() {
        active = true;
        eventTimer = EVENT_DURATION; // 5s duration

        int r = rand.nextInt(3) + 1; // 1 to 3
        currentEventType = r;

        switch (currentEventType) {
            case EVT_WIND:
                currentEvent = "EVENT: WIND!";
                break;
            case EVT_HEAVY:
                currentEvent = "EVENT: HEAVY GRAVITY!";
                break;
            case EVT_REVERSE:
                currentEvent = "EVENT: REVERSE CONTROLS!";
                break;
        }

        // Visual cue
        pm.effectManager.addFloatingText(pm.left_x + 100, pm.top_y + 200, currentEvent, Color.MAGENTA);
    }

    private void applyEffect() {
        if (pm.currentMino != null && pm.currentMino.active) {
            switch (currentEventType) {
                case EVT_WIND:
                    // Push right randomly
                    if (rand.nextInt(10) < 2) {
                        pm.currentMino.body.applyLinearImpulse(new org.jbox2d.common.Vec2(20.0f, 0),
                                pm.currentMino.body.getWorldCenter());
                    } else if (rand.nextInt(10) > 8) {
                        pm.currentMino.body.applyLinearImpulse(new org.jbox2d.common.Vec2(-20.0f, 0),
                                pm.currentMino.body.getWorldCenter());
                    }
                    break;
                case EVT_HEAVY:
                    // Push down
                    pm.currentMino.body.applyLinearImpulse(new org.jbox2d.common.Vec2(0, 15.0f),
                            pm.currentMino.body.getWorldCenter());
                    break;
                // Reverse handled in PlayManager input check
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
