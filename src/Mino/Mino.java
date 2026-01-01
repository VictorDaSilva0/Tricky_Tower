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

    // NOUVEAU : Gestion du mouvement case par case
    long lastMoveTime = 0;
    int moveDelay = 100; // 100ms de délai entre deux mouvements (réglable)

    // Limites du terrain (en mètres JBox2D)
    public float minX, maxX;

    public void create(Color c) {
        this.c = c;
    }

    public abstract void setShape();

    public void setLimits(int leftPixelX, int rightPixelX) {
        float halfSize = (Block.SIZE / 2.0f) / PlayManager.SCALE;

        // 1. Chercher les points extrêmes de la forme actuelle
        float minOffset = 0; // Le décalage le plus à gauche (ex: -1.0)
        float maxOffset = 0; // Le décalage le plus à droite (ex: +2.0)

        for (Vec2 offset : blockOffsets) {
            if (offset.x < minOffset) minOffset = offset.x;
            if (offset.x > maxOffset) maxOffset = offset.x;
        }

        // 2. Calculer les limites en compensant ces décalages
        // minX : On part du mur gauche, on ajoute la demi-taille du bloc,
        // et on SOUSTRAIT le décalage négatif (ce qui revient à ajouter une marge vers la droite)
        this.minX = (leftPixelX / PlayManager.SCALE) + halfSize - minOffset;

        // maxX : On part du mur droit, on retire la demi-taille,
        // et on retire le décalage positif (on recule vers la gauche)
        this.maxX = (rightPixelX / PlayManager.SCALE) - halfSize - maxOffset;
    }

    public void createBody(World world, float xPixels, float yPixels, boolean isStatic) {
        setShape();

        BodyDef bd = new BodyDef();
        bd.type = isStatic ? BodyType.STATIC : BodyType.DYNAMIC;
        bd.position.set(xPixels / PlayManager.SCALE, yPixels / PlayManager.SCALE);

        bd.fixedRotation = true;
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

    public void update(boolean up, boolean left, boolean right, boolean down) {
        if (body == null || !active) return;

        // 1. DÉTECTION DE COLLISION
        if (!collided) {
            for (org.jbox2d.dynamics.contacts.ContactEdge edge = body.getContactList(); edge != null; edge = edge.next) {
                if (edge.contact.isTouching()) {
                    collided = true;
                    // Arrêt complet
                    body.setLinearVelocity(new Vec2(0, 0));
                    body.setAngularVelocity(0);
                    // Physique activée
                    body.setGravityScale(1.0f);
                    body.setFixedRotation(false);
                    body.setAwake(true);
                    return;
                }
            }
        }

        if (collided) {
            return;
        } else {
            // 2. MOUVEMENT EN L'AIR

            long currentTime = System.currentTimeMillis();
            Vec2 currentPos = body.getPosition();
            float newX = currentPos.x;
            boolean movedHorizontal = false;

            // --- A. MOUVEMENT LATÉRAL (Case par case) ---
            if (currentTime - lastMoveTime > moveDelay) {

                // Calcul de la taille d'un pas (1 bloc)
                float step = (float)Block.SIZE / PlayManager.SCALE;

                if (left) {
                    // Vérifie si on ne dépasse pas le mur de gauche
                    if (currentPos.x - step >= minX - 0.1f) { // 0.1f marge erreur flottante
                        newX -= step;
                        movedHorizontal = true;
                    }
                }
                if (right) {
                    // Vérifie si on ne dépasse pas le mur de droite
                    if (currentPos.x + step <= maxX + 0.1f) {
                        newX += step;
                        movedHorizontal = true;
                    }
                }

                // --- B. ROTATION (Avec délai) ---
                if (up) {
                    float currentAngle = body.getAngle();
                    body.setTransform(new Vec2(newX, currentPos.y), currentAngle + (float)(Math.PI / 2));
                    body.setAngularVelocity(0);
                    lastMoveTime = currentTime;
                    // Si on tourne, on a déjà appliqué le setTransform, donc on skip la suite pour cette frame
                    return;
                }

                if (movedHorizontal) {
                    // Applique le déplacement X instantané
                    body.setTransform(new Vec2(newX, currentPos.y), body.getAngle());
                    lastMoveTime = currentTime;
                }
            }

            // --- C. CHUTE (Fluide) ---
            float fallSpeed = 2.0f;  // Vitesse lente par défaut
            float dropSpeed = 25.0f; // Vitesse rapide (bas)

            float yVel = fallSpeed;
            if (down) yVel = dropSpeed;

            // On force X à 0 pour éviter le glissement résiduel
            body.setLinearVelocity(new Vec2(0, yVel));
        }
    }

    public boolean isStopped() {
        if (body == null) return false;
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

    // Méthode pour l'affichage UI (Next Mino)
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