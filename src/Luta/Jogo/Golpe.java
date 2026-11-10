package Luta.Jogo;

/**
 * Parte "física" de um golpe: quando ele acerta e onde fica a caixa de colisão (hitbox).
 * O índice é o mesmo da habilidade no Personagem (0, 1, 2 = golpes; 3 = especial).
 * O dano e o especial continuam vindo da Habilidade do Personagem.
 *
 * Fases do golpe:  [ início ][ ativo ][ recuperação ]
 *   início      = preparação, ainda não acerta
 *   ativo       = a hitbox existe e pode acertar (1 vez por golpe)
 *   recuperação = o lutador fica parado até poder agir de novo
 */
public class Golpe {

    public final double inicio;        // segundos
    public final double ativo;
    public final double recuperacao;
    public final double alcance;       // largura da hitbox, a partir da frente do corpo
    public final double altura;        // altura da hitbox
    public final double alturaDoChao;  // distância entre o chão e a base da hitbox
    public final double empurrao;      // força que joga o alvo para trás

    public Golpe(double inicio, double ativo, double recuperacao,
                  double alcance, double altura, double alturaDoChao, double empurrao) {
        this.inicio = inicio;
        this.ativo = ativo;
        this.recuperacao = recuperacao;
        this.alcance = alcance;
        this.altura = altura;
        this.alturaDoChao = alturaDoChao;
        this.empurrao = empurrao;
    }

    // Tabela PADRÃO (usada por quem não definir os próprios golpes). Cada Personagem pode ter a sua.
    private static final Golpe[] TABELA = {
            //        início  ativo  recup.  alcance altura chão  empurrão
            new Golpe(0.08,  0.08,  0.18,   90,     70,    85,   260),   // 0: arco d'água
            new Golpe(0.10,  0.10,  0.22,   115,    80,    75,   330),   // 1: dois arcos d'água
            new Golpe(0.14,  0.12,  0.28,   150,    90,    70,   430),   // 2: lâmina d'água
            new Golpe(0.30,  0.25,  0.45,   300,    175,   0,    800),   // 3: tsunami (especial)
    };

    public static Golpe de(int indice) {
        return TABELA[Math.max(0, Math.min(TABELA.length - 1, indice))];
    }

    public double total() {
        return inicio + ativo + recuperacao;
    }
}
