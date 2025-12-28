package Mino;

import Main.PlayManager;
import org.jbox2d.common.Vec2;
import java.awt.Color;

public class Mino_Bar extends Mino {
    public Mino_Bar() {
        create(Color.cyan);
    }

    @Override
    public void setShape() {
        // Forme originale :
        // 1 0 2 3

        float s = (float)Block.SIZE / PlayManager.SCALE;

        blockOffsets[0] = new Vec2(0, 0);       // Centre
        blockOffsets[1] = new Vec2(-s, 0);      // Gauche
        blockOffsets[2] = new Vec2(s, 0);       // Droite
        blockOffsets[3] = new Vec2(s * 2, 0);   // Extrême Droite
    }
}