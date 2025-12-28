package Mino;

import Main.PlayManager;
import org.jbox2d.common.Vec2;
import java.awt.Color;

public class Mino_T extends Mino {

    public Mino_T() {
        create(Color.magenta);
    }

    @Override
    public void setShape() {
        // Définir la forme du T
        //     [1]
        // [2] [0] [3]

        float s = (Block.SIZE) / PlayManager.SCALE; // Taille en mètres

        // Le bloc 0 est au centre (0,0)
        blockOffsets[0] = new Vec2(0, 0);
        // Le bloc 1 est au dessus
        blockOffsets[1] = new Vec2(0, -s);
        // Le bloc 2 est à gauche
        blockOffsets[2] = new Vec2(-s, 0);
        // Le bloc 3 est à droite
        blockOffsets[3] = new Vec2(s, 0);
    }

    // Supprimez les anciennes méthodes setXY, getDirection... elles ne servent plus !
}