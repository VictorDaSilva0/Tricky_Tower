package Main;

import Mino.*;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.BodyType;
import org.jbox2d.dynamics.World;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.Random;

public class PlayManager {

    // --- SETTINGS ---
    final int WIDTH = 360;
    final int HEIGHT = 600;
    public int left_x, right_x, top_y, bottom_y;

    // Caméra
    double cameraY = 0; // Décalage vertical
    double targetCameraY = 0;

    // Modes de jeu
    int mode; // 0 = SOLO, 1 = MULTI
    public static final int MODE_SOLO = 0;
    public static final int MODE_MULTI = 1;

    // Conditions de victoire / défaite
    int lives = 3; // Pour le Solo
    final int FINISH_LINE_HEIGHT = 2000;
    boolean gameFinished = false;
    String endMessage = "";

    // JBox2D
    public World world;
    public static final float SCALE = 30.0f;
    float timeStep = 1.0f / 60.0f;
    int velocityIterations = 8;
    int positionIterations = 3;

    // Minos
    Mino currentMino;
    final int MINO_START_X;
    final int MINO_START_Y;

    Mino nextMino;
    final int NEXTMINO_X; // Déplacé ici pour être accessible dans draw()
    final int NEXTMINO_Y; // Déplacé ici pour être accessible dans draw()

    KeyHandler keyH;
    int playerID;

    // Liste des corps à supprimer (pour les vies perdues)
    ArrayList<Body> bodiesonDestroy = new ArrayList<>();

    // Variable pour savoir si on doit respawn
    boolean forceSpawnNext = false;

    public PlayManager(int startX, KeyHandler keyH, int playerID, int mode) {
        this.left_x = startX;
        this.right_x = left_x + WIDTH;
        this.top_y = 50;
        this.bottom_y = top_y + HEIGHT;
        this.keyH = keyH;
        this.playerID = playerID;
        this.mode = mode;

        MINO_START_X = left_x + (WIDTH / 2);
        MINO_START_Y = top_y + Block.SIZE;

        // Initialisation des positions de la Next Mino
        NEXTMINO_X = right_x + 85;
        NEXTMINO_Y = top_y + 100;

        world = new World(new Vec2(0, 20.0f));
        createGround();

        currentMino = pickMino();
        currentMino.createBody(world, MINO_START_X, MINO_START_Y, false);
        nextMino = pickMino();
    }

    private void createGround() {
        BodyDef groundDef = new BodyDef();
        groundDef.position.set((left_x + WIDTH / 2) / SCALE, (bottom_y) / SCALE);
        groundDef.type = BodyType.STATIC;
        Body groundBody = world.createBody(groundDef);

        org.jbox2d.collision.shapes.PolygonShape groundShape = new org.jbox2d.collision.shapes.PolygonShape();
        groundShape.setAsBox((WIDTH / 2) / SCALE, 10 / SCALE);
        groundBody.createFixture(groundShape, 0.0f);
    }

    private Mino pickMino() {
        int i = new Random().nextInt(7);
        Mino mino = null;
        switch (i) {
            case 0: mino = new Mino_L1(); break;
            case 1: mino = new Mino_L2(); break;
            case 2: mino = new Mino_Square(); break;
            case 3: mino = new Mino_Bar(); break;
            case 4: mino = new Mino_T(); break;
            case 5: mino = new Mino_Z1(); break;
            case 6: mino = new Mino_Z2(); break;
            default: mino = new Mino_L1(); break;
        }
        return mino;
    }

    public void update() {
        if (gameFinished) return;

        // 1. Physique
        world.step(timeStep, velocityIterations, positionIterations);

        // Supprimer les corps morts
        for (Body b : bodiesonDestroy) {
            world.destroyBody(b);
        }
        bodiesonDestroy.clear();

        // 2. Caméra
        updateCamera();

        // 3. Règles du jeu
        checkGameStatus();

        // 4. Inputs
        boolean up = (playerID == 1) ? keyH.upPressed1 : keyH.upPressed2;
        boolean down = (playerID == 1) ? keyH.downPressed1 : keyH.downPressed2;
        boolean left = (playerID == 1) ? keyH.leftPressed1 : keyH.leftPressed2;
        boolean right = (playerID == 1) ? keyH.rightPressed1 : keyH.rightPressed2;

        if (up) {
            if (playerID == 1) keyH.upPressed1 = false;
            else keyH.upPressed2 = false;
        }

        // 5. Logique des Minos
        if (forceSpawnNext) {
            spawnNextMino();
            forceSpawnNext = false;
        }
        else if (currentMino != null && currentMino.active) {
            currentMino.update(up, left, right, down);

            if (currentMino.isStopped()) {
                currentMino.active = false;
                spawnNextMino();
            }
        }
    }

    private void updateCamera() {
        float highestY = bottom_y;

        Body b = world.getBodyList();
        while (b != null) {
            if (b.getType() == BodyType.DYNAMIC) {
                if (currentMino != null && b == currentMino.body) {
                    b = b.getNext();
                    continue;
                }
                float pixelY = b.getPosition().y * SCALE;
                if (pixelY < highestY) {
                    highestY = pixelY;
                }
            }
            b = b.getNext();
        }

        float screenMiddle = top_y + HEIGHT / 2.0f;
        targetCameraY = screenMiddle - highestY;
        cameraY += (targetCameraY - cameraY) * 0.05f;

        if (cameraY < 0) {
            cameraY = 0;
        }
    }

    private void spawnNextMino() {
        currentMino = nextMino;
        // On fait apparaître la pièce un peu au-dessus de la caméra visible
        float spawnY = (float) (MINO_START_Y - cameraY);
        currentMino.createBody(world, MINO_START_X, spawnY, false);
        nextMino = pickMino();
    }

    private void checkGameStatus() {
        if (mode == MODE_MULTI) {
            Body b = world.getBodyList();
            while (b != null) {
                if (b.getType() == BodyType.DYNAMIC && b.getLinearVelocity().length() < 0.1f) {
                    float pixelY = b.getPosition().y * SCALE;
                    if (pixelY < bottom_y - FINISH_LINE_HEIGHT) {
                        gameFinished = true;
                        endMessage = "VICTOIRE !";
                    }
                }
                b = b.getNext();
            }
        }
        else if (mode == MODE_SOLO) {
            Body b = world.getBodyList();
            while (b != null) {
                if (b.getType() == BodyType.DYNAMIC) {
                    float pixelY = b.getPosition().y * SCALE;
                    if (pixelY > bottom_y + HEIGHT + 100 || pixelY > bottom_y + 100) {
                        Mino m = (Mino) b.getUserData();
                        if (m != null) {
                            if (!bodiesonDestroy.contains(b)) {
                                lives--;
                                bodiesonDestroy.add(b);
                                if (m == currentMino) {
                                    currentMino.active = false;
                                    forceSpawnNext = true;
                                }
                                if (lives <= 0) {
                                    gameFinished = true;
                                    endMessage = "GAME OVER";
                                }
                            }
                        }
                    }
                }
                b = b.getNext();
            }
        }
    }

    public void draw(Graphics2D g2) {
        // --- DESSIN DU MONDE (AFFECTÉ PAR LA CAMÉRA) ---
        AffineTransform original = g2.getTransform();
        g2.translate(0, cameraY);

        // 1. Cadre
        g2.setColor(Color.white);
        g2.setStroke(new BasicStroke(4f));
        g2.drawRect(left_x, top_y - (int)cameraY, WIDTH, HEIGHT + (int)cameraY);

        // 2. Ligne d'arrivée
        if (mode == MODE_MULTI) {
            int finishY = bottom_y - FINISH_LINE_HEIGHT;
            g2.setColor(Color.GREEN);
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{9}, 0));
            g2.drawLine(left_x - 20, finishY, right_x + 20, finishY);
            g2.drawString("FINISH", right_x + 10, finishY);
        }

        // 3. Dessin des corps physiques
        Body b = world.getBodyList();
        while (b != null) {
            Object userData = b.getUserData();
            if (userData instanceof Mino) {
                ((Mino) userData).draw(g2);
            } else if (b.getType() == BodyType.STATIC) {
                // Sol
                Vec2 pos = b.getPosition();
                g2.setColor(Color.gray);
                g2.fillRect(
                        (int)(pos.x * SCALE) - (WIDTH/2),
                        (int)(pos.y * SCALE) - 10,
                        WIDTH, 20
                );
            }
            b = b.getNext();
        }

        // --- DESSIN UI (FIXE) ---
        g2.setTransform(original);

        // UI Next Mino
        g2.setColor(Color.white);
        int uiX = right_x + 20;
        int uiY = top_y + 100;
        g2.drawString("Next:", uiX, uiY);

        // --- CORRECTION ICI : Dessiner le Next Mino ---
        if (nextMino != null) {
            nextMino.drawStatic(g2, NEXTMINO_X, NEXTMINO_Y);
        }
        // ----------------------------------------------

        // UI Infos
        if (mode == MODE_SOLO) {
            g2.setColor(Color.RED);
            g2.setFont(new Font("Arial", Font.BOLD, 20));
            g2.drawString("Vies: " + lives, left_x + 20, top_y + 30);
        } else {
            g2.setColor(Color.GREEN);
            g2.drawString("H: " + (int)cameraY, left_x + 20, top_y + 30);
        }

        // Message fin de partie
        if (gameFinished) {
            g2.setColor(Color.CYAN);
            g2.setFont(new Font("Arial", Font.BOLD, 40));
            int textW = g2.getFontMetrics().stringWidth(endMessage);
            g2.drawString(endMessage, left_x + WIDTH/2 - textW/2, top_y + HEIGHT/2);
        }
    }
}