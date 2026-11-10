package Luta.Personagens;

import Luta.Jogo.Golpe;
import Luta.Personagem.Habilidade;
import Luta.Personagem.Personagem;

/** Lutador: alcance curto, golpes rápidos e muita vida. Precisa chegar perto. */
public class Crys extends Personagem {

    public Crys() {
        super("Crys", "amarelo", 310, 100);

        // ----- regras: nome, dano, custo de especial, ganho de especial -----
        adicionarHabilidade(new Habilidade("murro", 9, 0, 15));
        adicionarHabilidade(new Habilidade("garras", 13, 0, 20));
        adicionarHabilidade(new Habilidade("soco fortificado", 17, 0, 25));
        adicionarHabilidade(new Habilidade("enchurrada de socos", 38, 100, 0));

        // ----- parte física -----
        definirMovimento(440, 1100);   // velocidade (un/s), força do pulo

        definirGolpes(
                //        início ativo recup. alcance altura chão empurrão
                new Golpe(0.06, 0.08, 0.17,  80,  70, 85, 240),   // leve: murro
                new Golpe(0.09, 0.10, 0.21, 100,  80, 75, 320),   // médio: garras
                new Golpe(0.13, 0.12, 0.27, 125,  90, 70, 460),   // forte: soco fortificado
                new Golpe(0.28, 0.25, 0.45, 240, 175,  0, 800)   // ESPECIAL: enchurrada de socos
        );
    }
}
