package Luta.Personagens;

import Luta.Jogo.Golpe;
import Luta.Personagem.Habilidade;
import Luta.Personagem.Personagem;

/** Ágil: o mais rápido e o que pula mais alto, com golpes rápidos de alcance curto. */
public class Keno extends Personagem {

    public Keno() {
        super("Keno", "Rosa", 275, 100);

        // ----- regras: nome, dano, custo de especial, ganho de especial -----
        adicionarHabilidade(new Habilidade("Chute", 9, 0, 25));
        adicionarHabilidade(new Habilidade("Chute Duplo", 12, 0, 30));
        adicionarHabilidade(new Habilidade("Pulo e chute", 16, 0, 35));
        adicionarHabilidade(new Habilidade("Pula bem alto e chuta por cima", 38, 100, 0));

        // ----- parte física -----
        definirMovimento(480, 1250);   // velocidade (un/s), força do pulo

        definirGolpes(
                //        início ativo recup. alcance altura chão empurrão
                new Golpe(0.07, 0.07, 0.16,  85,  70, 85, 260),   // leve: Chute
                new Golpe(0.09, 0.09, 0.20, 110,  80, 75, 330),   // médio: Chute Duplo
                new Golpe(0.12, 0.12, 0.26, 140,  90, 70, 430),   // forte: Pulo e chute
                new Golpe(0.26, 0.22, 0.42, 270, 175,  0, 800)   // ESPECIAL: Pula bem alto e chuta por cima
        );
    }
}
