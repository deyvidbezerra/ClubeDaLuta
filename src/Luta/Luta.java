package Luta;

import java.util.List;
import java.util.Random;
import java.util.Scanner;

public class Luta {

    private final Personagem jogador1;
    private final Personagem jogador2;
    private final Scanner scanner;
    private final Random random = new Random();

    public Luta(Personagem jogador1, Personagem jogador2, Scanner scanner) {
        if (jogador1 == null || jogador2 == null) {
            throw new IllegalArgumentException("Os dois jogadores são obrigatórios");
        }
        this.jogador1 = jogador1;
        this.jogador2 = jogador2;
        this.scanner = scanner;
    }

    // Maior velocidade começa; em caso de empate, sorteia
    public Personagem definirQuemComeca() {
        if (jogador1.getVelocidade() > jogador2.getVelocidade()) {
            return jogador1;
        }
        if (jogador2.getVelocidade() > jogador1.getVelocidade()) {
            return jogador2;
        }
        return random.nextBoolean() ? jogador1 : jogador2;
    }

    public void iniciar() {
        Personagem atacante = definirQuemComeca();
        Personagem defensor = (atacante == jogador1) ? jogador2 : jogador1;

        System.out.println("\n" + atacante.getNome() + " começa atacando!");

        while (jogador1.estaVivo() && jogador2.estaVivo()) {
            executarTurno(atacante, defensor);

            // troca os papéis
            Personagem temp = atacante;
            atacante = defensor;
            defensor = temp;
        }

        Personagem vencedor = jogador1.estaVivo() ? jogador1 : jogador2;
        System.out.println("\n" + vencedor.getNome() + " venceu!");
    }

    private void executarTurno(Personagem atacante, Personagem defensor) {
        atacante.iniciarTurno();   // a guarda do atacante termina aqui

        System.out.println("\n===== Vez de " + atacante.getNome() + " =====");
        System.out.println(atacante);
        System.out.println(defensor);

        List<Habilidade> habilidades = atacante.getHabilidades();
        int opcaoDefender = habilidades.size() + 1;
        boolean agiu = false;

        while (!agiu) {
            System.out.println();
            for (int i = 0; i < habilidades.size(); i++) {
                Habilidade h = habilidades.get(i);
                boolean disponivel = atacante.getEspecial() >= h.getCustoEspecial();
                System.out.println((i + 1) + " - " + h
                        + (disponivel ? "" : "  [especial insuficiente]"));
            }
            System.out.println(opcaoDefender + " - Defender");

            int escolha = lerOpcao(scanner, 1, opcaoDefender);

            if (escolha == opcaoDefender) {
                atacante.defender();
                System.out.println(atacante.getNome() + " assumiu a guarda!");
                agiu = true;
            } else {
                Habilidade h = habilidades.get(escolha - 1);
                if (atacante.getEspecial() < h.getCustoEspecial()) {
                    System.out.println("Especial insuficiente. Escolha outra ação.");
                } else {
                    int dano = atacante.usarHabilidade(escolha - 1, defensor);
                    System.out.println(atacante.getNome() + " usou " + h.getNome()
                            + " e causou " + dano + " de dano em " + defensor.getNome());
                    agiu = true;
                }
            }
        }
    }

    // Lê um número entre minimo e maximo, repetindo até ser válido
    public static int lerOpcao(Scanner scanner, int minimo, int maximo) {
        while (true) {
            System.out.print("Escolha (" + minimo + "-" + maximo + "): ");
            String entrada = scanner.nextLine().trim();
            try {
                int valor = Integer.parseInt(entrada);
                if (valor >= minimo && valor <= maximo) {
                    return valor;
                }
            } catch (NumberFormatException e) {
                // cai na mensagem abaixo
            }
            System.out.println("Opção inválida.");
        }
    }
}