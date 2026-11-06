package Luta;

import Luta.Personagem.Habilidade;
import Luta.Personagem.Personagem;
import Luta.Personagens.Aqua;
import Luta.Personagens.Crys;
import Luta.Personagens.Jiu;
import Luta.Personagens.Keno;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

/**
 * Interface gráfica (Swing) do jogo de luta por turnos.
 * Reaproveita Personagem e Habilidade sem alterá-las.
 * Execute esta classe (ela tem o próprio main).
 */
public class MainLuta extends JFrame {

    // ===== CONFIGURAÇÃO =====
    private static final int DEFENDER = -1;               // "índice" da ação defender
    private static final int ATRASO_CPU_MS = 900;
    private static final double CHANCE_CPU_DEFENDER = 0.25;
    private static final int MAX_HABILIDADES = 4;

    // Para adicionar um personagem novo: inclua uma linha aqui (ex.: Dragon::new)
    private static final List<Supplier<Personagem>> ELENCO = List.of(
            Aqua::new,
            Crys::new,
            Jiu::new,
            Keno::new
    );

    private final Random random = new Random();

    // ===== LAYOUT PRINCIPAL =====
    private final CardLayout cartas = new CardLayout();
    private final JPanel raiz = new JPanel(cartas);

    // ===== TELA DE SELEÇÃO =====
    private final JRadioButton modoPvp = new JRadioButton("Jogador vs Jogador");
    private final JRadioButton modoCpu = new JRadioButton("Jogador vs CPU", true);
    private final JComboBox<String> comboP1 = new JComboBox<>();
    private final JComboBox<String> comboP2 = new JComboBox<>();
    private final JLabel previewP1 = new JLabel();
    private final JLabel previewP2 = new JLabel();
    private final JLabel tituloP2 = new JLabel("", SwingConstants.CENTER);
    private final List<Personagem> exemplos = new ArrayList<>();

    // ===== TELA DE LUTA =====
    private final PainelStatus painel1 = new PainelStatus();
    private final PainelStatus painel2 = new PainelStatus();
    private final JLabel lblVez = new JLabel("", SwingConstants.CENTER);
    private final JTextArea log = new JTextArea();
    private final JButton[] botoesHabilidade = new JButton[MAX_HABILIDADES];
    private final JButton btnDefender = new JButton("Defender");

    // ===== ESTADO DA LUTA =====
    private Lutador atual;
    private Lutador oponente;

    // ===== LUTADOR: personagem + quem controla + painel =====
    private static class Lutador {
        private final Personagem personagem;
        private final boolean cpu;
        private final PainelStatus painel;

        private Lutador(Personagem personagem, boolean cpu, PainelStatus painel) {
            this.personagem = personagem;
            this.cpu = cpu;
            this.painel = painel;
        }
    }

    // ===== CONSTRUÇÃO DA JANELA =====
    public MainLuta() {
        super("Clube da Luta");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        for (Supplier<Personagem> fabrica : ELENCO) {
            exemplos.add(fabrica.get());
        }

        raiz.add(construirTelaSelecao(), "selecao");
        raiz.add(construirTelaLuta(), "luta");
        setContentPane(raiz);

        setSize(860, 620);
        setLocationRelativeTo(null);
        cartas.show(raiz, "selecao");
    }

    private JPanel construirTelaSelecao() {
        JPanel tela = new JPanel(new BorderLayout(10, 10));
        tela.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        // Modo de jogo
        ButtonGroup grupo = new ButtonGroup();
        grupo.add(modoPvp);
        grupo.add(modoCpu);
        modoPvp.addActionListener(e -> atualizarTituloP2());
        modoCpu.addActionListener(e -> atualizarTituloP2());

        JPanel topo = new JPanel(new GridLayout(2, 1, 0, 6));
        JLabel titulo = new JLabel("CLUBE DA LUTA", SwingConstants.CENTER);
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 28f));
        JPanel modos = new JPanel();
        modos.add(modoPvp);
        modos.add(modoCpu);
        topo.add(titulo);
        topo.add(modos);
        tela.add(topo, BorderLayout.NORTH);

        // Seleção de personagens
        for (Personagem p : exemplos) {
            comboP1.addItem(p.getNome());
            comboP2.addItem(p.getNome());
        }
        comboP2.setSelectedIndex(Math.min(1, exemplos.size() - 1));
        comboP1.addActionListener(e -> atualizarPreview(comboP1, previewP1));
        comboP2.addActionListener(e -> atualizarPreview(comboP2, previewP2));

        JPanel colunas = new JPanel(new GridLayout(1, 2, 16, 0));
        colunas.add(construirColuna(new JLabel("Jogador 1", SwingConstants.CENTER), comboP1, previewP1));
        colunas.add(construirColuna(tituloP2, comboP2, previewP2));
        tela.add(colunas, BorderLayout.CENTER);

        // Botão lutar
        JButton btnLutar = new JButton("LUTAR!");
        btnLutar.setFont(btnLutar.getFont().deriveFont(Font.BOLD, 18f));
        btnLutar.addActionListener(e -> iniciarLuta());
        tela.add(btnLutar, BorderLayout.SOUTH);

        atualizarTituloP2();
        atualizarPreview(comboP1, previewP1);
        atualizarPreview(comboP2, previewP2);
        return tela;
    }

    private JPanel construirColuna(JLabel titulo, JComboBox<String> combo, JLabel preview) {
        JPanel coluna = new JPanel(new BorderLayout(6, 6));
        coluna.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 16f));

        JPanel cabecalho = new JPanel(new GridLayout(2, 1, 0, 4));
        cabecalho.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 8));
        cabecalho.add(titulo);
        cabecalho.add(combo);

        preview.setVerticalAlignment(SwingConstants.TOP);
        preview.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        coluna.add(cabecalho, BorderLayout.NORTH);
        coluna.add(preview, BorderLayout.CENTER);
        return coluna;
    }

    private void atualizarTituloP2() {
        tituloP2.setText(modoCpu.isSelected() ? "CPU" : "Jogador 2");
    }

    private void atualizarPreview(JComboBox<String> combo, JLabel preview) {
        int i = combo.getSelectedIndex();
        if (i < 0) {
            return;
        }
        Personagem p = exemplos.get(i);
        StringBuilder sb = new StringBuilder("<html><b>")
                .append(p.getNome()).append("</b> (").append(p.getCor()).append(")<br>")
                .append("HP ").append(p.getVidaMaxima())
                .append(" | SP ").append(p.getEspecialMaximo()).append("<br><br>");
        for (Habilidade h : p.getHabilidades()) {
            sb.append("&bull; ").append(h).append("<br>");
        }
        preview.setText(sb.append("</html>").toString());
    }

    private JPanel construirTelaLuta() {
        JPanel tela = new JPanel(new BorderLayout(8, 8));
        tela.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Topo: vez + status dos dois lutadores
        lblVez.setFont(lblVez.getFont().deriveFont(Font.BOLD, 18f));
        JPanel status = new JPanel(new GridLayout(1, 2, 12, 0));
        status.add(painel1);
        status.add(painel2);
        JPanel topo = new JPanel(new BorderLayout(0, 6));
        topo.add(lblVez, BorderLayout.NORTH);
        topo.add(status, BorderLayout.CENTER);
        tela.add(topo, BorderLayout.NORTH);

        // Centro: log
        log.setEditable(false);
        log.setLineWrap(true);
        log.setWrapStyleWord(true);
        log.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        tela.add(new JScrollPane(log), BorderLayout.CENTER);

        // Base: ações
        JPanel acoes = new JPanel(new GridLayout(1, MAX_HABILIDADES + 1, 8, 0));
        for (int i = 0; i < MAX_HABILIDADES; i++) {
            final int indice = i;
            JButton botao = new JButton();
            botao.addActionListener(e -> executarAcao(indice));
            botoesHabilidade[i] = botao;
            acoes.add(botao);
        }
        btnDefender.addActionListener(e -> executarAcao(DEFENDER));
        acoes.add(btnDefender);
        acoes.setPreferredSize(new java.awt.Dimension(0, 70));
        tela.add(acoes, BorderLayout.SOUTH);

        return tela;
    }

    // ===== FLUXO DA LUTA =====
    private void iniciarLuta() {
        Personagem a = ELENCO.get(comboP1.getSelectedIndex()).get();   // instâncias novas
        Personagem b = ELENCO.get(comboP2.getSelectedIndex()).get();

        Lutador lutadorA = new Lutador(a, false, painel1);
        Lutador lutadorB = new Lutador(b, modoCpu.isSelected(), painel2);

        // sorteio de quem começa
        boolean aComeca = random.nextBoolean();
        atual = aComeca ? lutadorA : lutadorB;
        oponente = aComeca ? lutadorB : lutadorA;

        log.setText("");
        registrar(">>> " + a.getNome() + " VS " + b.getNome() + " <<<");
        registrar("Sorteio: " + atual.personagem.getNome() + " ataca primeiro!");

        cartas.show(raiz, "luta");
        comecarTurno();
    }

    private void comecarTurno() {
        atual.personagem.iniciarTurno();   // a guarda anterior cai no começo do turno do próprio personagem
        atualizarPaineis();
        lblVez.setText("Vez de " + atual.personagem.getNome()
                + (atual.cpu ? " (CPU)" : ""));

        if (atual.cpu) {
            definirBotoesAtivos(false);
            Timer timer = new Timer(ATRASO_CPU_MS, e -> executarAcao(escolherAcaoCpu(atual.personagem)));
            timer.setRepeats(false);
            timer.start();
        } else {
            configurarBotoes(atual.personagem);
        }
    }

    private void executarAcao(int indice) {
        definirBotoesAtivos(false);

        Personagem p = atual.personagem;
        Personagem alvo = oponente.personagem;

        if (indice == DEFENDER) {
            p.defender();
            registrar(p.getNome() + " assume a guarda!");
        } else {
            Habilidade habilidade = p.getHabilidades().get(indice);
            boolean alvoDefendendo = alvo.estaDefendendo();

            int dano = p.usarHabilidade(indice, alvo);

            registrar(p.getNome() + " usou " + habilidade.getNome()
                    + " e causou " + dano + " de dano em " + alvo.getNome()
                    + (alvoDefendendo ? " (defendido!)" : "") + ".");
        }

        atualizarPaineis();

        if (!alvo.estaVivo()) {
            registrar(alvo.getNome() + " foi nocauteado!");
            finalizarLuta(p);
            return;
        }

        // troca quem joga
        Lutador temp = atual;
        atual = oponente;
        oponente = temp;
        comecarTurno();
    }

    private void finalizarLuta(Personagem vencedor) {
        lblVez.setText("VENCEDOR: " + vencedor.getNome() + "!");

        String[] opcoes = {"Jogar novamente", "Sair"};
        int resposta = JOptionPane.showOptionDialog(this,
                "Vencedor: " + vencedor.getNome() + "!",
                "Fim de luta",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null, opcoes, opcoes[0]);

        if (resposta == 1) {
            dispose();
        } else {
            cartas.show(raiz, "selecao");
        }
    }

    // ===== BOTÕES E PAINÉIS =====
    private void configurarBotoes(Personagem p) {
        List<Habilidade> habilidades = p.getHabilidades();
        for (int i = 0; i < MAX_HABILIDADES; i++) {
            JButton botao = botoesHabilidade[i];
            if (i < habilidades.size()) {
                Habilidade h = habilidades.get(i);
                botao.setVisible(true);
                botao.setText("<html><center><b>" + h.getNome() + "</b><br>dano " + h.getDano()
                        + " | custo " + h.getCustoEspecial()
                        + " | ganho " + h.getGanhoEspecial() + "</center></html>");
                botao.setEnabled(podeUsar(p, h));
            } else {
                botao.setVisible(false);
            }
        }
        btnDefender.setEnabled(true);
    }

    private void definirBotoesAtivos(boolean ativo) {
        for (JButton botao : botoesHabilidade) {
            botao.setEnabled(ativo);
        }
        btnDefender.setEnabled(ativo);
    }

    private void atualizarPaineis() {
        atual.painel.atualizar(atual.personagem);
        oponente.painel.atualizar(oponente.personagem);
    }

    private void registrar(String mensagem) {
        log.append(mensagem + "\n");
        log.setCaretPosition(log.getDocument().getLength());
    }

    private static boolean podeUsar(Personagem p, Habilidade h) {
        return p.getEspecial() >= h.getCustoEspecial();
    }

    // ===== IA DA CPU =====
    private int escolherAcaoCpu(Personagem p) {
        List<Habilidade> habilidades = p.getHabilidades();

        // 1) Se consegue soltar o especial (golpe com custo), solta.
        for (int i = 0; i < habilidades.size(); i++) {
            Habilidade h = habilidades.get(i);
            if (h.getCustoEspecial() > 0 && podeUsar(p, h)) {
                return i;
            }
        }

        // 2) Às vezes defende.
        if (random.nextDouble() < CHANCE_CPU_DEFENDER) {
            return DEFENDER;
        }

        // 3) Senão, golpe básico aleatório.
        List<Integer> disponiveis = new ArrayList<>();
        for (int i = 0; i < habilidades.size(); i++) {
            if (podeUsar(p, habilidades.get(i))) {
                disponiveis.add(i);
            }
        }
        return disponiveis.isEmpty()
                ? DEFENDER
                : disponiveis.get(random.nextInt(disponiveis.size()));
    }

    // ===== COR DO PERSONAGEM (a partir do texto de getCor) =====
    private static Color corPorNome(String cor) {
        if (cor == null) {
            return new Color(210, 210, 210);
        }
        switch (cor.toLowerCase()) {
            case "cinza":    return new Color(176, 176, 176);
            case "rosa":     return new Color(255, 150, 190);
            case "azul":     return new Color(110, 165, 255);
            case "amarelo":  return new Color(255, 220, 80);
            default:         return new Color(210, 210, 210);
        }
    }

    // ===== PAINEL DE STATUS (nome + HP + SP + guarda) =====
    private static class PainelStatus extends JPanel {
        private final JLabel nome = new JLabel("", SwingConstants.CENTER);
        private final JProgressBar vida = new JProgressBar();
        private final JProgressBar especial = new JProgressBar();
        private final JLabel guarda = new JLabel(" ", SwingConstants.CENTER);

        private PainelStatus() {
            setLayout(new GridLayout(4, 1, 0, 4));
            setBorder(BorderFactory.createLineBorder(Color.GRAY));

            nome.setOpaque(true);
            nome.setForeground(Color.BLACK);
            nome.setFont(nome.getFont().deriveFont(Font.BOLD, 18f));

            vida.setStringPainted(true);
            vida.setForeground(new Color(60, 170, 80));
            especial.setStringPainted(true);
            especial.setForeground(new Color(70, 110, 220));
            guarda.setFont(guarda.getFont().deriveFont(Font.BOLD));

            add(nome);
            add(vida);
            add(especial);
            add(guarda);
        }

        private void atualizar(Personagem p) {
            nome.setText(p.getNome());
            nome.setBackground(corPorNome(p.getCor()));

            vida.setMaximum(p.getVidaMaxima());
            vida.setValue(p.getVida());
            vida.setString("HP " + p.getVida() + "/" + p.getVidaMaxima());

            especial.setMaximum(Math.max(1, p.getEspecialMaximo()));
            especial.setValue(p.getEspecial());
            especial.setString("SP " + p.getEspecial() + "/" + p.getEspecialMaximo());

            guarda.setText(p.estaDefendendo() ? "[DEFENDENDO]" : " ");
        }
    }

    // ===== PONTO DE ENTRADA =====
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception ignorado) {
                // segue com o visual padrão
            }
            new MainLuta().setVisible(true);
        });
    }
}