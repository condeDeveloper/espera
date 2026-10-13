package br.com.conde.espera;

import java.util.ArrayList;
import java.util.List;

/**
 * As MEDIDAS, em tempo SIMULADO. Nunca em segundos de relogio.
 *
 * <p>O tempo simulado e uma funcao da semente: a mesma semente da a mesma fila em qualquer
 * maquina. O tempo de relogio mediria quao rapido o simulador roda, que nao e assunto.
 *
 * <p>E as duracoes sao sempre divididas pelo tempo medio de ATENDIMENTO, para a coluna ser
 * lida sem conhecer a escala: 1,0 quer dizer "esperou o tempo de um atendimento".
 *
 * <p>Toda tabela daqui tem um teste do lado que afirma os mesmos numeros.
 */
public final class Medidas {

    private Medidas() {
    }

    /** Quantos clientes sao medidos em cada ponto das tabelas. */
    public static final int CLIENTES = 1_000_000;

    /** Quantos passam antes, com o contador desligado, para a fila entrar em regime. */
    public static final int AQUECIMENTO = 50_000;

    /** O tempo medio de atendimento, em tiques. E a unidade de todas as colunas de tempo. */
    public static final int ATENDIMENTO = 1_000;

    public static final long SEMENTE = 20_251_012L;

    /** Uma fila de um servidor com a utilizacao pedida e o atendimento pedido. */
    private static Fila umServidor(double rho, Tempos atendimento, Fila.Disciplina disciplina) {
        return new Fila(1, new Tempos.Exponencial(ATENDIMENTO / rho), atendimento, disciplina);
    }

    private static Contador rodar(Fila fila) {
        Contador contador = new Contador();

        fila.simular(CLIENTES, AQUECIMENTO, SEMENTE, contador);

        return contador;
    }

    // ───────────────────── 1. a correcao ─────────────────────

    public record LinhaCorrecao(String fila, String conta, double medido, double previsto,
                                double erro) {
    }

    /**
     * OS DOIS TEOREMAS VALEM EM TODA CONFIGURACAO.
     *
     * <p>A lei de Little nao supoe nada sobre a fila, e os dois lados dela sao medidos por
     * caminhos sem nada em comum: um pela integral da fila no tempo, outro pela media das
     * permanencias.
     */
    public static List<LinhaCorrecao> aCorrecao() {
        List<LinhaCorrecao> linhas = new ArrayList<>();

        var filas = new java.util.LinkedHashMap<String, Fila>();

        filas.put("exponencial 0,5", umServidor(0.5, new Tempos.Exponencial(ATENDIMENTO),
                Fila.Disciplina.CHEGADA));
        filas.put("exponencial 0,9", umServidor(0.9, new Tempos.Exponencial(ATENDIMENTO),
                Fila.Disciplina.CHEGADA));
        filas.put("fixo 0,8", umServidor(0.8, new Tempos.Fixo(ATENDIMENTO),
                Fila.Disciplina.CHEGADA));
        filas.put("cauda pesada 0,8", umServidor(0.8,
                new Tempos.PesadaNaCauda(ATENDIMENTO, 0.01, 50), Fila.Disciplina.CHEGADA));
        filas.put("mais curto 0,9", umServidor(0.9, new Tempos.Exponencial(ATENDIMENTO),
                Fila.Disciplina.MAIS_CURTO_PRIMEIRO));
        filas.put("dois servidores 0,9", new Fila(2,
                new Tempos.Exponencial(ATENDIMENTO / 1.8), new Tempos.Exponencial(ATENDIMENTO),
                Fila.Disciplina.CHEGADA));

        for (var entrada : filas.entrySet()) {
            Contador contador = rodar(entrada.getValue());

            for (Juiz.Veredito veredito
                    : Juiz.tudoQueDaParaConferir(entrada.getValue(), contador, 0.05))
                linhas.add(new LinhaCorrecao(entrada.getKey(), veredito.conta(),
                        veredito.medido(), veredito.previsto(), veredito.erro()));
        }

        return linhas;
    }

    // ───────────────────── 2. a utilizacao ─────────────────────

    public record LinhaUtilizacao(double rho, double esperaMedida, double esperaPrevista,
                                  double naFila, double maiorEspera) {
    }

    /**
     * A FILA POR UTILIZACAO, com um servidor e tudo exponencial.
     *
     * <p>E a curva mais conhecida da area e ela nao e uma reta: a espera e rho sobre um
     * menos rho, e perto de um esse quociente explode.
     *
     * <p>De 50% para 90% de ocupacao, a carga quase dobra e a espera fica NOVE vezes maior.
     * De 90% para 95%, a carga sobe 5% e a espera dobra de novo.
     *
     * <p>A coluna da direita e a maior espera observada, e ela existe porque media nao e
     * experiencia: com rho de 0,9 a media e nove atendimentos e o pior cliente espera
     * dezenas deles.
     */
    public static List<LinhaUtilizacao> aUtilizacao() {
        List<LinhaUtilizacao> linhas = new ArrayList<>();

        for (double rho : new double[] {0.1, 0.3, 0.5, 0.7, 0.8, 0.9, 0.95}) {
            Fila fila = umServidor(rho, new Tempos.Exponencial(ATENDIMENTO),
                    Fila.Disciplina.CHEGADA);

            Contador contador = rodar(fila);

            linhas.add(new LinhaUtilizacao(rho, contador.esperaMedia() / ATENDIMENTO,
                    Juiz.esperaClassica(ATENDIMENTO, rho) / ATENDIMENTO,
                    contador.naFilaMedio(), contador.maiorEspera() / (double) ATENDIMENTO));
        }

        return linhas;
    }

    // ───────────────────── 3. a variabilidade ─────────────────────

    public record LinhaVariabilidade(String atendimento, double segundoMomentoRelativo,
                                     double esperaMedida, double esperaPrevista,
                                     double vezesOFixo) {
    }

    /**
     * A FILA POR VARIABILIDADE, com a MESMA media de atendimento e a MESMA utilizacao.
     *
     * <p>Esta e a tabela que diz o que a de utilizacao nao diz. Tres filas com rho de 0,8 e
     * atendimento medio identico, e esperas que diferem por mais de dez vezes.
     *
     * <p>A formula de Pollaczek e Khinchine explica e preve: a espera e proporcional ao
     * SEGUNDO MOMENTO do atendimento, nao a media dele. A coluna do segundo momento
     * relativo e ele dividido pelo quadrado da media, que e 1 para o fixo e 2 para o
     * exponencial.
     *
     * <p>A consequencia pratica e desconfortavel: tornar o servico mais PREVISIVEL, sem
     * torna-lo mais rapido, corta a fila pela metade. E o inverso tambem: um servico que as
     * vezes demora cinquenta vezes mais faz a fila explodir sem que a media tenha mudado.
     */
    public static List<LinhaVariabilidade> aVariabilidade() {
        List<LinhaVariabilidade> linhas = new ArrayList<>();
        double rho = 0.8;

        var atendimentos = new java.util.LinkedHashMap<String, Tempos>();

        atendimentos.put("fixo", new Tempos.Fixo(ATENDIMENTO));
        atendimentos.put("exponencial", new Tempos.Exponencial(ATENDIMENTO));
        atendimentos.put("1% dez vezes", new Tempos.PesadaNaCauda(ATENDIMENTO, 0.01, 10));
        atendimentos.put("1% cinquenta vezes", new Tempos.PesadaNaCauda(ATENDIMENTO, 0.01, 50));

        double doFixo = 0;

        for (var entrada : atendimentos.entrySet()) {
            Fila fila = umServidor(rho, entrada.getValue(), Fila.Disciplina.CHEGADA);
            Contador contador = rodar(fila);

            double espera = contador.esperaMedia() / ATENDIMENTO;

            if (entrada.getKey().equals("fixo")) doFixo = espera;

            linhas.add(new LinhaVariabilidade(entrada.getKey(),
                    entrada.getValue().segundoMomento()
                            / (entrada.getValue().media() * entrada.getValue().media()),
                    espera, Juiz.esperaPrevista(fila) / ATENDIMENTO, espera / doFixo));
        }

        return linhas;
    }

    // ───────────────────── 4. os servidores ─────────────────────

    public record LinhaServidores(String arranjo, int servidores, double atendimentoMedio,
                                  double esperaMedida, double permanencia, double maiorEspera) {
    }

    /**
     * UM SERVIDOR RAPIDO CONTRA DOIS LENTOS, com a mesma capacidade total.
     *
     * <p>Um caixa que atende em metade do tempo e dois caixas que atendem no tempo normal
     * tem a mesma vazao maxima e a mesma utilizacao. A intuicao diz que da no mesmo.
     *
     * <p>Nao da, e eu escrevi para que lado ia e errei. Eu escrevi que o servidor unico e
     * mais rapido ganha. Ele ganha numa coluna e PERDE na outra, e as duas sao legitimas.
     *
     * <p>Na ESPERA NA FILA, dois servidores normais ganham: 4,008 contra 4,244. Dois
     * servidores aproveitam melhor as rajadas, porque dois clientes que chegam juntos sao
     * atendidos em paralelo em vez de um esperar o outro.
     *
     * <p>Na PERMANENCIA, que soma a espera com o atendimento, o rapido ganha: 4,743 contra
     * 5,005. Ele perde na fila e recupera com juros no atendimento, porque atende em metade
     * do tempo.
     *
     * <p>E a conferencia disso nao e so a simulacao. Para M/M/2 com rho de 0,9 a formula de
     * Erlang da 4,263 atendimentos de espera, e para M/M/1 com o dobro da velocidade a
     * formula classica da 4,500: a teoria concorda que dois servidores esperam menos.
     *
     * <p>Qual das duas colunas importa depende da pergunta. "Quanto tempo ate ser atendido"
     * e a primeira; "quanto tempo ate ir embora" e a segunda. Quase toda discussao sobre
     * isso confunde as duas.
     */
    public static List<LinhaServidores> osServidores() {
        List<LinhaServidores> linhas = new ArrayList<>();
        double rho = 0.9;
        double entreChegadas = ATENDIMENTO / rho;

        var arranjos = new java.util.LinkedHashMap<String, Fila>();

        arranjos.put("um normal", new Fila(1, new Tempos.Exponencial(entreChegadas),
                new Tempos.Exponencial(ATENDIMENTO), Fila.Disciplina.CHEGADA));

        arranjos.put("dois normais", new Fila(2, new Tempos.Exponencial(entreChegadas / 2),
                new Tempos.Exponencial(ATENDIMENTO), Fila.Disciplina.CHEGADA));

        arranjos.put("um dobrado", new Fila(1, new Tempos.Exponencial(entreChegadas / 2),
                new Tempos.Exponencial(ATENDIMENTO / 2.0), Fila.Disciplina.CHEGADA));

        arranjos.put("quatro normais", new Fila(4, new Tempos.Exponencial(entreChegadas / 4),
                new Tempos.Exponencial(ATENDIMENTO), Fila.Disciplina.CHEGADA));

        arranjos.put("um quadruplicado", new Fila(1, new Tempos.Exponencial(entreChegadas / 4),
                new Tempos.Exponencial(ATENDIMENTO / 4.0), Fila.Disciplina.CHEGADA));

        for (var entrada : arranjos.entrySet()) {
            Contador contador = rodar(entrada.getValue());

            linhas.add(new LinhaServidores(entrada.getKey(), entrada.getValue().servidores(),
                    contador.atendimentoMedio() / ATENDIMENTO,
                    contador.esperaMedia() / ATENDIMENTO,
                    contador.permanenciaMedia() / ATENDIMENTO,
                    contador.maiorEspera() / (double) ATENDIMENTO));
        }

        return linhas;
    }

    // ───────────────────── 5. a disciplina ─────────────────────

    public record LinhaDisciplina(String disciplina, String atendimento, double esperaMedia,
                                  double maiorEspera, double naFila) {
    }

    /**
     * A ORDEM DE ATENDIMENTO, com rho de 0,9.
     *
     * <p>Atender o mais curto primeiro minimiza a espera media, e isso e um teorema. O que
     * ela cobra esta na cauda, e a coluna da maior espera e onde isso aparece.
     *
     * <p>Com atendimento de cauda pesada a diferenca fica brutal nas duas direcoes: os
     * pedidos curtos param de esperar atras dos longos, e os longos esperam muito mais.
     */
    public static List<LinhaDisciplina> aDisciplina() {
        List<LinhaDisciplina> linhas = new ArrayList<>();
        double rho = 0.9;

        var atendimentos = new java.util.LinkedHashMap<String, Tempos>();

        atendimentos.put("exponencial", new Tempos.Exponencial(ATENDIMENTO));
        atendimentos.put("1% cinquenta vezes", new Tempos.PesadaNaCauda(ATENDIMENTO, 0.01, 50));

        for (var atendimento : atendimentos.entrySet())
            for (Fila.Disciplina disciplina : Fila.Disciplina.values()) {
                Fila fila = umServidor(rho, atendimento.getValue(), disciplina);
                Contador contador = rodar(fila);

                linhas.add(new LinhaDisciplina(
                        disciplina == Fila.Disciplina.CHEGADA ? "por chegada" : "mais curto",
                        atendimento.getKey(), contador.esperaMedia() / ATENDIMENTO,
                        contador.maiorEspera() / (double) ATENDIMENTO, contador.naFilaMedio()));
            }

        return linhas;
    }
}
