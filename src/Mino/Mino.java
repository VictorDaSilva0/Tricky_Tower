package Mino;

import Main.PlayManager;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.*;
import java.awt.*;
import java.awt.geom.AffineTransform;

public abstract class Mino {

    public Body body;
    public Color c;
    public boolean active = true;
    public boolean collided = false; // Pour savoir si on est posé

    protected Vec2[] blockOffsets = new Vec2[4];

    public void create(Color c) {
        this.c = c;
    }

    public abstract void setShape();

    public void createBody(World world, float xPixels, float yPixels, boolean isStatic) {
        setShape();

        BodyDef bd = new BodyDef();
        bd.type = isStatic ? BodyType.STATIC : BodyType.DYNAMIC;
        bd.position.set(xPixels / PlayManager.SCALE, yPixels / PlayManager.SCALE);

        // Configuration Tetris Classique (en l'air)
        bd.fixedRotation = true; // Empêche de tourner tout seul en frottant les murs
        bd.gravityScale = 0.0f;  // On gère la chute manuellement

        body = world.createBody(bd);
        body.setUserData(this);

        for (Vec2 offset : blockOffsets) {
            PolygonShape shape = new PolygonShape();
            float halfSize = (Block.SIZE / 2.0f) / PlayManager.SCALE;
            shape.setAsBox(halfSize, halfSize, offset, 0);

            FixtureDef fd = new FixtureDef();
            fd.shape = shape;
            fd.density = 1.0f;
            fd.friction = 0.3f; // Friction plus basse pour ne pas "accrocher" les murs
            fd.restitution = 0.0f;

            body.createFixture(fd);
        }
    }

    public void update(boolean up, boolean left, boolean right, boolean down) {
        if (body == null || !active) return;

        // 1. DÉTECTION DE COLLISION (Code corrigé avec ContactEdge)
        if (!collided) {
            for (org.jbox2d.dynamics.contacts.ContactEdge edge = body.getContactList(); edge != null; edge = edge.next) {
                if (edge.contact.isTouching()) {
                    collided = true;
                    // On stoppe net la pièce pour annuler l'élan du joueur
                    body.setLinearVelocity(new Vec2(0, 0));
                    body.setAngularVelocity(0);
                    // Activation de la physique réaliste
                    body.setGravityScale(1.0f);
                    body.setFixedRotation(false);
                    body.setAwake(true);
                    return;
                }
            }
        }

        // 2. MOUVEMENT
        if (collided) {
            // Une fois posé, on laisse la physique faire (Tricky Towers)
            return;
        } else {
            // En l'air : Mode Arcade / Tetris

            // VITESSE LATÉRALE : Réduite pour plus de précision (10.0f -> 8.0f ou 9.0f)
            float moveSpeed = 9.0f;
            float fallSpeed = 5.0f;  // Chute constante
            float dropSpeed = 25.0f; // Flèche bas

            float xVel = 0;
            float yVel = fallSpeed;

            if (left) xVel = -moveSpeed;
            if (right) xVel = moveSpeed;
            if (down) yVel = dropSpeed;

            // Application de la vitesse
            body.setLinearVelocity(new Vec2(xVel, yVel));

            // Rotation 90°
            if (up) {
                float currentAngle = body.getAngle();
                body.setTransform(body.getPosition(), currentAngle + (float)(Math.PI / 2));
                body.setAngularVelocity(0);
            }
        }
    }

    public boolean isStopped() {
        if (body == null) return false;
        // La pièce est arrêtée si elle bouge très peu
        return body.getLinearVelocity().length() < 0.1f && Math.abs(body.getAngularVelocity()) < 0.1f;
    }

    public void draw(Graphics2D g2) {
        if (body == null) return;
        Vec2 pos = body.getPosition();
        float angle = body.getAngle();

        AffineTransform old = g2.getTransform();
        g2.translate(pos.x * PlayManager.SCALE, pos.y * PlayManager.SCALE);
        g2.rotate(angle);
        g2.setColor(c);

        for (Vec2 offset : blockOffsets) {
            int size = Block.SIZE;
            int x = (int)(offset.x * PlayManager.SCALE) - size/2;
            int y = (int)(offset.y * PlayManager.SCALE) - size/2;
            g2.fillRect(x, y, size - 2, size - 2);
        }
        g2.setTransform(old);
    }

    public void drawStatic(Graphics2D g2, int x, int y) {
        // 1. On s'assure que la forme est bien définie (car createBody n'a pas été appelé)
        setShape();

        g2.setColor(c);

        // 2. On parcourt les offsets (positions relatives des blocs)
        for (Vec2 offset : blockOffsets) {
            if (offset != null) {
                // Conversion Mètres JBox2D -> Pixels
                // Note : offset.x est en mètres (ex: 1.0), on multiplie par SCALE (30.0)
                int pixelOffsetX = (int) (offset.x * PlayManager.SCALE);
                int pixelOffsetY = (int) (offset.y * PlayManager.SCALE);

                // Dessin centré sur x, y
                // On retire Block.SIZE/2 pour que x,y soit le centre du bloc et non le coin
                g2.fillRect(
                        x + pixelOffsetX - Block.SIZE / 2,
                        y + pixelOffsetY - Block.SIZE / 2,
                        Block.SIZE - 2,
                        Block.SIZE - 2
                );
            }
        }
    }
}