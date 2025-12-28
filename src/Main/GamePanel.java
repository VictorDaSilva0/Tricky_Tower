package Main;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class GamePanel extends JPanel implements Runnable {

    public static final int WIDTH = 1280;
    public static final int HEIGHT = 720;
    final int FPS = 60;

    Thread gameThread;
    KeyHandler keyH = new KeyHandler();

    // Gestion des états du jeu
    public int gameState;
    public final int titleState = 0;
    public final int playState = 1;

    // PlayManagers
    PlayManager pm1;
    PlayManager pm2;

    public GamePanel() {
        this.setPreferredSize(new Dimension(WIDTH, HEIGHT));
        this.setBackground(Color.black);
        this.setLayout(null);
        this.addKeyListener(keyH);
        this.setFocusable(true);

        // Gestion de la souris pour le Menu
        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (gameState == titleState) {
                    int x = e.getX();
                    int y = e.getY();

                    // Bouton SOLO (au centre haut)
                    if (x > WIDTH/2 - 100 && x < WIDTH/2 + 100 && y > 300 && y < 350) {
                        startGame(0); // 0 = SOLO
                    }
                    // Bouton MULTI (au centre bas)
                    else if (x > WIDTH/2 - 100 && x < WIDTH/2 + 100 && y > 400 && y < 450) {
                        startGame(1); // 1 = MULTI
                    }
                }
            }
        });

        gameState = titleState; // On commence sur le menu
    }

    public void startGame(int mode) {
        // Initialisation selon le mode
        if (mode == 0) { // SOLO
            // On centre le joueur 1 et on désactive le joueur 2
            pm1 = new PlayManager(WIDTH/2 - 180, keyH, 1, 0); // Mode 0 = Solo
            pm2 = null;
        } else { // MULTI
            // Deux joueurs côte à côte
            pm1 = new PlayManager(150, keyH, 1, 1); // Mode 1 = Multi
            pm2 = new PlayManager(750, keyH, 2, 1);
        }
        gameState = playState;
    }

    public void launchGame() {
        gameThread = new Thread(this);
        gameThread.start();
    }

    @Override
    public void run() {
        // ... (Votre boucle run() existante ne change pas) ...
        // Copiez-collez votre boucle while(gameThread != null) ici
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
        if (gameState == playState && !keyH.pausePressed) {
            if (pm1 != null) pm1.update();
            if (pm2 != null) pm2.update();
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (gameState == titleState) {
            drawMenu(g2);
        } else {
            if (pm1 != null) pm1.draw(g2);
            if (pm2 != null) pm2.draw(g2);

            // Pause
            if (keyH.pausePressed) {
                g2.setColor(Color.YELLOW);
                g2.setFont(new Font("Arial", Font.BOLD, 60));
                drawCenteredText("PAUSE", g2, HEIGHT/2);
            }
        }
        g2.dispose();
    }

    private void drawMenu(Graphics2D g2) {
        g2.setColor(Color.white);
        g2.setFont(new Font("Arial", Font.BOLD, 60));
        drawCenteredText("TRICKY TOWERS", g2, 150);

        // Bouton Solo
        g2.setColor(Color.gray);
        g2.fillRect(WIDTH/2 - 100, 300, 200, 50);
        g2.setColor(Color.white);
        g2.setFont(new Font("Arial", Font.BOLD, 30));
        drawCenteredText("SOLO", g2, 335);

        // Bouton Multi
        g2.setColor(Color.gray);
        g2.fillRect(WIDTH/2 - 100, 400, 200, 50);
        g2.setColor(Color.white);
        drawCenteredText("MULTIJOUEUR", g2, 435);
    }

    private void drawCenteredText(String text, Graphics2D g2, int y) {
        int length = (int)g2.getFontMetrics().getStringBounds(text, g2).getWidth();
        g2.drawString(text, (WIDTH/2) - (length/2), y);
    }
}