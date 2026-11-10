package Luta.Personagens;

import Luta.Jogo.Golpe;
import Luta.Personagem.Habilidade;
import Luta.Personagem.Personagem;

/** Espadachim: alcance médio-longo, golpes pesados e movimento um pouco lento. */
public class Jiu extends Personagem {

    public Jiu() {
        super("Jiu", "Cinza", 290, 100);

        // ----- regras: nome, dano, custo de especial, ganho de especial -----
        adicionarHabilidade(new Habilidade("Estocada", 11, 0, 15));
        adicionarHabilidade(new Habilidade("Arremesso", 15, 0, 20));
        adicionarHabilidade(new Habilidade("Ataque Cruzado", 19, 0, 25));
        adicionarHabilidade(new Habilidade("Claymore", 46, 100, 0));

        // ----- parte física -----
        definirMovimento(400, 1050);   // velocidade (un/s), força do pulo

        definirGolpes(
                //        início ativo recup. alcance altura chão empurrão
                new Golpe(0.08, 0.08, 0.19, 105,  70, 85, 260),   // leve: Estocada
                new Golpe(0.11, 0.10, 0.23, 135,  80, 75, 330),   // médio: Arremesso
                new Golpe(0.15, 0.12, 0.30, 170,  90, 70, 430),   // forte: Ataque Cruzado
                new Golpe(0.32, 0.25, 0.48, 320, 175,  0, 800)   // ESPECIAL: Claymore
        );
    }
}
