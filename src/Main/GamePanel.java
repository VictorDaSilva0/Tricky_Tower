package Main;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

public class GamePanel extends JPanel implements Runnable {

    // DIMENSIONS DYNAMIQUES (Plein écran)
    public int WIDTH;
    public int HEIGHT;
    final int FPS = 60;

    Thread gameThread;
    KeyHandler keyH = new KeyHandler();

    // États du jeu
    public int gameState;
    public final int titleState = 0;
    public final int playState = 1;
    public final int multiSelectState = 2; // NEW STATE

    // PlayManagers
    PlayManager pm1;
    PlayManager pm2;

    // Image de fond (Optionnel, sinon dégradé)
    BufferedImage backgroundImage;

    // --- VARIABLES MENU ---
    // Zones des boutons (Title)
    Rectangle btnSoloRect;
    Rectangle btnMultiRect;
    Rectangle btnQuitRect;

    // Zones Des Boutons (Multi Select)
    Rectangle btnClassicRect;
    Rectangle btnPuzzleMultiRect;
    Rectangle btnBackRect;

    // État de survol (Hover)
    boolean hoverSolo = false;
    boolean hoverMulti = false;
    boolean hoverQuit = false;

    boolean hoverClassic = false;
    boolean hoverPuzzleMulti = false;
    boolean hoverBack = false;

    public GamePanel() {
        // ... (Screen size logic) ...
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        this.WIDTH = (int) screenSize.getWidth();
        this.HEIGHT = (int) screenSize.getHeight();

        this.setPreferredSize(new Dimension(WIDTH, HEIGHT));
        this.setBackground(Color.black);
        this.setLayout(null);
        this.addKeyListener(keyH);
        this.setFocusable(true);

        try {
            backgroundImage = ImageIO.read(getClass().getResourceAsStream("/res/background.jpg"));
        } catch (Exception e) {
        }

        // Initialisation des positions des boutons (Main Menu)
        int btnW = 300;
        int btnH = 60;
        int gap = 30;
        int centerX = WIDTH / 2 - btnW / 2;
        int startY = HEIGHT / 2 - 50;

        btnSoloRect = new Rectangle(centerX, startY, btnW, btnH);
        btnMultiRect = new Rectangle(centerX, startY + btnH + gap, btnW, btnH);
        btnQuitRect = new Rectangle(centerX, startY + (btnH + gap) * 2, btnW, btnH);

        // Initialisation des boutons (Multi Select)
        btnClassicRect = new Rectangle(centerX, startY, btnW, btnH);
        btnPuzzleMultiRect = new Rectangle(centerX, startY + btnH + gap, btnW, btnH);
        btnBackRect = new Rectangle(centerX, startY + (btnH + gap) * 2, btnW, btnH);

        // --- GESTION SOURIS (CLIC & MOUVEMENT) ---
        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (gameState == titleState) {
                    hoverSolo = btnSoloRect.contains(e.getPoint());
                    hoverMulti = btnMultiRect.contains(e.getPoint());
                    hoverQuit = btnQuitRect.contains(e.getPoint());
                } else if (gameState == multiSelectState) {
                    hoverClassic = btnClassicRect.contains(e.getPoint());
                    hoverPuzzleMulti = btnPuzzleMultiRect.contains(e.getPoint());
                    hoverBack = btnBackRect.contains(e.getPoint());
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (gameState == titleState) {
                    if (btnSoloRect.contains(e.getPoint())) {
                        startGame(0); // SOLO
                    } else if (btnMultiRect.contains(e.getPoint())) {
                        gameState = multiSelectState; // Go to Sub-menu
                    } else if (btnQuitRect.contains(e.getPoint())) {
                        System.exit(0);
                    }
                } else if (gameState == multiSelectState) {
                    if (btnClassicRect.contains(e.getPoint())) {
                        startGame(1); // MULTI RACE (Use existing ID 1)
                    } else if (btnPuzzleMultiRect.contains(e.getPoint())) {
                        startGame(2); // MULTI PUZZLE (Use existing ID 2)
                    } else if (btnBackRect.contains(e.getPoint())) {
                        gameState = titleState; // Back
                    }
                }
            }
        };

        this.addMouseListener(mouseHandler);
        this.addMouseMotionListener(mouseHandler);

        gameState = titleState;
    }

    public void startGame(int mode) {
        int pmWidth = 360;
        int pmHeight = 600;

        // Espace entre les deux zones de jeu
        int gap = 400;

        // Centrage vertical
        int startY = (this.HEIGHT - pmHeight) / 2;

        int startX1, startX2;

        if (mode == 0) { // MODE SOLO
            startX1 = (WIDTH / 2) - (pmWidth / 2);

            // CORRECTION : Ajout de ", WIDTH" à la fin
            pm1 = new PlayManager(startX1, startY, keyH, 1, 0, WIDTH);
            pm2 = null;

        } else if (mode == 2) { // MODE PUZZLE (1v1)
            int totalW = (pmWidth * 2) + gap;
            int startX = (WIDTH - totalW) / 2;
            startX1 = startX;
            startX2 = startX + pmWidth + gap;

            // CORRECTION : Ajout de ", WIDTH" à la fin pour les deux joueurs
            pm1 = new PlayManager(startX1, startY, keyH, 1, 2, WIDTH);
            pm2 = new PlayManager(startX2, startY, keyH, 2, 2, WIDTH);

            pm1.setOpponent(pm2);
            pm2.setOpponent(pm1);

        } else { // MODE MULTI CLASSIC
            int totalW = (pmWidth * 2) + gap;
            int startX = (WIDTH - totalW) / 2;
            startX1 = startX;
            startX2 = startX + pmWidth + gap;

            // CORRECTION : Ajout de ", WIDTH" à la fin pour les deux joueurs
            pm1 = new PlayManager(startX1, startY, keyH, 1, 1, WIDTH);
            pm2 = new PlayManager(startX2, startY, keyH, 2, 1, WIDTH);

            pm1.setOpponent(pm2);
            pm2.setOpponent(pm1);
        }
        gameState = playState;
    }

    public void launchGame() {
        gameThread = new Thread(this);
        gameThread.start();
    }

    @Override
    public void run() {
        double drawInterval = 1000000000 / FPS;
        double delta = 0;
        long lastTime = System.nanoTime();
        long currentTime;

        while (gameThread != null) {
            currentTime = System.nanoTime();
            delta += (currentTime - lastTime) / drawInterval;
            lastTime = currentTime;

            if (delta >= 1) {
                update();
                repaint();
                delta--;
            }
        }
    }

    private void update() {
        // Retour menu via Echap
        if (gameState == playState) {
            if (keyH.escapePressed) {
                gameState = titleState;
                keyH.escapePressed = false;
                keyH.pausePressed = false;
                return;
            }
            if (!keyH.pausePressed) {
                if (pm1 != null)
                    pm1.update();
                if (pm2 != null)
                    pm2.update();
            }
        }
    }

    private void drawMultiSelectMenu(Graphics2D g2) {
        g2.setColor(Color.white);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 60));
        g2.setColor(new Color(0, 0, 0, 100));
        drawCenteredText("MODE SELECTION", g2, 185);
        g2.setColor(Color.ORANGE);
        drawCenteredText("MODE SELECTION", g2, 180);

        drawButton(g2, btnClassicRect, "RACE (CLASSIC)", hoverClassic);
        drawButton(g2, btnPuzzleMultiRect, "PUZZLE (1v1)", hoverPuzzleMulti);
        drawButton(g2, btnBackRect, "BACK", hoverBack);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        // Activer l'anti-aliasing pour des textes et formes lisses
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // --- 1. DESSINER LE FOND ---
        drawBackground(g2);

        // --- 2. DESSINER LE JEU OU LE MENU ---
        if (gameState == titleState) {
            drawMenu(g2);
        } else if (gameState == multiSelectState) {
            drawMultiSelectMenu(g2);
        } else {
            if (pm1 != null)
                pm1.draw(g2);
            if (pm2 != null)
                pm2.draw(g2);

            // UI Pause
            if (keyH.pausePressed) {
                g2.setColor(new Color(0, 0, 0, 150)); // Voile noir transparent
                g2.fillRect(0, 0, WIDTH, HEIGHT);
                g2.setColor(Color.YELLOW);
                g2.setFont(new Font("Arial", Font.BOLD, 80));
                drawCenteredText("PAUSE", g2, HEIGHT / 2);
                g2.setFont(new Font("Arial", Font.PLAIN, 30));
                drawCenteredText("Appuyez sur ESPACE pour reprendre", g2, HEIGHT / 2 + 50);
                drawCenteredText("Appuyez sur ECHAP pour quitter", g2, HEIGHT / 2 + 90);
            }
        }
        g2.dispose();
    }

    private void drawBackground(Graphics2D g2) {
        if (gameState == playState && backgroundImage != null) {
            g2.drawImage(backgroundImage, 0, 0, WIDTH, HEIGHT, null);
        } else {
            // Fond Dégradé (Joli effet Nuit) pour les menus
            GradientPaint gp = new GradientPaint(0, 0, new Color(20, 20, 60), 0, HEIGHT, new Color(10, 10, 20));
            g2.setPaint(gp);
            g2.fillRect(0, 0, WIDTH, HEIGHT);
        }
    }

    private void drawMenu(Graphics2D g2) {
        // Titre
        g2.setColor(Color.white);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 90));

        // Ombre du titre
        g2.setColor(new Color(0, 0, 0, 100));
        drawCenteredText("MINO", g2, 205);

        // Titre principal
        g2.setColor(Color.CYAN);
        drawCenteredText("MINO", g2, 200);

        // Dessin des boutons
        drawButton(g2, btnSoloRect, "SOLO", hoverSolo);
        drawButton(g2, btnMultiRect, "MULTIJOUEUR", hoverMulti);
        drawButton(g2, btnQuitRect, "QUITTER", hoverQuit);

        // Crédits bas de page
        g2.setFont(new Font("Arial", Font.PLAIN, 15));
        g2.setColor(Color.GRAY);
        g2.drawString("Projet Java - Physics Edition", 20, HEIGHT - 20);
    }

    // Méthode utilitaire pour dessiner un bouton stylé
    private void drawButton(Graphics2D g2, Rectangle rect, String text, boolean hover) {
        // Couleur du bouton (Change si survolé)
        if (hover) {
            g2.setColor(new Color(255, 255, 255, 200)); // Blanc brillant
        } else {
            g2.setColor(new Color(255, 255, 255, 50)); // Blanc transparent
        }

        // Forme arrondie
        g2.fill(new RoundRectangle2D.Double(rect.x, rect.y, rect.width, rect.height, 30, 30));

        // Bordure
        g2.setColor(Color.white);
        g2.setStroke(new BasicStroke(2));
        g2.draw(new RoundRectangle2D.Double(rect.x, rect.y, rect.width, rect.height, 30, 30));

        // Texte du bouton
        g2.setColor(hover ? new Color(20, 20, 60) : Color.white);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 30));

        // Centrer le texte dans le bouton
        FontMetrics fm = g2.getFontMetrics();
        int textX = rect.x + (rect.width - fm.stringWidth(text)) / 2;
        int textY = rect.y + (rect.height - fm.getHeight()) / 2 + fm.getAscent();

        g2.drawString(text, textX, textY);
    }

    private void drawCenteredText(String text, Graphics2D g2, int y) {
        int length = (int) g2.getFontMetrics().getStringBounds(text, g2).getWidth();
        g2.drawString(text, (WIDTH / 2) - (length / 2), y);
    }
}