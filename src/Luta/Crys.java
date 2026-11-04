package Luta;

public class Crys extends Personagem {

    public Crys() {
        super("Crys", "Amarelo", 100, 100, 12);   // 12 = velocidade

        adicionarHabilidade(new Habilidade("Murro",                10,  0,  20));
        adicionarHabilidade(new Habilidade("Garras",               14,  0,  25));
        adicionarHabilidade(new Habilidade("Soco fortificado",     18,  0,  30));
        adicionarHabilidade(new Habilidade("Enchurrada de socos",  40,  100, 0));
    }
}