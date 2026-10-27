package Luta.Personagens;

import Luta.Personagem.Habilidade;
import Luta.Personagem.Personagem;

public class Dragon extends Personagem {

    public Dragon() {
        super("Dragon", "Vermelho", 120, 100);

        adicionarHabilidade(new Habilidade("Garra",          10,  0,  20));
        adicionarHabilidade(new Habilidade("Cauda",          14,  0,  25));
        adicionarHabilidade(new Habilidade("Mordida",        18,  0,  30));
        adicionarHabilidade(new Habilidade("Sopro de Fogo",  40,  100, 0));

    }
}