package Luta.Jogo;

import Luta.Personagem.Personagem;
import Luta.Personagens.Aqua;
import Luta.Personagens.Keno;

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
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

/**
 * Tela de escolha de personagem, antes da luta.
 *
 * Controles:
 *   Jogador 1: A / D escolhe, J confirma, K cancela
 *   Jogador 2: setas esquerda/direita escolhe, ENTER (ou 7) confirma, BACKSPACE (ou 8) cancela
 *   Mouse: clique em um personagem (o 1º clique vale para o jogador 1, o 2º para o jogador 2)
 *
 * O último cartão (?) sorteia um personagem na hora de começar a luta.
 *
 * Para incluir um personagem novo: crie o ConjuntoSprites dele, ponha *_icon.png e *_portrait.png
 * em assets/<nome>/ e acrescente uma linha em montarOpcoes().
 */
public class MenuSelecao extends JPanel {

    /** Avisa o programa quando os dois jogadores escolheram. */
    public interface Ouvinte {
        void lutar(Supplier<Personagem> jogador1, Supplier<Personagem> jogador2);
    }

    private static final int TELA_L = 1000;
    private static final int TELA_A = 600;

    private static final int CARTAO = 104;        // tamanho de cada cartão
    private static final int ESPACO = 16;
    private static final int CARTOES_Y = 232;

    private static final Color[] COR_JOGADOR = {new Color(90, 170, 255), new Color(255, 110, 100)};
    private static final String[] ROTULO = {"1P", "2P"};

    private static class Opcao {
        final String nome;
        final Supplier<Personagem> fabrica;   // null = aleatório
        final BufferedImage icone;
        final BufferedImage retrato;

        Opcao(String nome, Supplier<Personagem> fabrica, BufferedImage icone, BufferedImage retrato) {
            this.nome = nome;
            this.fabrica = fabrica;
            this.icone = icone;
            this.retrato = retrato;
        }

        boolean aleatorio() { return fabrica == null; }
    }

    private final List<Opcao> opcoes = new ArrayList<>();
    private final int[] cursor = {0, 1};
    private final boolean[] pronto = new boolean[2];
    private final Ouvinte ouvinte;
    private final Timer timer;
    private final Random sorteio = new Random();

    private long ultimoQuadro;
    private double relogio;
    private double contagem;        // segundos desde que os dois confirmaram
    private boolean iniciando;
    private Supplier<Personagem> sorteado1;
    private Supplier<Personagem> sorteado2;
    private String nomeSorteado1;
    private String nomeSorteado2;

    public MenuSelecao(Ouvinte ouvinte) {
        this.ouvinte = ouvinte;
        montarOpcoes();
        if (opcoes.size() > 2) cursor[1] = 1;   // P2 começa no segundo personagem

        setPreferredSize(new Dimension(TELA_L, TELA_A));
        setBackground(new Color(14, 18, 44));
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                tecla(e.getKeyCode());
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                clicar(e.getX(), e.getY());
            }
        });

        timer = new Timer(1000 / 60, e -> {
            long agora = System.nanoTime();
            double dt = Math.min((agora - ultimoQuadro) / 1e9, 0.05);
            ultimoQuadro = agora;
            atualizar(dt);
            repaint();
        });
    }

    private void montarOpcoes() {
        opcoes.add(new Opcao("Aqua", Aqua::new,
                Animacao.lerImagem("/assets/aqua/aqua_icon.png"), Animacao.lerImagem("/assets/aqua/aqua_portrait.png")));
        opcoes.add(new Opcao("Keno", Keno::new,
                Animacao.lerImagem("/assets/keno/keno_icon.png"), Animacao.lerImagem("/assets/keno/keno_portrait.png")));
        BufferedImage dado = Animacao.lerImagem("/assets/randomizer_icon.png");
        opcoes.add(new Opcao("Aleatório", null, dado, dado));
    }

    public void iniciar() {
        requestFocusInWindow();
        ultimoQuadro = System.nanoTime();
        timer.start();
    }

    public void parar() {
        timer.stop();
    }

    // =================================================================== LÓGICA

    private void tecla(int k) {
        if (iniciando) return;
        switch (k) {
            case KeyEvent.VK_A:      mover(0, -1); break;
            case KeyEvent.VK_D:      mover(0, 1); break;
            case KeyEvent.VK_J:      confirmar(0); break;
            case KeyEvent.VK_K:      pronto[0] = false; break;

            case KeyEvent.VK_LEFT:   mover(1, -1); break;
            case KeyEvent.VK_RIGHT:  mover(1, 1); break;
            case KeyEvent.VK_ENTER:
            case KeyEvent.VK_7:
            case KeyEvent.VK_NUMPAD1: confirmar(1); break;
            case KeyEvent.VK_BACK_SPACE:
            case KeyEvent.VK_8:
            case KeyEvent.VK_NUMPAD2: pronto[1] = false; break;
            default: break;
        }
    }

    private void mover(int jogador, int passo) {
        if (pronto[jogador]) return;   // já confirmou: cancele antes de trocar
        int n = opcoes.size();
        cursor[jogador] = (cursor[jogador] + passo + n) % n;
    }

    private void confirmar(int jogador) {
        pronto[jogador] = true;
    }

    private void clicar(int mx, int my) {
        if (iniciando) return;
        for (int i = 0; i < opcoes.size(); i++) {
            int cx = cartaoX(i);
            if (mx >= cx && mx < cx + CARTAO && my >= CARTOES_Y && my < CARTOES_Y + CARTAO) {
                int jogador = !pronto[0] ? 0 : (!pronto[1] ? 1 : -1);
                if (jogador >= 0) {
                    cursor[jogador] = i;
                    pronto[jogador] = true;
                }
                return;
            }
        }
    }

    private void atualizar(double dt) {
        relogio += dt;
        if (pronto[0] && pronto[1]) {
            if (!iniciando) {
                iniciando = true;
                contagem = 0;
                resolverSorteios();
            }
            contagem += dt;
            if (contagem >= 1.1) {
                timer.stop();
                ouvinte.lutar(sorteado1, sorteado2);
            }
        }
    }

    /** Se alguém escolheu "?", sorteia um personagem de verdade. */
    private void resolverSorteios() {
        Opcao o1 = escolhaFinal(0);
        Opcao o2 = escolhaFinal(1);
        sorteado1 = o1.fabrica;
        sorteado2 = o2.fabrica;
        nomeSorteado1 = o1.nome;
        nomeSorteado2 = o2.nome;
    }

    private Opcao escolhaFinal(int jogador) {
        Opcao o = opcoes.get(cursor[jogador]);
        if (!o.aleatorio()) return o;
        List<Opcao> reais = new ArrayList<>();
        for (Opcao x : opcoes) if (!x.aleatorio()) reais.add(x);
        return reais.get(sorteio.nextInt(reais.size()));
    }

    // =================================================================== DESENHO

    private int cartaoX(int i) {
        int total = opcoes.size() * CARTAO + (opcoes.size() - 1) * ESPACO;
        return (TELA_L - total) / 2 + i * (CARTAO + ESPACO);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        int w = getWidth();
        int h = getHeight();
        g.scale(w / (double) TELA_L, h / (double) TELA_A);   // o menu é desenhado em 1000x600 e se ajusta à janela

        desenharFundo(g);
        desenharTitulo(g);
        desenharPainelJogador(g, 0, 24, 100, 300, 460);
        desenharPainelJogador(g, 1, TELA_L - 24 - 300, 100, 300, 460);
        desenharCartoes(g);
        desenharAjuda(g);
        if (iniciando) desenharLutar(g);

        g.dispose();
    }

    private void desenharFundo(Graphics2D g) {
        g.setPaint(new GradientPaint(0, 0, new Color(16, 22, 56), 0, TELA_A, new Color(44, 66, 128)));
        g.fillRect(0, 0, TELA_L, TELA_A);

        // faixas diagonais suaves
        g.setColor(new Color(255, 255, 255, 10));
        for (int i = -TELA_A; i < TELA_L; i += 70) {
            int off = (int) ((relogio * 18) % 70);
            g.fillPolygon(new int[]{i + off, i + off + 28, i + off + 28 + TELA_A, i + off + TELA_A},
                    new int[]{TELA_A, TELA_A, 0, 0}, 4);
        }
        // chão
        g.setColor(new Color(10, 12, 30, 150));
        g.fillRect(0, 560, TELA_L, 40);
    }

    private void desenharTitulo(Graphics2D g) {
        g.setFont(new Font("SansSerif", Font.BOLD, 40));
        centralizado(g, "ESCOLHA SEU LUTADOR", TELA_L / 2, 62, new Color(255, 214, 90), true);
        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        centralizado(g, "cada jogador escolhe o seu personagem", TELA_L / 2, 88, new Color(190, 205, 235), false);
    }

    private void desenharPainelJogador(Graphics2D g, int jogador, int x, int y, int w, int h) {
        Color cor = COR_JOGADOR[jogador];
        Opcao op = opcoes.get(cursor[jogador]);
        boolean ok = pronto[jogador];

        // painel
        g.setColor(new Color(8, 10, 28, ok ? 170 : 120));
        g.fillRoundRect(x, y, w, h, 22, 22);
        double pulso = 0.5 + 0.5 * Math.sin(relogio * 5);
        int alfa = ok ? 255 : (int) (120 + 90 * pulso);
        g.setColor(new Color(cor.getRed(), cor.getGreen(), cor.getBlue(), alfa));
        g.setStroke(new BasicStroke(ok ? 4f : 2.5f));
        g.drawRoundRect(x, y, w, h, 22, 22);

        // retrato (o do jogador 2 é espelhado, para os dois ficarem de frente um para o outro)
        BufferedImage img = op.retrato;
        int areaL = w - 24;
        int areaA = h - 96;
        double escala = op.aleatorio() ? 1.6 : Math.min(areaL / (double) img.getWidth(), areaA / (double) img.getHeight());
        int dw = (int) (img.getWidth() * escala);
        int dh = (int) (img.getHeight() * escala);
        int dx = x + (w - dw) / 2;
        int dy = y + 14 + (areaA - dh) / 2 + (op.aleatorio() ? 0 : 0);
        java.awt.Composite comp = g.getComposite();
        if (!ok) g.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 0.82f));
        if (jogador == 1 && !op.aleatorio()) {
            g.drawImage(img, dx + dw, dy, -dw, dh, null);
        } else {
            g.drawImage(img, dx, dy, dw, dh, null);
        }
        g.setComposite(comp);

        // selo do jogador, nome e estado
        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        FontMetrics fm = g.getFontMetrics();
        int seloL = fm.stringWidth(ROTULO[jogador]) + 18;
        g.setColor(cor);
        g.fillRoundRect(x + 16, y + 14, seloL, 24, 8, 8);
        g.setColor(new Color(10, 10, 20));
        g.drawString(ROTULO[jogador], x + 25, y + 31);

        String nome = op.nome.toUpperCase();
        if (iniciando) nome = (jogador == 0 ? nomeSorteado1 : nomeSorteado2).toUpperCase();
        g.setFont(new Font("SansSerif", Font.BOLD, 30));
        centralizado(g, nome, x + w / 2, y + h - 40, Color.WHITE, true);

        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        if (ok) {
            centralizado(g, "PRONTO!", x + w / 2, y + h - 16, new Color(120, 255, 150), true);
        } else {
            centralizado(g, "escolhendo...", x + w / 2, y + h - 16, new Color(190, 205, 235), false);
        }
    }

    private void desenharCartoes(Graphics2D g) {
        for (int i = 0; i < opcoes.size(); i++) {
            Opcao op = opcoes.get(i);
            int x = cartaoX(i);
            int y = CARTOES_Y;

            g.setColor(new Color(8, 10, 28, 200));
            g.fillRoundRect(x, y, CARTAO, CARTAO, 14, 14);
            g.drawImage(op.icone, x + 7, y + 7, CARTAO - 14, CARTAO - 14, null);
            g.setStroke(new BasicStroke(2f));
            g.setColor(new Color(220, 228, 245, 160));
            g.drawRoundRect(x, y, CARTAO, CARTAO, 14, 14);

            // molduras dos jogadores que estão neste cartão
            boolean doisAqui = cursor[0] == i && cursor[1] == i;
            for (int j = 0; j < 2; j++) {
                if (cursor[j] != i) continue;
                int folga = (doisAqui && j == 1) ? 9 : 4;
                double pulso = 0.5 + 0.5 * Math.sin(relogio * 6 + j);
                int alfa = pronto[j] ? 255 : (int) (140 + 115 * pulso);
                Color c = COR_JOGADOR[j];
                g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), alfa));
                g.setStroke(new BasicStroke(pronto[j] ? 5f : 3.5f));
                g.drawRoundRect(x - folga, y - folga, CARTAO + 2 * folga, CARTAO + 2 * folga, 18, 18);

                // etiqueta 1P / 2P em cima do cartão
                g.setFont(new Font("SansSerif", Font.BOLD, 13));
                int tl = g.getFontMetrics().stringWidth(ROTULO[j]) + 14;
                int tx = (doisAqui) ? (j == 0 ? x : x + CARTAO - tl) : x + (CARTAO - tl) / 2;
                int ty = y - folga - 26;
                g.setColor(c);
                g.fillRoundRect(tx, ty, tl, 20, 8, 8);
                g.setColor(new Color(10, 10, 20));
                g.drawString(ROTULO[j], tx + 7, ty + 15);
            }

            g.setFont(new Font("SansSerif", Font.BOLD, 15));
            centralizado(g, op.nome.toUpperCase(), x + CARTAO / 2, y + CARTAO + 28, new Color(235, 240, 250), true);
        }
    }

    private void desenharAjuda(Graphics2D g) {
        String[] linhas = {
                "1P:  A / D escolhe   ·   J confirma   ·   K cancela",
                "2P:  \u2190 / \u2192 escolhe   ·   ENTER confirma   ·   BACKSPACE cancela",
                "Também dá para clicar no personagem com o mouse"
        };
        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        FontMetrics fm = g.getFontMetrics();
        int largura = 0;
        for (String l : linhas) largura = Math.max(largura, fm.stringWidth(l));
        int caixaL = largura + 40;
        int caixaA = linhas.length * 20 + 18;
        int x = (TELA_L - caixaL) / 2;
        int y = 386;
        g.setColor(new Color(0, 0, 0, 120));
        g.fillRoundRect(x, y, caixaL, caixaA, 12, 12);
        for (int i = 0; i < linhas.length; i++) {
            Color c = i == 0 ? COR_JOGADOR[0] : (i == 1 ? COR_JOGADOR[1] : new Color(190, 205, 235));
            g.setColor(c);
            g.drawString(linhas[i], x + 20, y + 26 + i * 20);
        }
    }

    private void desenharLutar(Graphics2D g) {
        float aparece = (float) Math.min(1, contagem / 0.25);
        g.setColor(new Color(0, 0, 0, (int) (150 * aparece)));
        g.fillRect(0, 0, TELA_L, TELA_A);
        double escala = 1 + 0.25 * (1 - aparece);
        g.translate(TELA_L / 2.0, TELA_A / 2.0);
        g.scale(escala, escala);
        g.setFont(new Font("SansSerif", Font.BOLD, 110));
        centralizado(g, "LUTAR!", 0, 38, new Color(255, 214, 90), true);
    }

    /** Texto centralizado no ponto x (com sombra opcional). */
    private void centralizado(Graphics2D g, String texto, int x, int baseline, Color cor, boolean sombra) {
        FontMetrics fm = g.getFontMetrics();
        int tx = x - fm.stringWidth(texto) / 2;
        if (sombra) {
            g.setColor(new Color(0, 0, 0, 190));
            g.drawString(texto, tx + 2, baseline + 2);
        }
        g.setColor(cor);
        g.drawString(texto, tx, baseline);
    }
}
