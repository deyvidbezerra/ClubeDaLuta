package Luta.Personagens;

import Luta.Personagem.Habilidade;
import Luta.Personagem.Personagem;

public class Aqua extends Personagem {

    public Aqua() {
        super("Aqua", "azul", 120, 100);

        adicionarHabilidade(new Habilidade("arco d'água",          10,  0,  20));
        adicionarHabilidade(new Habilidade("dois arcos d'água",          14,  0,  25));
        adicionarHabilidade(new Habilidade("lâmina d'água",        18,  0,  30));
        adicionarHabilidade(new Habilidade("tsunami",  40,  100, 0));

    }
}