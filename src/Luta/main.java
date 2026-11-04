package Luta;

import java.util.Scanner;

public class main {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== CLUBE DA LUTA ===");
        Personagem p1 = escolherPersonagem(scanner, "Jogador 1");
        Personagem p2 = escolherPersonagem(scanner, "Jogador 2");

        Luta partida = new Luta(p1, p2, scanner);
        partida.iniciar();

        scanner.close();
    }

    private static Personagem escolherPersonagem(Scanner scanner, String jogador) {
        System.out.println("\n" + jogador + ", escolha seu personagem:");
        System.out.println("1 - Aqua");
        System.out.println("2 - Crys");
        System.out.println("3 - Jiu");
        System.out.println("4 - Keno");

        int opcao = Luta.lerOpcao(scanner, 1, 4);

        // cria uma instância nova a cada escolha (os dois podem escolher o mesmo)
        switch (opcao) {
            case 1:  return new Aqua();
            case 2:  return new Crys();
            case 3:  return new Jiu();
            default: return new Keno();
        }
    }
}