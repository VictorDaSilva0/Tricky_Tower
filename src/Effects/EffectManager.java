package Effects;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class EffectManager {

    ArrayList<Particle> particles = new ArrayList<>();
    ArrayList<FloatingText> texts = new ArrayList<>();
    Random rand = new Random();

    public void update() {
        Iterator<Particle> pIter = particles.iterator();
        while (pIter.hasNext()) {
            Particle p = pIter.next();
            if (!p.update()) {
                pIter.remove();
            }
        }

        Iterator<FloatingText> tIter = texts.iterator();
        while (tIter.hasNext()) {
            FloatingText t = tIter.next();
            if (!t.update()) {
                tIter.remove();
            }
        }
    }

    public void draw(Graphics2D g2) {
        for (Particle p : particles) {
            p.draw(g2);
        }
        for (FloatingText t : texts) {
            t.draw(g2);
        }
    }

    public void addLandingEffect(int x, int y) {
        // Create dust/sparks
        for (int i = 0; i < 20; i++) {
            double vx = (rand.nextDouble() - 0.5) * 6;
            double vy = (rand.nextDouble() - 0.5) * 6 - 2; // Bias slightly upwards
            int life = 20 + rand.nextInt(20);
            int size = 3 + rand.nextInt(4);
            // Gray/White dust
            Color c = new Color(200, 200, 200);
            particles.add(new Particle(x, y, vx, vy, life, size, c));
        }
    }

    public void addConfetti(int x, int y) {
        // Colorful victory particles
        for (int i = 0; i < 5; i++) { // Add a few per frame called
            double vx = (rand.nextDouble() - 0.5) * 10;
            double vy = (rand.nextDouble() - 1.0) * 10 - 5; // Shoot up
            int life = 60 + rand.nextInt(60);
            int size = 5 + rand.nextInt(5);
            Color c = Color.getHSBColor(rand.nextFloat(), 0.8f, 0.8f);
            particles.add(new Particle(x, y, vx, vy, life, size, c));
        }
    }

    public void addFloatingText(int x, int y, String msg, Color c) {
        texts.add(new FloatingText(x, y, msg, c));
    }

    public void addLossEffect(int x, int y) {
        // Red particles rising
        for (int i = 0; i < 15; i++) {
            double vx = (rand.nextDouble() - 0.5) * 4;
            double vy = -(rand.nextDouble() * 3 + 2); // Upwards
            int life = 30 + rand.nextInt(30);
            int size = 4 + rand.nextInt(6);
            Color c = Color.RED;
            particles.add(new Particle(x, y, vx, vy, life, size, c));
        }
    }

    public void addExplosion(int x, int y) {
        // Big BOOM
        for (int i = 0; i < 40; i++) {
            double angle = rand.nextDouble() * 2 * Math.PI;
            double speed = rand.nextDouble() * 8 + 2;
            double vx = Math.cos(angle) * speed;
            double vy = Math.sin(angle) * speed;

            int life = 40 + rand.nextInt(30);
            int size = 6 + rand.nextInt(8);

            // Fire colors
            Color c;
            int r = rand.nextInt(3);
            if (r == 0)
                c = Color.RED;
            else if (r == 1)
                c = Color.ORANGE;
            else
                c = Color.YELLOW;

            particles.add(new Particle(x, y, vx, vy, life, size, c));
        }
    }

    public void addTrail(int x, int y, Color c) {
        // Small particle staying mostly still to create a trail
        double vx = (rand.nextDouble() - 0.5) * 1;
        double vy = (rand.nextDouble() - 0.5) * 1;
        int life = 15; // Short life
        int size = 4;
        // Make color slightly transparent or lighter?
        particles.add(new Particle(x, y, vx, vy, life, size, c));
    }

    public void addLockFlash(int x, int y) {
        // Bright flash
        for (int i = 0; i < 10; i++) {
            double vx = (rand.nextDouble() - 0.5) * 10;
            double vy = (rand.nextDouble() - 0.5) * 10;
            int life = 10;
            int size = 2 + rand.nextInt(10);
            particles.add(new Particle(x, y, vx, vy, life, size, Color.WHITE));
        }
    }
}
