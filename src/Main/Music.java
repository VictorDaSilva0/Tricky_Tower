package Main;

import javax.sound.sampled.*;
import java.io.File;

public class Music {
    Clip clip;

    public void setFile(String soundFileName) {
        try {
            // Close previous clip if it exists to avoid overlaps
            if (clip != null) {
                if (clip.isRunning())
                    clip.stop();
                if (clip.isOpen())
                    clip.close();
            }

            File file = new File(soundFileName);
            AudioInputStream sound = AudioSystem.getAudioInputStream(file);
            clip = AudioSystem.getClip();
            clip.open(sound);
        } catch (Exception e) {
            System.out.println("Erreur lors de la lecture du fichier son : " + e);
        }
    }

    public void play() {
        if (clip != null) {
            clip.setFramePosition(0);
            clip.start();
        }
    }

    public void loop() {
        if (clip != null) {
            clip.loop(Clip.LOOP_CONTINUOUSLY);
        }
    }

    public void stop() {
        if (clip != null) {
            clip.stop();
            clip.close(); // Ensure resources are released
        }
    }
}
