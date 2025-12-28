package Mino;

import Main.PlayManager;
import org.jbox2d.common.Vec2;
import java.awt.Color;

public class Mino_L1 extends Mino {
    public Mino_L1() {
        create(Color.orange);
    }

    @Override
    public void setShape() {
        // Forme originale :
        //   1
        //   0
        //   2 3

        float s = (float)Block.SIZE / PlayManager.SCALE;

        blockOffsets[0] = new Vec2(0, 0);       // Centre
        blockOffsets[1] = new Vec2(0, -s);      // Haut
        blockOffsets[2] = new Vec2(0, s);       // Bas
        blockOffsets[3] = new Vec2(s, s);       // Bas-Droite
    }
}