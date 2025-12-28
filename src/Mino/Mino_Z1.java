package Mino;

import Main.PlayManager;
import org.jbox2d.common.Vec2;
import java.awt.Color;

public class Mino_Z1 extends Mino {
    public Mino_Z1() {
        create(Color.red);
    }

    @Override
    public void setShape() {
        // Forme originale dans votre code :
        //   1
        // 2 0
        // 3

        float s = (float)Block.SIZE / PlayManager.SCALE;

        blockOffsets[0] = new Vec2(0, 0);       // Centre
        blockOffsets[1] = new Vec2(0, -s);      // Haut
        blockOffsets[2] = new Vec2(-s, 0);      // Gauche
        blockOffsets[3] = new Vec2(-s, s);      // Bas-Gauche
    }
}