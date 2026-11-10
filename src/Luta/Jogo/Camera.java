package Luta.Jogo;

import java.awt.Graphics2D;

/**
 * Câmera dinâmica: enquadra uma área do palco (a caixa que contém os dois lutadores)
 * e se move/dá zoom suavemente até ela.
 *
 * Para desenhar o mundo: {@link #aplicar} troca o "sistema de coordenadas" do Graphics2D
 * de "tela" para "mundo". Depois disso é só desenhar normalmente nas coordenadas do palco.
 */
public class Camera {

    private static final double ZOOM_MAX = 1.5;      // o quanto pode se aproximar
    private static final double SUAVIDADE = 6.0;     // maior = acompanha mais rápido

    private double x;            // centro da câmera, em coordenadas do mundo
    private double y;
    private double zoom = 1;
    private boolean iniciada;

    /**
     * @param minX..maxY área do mundo que precisa aparecer (já com margens)
     * @param telaL      largura da tela em pixels
     * @param telaA      altura da tela em pixels
     */
    public void seguir(double dt, double minX, double minY, double maxX, double maxY, int telaL, int telaA) {
        // zoom mínimo: o necessário para a tela nunca mostrar nada além do palco
        double zoomMin = Math.max(telaL / Cenario.LARGURA, telaA / Cenario.ALTURA);

        // zoom que faz a área desejada caber inteira na tela (nos dois eixos)
        double zoomAlvo = Math.min(telaL / (maxX - minX), telaA / (maxY - minY));
        zoomAlvo = limitar(zoomAlvo, zoomMin, ZOOM_MAX);

        double alvoX = (minX + maxX) / 2;
        double alvoY = (minY + maxY) / 2;

        if (!iniciada) {                       // primeiro quadro: já começa enquadrado
            x = alvoX;
            y = alvoY;
            zoom = zoomAlvo;
            iniciada = true;
        } else {
            // suavização independente de FPS: percorre uma fração do caminho a cada quadro
            double a = 1 - Math.exp(-SUAVIDADE * dt);
            x += (alvoX - x) * a;
            y += (alvoY - y) * a;
            zoom += (zoomAlvo - zoom) * a;
        }

        zoom = limitar(zoom, zoomMin, ZOOM_MAX);

        // mantém a câmera dentro do palco
        double metadeL = telaL / (2 * zoom);
        double metadeA = telaA / (2 * zoom);
        x = limitar(x, metadeL, Cenario.LARGURA - metadeL);
        y = limitar(y, metadeA, Cenario.ALTURA - metadeA);
    }

    /** Passa a desenhar em coordenadas do mundo. Guarde o transform original para voltar depois. */
    public void aplicar(Graphics2D g, int telaL, int telaA) {
        g.translate(telaL / 2.0, telaA / 2.0);   // origem no meio da tela
        g.scale(zoom, zoom);                     // zoom
        g.translate(-x, -y);                     // centraliza na câmera
    }

    private static double limitar(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    public double getZoom() { return zoom; }
}
