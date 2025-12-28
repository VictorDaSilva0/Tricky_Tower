package Main;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class KeyHandler implements KeyListener {

    public boolean upPressed1, downPressed1, leftPressed1, rightPressed1; // Joueur 1
    public boolean upPressed2, downPressed2, leftPressed2, rightPressed2; // Joueur 2
    public boolean pausePressed;
    public boolean escapePressed;

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();

        // JOUEUR 1 (ZQSD)
        if (code == KeyEvent.VK_Z) upPressed1 = true;
        if (code == KeyEvent.VK_Q) leftPressed1 = true;
        if (code == KeyEvent.VK_S) downPressed1 = true;
        if (code == KeyEvent.VK_D) rightPressed1 = true;

        // JOUEUR 2 (Flèches)
        if (code == KeyEvent.VK_UP) upPressed2 = true;
        if (code == KeyEvent.VK_LEFT) leftPressed2 = true;
        if (code == KeyEvent.VK_DOWN) downPressed2 = true;
        if (code == KeyEvent.VK_RIGHT) rightPressed2 = true;

        if (code == KeyEvent.VK_SPACE) {
            pausePressed = !pausePressed; // Bascule Pause
        }

        if (code == KeyEvent.VK_ESCAPE) {
            escapePressed = true;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int code = e.getKeyCode();

        // JOUEUR 1
        if (code == KeyEvent.VK_Z) upPressed1 = false;
        if (code == KeyEvent.VK_Q) leftPressed1 = false;
        if (code == KeyEvent.VK_S) downPressed1 = false;
        if (code == KeyEvent.VK_D) rightPressed1 = false;

        // JOUEUR 2
        if (code == KeyEvent.VK_UP) upPressed2 = false;
        if (code == KeyEvent.VK_LEFT) leftPressed2 = false;
        if (code == KeyEvent.VK_DOWN) downPressed2 = false;
        if (code == KeyEvent.VK_RIGHT) rightPressed2 = false;
    }
}