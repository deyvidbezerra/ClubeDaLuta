package Luta;

public class Keno extends Personagem {

    public Keno() {
        super("Keno", "Rosa", 100, 100, 12);   // 12 = velocidade

        adicionarHabilidade(new Habilidade("Chute",                         10,  0,  20));
        adicionarHabilidade(new Habilidade("Chute duplo",                   14,  0,  25));
        adicionarHabilidade(new Habilidade("Pulo e chute",                  18,  0,  30));
        adicionarHabilidade(new Habilidade("Pula bem alto e chuta por cima",40,  100, 0));
    }
}