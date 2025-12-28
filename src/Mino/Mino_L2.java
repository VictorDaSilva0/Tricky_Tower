package Mino;

import Main.PlayManager;
import org.jbox2d.common.Vec2;
import java.awt.Color;

public class Mino_L2 extends Mino {
    public Mino_L2() {
        create(Color.BLUE);
    }

    @Override
    public void setShape() {
        // Forme originale :
        //   1
        //   0
        // 3 2

        float s = (float)Block.SIZE / PlayManager.SCALE;

        blockOffsets[0] = new Vec2(0, 0);       // Centre
        blockOffsets[1] = new Vec2(0, -s);      // Haut
        blockOffsets[2] = new Vec2(0, s);       // Bas
        blockOffsets[3] = new Vec2(-s, s);      // Bas-Gauche
    }
}