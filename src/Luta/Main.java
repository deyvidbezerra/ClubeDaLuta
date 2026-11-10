package Luta;

import Luta.Jogo.MenuSelecao;
import Luta.Jogo.PainelJogo;
import Luta.Personagem.Personagem;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.util.function.Supplier;

/**
 * Ponto de entrada: abre o menu de seleção de personagem e, depois que os dois jogadores escolhem,
 * troca para a luta. Na luta, M (ou ESC) volta ao menu.
 */
public class Main {

    public static void main(String[] args) {
        // Swing deve ser criado na thread de eventos (EDT)
        SwingUtilities.invokeLater(() -> {
            try {
                JFrame janela = new JFrame("Clube da Luta");
                janela.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

                MenuSelecao menu = criarMenu(janela);
                janela.setContentPane(menu);
                janela.pack();                       // usa o tamanho preferido do painel (1000x600)
                janela.setLocationRelativeTo(null);  // centraliza na tela
                janela.setVisible(true);

                menu.iniciar();
            } catch (IllegalStateException e) {
                JOptionPane.showMessageDialog(null, e.getMessage(), "Erro ao iniciar", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private static MenuSelecao criarMenu(JFrame janela) {
        return new MenuSelecao((p1, p2) -> iniciarLuta(janela, p1, p2));
    }

    private static void mostrarMenu(JFrame janela) {
        MenuSelecao menu = criarMenu(janela);
        trocarTela(janela, menu);
        menu.iniciar();
    }

    private static void iniciarLuta(JFrame janela, Supplier<Personagem> p1, Supplier<Personagem> p2) {
        try {
            PainelJogo painel = new PainelJogo(p1, p2, () -> mostrarMenu(janela));
            trocarTela(janela, painel);
            painel.iniciar();
        } catch (IllegalStateException e) {
            JOptionPane.showMessageDialog(janela, e.getMessage(), "Erro ao iniciar a luta", JOptionPane.ERROR_MESSAGE);
            mostrarMenu(janela);
        }
    }

    private static void trocarTela(JFrame janela, javax.swing.JPanel painel) {
        janela.setContentPane(painel);
        janela.revalidate();
        janela.repaint();
    }
}
