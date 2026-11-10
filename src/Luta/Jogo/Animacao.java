package Luta.Jogo;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Uma animação em loop feita de vários quadros (um PNG por quadro).
 * Os quadros (imagens) podem ser compartilhados entre vários lutadores;
 * o que é só de cada um é o "relógio" da animação.
 */
public class Animacao {

    private final List<BufferedImage> quadros;
    private final double duracaoQuadro;   // segundos que cada quadro fica na tela
    private final double duracaoTotal;
    private double tempo;

    public Animacao(List<BufferedImage> quadros, double duracaoQuadro) {
        if (quadros == null || quadros.isEmpty()) {
            throw new IllegalArgumentException("A animação precisa de pelo menos 1 quadro");
        }
        this.quadros = quadros;
        this.duracaoQuadro = duracaoQuadro;
        this.duracaoTotal = duracaoQuadro * quadros.size();
    }

    // Cada PNG é lido do disco uma única vez e reaproveitado (vários lutadores usam os mesmos quadros).
    private static final Map<String, BufferedImage> CACHE = new HashMap<>();

    /** Lê um PNG de dentro de src (ex.: "/assets/aqua/aqua_guard.png"). */
    public static BufferedImage lerImagem(String caminho) {
        BufferedImage img = CACHE.get(caminho);
        if (img != null) return img;
        URL url = Animacao.class.getResource(caminho);
        if (url == null) {
            throw new IllegalStateException("Sprite não encontrado: " + caminho
                    + "\nConfira se a pasta 'assets' está dentro de 'src' no projeto.");
        }
        try {
            img = ImageIO.read(url);
        } catch (IOException e) {
            throw new IllegalStateException("Erro ao ler o sprite " + caminho, e);
        }
        CACHE.put(caminho, img);
        return img;
    }

    /** Carrega pasta + prefixo + 0..quantidade-1 + ".png". Ex.: ("/assets/aqua/idle/", "aqua_idle", 6). */
    public static List<BufferedImage> carregarQuadros(String pasta, String prefixo, int quantidade) {
        return carregarQuadros(pasta, prefixo, 0, quantidade - 1, 1);
    }

    /**
     * Carrega pasta + prefixo + numero + ".png" para numero de "primeiro" até "ultimo".
     * "digitos" completa com zeros à esquerda (2 -> 00, 01, 02...).
     */
    public static List<BufferedImage> carregarQuadros(String pasta, String prefixo, int primeiro, int ultimo, int digitos) {
        List<BufferedImage> lista = new ArrayList<>();
        for (int i = primeiro; i <= ultimo; i++) {
            lista.add(lerImagem(pasta + prefixo + String.format("%0" + digitos + "d", i) + ".png"));
        }
        return lista;
    }

    public void atualizar(double dt) {
        tempo = (tempo + dt) % duracaoTotal;
    }

    /** Deixa a animação "adiantada" (usado para o clone não ficar sincronizado com o original). */
    public void adiantar(double segundos) {
        tempo = (tempo + segundos) % duracaoTotal;
    }

    /** Quadro correspondente a um progresso de 0 a 1 (usado em golpes: começa no 1º quadro e termina no último). */
    public BufferedImage getQuadroPorProgresso(double progresso) {
        int indice = (int) (Math.max(0, Math.min(0.999, progresso)) * quadros.size());
        return quadros.get(indice);
    }

    /**
     * Escolhe o quadro de um golpe em andamento, "esticando" os quadros para casar com as fases do golpe:
     *   quadros 0 .. qIni-1    -> fase de início (preparação)
     *   quadros qIni .. qFim   -> fase ativa (é aqui que a hitbox existe)
     *   quadros qFim+1 .. fim  -> recuperação
     * Assim o chute aparece na tela exatamente quando o golpe pode acertar.
     */
    public BufferedImage getQuadroSincronizado(double t, double inicio, double ativo, double total, int qIni, int qFim) {
        int n = quadros.size();
        qIni = Math.max(0, Math.min(n - 1, qIni));
        qFim = Math.max(qIni, Math.min(n - 1, qFim));
        int indice;
        if (t < inicio) {
            indice = qIni == 0 ? 0 : (int) (Math.max(0, t) / inicio * qIni);
            indice = Math.min(indice, Math.max(0, qIni - 1));
        } else if (t <= inicio + ativo) {
            int qtd = qFim - qIni + 1;
            indice = qIni + Math.min(qtd - 1, (int) ((t - inicio) / ativo * qtd));
        } else {
            int restantes = n - 1 - qFim;
            if (restantes <= 0) {
                indice = qFim;
            } else {
                double fase = (t - inicio - ativo) / Math.max(1e-6, total - inicio - ativo);
                indice = qFim + 1 + Math.min(restantes - 1, (int) (Math.min(0.999, fase) * restantes));
            }
        }
        return quadros.get(indice);
    }

    public BufferedImage getQuadro() {
        int indice = (int) (tempo / duracaoQuadro);
        return quadros.get(Math.min(indice, quadros.size() - 1));
    }
}
