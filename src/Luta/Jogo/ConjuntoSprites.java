package Luta.Jogo;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Todos os sprites de UM personagem: parado, golpes, defesa, dano, pulo e K.O.
 *
 * Os PNGs novos têm tamanhos diferentes (32x64, 64x64, 128x64) e o personagem não fica no
 * mesmo ponto em todos. Para o Lutador não precisar saber disso, cada imagem é copiada para uma
 * tela padrão (LARGURA_PNG x ALTURA_PNG) com o ponto dos PÉS exatamente em (ANC_X, ANC_Y).
 * Assim o Lutador desenha tudo com a mesma âncora.
 *
 * Para adicionar um personagem novo: crie um método como aqua()/keno() e inclua no carregar().
 */
public final class ConjuntoSprites {

    /** Ponto dos pés dentro da imagem padronizada (em pixels do sprite). */
    public static final int ANC_X = 48;
    public static final int ANC_Y = 64;
    private static final int LARGURA_PNG = 176;
    private static final int ALTURA_PNG = 72;

    /** Como o efeito visual de cada golpe é desenhado (além do sprite do personagem). */
    public static final int EFEITO_CODIGO = 0;     // desenhado por código (o jeito antigo)
    public static final int EFEITO_NENHUM = 1;     // o efeito já faz parte do sprite
    public static final int EFEITO_PROJETIL = 2;   // um sprite que voa para frente
    public static final int EFEITO_ONDA = 3;       // sprites da onda (tsunami)

    public final String nome;
    public final Animacao idle;
    /** Altura do corpo em pixels do sprite (define a hurtbox). */
    public final int alturaPx;

    // animação de cada golpe (índice 0..3); null = ainda sem sprite
    public final Animacao[] golpe = new Animacao[4];
    public final int[] quadroAtivoIni = new int[4];
    public final int[] quadroAtivoFim = new int[4];
    public final double[] saltoVisual = new double[4];   // "pulinho" só visual durante o golpe
    public final int[] efeito = new int[4];

    // poses únicas (null = não existe ainda)
    public BufferedImage guarda;
    public BufferedImage dano;
    public BufferedImage pulo;
    public BufferedImage derrotado;

    // efeitos
    public BufferedImage projetil;
    public List<BufferedImage> onda;

    private ConjuntoSprites(String nome, Animacao idle, int alturaPx) {
        this.nome = nome;
        this.idle = idle;
        this.alturaPx = alturaPx;
    }

    public static boolean temSprites(String nomePersonagem) {
        String n = nomePersonagem == null ? "" : nomePersonagem.toLowerCase();
        return n.equals("aqua") || n.equals("keno");
    }

    public static ConjuntoSprites carregar(String nomePersonagem) {
        String n = nomePersonagem == null ? "" : nomePersonagem.toLowerCase();
        switch (n) {
            case "aqua": return aqua();
            case "keno": return keno();
            default:
                throw new IllegalArgumentException("Ainda não há sprites para o personagem: " + nomePersonagem);
        }
    }

    // ============================================================ AQUA

    private static ConjuntoSprites aqua() {
        final String base = "/assets/aqua/";
        final double ax = 13.5, py = 61;   // centro do corpo e linha dos pés nos PNGs da Aqua

        ConjuntoSprites c = new ConjuntoSprites("Aqua",
                new Animacao(padronizar(Animacao.carregarQuadros(base + "idle/", "aqua_idle", 6), ax, py), 0.14), 58);

        c.guarda = padronizar(Animacao.lerImagem(base + "aqua_guard.png"), ax, py);
        c.dano = padronizar(Animacao.lerImagem(base + "aqua_hurt.png"), ax, py);
        c.pulo = padronizar(Animacao.lerImagem(base + "aqua_jump.png"), ax, py);
        // deitada: o centro do corpo está no meio do desenho e ela fica 4 px acima da linha dos pés
        c.derrotado = padronizar(Animacao.lerImagem(base + "aqua_defeated.png"), 33, 57);

        // atack_1 = golpe do arco d'água (5 quadros: preparação, preparação, e o arco abrindo)
        List<BufferedImage> ataque = padronizar(Animacao.carregarQuadros(base + "atack_1/", "aqua_atack_", 1, 5, 1), ax, py);

        // 0: arco d'água -> o arco aparece nos quadros 3 e 4 (índices 2 e 3)
        c.definirGolpe(0, ataque, 2, 3, EFEITO_NENHUM, 0);

        // 1: dois arcos d'água -> a mesma animação duas vezes seguidas (o 1º arco acerta, o 2º vem logo depois)
        List<BufferedImage> dobro = new ArrayList<>(ataque);
        dobro.addAll(ataque);
        c.definirGolpe(1, dobro, 2, 3, EFEITO_NENHUM, 0);

        // 2: lâmina d'água -> pose do ataque + projétil (atack_3 projectile) voando para frente
        c.definirGolpe(2, ataque, 2, 4, EFEITO_PROJETIL, 0);
        c.projetil = Animacao.lerImagem(base + "atack_3_projectile/aqua_water_bow.png");

        // 3: tsunami -> mão levantada e braço estendido, com a onda (special/aqua_tsunami_1..4) na frente
        List<BufferedImage> poseTsunami = new ArrayList<>();
        poseTsunami.add(ataque.get(1));
        poseTsunami.add(ataque.get(4));
        poseTsunami.add(ataque.get(4));
        c.definirGolpe(3, poseTsunami, 1, 1, EFEITO_ONDA, 0);
        c.onda = Animacao.carregarQuadros(base + "special/", "aqua_tsunami_", 1, 4, 1);

        return c;
    }

    // ============================================================ KENO

    private static ConjuntoSprites keno() {
        final String base = "/assets/keno/";
        final double ax = 10, py = 64;   // os pés do Keno ficam na última linha do PNG

        // O pacote de assets não trouxe a animação "idle" do Keno: por enquanto ele fica na pose parada
        // (keno_idle.png). Quando tiver os quadros, troque por Animacao.carregarQuadros(...) como a Aqua.
        List<BufferedImage> parado = new ArrayList<>();
        parado.add(padronizar(Animacao.lerImagem(base + "keno_idle.png"), ax, py));

        ConjuntoSprites c = new ConjuntoSprites("Keno", new Animacao(parado, 0.14), 51);

        c.dano = padronizar(Animacao.lerImagem(base + "keno_hurt.png"), ax, py);
        c.derrotado = padronizar(Animacao.lerImagem(base + "keno_defeated.png"), 28.5, 64);
        // no ar os pés ficam na linha 58 do PNG
        BufferedImage puloNormal = padronizar(Animacao.lerImagem(base + "keno_jump.png"), 13, 58);
        c.pulo = puloNormal;

        // 0: Chute -> o chute está no quadro 4 (índice 3)
        List<BufferedImage> chute = padronizar(Animacao.carregarQuadros(base + "keno_atack1/", "keno_atack", 0, 6, 1), ax, py);
        c.definirGolpe(0, chute, 3, 3, EFEITO_NENHUM, 0);

        // 1: Chute Duplo -> 10 quadros; o 1º chute acerta (quadros 4 e 5), o 2º vem na recuperação
        List<BufferedImage> duplo = padronizar(Animacao.carregarQuadros(base + "keno_atack2/", "keno_double_kick", 0, 9, 2), ax, py);
        c.definirGolpe(1, duplo, 3, 4, EFEITO_NENHUM, 0);

        // 2: Pulo e chute -> ainda não veio sprite próprio: usa o chute simples com um pulinho visual
        c.definirGolpe(2, chute, 3, 3, EFEITO_NENHUM, 42);

        // 3: especial -> começa saltando (keno_jump) e depois aparece keno_special (128x64, com as lâminas de vento)
        List<BufferedImage> especial = new ArrayList<>();
        especial.add(puloNormal);
        BufferedImage sp = padronizar(Animacao.lerImagem(base + "keno_special.png"), 19, 64);
        especial.add(sp);
        especial.add(sp);
        c.definirGolpe(3, especial, 1, 1, EFEITO_NENHUM, 0);

        return c;
    }

    // ============================================================ utilitários

    private void definirGolpe(int indice, List<BufferedImage> quadros, int qIni, int qFim, int efeito, double salto) {
        golpe[indice] = new Animacao(quadros, 0.1);
        quadroAtivoIni[indice] = qIni;
        quadroAtivoFim[indice] = qFim;
        this.efeito[indice] = efeito;
        saltoVisual[indice] = salto;
    }

    /** Copia a imagem para a tela padrão, com o ponto (ax, py) da imagem caindo em (ANC_X, ANC_Y). */
    private static BufferedImage padronizar(BufferedImage origem, double ax, double py) {
        BufferedImage saida = new BufferedImage(LARGURA_PNG, ALTURA_PNG, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = saida.createGraphics();
        g.drawImage(origem, (int) Math.round(ANC_X - ax), (int) Math.round(ANC_Y - py), null);
        g.dispose();
        return saida;
    }

    private static List<BufferedImage> padronizar(List<BufferedImage> origem, double ax, double py) {
        List<BufferedImage> lista = new ArrayList<>();
        for (BufferedImage img : origem) lista.add(padronizar(img, ax, py));
        return lista;
    }
}
