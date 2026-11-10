package Luta.Jogo;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.util.Random;

/**
 * O palco. Tudo aqui está em "unidades do mundo" (não em pixels da tela):
 * quem converte para a tela é a Camera.
 */
public class Cenario {

    public static final double LARGURA = 1800;   // largura total do palco
    public static final double ALTURA = 1080;    // altura total (proporção 5:3, igual à janela)
    public static final double CHAO_Y = 900;     // y da linha do chão
    public static final double MARGEM_PAREDE = 80; // distância entre a borda do palco e o limite de movimento

    private static final int[][] PREDIOS = gerarPredios();

    /** Prédios de fundo gerados uma vez só (sempre iguais). */
    private static int[][] gerarPredios() {
        Random r = new Random(42);
        int[][] lista = new int[40][3];
        int x = -20;
        int n = 0;
        while (x < LARGURA && n < lista.length) {
            int largura = 80 + r.nextInt(90);
            int altura = 140 + r.nextInt(260);
            lista[n++] = new int[]{x, largura, altura};
            x += largura + 6;
        }
        return lista;
    }

    public void desenhar(Graphics2D g) {
        // céu
        g.setPaint(new GradientPaint(0, 0, new Color(16, 22, 54), 0, (float) CHAO_Y, new Color(74, 112, 172)));
        g.fillRect(0, 0, (int) LARGURA, (int) CHAO_Y);

        // lua
        g.setColor(new Color(235, 240, 255, 230));
        g.fill(new Ellipse2D.Double(1350, 170, 130, 130));
        g.setColor(new Color(16, 22, 54, 70));
        g.fill(new Ellipse2D.Double(1390, 200, 28, 28));
        g.fill(new Ellipse2D.Double(1425, 240, 18, 18));

        // prédios de fundo
        for (int[] p : PREDIOS) {
            if (p == null || p[1] == 0) continue;
            g.setColor(new Color(24, 32, 70));
            g.fillRect(p[0], (int) CHAO_Y - p[2], p[1], p[2]);
            g.setColor(new Color(250, 230, 150, 120));
            for (int jy = (int) CHAO_Y - p[2] + 20; jy < CHAO_Y - 30; jy += 34) {
                for (int jx = p[0] + 12; jx < p[0] + p[1] - 16; jx += 24) {
                    if (((jx * 31 + jy * 17) % 5) < 2) g.fillRect(jx, jy, 10, 14);
                }
            }
        }

        // chão
        g.setPaint(new GradientPaint(0, (float) CHAO_Y, new Color(52, 46, 78), 0, (float) ALTURA, new Color(24, 20, 40)));
        g.fillRect(0, (int) CHAO_Y, (int) LARGURA, (int) (ALTURA - CHAO_Y));
        g.setColor(new Color(255, 255, 255, 110));
        g.fillRect(0, (int) CHAO_Y, (int) LARGURA, 4);

        // marcações no chão (ajudam a perceber o movimento e o zoom)
        g.setStroke(new BasicStroke(2));
        for (int x = 0; x <= LARGURA; x += 100) {
            boolean grande = x % 500 == 0;
            g.setColor(new Color(255, 255, 255, grande ? 70 : 30));
            g.drawLine(x, (int) CHAO_Y, x, (int) ALTURA);
        }
        g.setColor(new Color(255, 255, 255, 25));
        for (int y = (int) CHAO_Y + 60; y < ALTURA; y += 60) {
            g.drawLine(0, y, (int) LARGURA, y);
        }

        // limites do palco (onde os lutadores encostam)
        g.setColor(new Color(255, 90, 90, 90));
        g.fillRect((int) MARGEM_PAREDE - 3, (int) CHAO_Y - 40, 6, 40 + (int) (ALTURA - CHAO_Y));
        g.fillRect((int) (LARGURA - MARGEM_PAREDE) - 3, (int) CHAO_Y - 40, 6, 40 + (int) (ALTURA - CHAO_Y));
    }
}
