package Luta.Jogo;

import Luta.Personagem.Personagem;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

/**
 * Barra de vida (e de especial) de um lutador, desenhada na tela (HUD).
 *
 * Detalhes:
 *  - formato de paralelogramo inclinado, com borda dupla;
 *  - cor muda de verde para vermelho conforme a vida cai;
 *  - "barra fantasma" amarela: ao levar dano, ela demora um instante e desce devagar;
 *  - flash branco quando o dano acontece;
 *  - divisões a cada 10% e brilho no topo;
 *  - nome e barra de especial logo abaixo (a vida é só a barra, sem número).
 */
public class BarraDeVida {

    private static final double INCLINACAO = 16;      // quanto as pontas são inclinadas (px)

    private final boolean ladoEsquerdo;               // true = jogador 1, false = jogador 2
    private final String rotuloJogador;

    private double fantasma = 1;                      // vida "antiga" (0..1) que desce devagar
    private double atraso;                            // tempo de espera antes da fantasma descer
    private double flash;                             // 1 = flash máximo, vai a 0
    private double ultimaVida = 1;

    public BarraDeVida(boolean ladoEsquerdo) {
        this.ladoEsquerdo = ladoEsquerdo;
        this.rotuloJogador = ladoEsquerdo ? "1P" : "2P";
    }

    public void atualizar(double dt, Personagem p) {
        double vida = p.getVida() / (double) p.getVidaMaxima();

        if (vida < ultimaVida - 1e-9) {       // levou dano
            flash = 1;
            atraso = 0.5;
        }
        if (vida > fantasma) {                // curou ou reiniciou
            fantasma = vida;
        } else if (atraso > 0) {
            atraso -= dt;
        } else {
            fantasma = Math.max(vida, fantasma - 0.45 * dt);
        }
        flash = Math.max(0, flash - 5 * dt);
        ultimaVida = vida;
    }

    public void desenhar(Graphics2D g, Personagem p, int x, int y, int w, int h) {
        double vida = p.getVida() / (double) p.getVidaMaxima();
        Color cor = corDoPersonagem(p.getCor());

        desenharCabecalho(g, p, cor, x, y, w);

        // ----- barra de vida -----
        Shape forma = paralelogramo(x, y, w, h);
        g.setColor(new Color(12, 12, 22, 230));
        g.fill(forma);

        Shape clipOriginal = g.getClip();
        g.clip(forma);

        // fantasma (dano recente)
        g.setColor(new Color(255, 205, 80));
        g.fill(fatia(x, y, w, h, fantasma, true));

        // vida atual (a cor vai de vermelho a verde)
        float matiz = (float) (0.33 * vida);
        Color claro = Color.getHSBColor(matiz, 0.80f, 1.00f);
        Color escuro = Color.getHSBColor(matiz, 0.95f, 0.65f);
        g.setPaint(new GradientPaint(0, y, claro, 0, y + h, escuro));
        g.fill(fatia(x, y, w, h, vida, true));

        // divisões a cada 10%
        g.setColor(new Color(0, 0, 0, 70));
        g.setStroke(new BasicStroke(1.5f));
        for (int i = 1; i < 10; i++) {
            int dx = x + w * i / 10;
            g.drawLine(dx - (int) INCLINACAO / 2, y, dx - (int) INCLINACAO / 2, y + h);
        }

        // brilho no topo
        g.setPaint(new GradientPaint(0, y, new Color(255, 255, 255, 110), 0, y + h * 0.55f, new Color(255, 255, 255, 0)));
        g.fill(new Rectangle2D.Double(x, y, w, h * 0.55));

        // flash de dano
        if (flash > 0) {
            g.setColor(new Color(255, 255, 255, (int) (200 * flash)));
            g.fill(new Rectangle2D.Double(x, y, w, h));
        }
        g.setClip(clipOriginal);

        // borda dupla
        g.setStroke(new BasicStroke(5f));
        g.setColor(new Color(10, 10, 16));
        g.draw(forma);
        g.setStroke(new BasicStroke(2f));
        g.setColor(new Color(235, 235, 245));
        g.draw(forma);

        desenharEspecial(g, p, cor, x, y + h + 8, (int) (w * 0.62));
    }

    // ---------------------------------------------------------------- partes

    private void desenharCabecalho(Graphics2D g, Personagem p, Color cor, int x, int y, int w) {
        Font fonteNome = new Font("SansSerif", Font.BOLD, 16);
        int baseline = y - 9;

        // selo "1P" / "2P" na ponta de fora
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        FontMetrics fmSelo = g.getFontMetrics();
        int seloL = fmSelo.stringWidth(rotuloJogador) + 12;
        int seloX = ladoEsquerdo ? x + 6 : x + w - 6 - seloL;
        g.setColor(cor);
        g.fillRoundRect(seloX, baseline - 14, seloL, 18, 6, 6);
        g.setColor(new Color(10, 10, 20));
        g.drawString(rotuloJogador, seloX + 6, baseline);

        // nome ao lado do selo
        g.setFont(fonteNome);
        FontMetrics fmNome = g.getFontMetrics();
        String nome = p.getNome().toUpperCase();
        int nomeX = ladoEsquerdo ? seloX + seloL + 8 : seloX - 8 - fmNome.stringWidth(nome);
        sombra(g, nome, nomeX, baseline, Color.WHITE);
    }

    private void desenharEspecial(Graphics2D g, Personagem p, Color cor, int x, int y, int w) {
        int h = 11;
        double razao = p.getEspecialMaximo() == 0 ? 0 : p.getEspecial() / (double) p.getEspecialMaximo();
        boolean cheio = razao >= 1;

        // a barra de especial fica do lado de fora (P1 à esquerda, P2 à direita)
        int sx = ladoEsquerdo ? x : x + (int) (w / 0.62) - w;
        Shape forma = paralelogramo(sx, y, w, h);

        g.setColor(new Color(12, 12, 22, 230));
        g.fill(forma);

        Shape clipOriginal = g.getClip();
        g.clip(forma);

        Color a, b;
        if (cheio) {                                // pisca em dourado quando está cheio
            double pulso = 0.5 + 0.5 * Math.sin(System.nanoTime() / 1e9 * 9);
            a = new Color(255, (int) (210 + 45 * pulso), (int) (60 + 90 * pulso));
            b = new Color(230, 150, 20);
        } else {
            a = new Color(120, 235, 255);
            b = new Color(30, 110, 220);
        }
        g.setPaint(new GradientPaint(0, y, a, 0, y + h, b));
        g.fill(fatia(sx, y, w, h, razao, false));

        g.setPaint(new GradientPaint(0, y, new Color(255, 255, 255, 100), 0, y + h * 0.5f, new Color(255, 255, 255, 0)));
        g.fill(new Rectangle2D.Double(sx, y, w, h * 0.5));
        g.setClip(clipOriginal);

        g.setStroke(new BasicStroke(3.5f));
        g.setColor(new Color(10, 10, 16));
        g.draw(forma);
        g.setStroke(new BasicStroke(1.5f));
        g.setColor(cheio ? new Color(255, 225, 120) : new Color(200, 210, 230));
        g.draw(forma);

        // rótulo "SP"
        g.setFont(new Font("SansSerif", Font.BOLD, 11));
        FontMetrics fm = g.getFontMetrics();
        int tx = ladoEsquerdo ? sx + w + 10 : sx - 10 - fm.stringWidth("SP");
        sombra(g, "SP", tx, y + h - 1, cheio ? new Color(255, 225, 120) : new Color(160, 215, 255));
    }

    // ---------------------------------------------------------------- utilitários

    /**
     * Pedaço preenchido da barra. A vida fica "ancorada" no lado do centro da tela
     * (assim ela encolhe em direção às bordas, como nos jogos de luta clássicos).
     */
    private Rectangle2D fatia(int x, int y, int w, int h, double razao, boolean ancoradaNoCentro) {
        double largura = w * Math.max(0, Math.min(1, razao));
        boolean ancoraDireita = ancoradaNoCentro ? ladoEsquerdo : !ladoEsquerdo;
        double inicio = ancoraDireita ? x + w - largura : x;
        return new Rectangle2D.Double(inicio, y, largura, h);
    }

    /** P1: inclina para a direita "/"; P2: inclina para a esquerda "\". */
    private Shape paralelogramo(int x, int y, int w, int h) {
        double s = INCLINACAO * h / 30.0;
        Path2D p = new Path2D.Double();
        if (ladoEsquerdo) {
            p.moveTo(x + s, y);
            p.lineTo(x + w, y);
            p.lineTo(x + w - s, y + h);
            p.lineTo(x, y + h);
        } else {
            p.moveTo(x, y);
            p.lineTo(x + w - s, y);
            p.lineTo(x + w, y + h);
            p.lineTo(x + s, y + h);
        }
        p.closePath();
        return p;
    }

    private void sombra(Graphics2D g, String texto, int x, int y, Color cor) {
        g.setColor(new Color(0, 0, 0, 190));
        g.drawString(texto, x + 1, y + 1);
        g.setColor(cor);
        g.drawString(texto, x, y);
    }

    /** Converte o texto de cor do Personagem ("azul", "Rosa"...) em uma cor de verdade. */
    public static Color corDoPersonagem(String nome) {
        String c = nome == null ? "" : nome.toLowerCase();
        switch (c) {
            case "azul":    return new Color(90, 170, 255);
            case "amarelo": return new Color(255, 218, 70);
            case "cinza":   return new Color(195, 198, 210);
            case "rosa":    return new Color(255, 135, 190);
            default:        return Color.WHITE;
        }
    }
}
