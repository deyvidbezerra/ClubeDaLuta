package Luta.Personagem;

public class Habilidade {

    private final String nome;
    private final int dano;
    private final int custoEspecial;   // quanto de especial o golpe gasta
    private final int ganhoEspecial;   // quanto de especial o golpe carrega ao bater

    public Habilidade(String nome, int dano, int custoEspecial, int ganhoEspecial) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Nome da habilidade é obrigatório");
        }
        if (dano < 0 || custoEspecial < 0 || ganhoEspecial < 0) {
            throw new IllegalArgumentException("Dano, custo e ganho não podem ser negativos");
        }
        this.nome = nome;
        this.dano = dano;
        this.custoEspecial = custoEspecial;
        this.ganhoEspecial = ganhoEspecial;
    }

    public String getNome() { return nome; }
    public int getDano() { return dano; }
    public int getCustoEspecial() { return custoEspecial; }
    public int getGanhoEspecial() { return ganhoEspecial; }

    @Override
    public String toString() {
        return nome + " (dano " + dano + " | custo " + custoEspecial + " | ganho " + ganhoEspecial + ")";
    }
}