package Luta.Personagens;

import Luta.Jogo.Golpe;
import Luta.Personagem.Habilidade;
import Luta.Personagem.Personagem;

/** Mago: o maior alcance do jogo, mas anda devagar e demora mais para golpear e se recuperar. */
public class Aqua extends Personagem {

    public Aqua() {
        super("Aqua", "azul", 275, 100);

        // ----- regras: nome, dano, custo de especial, ganho de especial -----
        adicionarHabilidade(new Habilidade("arco d'água", 10, 0, 25));
        adicionarHabilidade(new Habilidade("dois arcos d'água", 14, 0, 30));
        adicionarHabilidade(new Habilidade("lâmina d'água", 19, 0, 35));
        adicionarHabilidade(new Habilidade("tsunami", 50, 100, 0));

        // ----- parte física -----
        definirMovimento(380, 1100);   // velocidade (un/s), força do pulo

        definirGolpes(
                //        início ativo recup. alcance altura chão empurrão
                new Golpe(0.09, 0.08, 0.20, 115,  70, 85, 260),   // leve: arco d'água
                new Golpe(0.12, 0.10, 0.24, 145,  80, 75, 330),   // médio: dois arcos d'água
                new Golpe(0.16, 0.12, 0.30, 185,  90, 70, 430),   // forte: lâmina d'água
                new Golpe(0.34, 0.25, 0.50, 380, 175,  0, 800)   // ESPECIAL: tsunami
        );
    }
}
