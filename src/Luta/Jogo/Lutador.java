package Luta.Jogo;

import Luta.Personagem.Habilidade;
import Luta.Personagem.Personagem;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Um lutador na tela. O {@link Personagem} guarda as REGRAS (vida, especial, habilidades, dano);
 * o Lutador guarda o que é visual/físico: posição, pulo, colisão, golpes e animação.
 *
 * Convenção: (x, y) é o ponto dos PÉS, no centro do corpo. O y cresce para baixo.
 */
public class Lutador {

    // ===== medidas (unidades do mundo) =====
    public static final int ESCALA = 3;            // cada pixel do sprite vira 3x3 no mundo
    public static final double LARGURA = 60;       // caixa de colisão do corpo (hurtbox)
    // a ALTURA agora depende do personagem (Aqua 58 px, Keno 51 px): veja o campo "altura"

    // (o centro do corpo e a linha dos pés dentro do PNG são padronizados pelo ConjuntoSprites)
    /** Troque para false se o seu sprite original olhar para a ESQUERDA. */
    private static final boolean SPRITE_OLHA_PARA_DIREITA = true;

    // ===== física (velocidade e pulo agora vêm do Personagem) =====
    private static final double GRAVIDADE = 3000;

    private Personagem personagem;
    private ConjuntoSprites sprites;   // todas as poses/animações deste personagem
    private double altura;             // altura da hurtbox (em unidades do mundo)

    private double x;
    private double y;
    private double vy;
    private double empurrao;          // velocidade horizontal causada por golpes recebidos
    private boolean noChao;
    private boolean paraDireita;

    // ===== combate =====
    private int golpeAtual = -1;      // -1 = não está golpeando
    private double tempoGolpe;
    private boolean acertou;          // cada golpe acerta no máximo 1 vez
    private double atordoamento;      // tempo restante sem poder agir (levou golpe)
    private double impactoTempo;      // efeito de faísca no ponto do acerto
    private double impactoX;
    private double impactoY;

    public Lutador(Personagem personagem, double xInicial, boolean paraDireita) {
        resetar(personagem, xInicial, paraDireita);
        if (!paraDireita) sprites.idle.adiantar(0.42);   // o 2º lutador respira "fora de sincronia"
    }

    public void resetar(Personagem novo, double xInicial, boolean paraDireita) {
        // só recarrega os sprites se o personagem mudou (reiniciar a luta com o mesmo personagem reaproveita)
        if (sprites == null || !sprites.nome.equalsIgnoreCase(novo.getNome())) {
            sprites = ConjuntoSprites.carregar(novo.getNome());
        }
        this.altura = sprites.alturaPx * ESCALA;
        this.personagem = novo;
        this.x = xInicial;
        this.y = Cenario.CHAO_Y;
        this.vy = 0;
        this.empurrao = 0;
        this.noChao = true;
        this.paraDireita = paraDireita;
        this.golpeAtual = -1;
        this.tempoGolpe = 0;
        this.atordoamento = 0;
        this.impactoTempo = 0;
    }

    // ===================== ATUALIZAÇÃO =====================

    /**
     * @param direcao      -1 esquerda, 0 parado, +1 direita
     * @param pular        botão de pulo apertado
     * @param golpePedido  índice da habilidade que acabou de ser apertada (0..3), ou -1
     * @param defender     botão de defesa apertado
     */
    public void atualizar(double dt, int direcao, boolean pular, int golpePedido, boolean defender, Lutador oponente) {
        sprites.idle.atualizar(dt);
        if (atordoamento > 0) atordoamento -= dt;
        if (impactoTempo > 0) impactoTempo -= dt;

        boolean vivo = personagem.estaVivo();

        // defesa: segurar o botão no chão reduz o dano à metade (regra está no Personagem)
        if (vivo && golpeAtual < 0 && noChao && defender) personagem.defender();
        else personagem.iniciarTurno();

        boolean livre = vivo && atordoamento <= 0 && golpeAtual < 0 && !personagem.estaDefendendo();
        if (livre && golpePedido >= 0) iniciarGolpe(golpePedido);
        boolean podeMover = livre && golpeAtual < 0;

        // 1) movimento horizontal + colisões no eixo X
        x += ((podeMover ? direcao : 0) * personagem.getVelocidade() + empurrao) * dt;
        empurrao *= Math.exp(-9 * dt);
        if (Math.abs(empurrao) < 5) empurrao = 0;

        limitarNasParedes();
        if (sobrepoe(oponente)) {
            // empurra para fora, para o lado em que já estava
            x = (x < oponente.x) ? oponente.esquerda() - LARGURA / 2 : oponente.direita() + LARGURA / 2;
        }
        limitarNasParedes();

        // 2) pulo e gravidade
        if (pular && noChao && podeMover) {
            vy = -personagem.getForcaPulo();
        }
        vy += GRAVIDADE * dt;
        y += vy * dt;
        noChao = false;

        // 3) colisões no eixo Y: chão...
        if (y >= Cenario.CHAO_Y) {
            y = Cenario.CHAO_Y;
            vy = 0;
            noChao = true;
        }
        // ...e o outro lutador (pousar na cabeça dele / bater a cabeça nos pés dele)
        if (sobrepoe(oponente)) {
            if (vy >= 0) {
                y = oponente.topo();
                vy = 0;
                noChao = true;
            } else {
                y = oponente.base() + altura;
                vy = 0;
            }
        }

        // 4) golpe em andamento (pode acertar o oponente)
        atualizarGolpe(dt, oponente);

        // 5) sempre olhando para o adversário (menos no meio de um golpe ou depois de cair)
        if (vivo && golpeAtual < 0) {
            if (oponente.x > x) paraDireita = true;
            else if (oponente.x < x) paraDireita = false;
        }
    }

    private void limitarNasParedes() {
        x = Math.max(Cenario.MARGEM_PAREDE + LARGURA / 2,
                Math.min(Cenario.LARGURA - Cenario.MARGEM_PAREDE - LARGURA / 2, x));
    }

    // ===================== COMBATE =====================

    private void iniciarGolpe(int indice) {
        List<Habilidade> habilidades = personagem.getHabilidades();
        if (indice < 0 || indice >= habilidades.size()) return;
        // o especial precisa estar carregado (o custo é gasto quando o golpe acerta)
        if (personagem.getEspecial() < habilidades.get(indice).getCustoEspecial()) return;

        golpeAtual = indice;
        tempoGolpe = 0;
        acertou = false;
    }

    private void atualizarGolpe(double dt, Lutador oponente) {
        if (golpeAtual < 0) return;
        Golpe golpe = personagem.getGolpe(golpeAtual);
        tempoGolpe += dt;

        boolean faseAtiva = tempoGolpe >= golpe.inicio && tempoGolpe <= golpe.inicio + golpe.ativo;
        if (faseAtiva && !acertou && oponente.personagem.estaVivo()) {
            Rectangle2D caixa = caixaDeAtaque(golpe);
            Rectangle2D corpo = oponente.corpo();
            if (caixa.intersects(corpo)) {
                acertou = true;
                // o Personagem aplica dano, defesa e carga do especial
                personagem.usarHabilidade(golpeAtual, oponente.personagem);
                oponente.receberGolpe(paraDireita ? 1 : -1, golpe.empurrao);

                Rectangle2D contato = caixa.createIntersection(corpo);
                impactoX = contato.getCenterX();
                impactoY = contato.getCenterY();
                impactoTempo = 0.25;
            }
        }

        if (tempoGolpe >= golpe.total()) {
            golpeAtual = -1;
        }
    }

    /** Chamado no alvo quando um golpe acerta: atordoa, empurra e interrompe o que ele fazia. */
    private void receberGolpe(int direcao, double forca) {
        boolean defendendo = personagem.estaDefendendo();
        atordoamento = defendendo ? 0.12 : 0.30;
        empurrao = direcao * forca * (defendendo ? 0.5 : 1.0);
        if (!personagem.estaVivo()) empurrao = direcao * forca * 1.3;
        golpeAtual = -1;
    }

    /** A hitbox do golpe: um retângulo à frente do corpo. */
    public Rectangle2D caixaDeAtaque(Golpe golpe) {
        double frente = paraDireita ? x + LARGURA / 2 : x - LARGURA / 2;
        double esquerda = paraDireita ? frente : frente - golpe.alcance;
        double topoCaixa = y - golpe.alturaDoChao - golpe.altura;
        return new Rectangle2D.Double(esquerda, topoCaixa, golpe.alcance, golpe.altura);
    }

    /** A "hurtbox": onde o lutador pode ser atingido. */
    public Rectangle2D corpo() {
        return new Rectangle2D.Double(esquerda(), topo(), LARGURA, altura);
    }

    // ===================== COLISÃO =====================

    public double esquerda() { return x - LARGURA / 2; }
    public double direita()  { return x + LARGURA / 2; }
    public double topo()     { return y - altura; }
    public double base()     { return y; }

    private boolean sobrepoe(Lutador o) {
        return esquerda() < o.direita() && direita() > o.esquerda()
                && topo() < o.base() && base() > o.topo();
    }

    // ===================== DESENHO =====================

    public void desenhar(Graphics2D g, boolean mostrarHitbox) {
        desenharSombra(g);
        desenharSprite(g);
        if (personagem.estaDefendendo() && sprites.guarda == null) desenharEscudo(g);   // com sprite de guarda o escudo já está desenhado nele
        desenharEfeitoDoGolpe(g);
        desenharImpacto(g);

        if (mostrarHitbox) {
            g.setStroke(new BasicStroke(2f));
            g.setColor(new Color(255, 40, 40, 200));                 // corpo (hurtbox)
            g.draw(corpo());
            if (golpeAtual >= 0) {
                Golpe golpe = personagem.getGolpe(golpeAtual);
                boolean ativa = tempoGolpe >= golpe.inicio && tempoGolpe <= golpe.inicio + golpe.ativo;
                g.setColor(ativa ? new Color(255, 170, 0, 230) : new Color(255, 170, 0, 70));
                g.draw(caixaDeAtaque(golpe));                        // golpe (hitbox)
            }
            g.setColor(Color.YELLOW);
            g.fill(new Ellipse2D.Double(x - 3, y - 3, 6, 6));
        }
    }

    private void desenharSprite(Graphics2D g) {
        AffineTransform original = g.getTransform();
        Composite compositeOriginal = g.getComposite();

        double sinal = paraDireita ? 1 : -1;
        boolean vivo = personagem.estaVivo();
        boolean golpeando = golpeAtual >= 0;
        boolean temSprite = golpeando && sprites.golpe[golpeAtual] != null;

        BufferedImage quadro = sprites.idle.getQuadro();
        double deslocamentoX = 0;
        double deslocamentoY = 0;
        double inclinacao = 0;
        boolean deitado = false;

        if (!vivo) {
            // K.O.: usa o sprite dele deitado; se não houver, gira o sprite parado (jeito antigo)
            if (sprites.derrotado != null) quadro = sprites.derrotado;
            else deitado = true;
        } else if (golpeando) {
            Golpe golpe = personagem.getGolpe(golpeAtual);
            if (temSprite) {
                quadro = sprites.golpe[golpeAtual].getQuadroSincronizado(tempoGolpe, golpe.inicio, golpe.ativo,
                        golpe.total(), sprites.quadroAtivoIni[golpeAtual], sprites.quadroAtivoFim[golpeAtual]);
                double salto = sprites.saltoVisual[golpeAtual];
                if (salto > 0) {   // pulinho só visual (a hitbox continua no chão)
                    deslocamentoY = -salto * Math.sin(Math.PI * Math.min(1, tempoGolpe / golpe.total()));
                }
            } else {
                // sem sprite de ataque: o parado "avança" e se inclina para frente
                double progresso = Math.min(1, tempoGolpe / golpe.total());
                double forca = Math.sin(Math.PI * progresso);
                double intensidade = golpeAtual == 3 ? 34 : 18;
                deslocamentoX = sinal * intensidade * forca;
                inclinacao = sinal * (golpeAtual == 3 ? 0.16 : 0.09) * forca;
            }
        } else if (personagem.estaDefendendo() && sprites.guarda != null) {
            quadro = sprites.guarda;
        } else if (atordoamento > 0 && sprites.dano != null) {
            quadro = sprites.dano;
        } else if (!noChao && sprites.pulo != null) {
            quadro = sprites.pulo;
        }

        if (atordoamento > 0 && vivo) {
            deslocamentoX += (((int) (atordoamento * 60)) % 2 == 0 ? 4 : -4);   // tremida
            if (((int) (atordoamento * 30)) % 2 == 0) {
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.55f));
            }
        }

        if (deitado) {
            g.translate(x, y - 30);
            g.rotate(-sinal * Math.PI / 2);
        } else {
            g.translate(x + deslocamentoX, y + deslocamentoY);   // origem nos pés
            g.rotate(inclinacao);
        }

        if (paraDireita != SPRITE_OLHA_PARA_DIREITA) {
            g.scale(-1, 1);                       // espelha o sprite
        }
        g.scale(ESCALA, ESCALA);
        g.translate(-ConjuntoSprites.ANC_X, -ConjuntoSprites.ANC_Y);
        g.drawImage(quadro, 0, 0, null);

        g.setTransform(original);
        g.setComposite(compositeOriginal);
    }

    /** Bolha azul na frente do lutador enquanto ele defende. */
    private void desenharEscudo(Graphics2D g) {
        double sinal = paraDireita ? 1 : -1;
        Ellipse2D bolha = new Ellipse2D.Double(x + sinal * 14 - 55, y - altura / 2 - 105, 110, 210);
        g.setColor(new Color(120, 200, 255, 45));
        g.fill(bolha);
        g.setStroke(new BasicStroke(3f));
        g.setColor(new Color(170, 225, 255, 190));
        g.draw(bolha);
    }

    /** A sombra fica no chão e diminui quanto mais alto o lutador está. */
    private void desenharSombra(Graphics2D g) {
        double altura = Cenario.CHAO_Y - y;
        double fator = Math.max(0.35, 1 - altura / 700);
        double largura = 90 * fator;
        g.setColor(new Color(0, 0, 0, (int) (90 * fator)));
        g.fill(new Ellipse2D.Double(x - largura / 2, Cenario.CHAO_Y - 7, largura, 14));
    }

    // ===================== EFEITOS DOS GOLPES =====================

    /**
     * Efeito visual de água de cada golpe (desenhado por código, sem sprite).
     * Truque: movemos a origem para a frente do corpo e espelhamos se ele olha para a esquerda;
     * assim todos os efeitos são escritos como se o lutador sempre olhasse para a direita.
     * Nessas coordenadas locais o chão é y = 0 e "para cima" é negativo.
     */
    private void desenharEfeitoDoGolpe(Graphics2D g) {
        if (golpeAtual < 0) return;
        Golpe golpe = personagem.getGolpe(golpeAtual);

        double t = (tempoGolpe - golpe.inicio) / golpe.ativo;   // 0..1 durante a fase ativa
        if (t < 0) return;
        double fade = 1 - Math.max(0, (t - 1) / 0.8);           // some logo depois da fase ativa
        if (fade <= 0) return;
        double progresso = Math.min(1, t);

        Color cor = BarraDeVida.corDoPersonagem(personagem.getCor());
        AffineTransform original = g.getTransform();
        Stroke strokeOriginal = g.getStroke();

        double sinal = paraDireita ? 1 : -1;
        g.translate(x + sinal * LARGURA / 2, y);
        g.scale(sinal, 1);

        // quando o golpe tem sprite próprio, o efeito vem dele (ou de um sprite de efeito); senão, desenha por código
        int tipo = sprites.golpe[golpeAtual] != null ? sprites.efeito[golpeAtual] : ConjuntoSprites.EFEITO_CODIGO;
        if (tipo == ConjuntoSprites.EFEITO_PROJETIL) {
            desenharProjetil(g, golpe, progresso, fade);
            g.setTransform(original);
            g.setStroke(strokeOriginal);
            return;
        }
        if (tipo == ConjuntoSprites.EFEITO_ONDA) {
            desenharOnda(g, golpe, progresso, fade);
            g.setTransform(original);
            g.setStroke(strokeOriginal);
            return;
        }
        if (tipo == ConjuntoSprites.EFEITO_NENHUM) {
            g.setTransform(original);
            g.setStroke(strokeOriginal);
            return;
        }

        switch (golpeAtual) {
            case 0:
                desenharArco(g, -125, golpe.alcance * 0.85, progresso, fade, cor, 14);
                break;
            case 1:
                desenharArco(g, -145, golpe.alcance * 0.70, progresso, fade, cor, 13);
                double atrasado = Math.max(0, Math.min(1, progresso * 1.6 - 0.6));
                if (atrasado > 0) desenharArco(g, -105, golpe.alcance * 0.90, atrasado, fade, cor, 13);
                break;
            case 2:
                desenharLamina(g, golpe, progresso, fade, cor);
                break;
            default:
                desenharTsunami(g, golpe, progresso, fade, cor);
                break;
        }

        g.setTransform(original);
        g.setStroke(strokeOriginal);
    }

    /** Projétil em sprite (ex.: a lâmina d'água da Aqua): sai da frente do corpo e voa até o fim do alcance. */
    private void desenharProjetil(Graphics2D g, Golpe golpe, double progresso, double fade) {
        BufferedImage img = sprites.projetil;
        if (img == null) return;
        double escala = ESCALA;
        double px = golpe.alcance * (0.12 + 0.88 * progresso);
        double py = -(golpe.alturaDoChao + golpe.altura / 2);   // no meio da hitbox
        Composite original = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) Math.max(0, Math.min(1, fade))));
        g.translate(px, py);
        g.scale(escala, escala);
        g.translate(-33, -33);   // centro do desenho dentro do PNG de 64x64
        g.drawImage(img, 0, 0, null);
        g.setComposite(original);
    }

    /** A onda do tsunami: 4 quadros (grande -> pequena) que avançam pelo chão enquanto o golpe acontece. */
    private void desenharOnda(Graphics2D g, Golpe golpe, double progresso, double fade) {
        if (sprites.onda == null || sprites.onda.isEmpty()) return;
        int n = sprites.onda.size();
        double depoisDoInicio = Math.max(0, tempoGolpe - golpe.inicio);
        double fase = Math.min(0.999, depoisDoInicio / Math.max(1e-6, golpe.total() - golpe.inicio));
        BufferedImage img = sprites.onda.get((int) (fase * n));

        double escala = ESCALA * 1.25;                       // a onda é maior que o lutador
        double larguraOnda = 64 * escala;
        double px = Math.max(0, golpe.alcance - larguraOnda) * Math.min(1, progresso);
        Composite original = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) Math.max(0, Math.min(1, fade))));
        g.translate(px, 0);
        g.scale(escala, escala);
        g.drawImage(img, -3, -52, null);                     // a base da onda (linha 52 do PNG) fica no chão
        g.setComposite(original);
    }

    /** Meia-lua de água que "varre" de cima para baixo na frente do lutador. */
    private void desenharArco(Graphics2D g, double centroY, double raio, double progresso,
                              double fade, Color cor, float largura) {
        Arc2D arco = new Arc2D.Double(-raio, centroY - raio, raio * 2, raio * 2, 55, -110 * progresso, Arc2D.OPEN);
        g.setStroke(new BasicStroke(largura, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(cor.getRed(), cor.getGreen(), cor.getBlue(), (int) (210 * fade)));
        g.draw(arco);
        g.setStroke(new BasicStroke(largura * 0.35f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(255, 255, 255, (int) (230 * fade)));
        g.draw(arco);
    }

    /** Lâmina fina e comprida que dispara para frente. */
    private void desenharLamina(Graphics2D g, Golpe golpe, double progresso, double fade, Color cor) {
        double comprimento = golpe.alcance * Math.min(1, progresso * 1.25);
        double yc = -115;
        Path2D lamina = new Path2D.Double();
        lamina.moveTo(0, yc);
        lamina.quadTo(comprimento * 0.5, yc - 32, comprimento, yc);
        lamina.quadTo(comprimento * 0.5, yc + 32, 0, yc);
        lamina.closePath();

        g.setPaint(new GradientPaint(0, 0, new Color(cor.getRed(), cor.getGreen(), cor.getBlue(), (int) (220 * fade)),
                (float) Math.max(1, comprimento), 0, new Color(255, 255, 255, (int) (235 * fade))));
        g.fill(lamina);
        g.setStroke(new BasicStroke(3f));
        g.setColor(new Color(255, 255, 255, (int) (240 * fade)));
        g.draw(lamina);

        // gotinhas ao redor
        g.setColor(new Color(220, 245, 255, (int) (200 * fade)));
        for (int i = 0; i < 4; i++) {
            double px = comprimento * (0.25 + 0.2 * i);
            double py = yc + ((i % 2 == 0) ? -46 : 46) * (0.6 + 0.4 * progresso);
            g.fill(new Ellipse2D.Double(px - 4, py - 4, 8, 8));
        }
    }

    /** Onda gigante que avança pelo chão e arrebenta na frente. */
    private void desenharTsunami(Graphics2D g, Golpe golpe, double progresso, double fade, Color cor) {
        double frente = golpe.alcance * progresso;
        double altura = 165 * Math.min(1, 0.35 + progresso);

        Path2D onda = new Path2D.Double();
        onda.moveTo(0, 0);
        onda.lineTo(0, -altura * 0.35);
        onda.curveTo(frente * 0.30, -altura * 0.50, frente * 0.55, -altura * 0.92, frente * 0.90, -altura);
        onda.curveTo(frente * 1.10, -altura * 1.03, frente * 1.14, -altura * 0.70, frente * 0.95, -altura * 0.62);
        onda.curveTo(frente * 1.06, -altura * 0.40, frente * 1.02, -altura * 0.15, frente, 0);
        onda.closePath();

        g.setPaint(new GradientPaint(0, (float) -altura, new Color(215, 246, 255, (int) (215 * fade)),
                0, 0, new Color(cor.getRed(), cor.getGreen(), cor.getBlue(), (int) (215 * fade))));
        g.fill(onda);
        g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(255, 255, 255, (int) (240 * fade)));
        g.draw(onda);

        // espuma na crista
        g.setColor(new Color(255, 255, 255, (int) (230 * fade)));
        for (int i = 0; i < 6; i++) {
            double ang = i * 1.1;
            double px = frente * 0.92 + Math.cos(ang) * 26;
            double py = -altura * 0.85 + Math.sin(ang) * 22;
            double r = 5 + (i % 3) * 3;
            g.fill(new Ellipse2D.Double(px - r, py - r, r * 2, r * 2));
        }
    }

    /** Faísca no ponto onde o golpe acertou (em coordenadas do mundo). */
    private void desenharImpacto(Graphics2D g) {
        if (impactoTempo <= 0) return;
        double k = 1 - impactoTempo / 0.25;   // 0 -> 1
        int alfa = (int) (235 * (1 - k));

        g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(255, 255, 255, alfa));
        for (int i = 0; i < 8; i++) {
            double ang = i * Math.PI / 4 + 0.2;
            double r1 = 10 + 30 * k;
            double r2 = 26 + 62 * k;
            g.draw(new Line2D.Double(impactoX + Math.cos(ang) * r1, impactoY + Math.sin(ang) * r1,
                    impactoX + Math.cos(ang) * r2, impactoY + Math.sin(ang) * r2));
        }
        double anel = 10 + 46 * k;
        g.setStroke(new BasicStroke(3f));
        g.setColor(new Color(150, 220, 255, alfa));
        g.draw(new Ellipse2D.Double(impactoX - anel, impactoY - anel, anel * 2, anel * 2));
    }

    // ===================== GETTERS =====================

    public Personagem getPersonagem() { return personagem; }
    public double getX() { return x; }
    public double getY() { return y; }
    public boolean estaNoChao() { return noChao; }
    public boolean estaViradoParaDireita() { return paraDireita; }
    public boolean estaGolpeando() { return golpeAtual >= 0; }
}
