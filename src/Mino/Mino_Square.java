package Mino;

import Main.PlayManager;
import org.jbox2d.common.Vec2;
import java.awt.Color;

public class Mino_Square extends Mino {
    public Mino_Square() {
        create(Color.yellow);
    }

    @Override
    public void setShape() {
        // Forme originale :
        // 0 2
        // 1 3
        // Note: Pour une physique réaliste, on décale tout pour que le centre de rotation
        // soit au milieu du carré (et non sur le coin b[0]).
        // Mais gardons votre logique b[0] = (0,0) pour l'instant.

        float s = (float)Block.SIZE / PlayManager.SCALE;

        blockOffsets[0] = new Vec2(0, 0);       // Haut-Gauche (Pivot)
        blockOffsets[1] = new Vec2(0, s);       // Bas-Gauche
        blockOffsets[2] = new Vec2(s, 0);       // Haut-Droite
        blockOffsets[3] = new Vec2(s, s);       // Bas-Droite
    }
}