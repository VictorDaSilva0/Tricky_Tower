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
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Random;

public class PlayManager {

    // --- SETTINGS ---
    final int WIDTH = 360;
    final int HEIGHT = 600;
    public int left_x, right_x, top_y, bottom_y;

    // MODIF : Pour connaitre le centre de l'écran global
    int screenWidth;

    // Caméra
    double cameraY = 0;
    double targetCameraY = 0;

    // Modes
    int mode;
    public static final int MODE_SOLO = 0;
    public static final int MODE_MULTI = 1;
    public static final int MODE_PUZZLE = 2;

    public float laserY = 0;

    // Victoire / Défaite
    int lives = 3;
    final float START_FINISH_LINE_HEIGHT = 2000f;
    float currentFinishLineHeight;
    final float LINE_DROP_SPEED = 0.15f;
    final float MIN_FINISH_HEIGHT = 300f;

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

    public EffectManager effectManager = new EffectManager();
    public PowerUpManager powerUpManager;

    public boolean inStartSequence = true;
    public float startTimer = 3.0f;

    public int magicCharge = 0;
    public boolean hasMagic = false;

    float currentHeight = 0;
    int lastMagicStep = 0;
    final int BONUS_STEP = 200;

    BufferedImage platformImage;

    // MODIF : Constructeur prend screenWidth maintenant
    public PlayManager(int startX, int startY, KeyHandler keyH, int playerID, int mode, int screenWidth) {
        this.left_x = startX;
        this.right_x = left_x + WIDTH;
        this.top_y = startY;
        this.bottom_y = top_y + HEIGHT;
        this.keyH = keyH;
        this.playerID = playerID;
        this.mode = mode;
        this.screenWidth = screenWidth; // Stockage

        MINO_START_X = left_x + (WIDTH / 2);
        MINO_START_Y = top_y + Block.SIZE;

        NEXTMINO_X = right_x + 85;
        NEXTMINO_Y = top_y + 100;

        world = new World(new Vec2(0, 9.8f));
        createGround();

        try {
            platformImage = javax.imageio.ImageIO.read(getClass().getResourceAsStream("/res/plateforme.png"));
        } catch (Exception e) {
            System.err.println("Error loading platform image");
        }

        currentMino = pickMino();
        currentMino.createBody(world, MINO_START_X, MINO_START_Y, false);

        // MODIF : Appliquer les limites personnalisées
        applyLimits();

        nextMino = pickMino();

        currentFinishLineHeight = START_FINISH_LINE_HEIGHT;

        if (mode == MODE_PUZZLE) {
            laserY = top_y + 200;
        }

        powerUpManager = new PowerUpManager(this);
    }

    // MODIF : Nouvelle méthode pour gérer les murs invisibles
    private void applyLimits() {
        if (currentMino == null)
            return;

        int screenCenter = screenWidth / 2;
        int noWall = 50000; // Valeur très grande (virtuellement infinie)

        int limitLeft, limitRight;

        if (mode == MODE_SOLO) {
            // SOLO : Liberté totale
            limitLeft = -noWall;
            limitRight = screenWidth + noWall;
        } else {
            // MULTIJOUEUR
            if (playerID == 1) {
                // JOUEUR 1 (Gauche) : Mur à droite (Centre), Libre à gauche
                limitLeft = -noWall;
                limitRight = screenCenter;
            } else {
                // JOUEUR 2 (Droite) : Mur à gauche (Centre), Libre à droite
                limitLeft = screenCenter;
                limitRight = screenWidth + noWall;
            }
        }
        currentMino.setLimits(limitLeft, limitRight);
    }

    private void calculateCurrentHeight() {
        float highestY = bottom_y;
        Body checkBody = world.getBodyList();
        while (checkBody != null) {
            if (checkBody.getType() == BodyType.DYNAMIC && checkBody != currentMino.body) {
                float pixelY = (checkBody.getPosition().y * SCALE) - (Block.SIZE / 2.0f);
                if (pixelY < highestY)
                    highestY = pixelY;
            }
            checkBody = checkBody.getNext();
        }
        currentHeight = bottom_y - highestY;
        if (currentHeight < 0)
            currentHeight = 0;

        int step = (int) (currentHeight / BONUS_STEP);
        if (step > lastMagicStep) {
            lastMagicStep = step;
            if (!hasMagic && mode != MODE_SOLO) {
                pickRandomSpell();
                effectManager.addFloatingText(left_x + WIDTH / 2, top_y + 150, "BONUS!", Color.YELLOW);
            }
        }
    }

    private void createGround() {
        BodyDef groundDef = new BodyDef();
        groundDef.position.set((left_x + WIDTH / 2) / SCALE, (bottom_y) / SCALE);
        groundDef.type = BodyType.STATIC;
        Body groundBody = world.createBody(groundDef);

        org.jbox2d.collision.shapes.PolygonShape groundShape = new org.jbox2d.collision.shapes.PolygonShape();
        // Updated Logic: Widen ground to match visual extension (WIDTH + 100)
        float totalWidth = WIDTH + 100;
        float groundWidthHalf = (totalWidth / 2) / SCALE;

        groundShape.setAsBox(groundWidthHalf, 10 / SCALE);
        groundBody.createFixture(groundShape, 0.0f);
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

    public PlayManager opponent;

    public void setOpponent(PlayManager op) {
        this.opponent = op;
    }

    public void update() {
        if (inStartSequence) {
            startTimer -= timeStep;
            if (startTimer <= 0) {
                inStartSequence = false;
                if (mode != MODE_SOLO) {
                    pickRandomSpell();
                }
            }
            return;
        }

        effectManager.update();
        if (!gameFinished) {
            calculateCurrentHeight();
            powerUpManager.update();

            if (playerID == 1 && keyH.castPressed1 && currentMagicType != 0) {
                castSpell();
                keyH.castPressed1 = false;
            }
            if (playerID == 2 && keyH.castPressed2 && currentMagicType != 0) {
                castSpell();
                keyH.castPressed2 = false;
            }
        }

        if (gameFinished) {
            if (endMessage.equals("VICTOIRE !")) {
                if (!victoryRoofSpawned) {
                    spawnVictoryRoof();
                    victoryRoofSpawned = true;
                }
                effectManager.addConfetti(left_x + WIDTH / 2, top_y + HEIGHT / 2);
                if (new Random().nextInt(10) < 3) {
                    int rx = left_x + new Random().nextInt(WIDTH);
                    int ry = top_y + new Random().nextInt(HEIGHT);
                    effectManager.addConfetti(rx, ry);
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

            // MODIF : Passage à la suivante si STABLE ou si COLLISION LOCK DELAY dépassé
            if (currentMino.isStopped() || currentMino.collided) {
                currentMino.active = false;

                if (mode == MODE_SOLO) {
                    int basePoints = 50;
                    effectManager.addLandingEffect((int) (currentMino.body.getPosition().x * SCALE),
                            (int) (currentMino.body.getPosition().y * SCALE));

                    float blockY = currentMino.body.getPosition().y * SCALE;
                    int heightBonus = (int) ((bottom_y - blockY) / 10);
                    if (heightBonus < 0)
                        heightBonus = 0;

                    int totalGain = basePoints + heightBonus;
                    score += totalGain;

                    effectManager.addFloatingText((int) (currentMino.body.getPosition().x * SCALE),
                            (int) (currentMino.body.getPosition().y * SCALE) - 20, "+" + totalGain, Color.YELLOW);
                    if (heightBonus > 20) {
                        effectManager.addFloatingText((int) (currentMino.body.getPosition().x * SCALE),
                                (int) (currentMino.body.getPosition().y * SCALE) - 40, "NICE HEIGHT!", Color.CYAN);
                    }
                } else {
                    effectManager.addLandingEffect((int) (currentMino.body.getPosition().x * SCALE),
                            (int) (currentMino.body.getPosition().y * SCALE));
                }
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

        int margin = Block.SIZE * 2;
        int minX = left_x + margin;
        int maxX = right_x - margin;
        int randomX = minX + new Random().nextInt(maxX - minX);

        currentMino.createBody(world, randomX, spawnY, false);

        // MODIF : Appliquer les murs
        applyLimits();

        nextMino = pickMino();
    }

    private void checkGameStatus() {
        if (mode == MODE_MULTI)
            checkMultiStatus();
        else if (mode == MODE_SOLO)
            checkSoloStatus();
        else if (mode == MODE_PUZZLE)
            checkPuzzleStatus();
    }

    private void checkMultiStatus() {
        boolean isTouchingFinish = false;
        Body b = world.getBodyList();
        while (b != null) {
            if (b.getType() == BodyType.DYNAMIC) {
                float pixelY = b.getPosition().y * SCALE;
                if (pixelY > bottom_y + 100) {
                    if (!bodiesonDestroy.contains(b)) {
                        effectManager.addLossEffect((int) (b.getPosition().x * SCALE), bottom_y - 20);
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

                float topY = pixelY - (Block.SIZE / 2.0f);
                if (topY < bottom_y - currentFinishLineHeight) {
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
    }

    private void checkSoloStatus() {
        Body b = world.getBodyList();
        while (b != null) {
            if (b.getType() == BodyType.DYNAMIC) {
                float pixelY = b.getPosition().y * SCALE;
                float deadZone = bottom_y + 500;
                if (pixelY > deadZone) {
                    Mino m = (Mino) b.getUserData();
                    if (m != null && !bodiesonDestroy.contains(b)) {
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
            b = b.getNext();
        }
    }

    private void checkPuzzleStatus() {
        int validBlocks = 0;
        Body b = world.getBodyList();
        while (b != null) {
            if (b.getType() == BodyType.DYNAMIC && b != currentMino.body) {
                float topY = (b.getPosition().y * SCALE) - (Block.SIZE / 2.0f);
                if (topY < laserY) {
                    gameFinished = true;
                    endMessage = "LASER HIT!";
                }
                if (!bodiesonDestroy.contains(b)) {
                    if ((b.getPosition().y * SCALE) < bottom_y + 50) {
                        validBlocks++;
                    }
                }
            }
            b = b.getNext();
        }
        if (mode == MODE_PUZZLE && !gameFinished) {
            score = validBlocks;
        }
    }

    public int currentMagicType = 0;

    private void pickRandomSpell() {
        hasMagic = true;
        currentMagicType = new Random().nextInt(3) + 1;
    }

    private void castSpell() {
        if (opponent != null) {
            opponent.powerUpManager.castMagic(opponent.playerID, currentMagicType);
            effectManager.addFloatingText(left_x + WIDTH / 2, top_y + 200, "ATTACK SENT!", Color.RED);
            opponent.effectManager.addFloatingText(opponent.left_x + opponent.WIDTH / 2, opponent.top_y + 200,
                    "INCOMING!", Color.RED);
        } else {
            powerUpManager.castMagic(playerID, currentMagicType);
            effectManager.addFloatingText(left_x + WIDTH / 2, top_y + 200, "SELF CAST!", Color.ORANGE);
        }
        hasMagic = false;
        currentMagicType = 0;
    }

    public void draw(Graphics2D g2) {
        AffineTransform original = g2.getTransform();
        g2.translate(0, cameraY);

        g2.setColor(new Color(0, 0, 0, 80));
        g2.fillRoundRect(left_x - 10, top_y - 5000, WIDTH + 20, HEIGHT + 10000, 20, 20);

        // MODIF : Ligne de séparation au milieu si multi
        if (mode != MODE_SOLO) {
            g2.setColor(new Color(255, 255, 255, 100));
            g2.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[] { 9 }, 0));
            // On dessine le mur invisible central pour info
            int centerX = screenWidth / 2;
            if (Math.abs(left_x + WIDTH - centerX) < 100 || Math.abs(left_x - centerX) < 100) {
                // Si ce playmanager est proche du centre
                // g2.drawLine(centerX, top_y - 5000, centerX, bottom_y); // Optionnel
            }
        }

        g2.setColor(new Color(255, 255, 255, 50));
        g2.setStroke(new BasicStroke(4));
        // Murs visuels de la zone (pas des murs physiques)
        g2.drawLine(left_x, top_y - 5000, left_x, bottom_y);
        g2.drawLine(right_x, top_y - 5000, right_x, bottom_y);

        if (mode == MODE_MULTI) {
            int finishY = (int) (bottom_y - currentFinishLineHeight);

            if (winTimer > 0)
                g2.setColor(Color.RED);
            else
                g2.setColor(Color.GREEN);

            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[] { 9 }, 0));
            g2.drawLine(left_x - 20, finishY, right_x + 20, finishY);

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

        } else if (mode == MODE_PUZZLE) {
            g2.setColor(Color.RED);
            g2.setStroke(new BasicStroke(3f));
            int lY = (int) laserY;
            g2.drawLine(left_x - 20, lY, right_x + 20, lY);
            g2.setColor(new Color(255, 0, 0, 50));
            g2.fillRect(left_x - 20, lY - 5, WIDTH + 40, 10);
            g2.setColor(Color.RED);
            g2.drawString("LASER LIMIT", left_x + 10, lY - 10);

            g2.setColor(new Color(255, 255, 255, 100));
            int groundY = bottom_y;
            for (int x = left_x; x < right_x; x += Block.SIZE) {
                g2.fillRect(x + 2, groundY - 5, Block.SIZE - 4, 5);
                g2.setColor(new Color(255, 255, 255, 30));
                g2.drawLine(x, groundY, x, groundY - 50);
                g2.drawLine(x + Block.SIZE, groundY, x + Block.SIZE, groundY - 50);
                g2.setColor(new Color(255, 255, 255, 100));
            }
        }

        // --- MAGIC UI & PROGRESS BAR ---
        // ... (Reste inchangé) ...
        if (hasMagic) {
            int cardX = left_x + 20;
            int cardY = top_y + 120;
            int cardW = 100;
            int cardH = 60;

            g2.setColor(new Color(0, 0, 0, 150));
            g2.fillRoundRect(cardX, cardY, cardW, cardH, 15, 15);
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2));
            g2.drawRoundRect(cardX, cardY, cardW, cardH, 15, 15);

            String spellName = "";
            Color spellColor = Color.WHITE;
            switch (currentMagicType) {
                case PowerUpManager.EVT_WIND:
                    spellName = "WIND";
                    spellColor = Color.CYAN;
                    break;
                case PowerUpManager.EVT_HEAVY:
                    spellName = "HEAVY";
                    spellColor = Color.GRAY;
                    break;
                case PowerUpManager.EVT_REVERSE:
                    spellName = "CHAOS";
                    spellColor = Color.MAGENTA;
                    break;
            }

            int iconCX = cardX + 30;
            int iconCY = cardY + 30;
            g2.setColor(spellColor);

            if (currentMagicType == PowerUpManager.EVT_WIND) {
                g2.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(iconCX - 15, iconCY - 5, iconCX + 5, iconCY - 5);
                g2.drawLine(iconCX - 10, iconCY + 5, iconCX + 15, iconCY + 5);
                g2.drawLine(iconCX - 20, iconCY, iconCX - 5, iconCY);
            } else if (currentMagicType == PowerUpManager.EVT_HEAVY) {
                g2.fillRect(iconCX - 12, iconCY - 8, 24, 10);
                g2.fillPolygon(new int[] { iconCX - 8, iconCX + 8, iconCX + 12, iconCX - 12 },
                        new int[] { iconCY + 2, iconCY + 2, iconCY + 12, iconCY + 12 }, 4);
            } else if (currentMagicType == PowerUpManager.EVT_REVERSE) {
                g2.setFont(new Font("Times New Roman", Font.BOLD, 35));
                g2.drawString("?", iconCX - 8, iconCY + 12);
            }

            g2.setFont(new Font("Arial", Font.BOLD, 14));
            g2.setColor(spellColor);
            g2.drawString(spellName, cardX + 55, cardY + 35);
            g2.setFont(new Font("Arial", Font.ITALIC, 10));
            g2.setColor(Color.LIGHT_GRAY);
            String key = (playerID == 1) ? "[E]" : "[M]";
            g2.drawString("PRESS " + key, cardX + 10, cardY + cardH + 12);

        } else {
            // ONLY SHOW PROGRESS BAR IF NOT SOLO
            if (mode != MODE_SOLO) {
                float progress = (currentHeight % BONUS_STEP) / (float) BONUS_STEP;
                int barW = 100;
                int barH = 10;
                int barX = left_x + 20;
                int barY = top_y + 130;

                g2.setColor(new Color(0, 0, 0, 150));
                g2.fillRect(barX, barY, barW, barH);
                g2.setColor(Color.MAGENTA);
                g2.fillRect(barX, barY, (int) (barW * progress), barH);
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(1));
                g2.drawRect(barX, barY, barW, barH);
                g2.setFont(new Font("Arial", Font.PLAIN, 10));
                g2.drawString("NEXT SPELL", barX + 20, barY - 5);
            }
        }

        powerUpManager.draw(g2);
        effectManager.draw(g2);

        // --- DRAW BODIES ---
        Body b = world.getBodyList();
        while (b != null) {
            Object userData = b.getUserData();
            if (userData instanceof Mino) {
                ((Mino) userData).draw(g2);
            } else if (b.getType() == BodyType.STATIC) {
                Vec2 pos = b.getPosition();
                if (platformImage != null) {
                    // Center of the playing field
                    int centerX = left_x + WIDTH / 2;
                    // New wider width for the platform (extending into the gap)
                    int platTotalWidth = WIDTH + 100;
                    int platHalfWidth = platTotalWidth / 2;

                    // Adjust Y to avoid "floating pieces" effect.
                    int drawY = (int) (pos.y * SCALE) - 60;
                    int drawH = 150;

                    // --- VISIBILITY FIX: Glow/Highlight behind the platform ---
                    Color glowColor = new Color(255, 255, 255, 40); // Soft white glow
                    g2.setColor(glowColor);
                    g2.fillOval(centerX - platHalfWidth - 20, drawY + 20, platTotalWidth + 40, drawH - 40);

                    // Draw the platform image
                    g2.drawImage(platformImage, centerX - platHalfWidth, drawY, platTotalWidth, drawH, null);

                    // --- VISIBILITY FIX: Clear Surface Line ---
                    // The physics body is a box of height 20 (half-height 10).
                    // Top surface is at pos.y - 10/SCALE.
                    int surfaceY = (int) ((pos.y * SCALE) - 10);

                    g2.setColor(new Color(100, 255, 200, 150)); // Bright cyan/green line
                    g2.setStroke(new BasicStroke(3));
                    g2.drawLine(centerX - platHalfWidth, surfaceY, centerX + platHalfWidth, surfaceY);

                    // Reset Stroke
                    g2.setStroke(new BasicStroke(1));

                } else {
                    g2.setColor(Color.gray);
                    g2.fillRect(left_x, (int) (pos.y * SCALE) - 10, WIDTH, 20);
                }
            }
            b = b.getNext();
        }

        g2.setTransform(original);

        g2.setColor(Color.white);
        int uiX = right_x + 20;
        int uiY = top_y + 100;
        g2.drawString("Next:", uiX, uiY);
        if (nextMino != null) {
            nextMino.drawStatic(g2, NEXTMINO_X, NEXTMINO_Y);
        }

        if (mode == MODE_SOLO) {
            g2.setColor(Color.RED);
            g2.setFont(new Font("Arial", Font.BOLD, 20));
            g2.drawString("Vies: " + lives, left_x + 20, top_y + 30);
            g2.setColor(Color.YELLOW);
            g2.drawString("Score: " + score, left_x + 20, top_y + 60);

        } else if (mode == MODE_PUZZLE) {
            g2.setColor(Color.MAGENTA);
            g2.setFont(new Font("Arial", Font.BOLD, 40));
            g2.drawString("" + score, left_x + WIDTH / 2 - 15, top_y + 50);
        } else {
            g2.setColor(Color.GREEN);
            float currentTopY = bottom_y;
            Body bodyH = world.getBodyList();
            while (bodyH != null) {
                if (bodyH.getType() == BodyType.DYNAMIC && bodyH != currentMino.body) {
                    float pY = (bodyH.getPosition().y * SCALE) - (Block.SIZE / 2.0f);
                    if (pY < currentTopY)
                        currentTopY = pY;
                }
                bodyH = bodyH.getNext();
            }
            int displayH = (int) (bottom_y - currentTopY);
            g2.drawString("H: " + displayH, left_x + 20, top_y + 30);
        }

        if (gameFinished) {
            g2.setColor(Color.CYAN);
            g2.setFont(new Font("Arial", Font.BOLD, 40));
            int textW = g2.getFontMetrics().stringWidth(endMessage);
            g2.drawString(endMessage, left_x + WIDTH / 2 - textW / 2, top_y + HEIGHT / 2);
        }

        if (inStartSequence) {
            g2.setColor(Color.YELLOW);
            g2.setFont(new Font("Arial", Font.BOLD, 100));
            int seconds = (int) Math.ceil(startTimer);
            String t = (seconds > 0) ? String.valueOf(seconds) : "GO!";
            int w = g2.getFontMetrics().stringWidth(t);
            g2.drawString(t, left_x + WIDTH / 2 - w / 2, top_y + HEIGHT / 2);
        }

        float highestY = bottom_y;
        Body checkBody = world.getBodyList();
        while (checkBody != null) {
            if (checkBody.getType() == BodyType.DYNAMIC && checkBody != currentMino.body) {
                float pixelY = (checkBody.getPosition().y * SCALE) - (Block.SIZE / 2.0f);
                if (pixelY < highestY)
                    highestY = pixelY;
            }
            checkBody = checkBody.getNext();
        }

        if (highestY < bottom_y) {
            g2.setColor(new Color(255, 255, 255, 100));
            g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[] { 5 }, 0));
            int lineY = (int) highestY;
            g2.drawLine(left_x, lineY, right_x, lineY);
            g2.setFont(new Font("Arial", Font.PLAIN, 12));
            int heightValue = (int) (bottom_y - highestY);
            g2.drawString("Max: " + heightValue, left_x + 5, lineY - 5);
        }
    }

    boolean victoryRoofSpawned = false;

    private void spawnVictoryRoof() {
        float highestY = bottom_y;
        Body b = world.getBodyList();
        while (b != null) {
            if (b.getType() == BodyType.DYNAMIC && b != currentMino.body) {
                float pixelY = (b.getPosition().y * SCALE) - (Block.SIZE / 2.0f);
                if (pixelY < highestY)
                    highestY = pixelY;
            }
            b = b.getNext();
        }

        if (highestY < bottom_y) {
            currentMino = new Mino_T();
            currentMino.createBody(world, left_x + WIDTH / 2, highestY - 40, true);
            currentMino.c = Color.YELLOW;
            effectManager.addFloatingText(left_x + WIDTH / 2, (int) highestY - 60, "CASTLE COMPLETE!", Color.YELLOW);
        }
    }
}