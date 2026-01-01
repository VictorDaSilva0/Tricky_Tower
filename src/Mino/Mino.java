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
    public boolean collided = false;

    protected Vec2[] blockOffsets = new Vec2[4];

    long lastMoveTime = 0;
    int moveDelay = 100; // Vitesse de déplacement latéral

    //On stocke la position des MURS, pas de la pièce
    public float leftWall, rightWall;

    public void create(Color c) {
        this.c = c;
    }

    public abstract void setShape();

    // 1. On enregistre simplement où sont les murs en mètres
    public void setLimits(int leftPixelX, int rightPixelX) {
        this.leftWall = (leftPixelX / PlayManager.SCALE);
        this.rightWall = (rightPixelX / PlayManager.SCALE);
    }

    public void createBody(World world, float xPixels, float yPixels, boolean isStatic) {
        setShape();
        BodyDef bd = new BodyDef();
        bd.type = isStatic ? BodyType.STATIC : BodyType.DYNAMIC;
        bd.position.set(xPixels / PlayManager.SCALE, yPixels / PlayManager.SCALE);
        bd.fixedRotation = false; // La rotation est gérée par la physique, mais on la contrôle
        bd.gravityScale = 0.0f;
        body = world.createBody(bd);
        body.setUserData(this);

        for (Vec2 offset : blockOffsets) {
            PolygonShape shape = new PolygonShape();
            float halfSize = (Block.SIZE / 2.0f) / PlayManager.SCALE;
            shape.setAsBox(halfSize, halfSize, offset, 0);
            FixtureDef fd = new FixtureDef();
            fd.shape = shape;
            fd.density = 1.0f;
            fd.friction = 0.3f;
            fd.restitution = 0.0f;
            body.createFixture(fd);
        }
    }

    public void update(boolean up, boolean left, boolean right, boolean down, boolean dash) {
        if (body == null || !active) return;

        // --- GESTION DES COLLISIONS (Identique) ---
        if (!collided) {
            for (org.jbox2d.dynamics.contacts.ContactEdge edge = body.getContactList(); edge != null; edge = edge.next) {
                if (edge.contact.isTouching()) {
                    collided = true;
                    body.setLinearVelocity(new Vec2(0, 0));
                    body.setAngularVelocity(0);
                    body.setGravityScale(1.0f);
                    body.setAwake(true);
                    return;
                }
            }
        }

        if (collided) {
            return;
        } else {
            long currentTime = System.currentTimeMillis();
            Vec2 currentPos = body.getPosition();
            float currentAngle = body.getAngle();
            float newX = currentPos.x;
            boolean movedHorizontal = false;

            // --- LIMITES DYNAMIQUES (Identique à la version précédente) ---
            float minRelativeX = Float.MAX_VALUE;
            float maxRelativeX = -Float.MAX_VALUE;
            float halfSize = (Block.SIZE / 2.0f) / PlayManager.SCALE;

            for (Vec2 offset : blockOffsets) {
                float rotX = (float)(offset.x * Math.cos(currentAngle) - offset.y * Math.sin(currentAngle));
                if (rotX - halfSize < minRelativeX) minRelativeX = rotX - halfSize;
                if (rotX + halfSize > maxRelativeX) maxRelativeX = rotX + halfSize;
            }

            float margin = 4.0f;
            float minBodyX = (leftWall - margin) - minRelativeX;
            float maxBodyX = (rightWall + margin) - maxRelativeX;

            // --- MOUVEMENT LATÉRAL (MODIFIÉ POUR LE DASH) ---

            // Si on dash, le délai passe à 20ms (très rapide) sinon 100ms (normal)
            int currentDelay = dash ? 20 : moveDelay;

            if (currentTime - lastMoveTime > currentDelay) {
                float step = (float)Block.SIZE / PlayManager.SCALE;

                if (left) {
                    if (currentPos.x - step >= minBodyX - 0.05f) {
                        newX -= step;
                        movedHorizontal = true;
                    } else {
                        newX = minBodyX;
                        movedHorizontal = true;
                    }
                }
                if (right) {
                    if (currentPos.x + step <= maxBodyX + 0.05f) {
                        newX += step;
                        movedHorizontal = true;
                    } else {
                        newX = maxBodyX;
                        movedHorizontal = true;
                    }
                }

                // ROTATION
                if (up) {
                    body.setTransform(new Vec2(newX, currentPos.y), currentAngle + (float)(Math.PI / 2));
                    body.setAngularVelocity(0);
                    lastMoveTime = currentTime;
                    return;
                }

                if (movedHorizontal) {
                    if (newX < minBodyX) newX = minBodyX;
                    if (newX > maxBodyX) newX = maxBodyX;

                    body.setTransform(new Vec2(newX, currentPos.y), currentAngle);
                    lastMoveTime = currentTime;
                }
            }

            // --- CHUTE ---
            float fallSpeed = 2.0f;
            float dropSpeed = 25.0f;
            float yVel = (down) ? dropSpeed : fallSpeed;
            body.setLinearVelocity(new Vec2(0, yVel));
        }
    }

    public boolean isStopped() {
        if (body == null) return false;
        return body.getLinearVelocity().length() < 0.1f && Math.abs(body.getAngularVelocity()) < 0.1f;
    }

    // ... (Gardez les méthodes draw et drawStatic telles quelles) ...
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
        setShape();
        g2.setColor(c);
        for (Vec2 offset : blockOffsets) {
            if (offset != null) {
                int pixelOffsetX = (int) (offset.x * PlayManager.SCALE);
                int pixelOffsetY = (int) (offset.y * PlayManager.SCALE);
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