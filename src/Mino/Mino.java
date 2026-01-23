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
    int moveDelay = 150; // Vitesse de déplacement

    // Limites de jeu
    public float minXLimit = -10000;
    public float maxXLimit = 10000;
    // Compatibilité
    public float leftWall, rightWall;

    // Lock Delay
    int lockCounter = 0;
    final int LOCK_DELAY_MAX = 60;
    boolean hasTouched = false;

    public void create(Color c) {
        this.c = c;
    }

    public abstract void setShape();

    public void setLimits(int leftPixelX, int rightPixelX) {
        this.minXLimit = (float)leftPixelX / PlayManager.SCALE;
        this.maxXLimit = (float)rightPixelX / PlayManager.SCALE;
        this.leftWall = this.minXLimit;
        this.rightWall = this.maxXLimit;
    }

    public void createBody(World world, float xPixels, float yPixels, boolean isStatic) {
        setShape();
        BodyDef bd = new BodyDef();
        bd.type = isStatic ? BodyType.STATIC : BodyType.DYNAMIC;
        bd.position.set(xPixels / PlayManager.SCALE, yPixels / PlayManager.SCALE);
        bd.fixedRotation = false;
        bd.gravityScale = 1.0f;
        bd.linearDamping = 3.5f;
        bd.angularDamping = 2.0f;
        body = world.createBody(bd);
        body.setUserData(this);

        for (Vec2 offset : blockOffsets) {
            PolygonShape shape = new PolygonShape();
            float halfSize = ((Block.SIZE / 2.0f) - 1.0f) / PlayManager.SCALE;
            shape.setAsBox(halfSize, halfSize, offset, 0);
            FixtureDef fd = new FixtureDef();
            fd.shape = shape;
            fd.density = 2.0f;
            fd.friction = 0.6f;
            fd.restitution = 0.1f;
            body.createFixture(fd);
        }
    }

    // Collision avec marge configurable (pour le "wedging")
    private boolean checkCollision(float targetPixelX, float targetPixelY, float targetAngle, World world, float margin) {
        float halfSize = (Block.SIZE / 2.0f) / PlayManager.SCALE;
        for (Vec2 offset : blockOffsets) {
            float rotX = (float) (offset.x * Math.cos(targetAngle) - offset.y * Math.sin(targetAngle));
            float rotY = (float) (offset.x * Math.sin(targetAngle) + offset.y * Math.cos(targetAngle));

            float centerX = (targetPixelX / PlayManager.SCALE) + rotX;
            float centerY = (targetPixelY / PlayManager.SCALE) + rotY;

            org.jbox2d.collision.AABB aabb = new org.jbox2d.collision.AABB();
            aabb.lowerBound.set(centerX - halfSize + margin, centerY - halfSize + margin);
            aabb.upperBound.set(centerX + halfSize - margin, centerY + halfSize - margin);

            final boolean[] collisionFound = { false };
            world.queryAABB(new org.jbox2d.callbacks.QueryCallback() {
                @Override
                public boolean reportFixture(Fixture fixture) {
                    if (fixture.getBody() != body && fixture.getBody().getType() != BodyType.DYNAMIC) {
                        collisionFound[0] = true;
                        return false;
                    }
                    if (fixture.getBody() != body && fixture.getBody().getType() == BodyType.DYNAMIC
                            && !((Mino) fixture.getBody().getUserData()).active) {
                        collisionFound[0] = true;
                        return false;
                    }
                    return true;
                }
            }, aabb);

            if (collisionFound[0]) return true;
        }
        return false;
    }

    // Surcharge par défaut
    private boolean checkCollision(float targetPixelX, float targetPixelY, float targetAngle, World world) {
        return checkCollision(targetPixelX, targetPixelY, targetAngle, world, 0.1f);
    }

    public void update(boolean up, boolean left, boolean right, boolean down, boolean dash, PlayManager pm) {
        if (body == null || !active) return;
        World world = pm.world;

        // Trail Effect
        if (down || dash) {
            if (pm.effectManager != null) {
                pm.effectManager.addTrail((int) (body.getPosition().x * PlayManager.SCALE),
                        (int) (body.getPosition().y * PlayManager.SCALE), this.c);
            }
        }

        // Lock Delay Check
        if (!collided) {
            boolean isTouching = false;
            for (org.jbox2d.dynamics.contacts.ContactEdge edge = body.getContactList(); edge != null; edge = edge.next) {
                if (edge.contact.isTouching()) {
                    isTouching = true;
                    break;
                }
            }
            if (isTouching) hasTouched = true;

            if (hasTouched) {
                lockCounter++;
                if (lockCounter > 45 && lockCounter % 5 == 0 && pm.effectManager != null) {
                    pm.effectManager.addLockFlash((int) (body.getPosition().x * PlayManager.SCALE),
                            (int) (body.getPosition().y * PlayManager.SCALE));
                }
                if (lockCounter >= LOCK_DELAY_MAX) {
                    collided = true;
                    if (pm.effectManager != null) {
                        pm.effectManager.addLockFlash((int) (body.getPosition().x * PlayManager.SCALE),
                                (int) (body.getPosition().y * PlayManager.SCALE));
                    }
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

            // Taille d'une case en mètres (ex: 1.0)
            float step = (float)Block.SIZE / PlayManager.SCALE;

            // --- 1. ROTATION (AVEC SNAP) ---
            if (up && currentTime - lastMoveTime > 200) {
                float targetRot = currentAngle + (float) (Math.PI / 2);

                // On aligne le pivot sur la grille
                float roundedX = Math.round(currentPos.x / step) * step;

                // Wall Kicks (Normal, Droite, Gauche, Haut)
                Vec2[] kicks = {
                        new Vec2(roundedX, currentPos.y),
                        new Vec2(roundedX + step, currentPos.y),
                        new Vec2(roundedX - step, currentPos.y),
                        new Vec2(roundedX, currentPos.y - step)
                };

                for (Vec2 kickPos : kicks) {
                    if (!checkCollision(kickPos.x * PlayManager.SCALE, kickPos.y * PlayManager.SCALE, targetRot, world, 0.1f)) {
                        body.setTransform(kickPos, targetRot);
                        body.setAngularVelocity(0);
                        lastMoveTime = currentTime;
                        break;
                    }
                }
            }

            // --- 2. MOUVEMENT LATÉRAL (AVEC SNAP FORCE) ---
            if (currentTime - lastMoveTime > moveDelay) {

                // ICI LA CORRECTION PRINCIPALE :
                // On ne fait pas "pos + step", on fait "colonne + 1".
                // Cela force la pièce à s'aligner parfaitement à chaque déplacement.

                int currentColumn = Math.round(currentPos.x / step);
                float targetX = currentPos.x;
                boolean moved = false;

                // Marge permissive pour le mouvement (0.25f permet de glisser si on dépasse un peu)
                float slideMargin = 0.25f;

                if (left) {
                    // On vise la colonne précédente exacte
                    float potentialX = (currentColumn - 1) * step;

                    if (potentialX > minXLimit) {
                        if (!checkCollision(potentialX * PlayManager.SCALE, currentPos.y * PlayManager.SCALE, currentAngle, world, slideMargin)) {
                            targetX = potentialX;
                            moved = true;
                        }
                    }
                }
                else if (right) {
                    // On vise la colonne suivante exacte
                    float potentialX = (currentColumn + 1) * step;

                    if (potentialX < maxXLimit) {
                        if (!checkCollision(potentialX * PlayManager.SCALE, currentPos.y * PlayManager.SCALE, currentAngle, world, slideMargin)) {
                            targetX = potentialX;
                            moved = true;
                        }
                    }
                }

                if (moved) {
                    // On téléporte la pièce sur la coordonnée grille exacte
                    body.setTransform(new Vec2(targetX, currentPos.y), currentAngle);
                    lastMoveTime = currentTime;
                }
            }

            // Stabilisation X (Annule la dérive latérale physique)
            Vec2 vel = body.getLinearVelocity();
            body.setLinearVelocity(new Vec2(0, vel.y));

            // Gravité / Dash
            if (down) {
                float dropSpeed = 20.0f;
                if (vel.y < dropSpeed) {
                    body.applyForce(new Vec2(0, 200.0f * body.getMass()), body.getWorldCenter());
                }
            } else if (dash) {
                body.applyLinearImpulse(new Vec2(0, 50 * body.getMass()), body.getWorldCenter());
            }
        }
    }

    public boolean isStopped() {
        if (body == null) return false;
        return body.getLinearVelocity().length() < 0.1f && Math.abs(body.getAngularVelocity()) < 0.1f;
    }

    private void drawStyledBlock(Graphics2D g2, int x, int y, int size, Color baseColor) {
        g2.setColor(baseColor.darker().darker());
        g2.fillRect(x, y, size, size);
        g2.setColor(baseColor);
        g2.fillRect(x + 2, y + 2, size - 4, size - 4);
        g2.setColor(new Color(255, 255, 255, 100));
        g2.fillRect(x + 2, y + 2, size - 4, 4);
        g2.fillRect(x + 2, y + 2, 4, size - 4);
        g2.setColor(new Color(0, 0, 0, 50));
        g2.fillRect(x + 2, y + size - 6, size - 4, 4);
        g2.fillRect(x + size - 6, y + 2, 4, size - 4);
        g2.setColor(baseColor.brighter());
        g2.fillRect(x + size / 2 - 4, y + size / 2 - 4, 8, 8);
        g2.setColor(new Color(255, 255, 255, 150));
        g2.drawRect(x + size / 2 - 4, y + size / 2 - 4, 8, 8);
    }

    public void draw(Graphics2D g2) {
        if (body == null) return;
        Vec2 pos = body.getPosition();
        float angle = body.getAngle();
        AffineTransform old = g2.getTransform();
        g2.translate(pos.x * PlayManager.SCALE, pos.y * PlayManager.SCALE);
        g2.rotate(angle);

        for (Vec2 offset : blockOffsets) {
            int size = Block.SIZE;
            int x = (int) (offset.x * PlayManager.SCALE) - size / 2;
            int y = (int) (offset.y * PlayManager.SCALE) - size / 2;
            drawStyledBlock(g2, x, y, size - 1, c);
        }
        g2.setTransform(old);
    }

    public void drawStatic(Graphics2D g2, int x, int y) {
        setShape();
        for (Vec2 offset : blockOffsets) {
            if (offset != null) {
                int pixelOffsetX = (int) (offset.x * PlayManager.SCALE);
                int pixelOffsetY = (int) (offset.y * PlayManager.SCALE);
                drawStyledBlock(g2,
                        x + pixelOffsetX - Block.SIZE / 2,
                        y + pixelOffsetY - Block.SIZE / 2,
                        Block.SIZE - 1, c);
            }
        }
    }
}