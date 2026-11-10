package Luta.Jogo;

/**
 * Timer da luta. Por enquanto é infinito (mostra "∞").
 * Para testar com contagem regressiva, crie com {@code Cronometro.contagem(99)}.
 */
public class Cronometro {

    private final boolean infinito;
    private double restante;

    private Cronometro(boolean infinito, double segundos) {
        this.infinito = infinito;
        this.restante = segundos;
    }

    public static Cronometro infinito() {
        return new Cronometro(true, 0);
    }

    public static Cronometro contagem(int segundos) {
        return new Cronometro(false, segundos);
    }

    public void atualizar(double dt) {
        if (!infinito && restante > 0) {
            restante = Math.max(0, restante - dt);
        }
    }

    public boolean acabou() {
        return !infinito && restante <= 0;
    }

    public String getTexto() {
        return infinito ? "\u221E" : String.valueOf((int) Math.ceil(restante));
    }
}
