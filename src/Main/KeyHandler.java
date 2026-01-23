package Main;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class KeyHandler implements KeyListener {

    public boolean upPressed1, downPressed1, leftPressed1, rightPressed1, dashPressed1, castPressed1;
    public boolean upPressed2, downPressed2, leftPressed2, rightPressed2, dashPressed2, castPressed2;
    public boolean pausePressed;
    public boolean escapePressed;

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();

        // JOUEUR 1 (ZQSD + SHIFT)
        if (code == KeyEvent.VK_Z)
            upPressed1 = true;
        if (code == KeyEvent.VK_Q)
            leftPressed1 = true;
        if (code == KeyEvent.VK_S)
            downPressed1 = true;
        if (code == KeyEvent.VK_D)
            rightPressed1 = true;
        if (code == KeyEvent.VK_SHIFT)
            dashPressed1 = true; // Dash J1
        if (code == KeyEvent.VK_E)
            castPressed1 = true; // Cast J1

        // JOUEUR 2 (Flèches + ENTREE)
        if (code == KeyEvent.VK_UP)
            upPressed2 = true;
        if (code == KeyEvent.VK_LEFT)
            leftPressed2 = true;
        if (code == KeyEvent.VK_DOWN)
            downPressed2 = true;
        if (code == KeyEvent.VK_RIGHT)
            rightPressed2 = true;
        if (code == KeyEvent.VK_ENTER)
            dashPressed2 = true; // Dash J2
        if (code == KeyEvent.VK_M)
            castPressed2 = true; // Cast J2

        if (code == KeyEvent.VK_SPACE) {
            pausePressed = !pausePressed;
        }
        if (code == KeyEvent.VK_ESCAPE) {
            escapePressed = true;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int code = e.getKeyCode();

        // JOUEUR 1
        if (code == KeyEvent.VK_Z)
            upPressed1 = false;
        if (code == KeyEvent.VK_Q)
            leftPressed1 = false;
        if (code == KeyEvent.VK_S)
            downPressed1 = false;
        if (code == KeyEvent.VK_D)
            rightPressed1 = false;
        if (code == KeyEvent.VK_SHIFT)
            dashPressed1 = false;
        if (code == KeyEvent.VK_E)
            castPressed1 = false;

        // JOUEUR 2
        if (code == KeyEvent.VK_UP)
            upPressed2 = false;
        if (code == KeyEvent.VK_LEFT)
            leftPressed2 = false;
        if (code == KeyEvent.VK_DOWN)
            downPressed2 = false;
        if (code == KeyEvent.VK_RIGHT)
            rightPressed2 = false;
        if (code == KeyEvent.VK_ENTER)
            dashPressed2 = false;
        if (code == KeyEvent.VK_M)
            castPressed2 = false;
    }
}