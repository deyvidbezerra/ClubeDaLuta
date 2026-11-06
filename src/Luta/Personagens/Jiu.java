package Luta.Personagens;

import Luta.Personagem.Habilidade;
import Luta.Personagem.Personagem;

public class Jiu extends Personagem {

    public Jiu() {
        super("Jiu", "Cinza", 120, 100);

        adicionarHabilidade(new Habilidade("Estocada",          10,  0,  20));
        adicionarHabilidade(new Habilidade("Arremesso",          14,  0,  25));
        adicionarHabilidade(new Habilidade("Ataque Cruzado",        18,  0,  30));
        adicionarHabilidade(new Habilidade("Claymore",  40,  100, 0));

    }
}