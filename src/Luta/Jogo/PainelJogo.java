package Luta.Jogo;

import Luta.Personagem.Personagem;

import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Onde tudo acontece: lê o teclado, atualiza o jogo ~60 vezes por segundo e desenha.
 *
 * Controles:
 *   Jogador 1: A / D mover, W pular, S defender, J K L I = habilidades 1 a 4 (4 = especial)
 *   Jogador 2: setas mover/pular, seta para baixo defender, 7 8 9 0 (ou NUMPAD 1-4) = habilidades 1 a 4
 *   R reinicia | M (ou ESC) volta ao menu de personagens | F3 liga/desliga o modo debug (hitboxes, zoom, FPS)
 */
public class PainelJogo extends JPanel {

    private static final int TELA_L = 1000;
    private static final int TELA_A = 600;

    // margens da área que a câmera precisa enquadrar (em unidades do mundo)
    private static final double MARGEM_LATERAL = 260;
    private static final double MARGEM_TOPO = 150;
    private static final double MARGEM_BAIXO = 90;

    // teclas de golpe: cada linha é uma habilidade (0..3), com uma ou mais teclas
    private static final int[][] TECLAS_GOLPE_P1 = {
            {KeyEvent.VK_J}, {KeyEvent.VK_K}, {KeyEvent.VK_L}, {KeyEvent.VK_I}};
    private static final int[][] TECLAS_GOLPE_P2 = {
            {KeyEvent.VK_7, KeyEvent.VK_NUMPAD1}, {KeyEvent.VK_8, KeyEvent.VK_NUMPAD2},
            {KeyEvent.VK_9, KeyEvent.VK_NUMPAD3}, {KeyEvent.VK_0, KeyEvent.VK_NUMPAD4}};

    private static final double POSICAO_P1 = Cenario.LARGURA / 2 - 250;
    private static final double POSICAO_P2 = Cenario.LARGURA / 2 + 250;

    private final Cenario cenario = new Cenario();
    private final Camera camera = new Camera();
    private final Cronometro cronometro = Cronometro.infinito();
    private final BarraDeVida barraP1 = new BarraDeVida(true);
    private final BarraDeVida barraP2 = new BarraDeVida(false);

    private final Lutador p1;
    private final Lutador p2;

    // "fábricas": cada uma cria um Personagem novinho (vida cheia) a cada reinício da luta
    private final Supplier<Personagem> fabricaP1;
    private final Supplier<Personagem> fabricaP2;
    private final Runnable aoVoltarAoMenu;

    // teclado: "pressionadas" = está segurando agora; "acionadas" = acabou de apertar (1 vez só)
    private final Set<Integer> pressionadas = new HashSet<>();
    private final Set<Integer> acionadas = new HashSet<>();

    private final Timer timer;
    private long ultimoQuadro;
    private double fps = 60;
    private boolean debug;

    public PainelJogo(Supplier<Personagem> fabricaP1, Supplier<Personagem> fabricaP2, Runnable aoVoltarAoMenu) {
        this.fabricaP1 = fabricaP1;
        this.fabricaP2 = fabricaP2;
        this.aoVoltarAoMenu = aoVoltarAoMenu;

        setPreferredSize(new Dimension(TELA_L, TELA_A));
        setBackground(Color.BLACK);
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);

        // os sprites de cada personagem são carregados pelo próprio Lutador (ConjuntoSprites)
        p1 = new Lutador(fabricaP1.get(), POSICAO_P1, true);
        p2 = new Lutador(fabricaP2.get(), POSICAO_P2, false);

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (pressionadas.add(e.getKeyCode())) {   // add retorna true só na 1ª vez (ignora repetição)
                    acionadas.add(e.getKeyCode());
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                pressionadas.remove(e.getKeyCode());
            }
        });
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                pressionadas.clear();   // evita personagem "andando sozinho" ao trocar de janela
            }
        });

        timer = new Timer(1000 / 60, e -> {
            long agora = System.nanoTime();
            double dt = Math.min((agora - ultimoQuadro) / 1e9, 0.05);   // limita saltos grandes
            ultimoQuadro = agora;
            if (dt > 0) fps += (1 / dt - fps) * 0.05;
            atualizar(dt);
            repaint();
        });
    }

    public void iniciar() {
        requestFocusInWindow();
        ultimoQuadro = System.nanoTime();
        timer.start();
    }

    /** Para o jogo (usado ao voltar para o menu). */
    public void parar() {
        timer.stop();
    }

    // =================================================================== LÓGICA

    void atualizar(double dt) {
        if (tratarTeclasDeUmaVez()) return;   // true = saiu para o menu

        int dir1 = (segurando(KeyEvent.VK_D) ? 1 : 0) - (segurando(KeyEvent.VK_A) ? 1 : 0);
        int dir2 = (segurando(KeyEvent.VK_RIGHT) ? 1 : 0) - (segurando(KeyEvent.VK_LEFT) ? 1 : 0);

        int golpe1 = golpePedido(TECLAS_GOLPE_P1);
        int golpe2 = golpePedido(TECLAS_GOLPE_P2);

        p1.atualizar(dt, dir1, segurando(KeyEvent.VK_W), golpe1, segurando(KeyEvent.VK_S), p2);
        p2.atualizar(dt, dir2, segurando(KeyEvent.VK_UP), golpe2, segurando(KeyEvent.VK_DOWN), p1);

        // câmera: enquadra a caixa que contém os dois lutadores (+ margens)
        double minX = Math.min(p1.esquerda(), p2.esquerda()) - MARGEM_LATERAL;
        double maxX = Math.max(p1.direita(), p2.direita()) + MARGEM_LATERAL;
        double minY = Math.min(p1.topo(), p2.topo()) - MARGEM_TOPO;
        double maxY = Cenario.CHAO_Y + MARGEM_BAIXO;
        camera.seguir(dt, minX, minY, maxX, maxY, larguraTela(), alturaTela());

        cronometro.atualizar(dt);
        barraP1.atualizar(dt, p1.getPersonagem());
        barraP2.atualizar(dt, p2.getPersonagem());

        acionadas.clear();   // as teclas "de uma vez só" valem apenas para este quadro
    }

    /** Qual habilidade (0..3) acabou de ser apertada? -1 se nenhuma. */
    private int golpePedido(int[][] teclas) {
        for (int i = 0; i < teclas.length; i++) {
            for (int tecla : teclas[i]) {
                if (acionou(tecla)) return i;
            }
        }
        return -1;
    }

    /** @return true se o jogo foi encerrado (voltou ao menu) e o quadro não deve continuar. */
    private boolean tratarTeclasDeUmaVez() {
        if (acionou(KeyEvent.VK_R)) {
            p1.resetar(fabricaP1.get(), POSICAO_P1, true);
            p2.resetar(fabricaP2.get(), POSICAO_P2, false);
        }
        if (acionou(KeyEvent.VK_M) || acionou(KeyEvent.VK_ESCAPE)) {
            parar();
            if (aoVoltarAoMenu != null) aoVoltarAoMenu.run();
            return true;
        }
        if (acionou(KeyEvent.VK_F3)) debug = !debug;
        return false;
    }

    private boolean segurando(int tecla) { return pressionadas.contains(tecla); }
    private boolean acionou(int tecla) { return acionadas.contains(tecla); }

    private int larguraTela() { return getWidth() > 0 ? getWidth() : TELA_L; }
    private int alturaTela() { return getHeight() > 0 ? getHeight() : TELA_A; }

    // =================================================================== DESENHO

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int w = getWidth();
        int h = getHeight();

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        // pixel art: sem suavizar ao ampliar, senão o sprite fica borrado
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        // ----- mundo (passa pela câmera) -----
        AffineTransform telaOriginal = g2.getTransform();
        camera.aplicar(g2, w, h);
        cenario.desenhar(g2);
        p1.desenhar(g2, debug);
        p2.desenhar(g2, debug);
        g2.setTransform(telaOriginal);

        // ----- HUD (fica fixo na tela, não é afetado pela câmera) -----
        desenharHud(g2, w, h);
        if (debug) desenharDebug(g2, h);

        g2.dispose();
    }

    private void desenharHud(Graphics2D g, int w, int h) {
        int margem = 22;
        int topo = 34;
        int alturaBarra = 30;
        int larguraTimer = 96;
        int espaco = 14;
        int larguraBarra = (w - 2 * margem - larguraTimer - 2 * espaco) / 2;

        barraP1.desenhar(g, p1.getPersonagem(), margem, topo, larguraBarra, alturaBarra);
        barraP2.desenhar(g, p2.getPersonagem(), w - margem - larguraBarra, topo, larguraBarra, alturaBarra);

        desenharKo(g, w, h);
        desenharTimer(g, w / 2 - larguraTimer / 2, topo - 12, larguraTimer, alturaBarra + 28);

        // legenda dos controles
        String[] linhas = {
                "P1: A/D mover, W pular, S defender, J K L I golpes (I = especial)",
                "P2: setas (\u2193 defende), 7 8 9 0 golpes (0 = especial)   |   R reiniciar   |   M menu   |   F3 debug"
        };
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        FontMetrics fm = g.getFontMetrics();
        int caixaL = 0;
        for (String l : linhas) caixaL = Math.max(caixaL, fm.stringWidth(l));
        int caixaA = linhas.length * 16 + 10;
        g.setColor(new Color(0, 0, 0, 120));
        g.fillRoundRect(10, h - caixaA - 10, caixaL + 20, caixaA, 10, 10);
        g.setColor(new Color(235, 240, 250));
        for (int i = 0; i < linhas.length; i++) {
            g.drawString(linhas[i], 20, h - caixaA - 10 + 20 + i * 16);
        }
    }

    private void desenharKo(Graphics2D g, int w, int h) {
        boolean ko1 = !p1.getPersonagem().estaVivo();
        boolean ko2 = !p2.getPersonagem().estaVivo();
        if (!ko1 && !ko2) return;

        String titulo = "K.O.!";
        String vencedor = (ko1 && ko2) ? "EMPATE" : (ko2 ? "1P VENCEU" : "2P VENCEU");

        g.setFont(new Font("SansSerif", Font.BOLD, 96));
        FontMetrics fm = g.getFontMetrics();
        int tx = (w - fm.stringWidth(titulo)) / 2;
        int ty = h / 2 - 10;
        g.setColor(new Color(0, 0, 0, 200));
        g.drawString(titulo, tx + 5, ty + 5);
        g.setColor(new Color(255, 80, 70));
        g.drawString(titulo, tx, ty);

        g.setFont(new Font("SansSerif", Font.BOLD, 30));
        fm = g.getFontMetrics();
        g.setColor(new Color(0, 0, 0, 200));
        g.drawString(vencedor, (w - fm.stringWidth(vencedor)) / 2 + 2, ty + 52);
        g.setColor(new Color(255, 244, 210));
        g.drawString(vencedor, (w - fm.stringWidth(vencedor)) / 2, ty + 50);

        g.setFont(new Font("SansSerif", Font.PLAIN, 16));
        fm = g.getFontMetrics();
        String dica = "Pressione R para reiniciar";
        g.setColor(new Color(230, 235, 245));
        g.drawString(dica, (w - fm.stringWidth(dica)) / 2, ty + 82);
    }

    private void desenharTimer(Graphics2D g, int x, int y, int w, int h) {
        double c = 14;   // cantos cortados (octógono achatado)
        Path2D forma = new Path2D.Double();
        forma.moveTo(x + c, y);
        forma.lineTo(x + w - c, y);
        forma.lineTo(x + w, y + c);
        forma.lineTo(x + w, y + h - c);
        forma.lineTo(x + w - c, y + h);
        forma.lineTo(x + c, y + h);
        forma.lineTo(x, y + h - c);
        forma.lineTo(x, y + c);
        forma.closePath();

        g.setPaint(new GradientPaint(0, y, new Color(40, 44, 72), 0, y + h, new Color(12, 12, 26)));
        g.fill(forma);
        g.setStroke(new BasicStroke(5f));
        g.setColor(new Color(10, 10, 16));
        g.draw(forma);
        g.setStroke(new BasicStroke(2.5f));
        g.setColor(new Color(255, 214, 90));
        g.draw(forma);

        // texto do timer
        boolean infinito = cronometro.getTexto().equals("\u221E");
        g.setFont(new Font("SansSerif", Font.BOLD, infinito ? 44 : 38));
        FontMetrics fm = g.getFontMetrics();
        String texto = cronometro.getTexto();
        int tx = x + (w - fm.stringWidth(texto)) / 2;
        int ty = y + (h + fm.getAscent() - fm.getDescent()) / 2 - (infinito ? 1 : 0);
        g.setColor(new Color(0, 0, 0, 180));
        g.drawString(texto, tx + 2, ty + 2);
        g.setColor(cronometro.acabou() ? new Color(255, 90, 90) : new Color(255, 244, 210));
        g.drawString(texto, tx, ty);
    }

    private void desenharDebug(Graphics2D g, int h) {
        double dist = Math.abs(p1.getX() - p2.getX());
        String info = String.format("FPS %.0f  |  zoom %.2f  |  distancia %.0f  |  P1 (%.0f, %.0f) %s  |  P2 (%.0f, %.0f) %s",
                fps, camera.getZoom(), dist,
                p1.getX(), p1.getY(), p1.estaNoChao() ? "chao" : "ar",
                p2.getX(), p2.getY(), p2.estaNoChao() ? "chao" : "ar");
        g.setFont(new Font("Monospaced", Font.BOLD, 12));
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(0, 110, g.getFontMetrics().stringWidth(info) + 16, 20);
        g.setColor(new Color(120, 255, 140));
        g.drawString(info, 8, 125);
    }
}
