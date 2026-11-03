package Luta.Personagens;

import Luta.Personagem.Habilidade;
import Luta.Personagem.Personagem;

public class Crys extends Personagem {

    public Crys() {
        super("Crys", "amarelo", 120, 100);

        adicionarHabilidade(new Habilidade("murro",          10,  0,  20));
        adicionarHabilidade(new Habilidade("garras",          14,  0,  25));
        adicionarHabilidade(new Habilidade("soco fortificado",        18,  0,  30));
        adicionarHabilidade(new Habilidade("enchurrada de socos",  40,  100, 0));

    }
}