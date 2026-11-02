package Luta.Personagem;

import java.util.ArrayList;
import java.util.List;

public abstract class Personagem {

    // ===== ATRIBUTOS =====
    private static final int MAX_HABILIDADES = 4;   // 3 golpes + 1 especial

    private final String nome;
    private final String cor;
    private final int vidaMaxima;
    private final int especialMaximo;
    private final List<Habilidade> habilidades = new ArrayList<>();

    private int vida;
    private int especial;
    private boolean defendendo;

    // ===== CONSTRUTOR =====
    protected Personagem(String nome, String cor, int vidaMaxima, int especialMaximo) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Nome é obrigatório");
        }
        if (vidaMaxima <= 0) {
            throw new IllegalArgumentException("Vida máxima deve ser maior que zero");
        }
        if (especialMaximo < 0) {
            throw new IllegalArgumentException("Especial máximo não pode ser negativo");
        }
        this.nome = nome;
        this.cor = cor;
        this.vidaMaxima = vidaMaxima;
        this.especialMaximo = especialMaximo;
        this.vida = vidaMaxima;   // começa com a vida cheia
        this.especial = 0;        // começa sem especial
        this.defendendo = false;
    }

    // ===== HABILIDADES =====
    protected void adicionarHabilidade(Habilidade habilidade) {
        if (habilidade == null) {
            throw new IllegalArgumentException("Habilidade não pode ser nula");
        }
        if (habilidades.size() >= MAX_HABILIDADES) {
            throw new IllegalStateException("Máximo de " + MAX_HABILIDADES + " habilidades");
        }
        habilidades.add(habilidade);
    }

    public int usarHabilidade(int indice, Personagem alvo) {
        if (!estaVivo()) {
            return 0;
        }
        if (indice < 0 || indice >= habilidades.size()) {
            throw new IllegalArgumentException("Habilidade inexistente");
        }

        Habilidade habilidade = habilidades.get(indice);

        if (!consumirEspecial(habilidade.getCustoEspecial())) {
            return 0;   // sem especial suficiente
        }

        int danoCausado = alvo.receberDano(habilidade.getDano());
        carregarEspecial(habilidade.getGanhoEspecial());   // o especial só carrega ao bater
        return danoCausado;
    }

    public List<Habilidade> getHabilidades() {
        return List.copyOf(habilidades);
    }

    // ===== DEFESA =====
    public void defender() {
        defendendo = true;
    }

    public void iniciarTurno() {
        defendendo = false;   // a guarda dura até o próximo turno do próprio personagem
    }

    public boolean estaDefendendo() {
        return defendendo;
    }

    // ===== VIDA E ESPECIAL =====
    public int receberDano(int dano) {
        if (dano < 0) {
            throw new IllegalArgumentException("Dano não pode ser negativo");
        }

        int danoReal = dano;

        if (defendendo && dano > 0) {
            danoReal = Math.max(1, dano / 2);   // metade, arredondada para baixo, mínimo 1
        }

        vida = Math.max(0, vida - danoReal);
        return danoReal;
    }

    protected void carregarEspecial(int quantidade) {
        especial = Math.min(especialMaximo, especial + quantidade);
    }

    protected boolean consumirEspecial(int custo) {
        if (especial < custo) {
            return false;
        }
        especial -= custo;
        return true;
    }

    public boolean estaVivo() {
        return vida > 0;
    }

    // ===== GETTERS =====
    public String getNome() { return nome; }
    public String getCor() { return cor; }
    public int getVida() { return vida; }
    public int getVidaMaxima() { return vidaMaxima; }
    public int getEspecial() { return especial; }
    public int getEspecialMaximo() { return especialMaximo; }

    @Override
    public String toString() {
        return nome + " [" + vida + "/" + vidaMaxima + " HP | "
                + especial + "/" + especialMaximo + " SP]";
    }
}