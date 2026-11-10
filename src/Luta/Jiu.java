package Luta;

public class Jiu extends Personagem {

    public Jiu() {
        super("Jiu", "Cinza", 100, 100, 12);   // 12 = velocidade

        adicionarHabilidade(new Habilidade("Estocada",             10,  0,  20));
        adicionarHabilidade(new Habilidade("Arremesso",            14,  0,  25));
        adicionarHabilidade(new Habilidade("Ataque cruzado",       18,  0,  30));
        adicionarHabilidade(new Habilidade("Claymore",             40,  100, 0));
    }
}