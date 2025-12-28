package Main;

import javax.swing.*;

public class Main {
    public static void main(String[] args) { // Note: public static void main standard
        JFrame window = new JFrame("Tricky Towers Clone");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // 1. PLEIN ÉCRAN SANS BORDURES
        window.setUndecorated(true);

        // Ajout du GamePanel
        GamePanel gp = new GamePanel();
        window.add(gp);

        // 2. MAXIMISER LA FENÊTRE
        window.setExtendedState(JFrame.MAXIMIZED_BOTH);

        // Pas besoin de pack() ou setResizable ici car on force le maximised
        window.setVisible(true);

        // Lancement de la musique et du jeu
        Music music = new Music();
        music.setFile("MINO.wav");
        music.play();
        music.loop(); // Boucler la musique

        gp.launchGame();
    }
}