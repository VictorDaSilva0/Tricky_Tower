package Main;

import Mino.*;
import Effects.*;
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
    double cameraY = 0;
    double targetCameraY = 0;

    // Modes
    int mode;
    public static final int MODE_SOLO = 0;
    public static final int MODE_MULTI = 1;

    // Victoire / Défaite
    int lives = 3;
    final float START_FINISH_LINE_HEIGHT = 2000f;
    float currentFinishLineHeight;
    final float LINE_DROP_SPEED = 0.15f;
    final float MIN_FINISH_HEIGHT = 300f;

    // Timer Victoire
    float winTimer = 0;
    final float TIME_TO_WIN = 3.0f;

    boolean gameFinished = false;
    String endMessage = "";

    int score = 0;

    // Physique
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
    final int NEXTMINO_X;
    final int NEXTMINO_Y;

    KeyHandler keyH;
    int playerID;

    ArrayList<Body> bodiesonDestroy = new ArrayList<>();

    boolean forceSpawnNext = false;

    // Effects
    public EffectManager effectManager = new EffectManager();
    public PowerUpManager powerUpManager;

    public PlayManager(int startX, int startY, KeyHandler keyH, int playerID, int mode) {
        this.left_x = startX;
        this.right_x = left_x + WIDTH;
        this.top_y = startY;
        this.bottom_y = top_y + HEIGHT;
        this.keyH = keyH;
        this.playerID = playerID;
        this.mode = mode;

        MINO_START_X = left_x + (WIDTH / 2);
        MINO_START_Y = top_y + Block.SIZE;

        NEXTMINO_X = right_x + 85;
        NEXTMINO_Y = top_y + 100;

        world = new World(new Vec2(0, 9.8f));
        createGround();
        createWalls();

        currentMino = pickMino();
        currentMino.createBody(world, MINO_START_X, MINO_START_Y, false);
        // NOUVEAU : On définit les limites pour la première pièce
        currentMino.setLimits(left_x, right_x);

        nextMino = pickMino();

        currentFinishLineHeight = START_FINISH_LINE_HEIGHT;

        powerUpManager = new PowerUpManager(this);
    }

    private void createGround() {
        BodyDef groundDef = new BodyDef();
        groundDef.position.set((left_x + WIDTH / 2) / SCALE, (bottom_y) / SCALE);
        groundDef.type = BodyType.STATIC;
        Body groundBody = world.createBody(groundDef);

        org.jbox2d.collision.shapes.PolygonShape groundShape = new org.jbox2d.collision.shapes.PolygonShape();
        // Logic adapted for Game Rules: Narrow ground for Survival
        float groundWidthHalf = (WIDTH / 2) / SCALE; // Default Mode
        if (mode == MODE_SOLO) {
            groundWidthHalf = (4 * Block.SIZE) / 2.0f / SCALE; // 4 blocks wide
        }

        groundShape.setAsBox(groundWidthHalf, 10 / SCALE);
        groundBody.createFixture(groundShape, 0.0f);
    }

    private void createWalls() {
        // No walls in Tricky Towers! You can fall off.
        // We leave this empty or remove it.
    }

    private Mino pickMino() {
        int i = new Random().nextInt(7);
        Mino mino = null;
        switch (i) {
            case 0:
                mino = new Mino_L1();
                break;
            case 1:
                mino = new Mino_L2();
                break;
            case 2:
                mino = new Mino_Square();
                break;
            case 3:
                mino = new Mino_Bar();
                break;
            case 4:
                mino = new Mino_T();
                break;
            case 5:
                mino = new Mino_Z1();
                break;
            case 6:
                mino = new Mino_Z2();
                break;
            default:
                mino = new Mino_L1();
                break;
        }
        return mino;
    }

    public void update() {
        effectManager.update();
        if (!gameFinished)
            powerUpManager.update();

        if (gameFinished) {
            if (endMessage.equals("VICTOIRE !")) {
                // FIREWORKS LOOP
                // FIREWORKS LOOP
                effectManager.addConfetti(left_x + WIDTH / 2, top_y + HEIGHT / 2);
                if (new Random().nextInt(10) < 3) {
                    int rx = left_x + new Random().nextInt(WIDTH);
                    int ry = top_y + new Random().nextInt(HEIGHT);
                    effectManager.addConfetti(rx, ry);

                    // Extra BIG explosion randomly
                    if (new Random().nextInt(20) < 1) {
                        effectManager.addExplosion(rx, ry);
                    }
                }
            }
            return;
        }

        world.step(timeStep, velocityIterations, positionIterations);

        for (Body b : bodiesonDestroy) {
            world.destroyBody(b);
        }
        bodiesonDestroy.clear();

        updateCamera();

        // Descente ligne d'arrivée
        if (mode == MODE_MULTI && winTimer == 0) {
            currentFinishLineHeight -= LINE_DROP_SPEED;
            if (currentFinishLineHeight < MIN_FINISH_HEIGHT) {
                currentFinishLineHeight = MIN_FINISH_HEIGHT;
            }
        }

        checkGameStatus();

        boolean up = (playerID == 1) ? keyH.upPressed1 : keyH.upPressed2;
        boolean down = (playerID == 1) ? keyH.downPressed1 : keyH.downPressed2;
        boolean left = (playerID == 1) ? keyH.leftPressed1 : keyH.leftPressed2;
        boolean right = (playerID == 1) ? keyH.rightPressed1 : keyH.rightPressed2;
        boolean dash = (playerID == 1) ? keyH.dashPressed1 : keyH.dashPressed2;

        // Reverse Controls Event
        if (powerUpManager.isReverseActive()) {
            boolean temp = left;
            left = right;
            right = temp;
        }

        if (up) {
            if (playerID == 1)
                keyH.upPressed1 = false;
            else
                keyH.upPressed2 = false;
        }

        if (forceSpawnNext) {
            spawnNextMino();
            forceSpawnNext = false;
        } else if (currentMino != null && currentMino.active) {
            currentMino.update(up, left, right, down, dash, this);

            if (currentMino.isStopped()) {
                currentMino.active = false;

                // --- NOUVEAU : CALCUL DU SCORE (Uniquement en Solo) ---
                if (mode == MODE_SOLO) {
                    // 1. Points de base pour avoir posé le bloc
                    int basePoints = 50;
                    effectManager.addLandingEffect((int) (currentMino.body.getPosition().x * SCALE),
                            (int) (currentMino.body.getPosition().y * SCALE));

                    // 2. Bonus de hauteur
                    // On récupère la position Y du bloc (en pixels)
                    float blockY = currentMino.body.getPosition().y * SCALE;

                    // En Java, Y=0 est en haut. Donc la hauteur réelle est (Sol - Y).
                    // On divise par 10 pour que les chiffres ne soient pas trop énormes.
                    int heightBonus = (int) ((bottom_y - blockY) / 10);

                    // Sécurité : pas de bonus négatif si on est sous le sol (peu probable mais bon)
                    if (heightBonus < 0)
                        heightBonus = 0;

                    int totalGain = basePoints + heightBonus;
                    score += totalGain;

                    // Floating Text
                    effectManager.addFloatingText((int) (currentMino.body.getPosition().x * SCALE),
                            (int) (currentMino.body.getPosition().y * SCALE) - 20, "+" + totalGain, Color.YELLOW);
                    if (heightBonus > 20) {
                        effectManager.addFloatingText((int) (currentMino.body.getPosition().x * SCALE),
                                (int) (currentMino.body.getPosition().y * SCALE) - 40, "NICE HEIGHT!", Color.CYAN);
                    }

                    System.out.println("Score: " + score); // Debug console
                } else {
                    // Multiplayer just visuals
                    effectManager.addLandingEffect((int) (currentMino.body.getPosition().x * SCALE),
                            (int) (currentMino.body.getPosition().y * SCALE));
                }
                // -----------------------------------------------------

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
        if (cameraY < 0)
            cameraY = 0;
    }

    private void spawnNextMino() {
        currentMino = nextMino;
        float spawnY = (float) (MINO_START_Y - cameraY);
        currentMino.createBody(world, MINO_START_X, spawnY, false);

        // NOUVEAU : On définit les limites à chaque apparition
        currentMino.setLimits(left_x, right_x);

        nextMino = pickMino();
    }

    private void checkGameStatus() {
        if (mode == MODE_MULTI) {
            boolean isTouchingFinish = false;

            Body b = world.getBodyList();
            while (b != null) {
                if (b.getType() == BodyType.DYNAMIC) {

                    // Check falling off screen
                    float pixelY = b.getPosition().y * SCALE;
                    if (pixelY > bottom_y + 100) {
                        if (!bodiesonDestroy.contains(b)) {
                            // Loss Effect
                            effectManager.addLossEffect((int) (b.getPosition().x * SCALE), bottom_y - 20);
                            effectManager.addExplosion((int) (b.getPosition().x * SCALE), bottom_y + 50);
                            bodiesonDestroy.add(b);
                            if (b == currentMino.body) {
                                currentMino.active = false;
                                forceSpawnNext = true;
                            }
                        }
                    }

                    if (currentMino != null && b == currentMino.body) {
                        b = b.getNext();
                        continue;
                    }
                    pixelY = b.getPosition().y * SCALE;

                    if (pixelY < bottom_y - currentFinishLineHeight) {
                        isTouchingFinish = true;
                        break;
                    }
                }
                b = b.getNext();
            }

            if (isTouchingFinish) {
                winTimer += timeStep;
                if (winTimer >= TIME_TO_WIN) {
                    gameFinished = true;
                    endMessage = "VICTOIRE !";
                }
            } else {
                winTimer = 0;
            }
        } else if (mode == MODE_SOLO) {
            Body b = world.getBodyList();
            while (b != null) {
                if (b.getType() == BodyType.DYNAMIC) {
                    float pixelY = b.getPosition().y * SCALE;

                    // --- MODIFICATION ICI : Augmenter la zone de tolérance ---
                    // Avant c'était + 100. On met + 500 pour laisser de la marge en bas.
                    float deadZone = bottom_y + 500;

                    if (pixelY > deadZone) {
                        Mino m = (Mino) b.getUserData();
                        if (m != null) {
                            if (!bodiesonDestroy.contains(b)) {
                                lives--; // On perd une vie seulement si on est vraiment très bas
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
        AffineTransform original = g2.getTransform();
        g2.translate(0, cameraY);

        // Ligne d'arrivée
        if (mode == MODE_MULTI) {
            int finishY = (int) (bottom_y - currentFinishLineHeight);

            if (winTimer > 0)
                g2.setColor(Color.RED);
            else
                g2.setColor(Color.GREEN);

            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[] { 9 }, 0));
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[] { 9 }, 0));
            g2.drawLine(left_x - 20, finishY, right_x + 20, finishY);

            powerUpManager.draw(g2);
            effectManager.draw(g2);

            g2.setFont(new Font("Arial", Font.BOLD, 15));
            g2.drawString("FINISH", right_x + 10, finishY);

            if (winTimer > 0) {
                g2.setColor(Color.YELLOW);
                g2.setFont(new Font("Arial", Font.BOLD, 40));
                float timeLeft = TIME_TO_WIN - winTimer;
                if (timeLeft < 0)
                    timeLeft = 0;
                String timeStr = String.format("%.1f", timeLeft);
                g2.drawString(timeStr, left_x + WIDTH / 2 - 30, finishY - 20);
            }
        }

        // Corps JBox2D
        Body b = world.getBodyList();
        while (b != null) {
            Object userData = b.getUserData();
            if (userData instanceof Mino) {
                ((Mino) userData).draw(g2);
            } else if (b.getType() == BodyType.STATIC) {
                Vec2 pos = b.getPosition();
                g2.setColor(Color.gray);

                // On dessine un rectangle qui correspond au sol (largeur écran, épaisseur 20)
                // Assure-toi que cela correspond à ton createGround()
                g2.fillRect(left_x, (int) (pos.y * SCALE) - 10, WIDTH, 20);
            }
            b = b.getNext();
        }

        g2.setTransform(original);

        // UI Next
        g2.setColor(Color.white);
        int uiX = right_x + 20;
        int uiY = top_y + 100;
        g2.drawString("Next:", uiX, uiY);
        if (nextMino != null) {
            nextMino.drawStatic(g2, NEXTMINO_X, NEXTMINO_Y);
        }

        // UI Infos
        if (mode == MODE_SOLO) {
            g2.setColor(Color.RED);
            g2.setFont(new Font("Arial", Font.BOLD, 20));
            g2.drawString("Vies: " + lives, left_x + 20, top_y + 30);
            g2.setColor(Color.YELLOW);
            g2.drawString("Score: " + score, left_x + 20, top_y + 60);
        } else {
            g2.setColor(Color.GREEN);
            g2.drawString("H: " + (int) cameraY, left_x + 20, top_y + 30);
        }

        if (gameFinished) {
            g2.setColor(Color.CYAN);
            g2.setFont(new Font("Arial", Font.BOLD, 40));
            int textW = g2.getFontMetrics().stringWidth(endMessage);
            g2.drawString(endMessage, left_x + WIDTH / 2 - textW / 2, top_y + HEIGHT / 2);
        }
    }
}