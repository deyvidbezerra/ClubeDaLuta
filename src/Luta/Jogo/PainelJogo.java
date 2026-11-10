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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Onde tudo acontece: lê o teclado, atualiza o jogo ~60 vezes por segundo e desenha.
 *
 * Controles:
 *   Jogador 1: A / D mover, W pular, S defender, J K L I = habilidades 1 a 4 (4 = especial)
 *   Jogador 2: setas mover/pular, seta para baixo defender, 7 8 9 0 (ou NUMPAD 1-4) = habilidades 1 a 4
 *   Melhor de 3 rounds (vence quem ganhar 2), cada round com 40 segundos
 *   R reinicia a partida | M (ou ESC) volta ao menu | F3 liga/desliga o modo debug (hitboxes, zoom, FPS)
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
    // ----- regras da partida: melhor de 3 rounds, 40 segundos cada -----
    private static final int ROUNDS_PARA_VENCER = 2;
    private static final int TEMPO_ROUND = 60;
    private static final double DURACAO_INTRO = 1.8;       // "ROUND n" / "LUTE!"
    private static final double DURACAO_FIM_ROUND = 3.0;   // pausa depois do K.O. / tempo esgotado

    private enum Fase { INTRO, LUTANDO, FIM_DE_ROUND, FIM_DE_PARTIDA }

    private Fase fase = Fase.INTRO;
    private double tempoFase;
    private int rodada = 1;
    private int vitoriasP1;
    private int vitoriasP2;
    private int vencedorDoRound;       // 0 = empate, 1 = P1, 2 = P2
    private boolean roundPorTempo;     // true = acabou no tempo, false = K.O.

    private final Cronometro cronometro = Cronometro.contagem(TEMPO_ROUND);
    private final BarraDeVida barraP1 = new BarraDeVida(true);
    private final BarraDeVida barraP2 = new BarraDeVida(false);

    private final Lutador p1;
    private final Lutador p2;

    // "fábricas": cada uma cria um Personagem novinho (vida cheia) a cada novo round
    private final Supplier<Personagem> fabricaP1;
    private final Supplier<Personagem> fabricaP2;
    private final Runnable aoVoltarAoMenu;

    // teclado: "pressionadas" = está segurando agora; "acionadas" = acabou de apertar (1 vez só)
    private final Set<Integer> pressionadas = new HashSet<>();
    private final Set<Integer> acionadas = new HashSet<>();

    // toque duplo (esquiva): guarda em que momento cada tecla de direção foi apertada pela última vez
    private static final double JANELA_TOQUE_DUPLO = 0.25;   // segundos entre os dois toques
    private final Map<Integer, Double> ultimoToque = new HashMap<>();
    private double relogio;

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

        // fora da fase LUTANDO os controles ficam travados (os lutadores só terminam a animação)
        boolean podeLutar = fase == Fase.LUTANDO;

        relogio += dt;
        if (podeLutar) {
            p1.esquivar(toqueDuplo(KeyEvent.VK_A, KeyEvent.VK_D));
            p2.esquivar(toqueDuplo(KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT));
        }

        int dir1 = podeLutar ? (segurando(KeyEvent.VK_D) ? 1 : 0) - (segurando(KeyEvent.VK_A) ? 1 : 0) : 0;
        int dir2 = podeLutar ? (segurando(KeyEvent.VK_RIGHT) ? 1 : 0) - (segurando(KeyEvent.VK_LEFT) ? 1 : 0) : 0;

        int golpe1 = podeLutar ? golpePedido(TECLAS_GOLPE_P1) : -1;
        int golpe2 = podeLutar ? golpePedido(TECLAS_GOLPE_P2) : -1;

        p1.atualizar(dt, dir1, podeLutar && segurando(KeyEvent.VK_W), golpe1,
                podeLutar && segurando(KeyEvent.VK_S), p2);
        p2.atualizar(dt, dir2, podeLutar && segurando(KeyEvent.VK_UP), golpe2,
                podeLutar && segurando(KeyEvent.VK_DOWN), p1);

        // câmera: enquadra a caixa que contém os dois lutadores (+ margens)
        double minX = Math.min(p1.esquerda(), p2.esquerda()) - MARGEM_LATERAL;
        double maxX = Math.max(p1.direita(), p2.direita()) + MARGEM_LATERAL;
        double minY = Math.min(p1.topo(), p2.topo()) - MARGEM_TOPO;
        double maxY = Cenario.CHAO_Y + MARGEM_BAIXO;
        camera.seguir(dt, minX, minY, maxX, maxY, larguraTela(), alturaTela());

        if (podeLutar) cronometro.atualizar(dt);   // o tempo só corre durante a luta
        atualizarFase(dt);
        barraP1.atualizar(dt, p1.getPersonagem());
        barraP2.atualizar(dt, p2.getPersonagem());

        acionadas.clear();   // as teclas "de uma vez só" valem apenas para este quadro
    }

    /** Controla a sequência: INTRO -> LUTANDO -> FIM_DE_ROUND -> (próximo round) ... -> FIM_DE_PARTIDA. */
    private void atualizarFase(double dt) {
        tempoFase += dt;
        switch (fase) {
            case INTRO:
                if (tempoFase >= DURACAO_INTRO) mudarFase(Fase.LUTANDO);
                break;
            case LUTANDO:
                verificarFimDoRound();
                break;
            case FIM_DE_ROUND:
                if (tempoFase >= DURACAO_FIM_ROUND) iniciarProximoRound();
                break;
            default:
                break;   // FIM_DE_PARTIDA espera o R
        }
    }

    private void mudarFase(Fase nova) {
        fase = nova;
        tempoFase = 0;
    }

    private void verificarFimDoRound() {
        boolean ko1 = !p1.getPersonagem().estaVivo();
        boolean ko2 = !p2.getPersonagem().estaVivo();
        boolean tempoEsgotado = cronometro.acabou();
        if (!ko1 && !ko2 && !tempoEsgotado) return;

        roundPorTempo = !ko1 && !ko2;
        if (ko1 && ko2) {
            vencedorDoRound = 0;
        } else if (ko1) {
            vencedorDoRound = 2;
        } else if (ko2) {
            vencedorDoRound = 1;
        } else {
            // acabou o tempo: ganha quem estiver com mais vida (em porcentagem)
            double vida1 = p1.getPersonagem().getVida() / (double) p1.getPersonagem().getVidaMaxima();
            double vida2 = p2.getPersonagem().getVida() / (double) p2.getPersonagem().getVidaMaxima();
            vencedorDoRound = Math.abs(vida1 - vida2) < 1e-9 ? 0 : (vida1 > vida2 ? 1 : 2);
        }

        // empate dá ponto para os dois (double K.O.)
        if (vencedorDoRound == 1 || vencedorDoRound == 0) vitoriasP1++;
        if (vencedorDoRound == 2 || vencedorDoRound == 0) vitoriasP2++;

        boolean acabouPartida = vitoriasP1 >= ROUNDS_PARA_VENCER || vitoriasP2 >= ROUNDS_PARA_VENCER;
        mudarFase(acabouPartida ? Fase.FIM_DE_PARTIDA : Fase.FIM_DE_ROUND);
    }

    private void iniciarProximoRound() {
        rodada++;
        prepararRound();
    }

    private void reiniciarPartida() {
        rodada = 1;
        vitoriasP1 = 0;
        vitoriasP2 = 0;
        prepararRound();
    }

    /** Coloca os lutadores no lugar, enche a vida, zera o relógio e mostra "ROUND n". */
    private void prepararRound() {
        p1.resetar(fabricaP1.get(), POSICAO_P1, true);
        p2.resetar(fabricaP2.get(), POSICAO_P2, false);
        cronometro.reiniciar();
        mudarFase(Fase.INTRO);
    }

    /**
     * Detecta o toque duplo nas teclas de esquerda/direita.
     * @return -1 (duplo na esquerda), +1 (duplo na direita) ou 0 (nada)
     */
    private int toqueDuplo(int teclaEsquerda, int teclaDireita) {
        int[] teclas = {teclaEsquerda, teclaDireita};
        int[] direcoes = {-1, 1};
        for (int i = 0; i < teclas.length; i++) {
            if (!acionou(teclas[i])) continue;
            Double anterior = ultimoToque.get(teclas[i]);
            if (anterior != null && relogio - anterior <= JANELA_TOQUE_DUPLO) {
                ultimoToque.remove(teclas[i]);   // consumiu os dois toques
                return direcoes[i];
            }
            ultimoToque.put(teclas[i], relogio);
        }
        return 0;
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
        if (acionou(KeyEvent.VK_R)) reiniciarPartida();
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

        desenharTimer(g, w / 2 - larguraTimer / 2, topo - 12, larguraTimer, alturaBarra + 28);
        desenharPlacar(g, w / 2, topo + alturaBarra + 34);
        desenharMensagemDeFase(g, w, h);

        // legenda dos controles
        String[] linhas = {
                "P1: A/D mover, W pular, S defender, J K L I golpes (I = especial)   |   toque duplo \u2190/\u2192 (A/D) = esquiva",
                "P2: setas (\u2193 defende), 7 8 9 0 golpes (0 = especial)   |   R reiniciar partida   |   M menu   |   F3 debug"
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

    /** Bolinhas de vitória de cada jogador (1P à esquerda do centro, 2P à direita). */
    private void desenharPlacar(Graphics2D g, int centroX, int y) {
        int raio = 7;
        for (int i = 0; i < ROUNDS_PARA_VENCER; i++) {
            desenharBolinha(g, centroX - 22 - i * 22, y, raio, i < vitoriasP1);
            desenharBolinha(g, centroX + 22 + i * 22, y, raio, i < vitoriasP2);
        }
    }

    private void desenharBolinha(Graphics2D g, int cx, int cy, int raio, boolean cheia) {
        g.setColor(new Color(10, 10, 16, 200));
        g.fillOval(cx - raio - 2, cy - raio - 2, 2 * raio + 4, 2 * raio + 4);
        g.setColor(cheia ? new Color(255, 214, 90) : new Color(70, 74, 100));
        g.fillOval(cx - raio, cy - raio, 2 * raio, 2 * raio);
    }

    private void desenharMensagemDeFase(Graphics2D g, int w, int h) {
        int ty = h / 2 - 10;
        switch (fase) {
            case INTRO:
                if (tempoFase < DURACAO_INTRO * 0.6) {
                    desenharTextoCentral(g, w, ty, "ROUND " + rodada, 80, new Color(255, 244, 210));
                } else {
                    desenharTextoCentral(g, w, ty, "LUTE!", 96, new Color(255, 214, 90));
                }
                break;
            case FIM_DE_ROUND:
            case FIM_DE_PARTIDA:
                String titulo = vencedorDoRound == 0 ? "DOUBLE K.O.!" : (roundPorTempo ? "TEMPO!" : "K.O.!");
                desenharTextoCentral(g, w, ty, titulo, 96, new Color(255, 80, 70));

                String vencedor;
                if (fase == Fase.FIM_DE_PARTIDA) {
                    if (vitoriasP1 == vitoriasP2) vencedor = "PARTIDA EMPATADA";
                    else vencedor = (vitoriasP1 > vitoriasP2 ? "1P" : "2P") + " VENCEU A PARTIDA";
                } else {
                    vencedor = vencedorDoRound == 0 ? "EMPATE"
                            : (vencedorDoRound == 1 ? "1P" : "2P") + " VENCEU O ROUND";
                }
                desenharTextoCentral(g, w, ty + 50, vencedor, 30, new Color(255, 244, 210));

                if (fase == Fase.FIM_DE_PARTIDA) {
                    desenharTextoCentral(g, w, ty + 82, "Pressione R para jogar de novo", 16, new Color(230, 235, 245));
                }
                break;
            default:
                break;
        }
    }

    /** Texto centralizado com sombra, no tamanho e cor pedidos. */
    private void desenharTextoCentral(Graphics2D g, int w, int baseY, String texto, int tamanho, Color cor) {
        g.setFont(new Font("SansSerif", tamanho >= 30 ? Font.BOLD : Font.PLAIN, tamanho));
        FontMetrics fm = g.getFontMetrics();
        int tx = (w - fm.stringWidth(texto)) / 2;
        int sombra = Math.max(2, tamanho / 20);
        g.setColor(new Color(0, 0, 0, 200));
        g.drawString(texto, tx + sombra, baseY + sombra);
        g.setColor(cor);
        g.drawString(texto, tx, baseY);
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
        boolean urgente = !infinito && cronometro.getRestante() <= 10;
        g.setColor(urgente ? new Color(255, 90, 90) : new Color(255, 244, 210));
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