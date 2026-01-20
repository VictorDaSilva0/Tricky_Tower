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

    // On stocke la position des MURS, pas de la pièce
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

    // Lock Delay Variables
    int lockCounter = 0;
    final int LOCK_DELAY_MAX = 30; // Approx 0.5s at 60fps

    public void createBody(World world, float xPixels, float yPixels, boolean isStatic) {
        setShape();
        BodyDef bd = new BodyDef();
        bd.type = isStatic ? BodyType.STATIC : BodyType.DYNAMIC;
        bd.position.set(xPixels / PlayManager.SCALE, yPixels / PlayManager.SCALE);
        bd.fixedRotation = false; // Rotation controlled by physics/snapping
        bd.gravityScale = 1.0f; // Enable natural gravity
        bd.linearDamping = 3.5f; // Stronger air resistance for slower fall
        bd.angularDamping = 2.0f; // Reduce spinning wildness
        body = world.createBody(bd);
        body.setUserData(this);

        for (Vec2 offset : blockOffsets) {
            PolygonShape shape = new PolygonShape();
            float halfSize = ((Block.SIZE / 2.0f) - 1.0f) / PlayManager.SCALE;
            shape.setAsBox(halfSize, halfSize, offset, 0);
            FixtureDef fd = new FixtureDef();
            fd.shape = shape;
            fd.density = 2.0f; // Heavier blocks feel better
            fd.friction = 0.6f; // Higher friction for stability when stacked
            fd.restitution = 0.1f; // Slight bounce
            body.createFixture(fd);
        }
    }

    // Helper collision check
    private boolean checkCollision(float targetPixelX, float targetPixelY, float targetAngle, World world) {
        float halfSize = (Block.SIZE / 2.0f) / PlayManager.SCALE;
        // Check strict collision for each block offset at the new position
        for (Vec2 offset : blockOffsets) {
            // Rotate offset
            float rotX = (float) (offset.x * Math.cos(targetAngle) - offset.y * Math.sin(targetAngle));
            float rotY = (float) (offset.x * Math.sin(targetAngle) + offset.y * Math.cos(targetAngle));

            // Calculate world center of this block
            float centerX = (targetPixelX / PlayManager.SCALE) + rotX;
            float centerY = (targetPixelY / PlayManager.SCALE) + rotY;

            // Create a small AABB (slightly smaller than block to avoid false positives
            // with neighbors)
            float margin = 0.1f; // 0.1 meter margin
            org.jbox2d.collision.AABB aabb = new org.jbox2d.collision.AABB();
            aabb.lowerBound.set(centerX - halfSize + margin, centerY - halfSize + margin);
            aabb.upperBound.set(centerX + halfSize - margin, centerY + halfSize - margin);

            final boolean[] collisionFound = { false };

            world.queryAABB(new org.jbox2d.callbacks.QueryCallback() {
                @Override
                public boolean reportFixture(Fixture fixture) {
                    if (fixture.getBody() != body && fixture.getBody().getType() != BodyType.DYNAMIC) {
                        // Collision with static body (ground or other locked blocks)
                        collisionFound[0] = true;
                        return false; // Terminate query
                    }
                    if (fixture.getBody() != body && fixture.getBody().getType() == BodyType.DYNAMIC
                            && !((Mino) fixture.getBody().getUserData()).active) {
                        // Collision with a locked dynamic body (shouldn't happen often if they are
                        // static)
                        // But in this game locked blocks might become static.
                        // Let's assume if it's not THIS body, it's an obstacle.
                        collisionFound[0] = true;
                        return false;
                    }
                    return true;
                }
            }, aabb);

            if (collisionFound[0])
                return true;
        }
        return false;
    }

    public void update(boolean up, boolean left, boolean right, boolean down, boolean dash, PlayManager pm) {
        if (body == null || !active)
            return;
        World world = pm.world;

        // TRAIL EFFECT
        if (down || dash) {
            if (pm.effectManager != null) {
                pm.effectManager.addTrail((int) (body.getPosition().x * PlayManager.SCALE),
                        (int) (body.getPosition().y * PlayManager.SCALE), this.c);
            }
        }

        // --- GESTION DES COLLISIONS (AVEC LOCK DELAY) ---
        if (!collided) {
            boolean isTouching = false;
            for (org.jbox2d.dynamics.contacts.ContactEdge edge = body
                    .getContactList(); edge != null; edge = edge.next) {
                if (edge.contact.isTouching()) {
                    isTouching = true;
                    // Check if hitting another Mino (not walls)
                    break;
                }
            }

            if (isTouching) {
                lockCounter++;
                if (lockCounter >= LOCK_DELAY_MAX) {
                    collided = true;
                    // Don't freeze physics! Just mark as collided so PlayManager spawns next.
                    // We might want to dampen velocity to help it settle?
                    // body.setLinearDamping(2.0f);

                    if (pm.effectManager != null) {
                        pm.effectManager.addLockFlash((int) (body.getPosition().x * PlayManager.SCALE),
                                (int) (body.getPosition().y * PlayManager.SCALE));
                    }
                    return;
                }
            } else {
                lockCounter = 0;
            }
        }

        if (collided) {
            return;
        } else {
            long currentTime = System.currentTimeMillis();
            Vec2 currentPos = body.getPosition();
            float currentAngle = body.getAngle();

            // --- ROTATION ---
            if (up && currentTime - lastMoveTime > 200) {
                float targetRot = currentAngle + (float) (Math.PI / 2);
                if (!checkCollision(currentPos.x * PlayManager.SCALE, currentPos.y * PlayManager.SCALE, targetRot,
                        world)) {
                    body.setTransform(currentPos, targetRot);
                    body.setAngularVelocity(0);
                    lastMoveTime = currentTime;
                }
            }

            // --- MOUVEMENT PAR FORCES ---
            Vec2 vel = body.getLinearVelocity();
            float desiredVelX = 0;
            float moveSpeed = 10.0f; // Max horizontal speed

            if (left)
                desiredVelX -= moveSpeed;
            if (right)
                desiredVelX += moveSpeed;

            // Impulse to change velocity instantly but respecting mass
            float velChangeX = desiredVelX - vel.x;
            float impulseX = body.getMass() * velChangeX;

            // Limit impulse to avoid tunneling/violent shakes?
            // Actually, for crisp controls, full impulse is good, but we rely on collisions
            // to stop us.
            // But if we push into a wall, impulse might be huge.
            // Let's use Force for smoother "push" but Impulse for "start/stop".

            // Hybrid: Apply impulse but clamping the max force?
            // Simple approach: pure impulse for X.
            body.applyLinearImpulse(new Vec2(impulseX, 0), body.getWorldCenter());

            // --- Vertical ---
            // Gravity is handling most of it.
            // Down = Add extra force/impulse downwards.
            if (down) {
                float dropSpeed = 20.0f;
                if (vel.y < dropSpeed) {
                    body.applyForce(new Vec2(0, 200.0f * body.getMass()), body.getWorldCenter());
                }
            }

            // Dash logic?
            if (dash) {
                body.applyLinearImpulse(new Vec2(0, 50 * body.getMass()), body.getWorldCenter()); // Big drop
            }

            // Remove manual clamping. Walls handle it now.
        }
    }

    public boolean isStopped() {
        if (body == null)
            return false;
        return body.getLinearVelocity().length() < 0.1f && Math.abs(body.getAngularVelocity()) < 0.1f;
    }

    // ... (Gardez les méthodes draw et drawStatic telles quelles) ...
    public void draw(Graphics2D g2) {
        if (body == null)
            return;
        Vec2 pos = body.getPosition();
        float angle = body.getAngle();
        AffineTransform old = g2.getTransform();
        g2.translate(pos.x * PlayManager.SCALE, pos.y * PlayManager.SCALE);
        g2.rotate(angle);
        g2.setColor(c);
        for (Vec2 offset : blockOffsets) {
            int size = Block.SIZE;
            int x = (int) (offset.x * PlayManager.SCALE) - size / 2;
            int y = (int) (offset.y * PlayManager.SCALE) - size / 2;
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
                        Block.SIZE - 2);
            }
        }
    }
}