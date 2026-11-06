package Luta.Personagens;

import Luta.Personagem.Habilidade;
import Luta.Personagem.Personagem;

public class Keno extends Personagem {

    public Keno() {
        super("Keno", "Rosa", 120, 100);

        adicionarHabilidade(new Habilidade("Chute",          10,  0,  20));
        adicionarHabilidade(new Habilidade("Chute Duplo",          14,  0,  25));
        adicionarHabilidade(new Habilidade("Pulo e chute",        18,  0,  30));
        adicionarHabilidade(new Habilidade("Pula bem alto e chuta por cima",  40,  100, 0));

    }
}