package br.com.conde.espera;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Os numeros do README, afirmados.
 *
 * <p>Um README e texto: ele nao quebra quando deixa de ser verdade. Cada afirmacao das
 * tabelas esta repetida aqui para quebrar.
 */
class MedidasTest {

    /* As tabelas sao calculadas uma vez so: cada uma simula milhoes de clientes. */
    private static final List<Medidas.LinhaUtilizacao> UTILIZACAO = Medidas.aUtilizacao();
    private static final List<Medidas.LinhaVariabilidade> VARIABILIDADE = Medidas.aVariabilidade();
    private static final List<Medidas.LinhaServidores> SERVIDORES = Medidas.osServidores();
    private static final List<Medidas.LinhaDisciplina> DISCIPLINA = Medidas.aDisciplina();

    // ───────────────────── 1. a correcao ─────────────────────

    @Test
    @DisplayName("os dois teoremas valem em toda configuracao")
    void osTeoremasValem() {
        List<Medidas.LinhaCorrecao> linhas = Medidas.aCorrecao();

        assertThat(linhas).hasSize(28);
        assertThat(linhas).allSatisfy(l ->
                assertThat(l.erro()).as(l.fila() + ", " + l.conta()).isLessThan(0.05));

        /*
         * A LEI DE LITTLE BATE EXATAMENTE, e nao aproximadamente. Os dois lados dela sao
         * medidos por caminhos sem nada em comum, e ainda assim o erro e zero ate a quinta
         * casa: e assim que se sabe que a contabilidade do tempo simulado esta certa.
         */
        assertThat(linhas).filteredOn(l -> l.conta().startsWith("L = "))
                .allSatisfy(l -> assertThat(l.erro()).as(l.fila()).isLessThan(0.00001));

        assertThat(linhas).filteredOn(l -> l.conta().startsWith("Lq = "))
                .allSatisfy(l -> assertThat(l.erro()).as(l.fila()).isLessThan(0.00001));
    }

    // ───────────────────── 2. a utilizacao ─────────────────────

    /**
     * A CURVA QUE EXPLODE, conferida contra a formula em todos os pontos.
     *
     * <p>De 50% para 90% de ocupacao, a carga quase dobra e a espera fica nove vezes maior.
     */
    @Test
    @DisplayName("a espera bate com rho sobre um menos rho em toda a faixa")
    void aEsperaSegueAFormula() {
        assertThat(UTILIZACAO).allSatisfy(l ->
                assertThat(l.esperaMedida())
                        .as("rho " + l.rho())
                        .isCloseTo(l.esperaPrevista(), within(l.esperaPrevista() * 0.03)));

        assertThat(utilizacao(0.5).esperaMedida()).isCloseTo(1.001, within(0.001));
        assertThat(utilizacao(0.9).esperaMedida()).isCloseTo(8.831, within(0.001));
        assertThat(utilizacao(0.95).esperaMedida()).isCloseTo(18.821, within(0.001));
    }

    @Test
    @DisplayName("dobrar a carga de 0,5 para 0,9 multiplica a espera por nove")
    void aExplosao() {
        assertThat(utilizacao(0.9).esperaMedida() / utilizacao(0.5).esperaMedida())
                .isCloseTo(8.8, within(0.2));

        assertThat(utilizacao(0.95).esperaMedida() / utilizacao(0.9).esperaMedida())
                .as("e de 0,9 para 0,95, cinco por cento de carga dobra a espera")
                .isCloseTo(2.1, within(0.1));
    }

    /**
     * MEDIA NAO E EXPERIENCIA.
     *
     * <p>Com rho de 0,9 a media e nove atendimentos e o pior cliente espera oitenta e tres.
     */
    @Test
    @DisplayName("a maior espera e quase dez vezes a media, em toda a faixa")
    void aMediaNaoEAExperiencia() {
        assertThat(utilizacao(0.9).maiorEspera()).isCloseTo(82.7, within(0.1));

        assertThat(utilizacao(0.9).maiorEspera() / utilizacao(0.9).esperaMedida())
                .isGreaterThan(9.0);

        assertThat(utilizacao(0.1).maiorEspera())
                .as("e mesmo com dez por cento de carga alguem espera doze atendimentos")
                .isGreaterThan(12.0);
    }

    private static Medidas.LinhaUtilizacao utilizacao(double rho) {
        return UTILIZACAO.stream()
                .filter(l -> Math.abs(l.rho() - rho) < 1e-9)
                .findFirst()
                .orElseThrow();
    }

    // ───────────────────── 3. a variabilidade ─────────────────────

    /**
     * A AFIRMACAO CENTRAL DO REPOSITORIO.
     *
     * <p>Quatro filas com a MESMA media de atendimento e a MESMA utilizacao, e esperas que
     * diferem por doze vezes. Quem decide e o segundo momento, nao a media.
     */
    @Test
    @DisplayName("mesma media e mesma carga, e doze vezes de diferenca na espera")
    void aVariabilidadeDecide() {
        assertThat(variabilidade("fixo").esperaMedida()).isCloseTo(2.011, within(0.001));
        assertThat(variabilidade("exponencial").esperaMedida()).isCloseTo(3.967, within(0.001));
        assertThat(variabilidade("1% cinquenta vezes").esperaMedida())
                .isCloseTo(23.971, within(0.001));

        assertThat(variabilidade("1% cinquenta vezes").vezesOFixo())
                .isCloseTo(11.92, within(0.01));

        assertThat(variabilidade("exponencial").vezesOFixo())
                .as("e o fixo corta a espera do exponencial pela metade, de graca")
                .isCloseTo(1.97, within(0.01));
    }

    /**
     * E A ORDEM SEGUE O SEGUNDO MOMENTO, e nao a intuicao sobre caudas.
     *
     * <p>A distribuicao de cauda com 1% dez vezes mais longo tem segundo momento 1,67, que
     * e MENOR que o 2,00 do exponencial, e a fila dela e menor tambem. Ter cauda nao basta:
     * o que conta e quanto ela pesa no quadrado.
     */
    @Test
    @DisplayName("a ordem das esperas segue a ordem dos segundos momentos, sem excecao")
    void aOrdemSegueOSegundoMomento() {
        List<Medidas.LinhaVariabilidade> porMomento = VARIABILIDADE.stream()
                .sorted((a, b) -> Double.compare(a.segundoMomentoRelativo(),
                        b.segundoMomentoRelativo()))
                .toList();

        for (int i = 1; i < porMomento.size(); i++)
            assertThat(porMomento.get(i).esperaMedida())
                    .as(porMomento.get(i).atendimento() + " tem segundo momento maior")
                    .isGreaterThan(porMomento.get(i - 1).esperaMedida());

        assertThat(variabilidade("1% dez vezes").segundoMomentoRelativo())
                .as("uma cauda pode ter segundo momento MENOR que o exponencial")
                .isLessThan(variabilidade("exponencial").segundoMomentoRelativo());

        assertThat(variabilidade("1% dez vezes").esperaMedida())
                .as("e ai a fila dela e menor tambem")
                .isLessThan(variabilidade("exponencial").esperaMedida());
    }

    @Test
    @DisplayName("a formula de Pollaczek-Khinchine preve cada uma das quatro")
    void aFormulaPreveTodas() {
        assertThat(VARIABILIDADE).allSatisfy(l ->
                assertThat(l.esperaMedida())
                        .as(l.atendimento())
                        .isCloseTo(l.esperaPrevista(), within(l.esperaPrevista() * 0.04)));
    }

    private static Medidas.LinhaVariabilidade variabilidade(String atendimento) {
        return VARIABILIDADE.stream()
                .filter(l -> l.atendimento().equals(atendimento))
                .findFirst()
                .orElseThrow();
    }

    // ───────────────────── 4. os servidores ─────────────────────

    /**
     * A CORRECAO: nao ha um vencedor, ha dois, um em cada coluna.
     *
     * <p>Eu escrevi que o servidor unico e mais rapido ganha. Ele ganha na permanencia e
     * PERDE na espera.
     */
    @Test
    @DisplayName("dois servidores normais esperam menos que um com o dobro da velocidade")
    void doisLentosEsperamMenos() {
        assertThat(servidores("dois normais").esperaMedida()).isCloseTo(4.174, within(0.001));
        assertThat(servidores("um dobrado").esperaMedida()).isCloseTo(4.415, within(0.001));

        assertThat(servidores("dois normais").esperaMedida())
                .as("dois servidores aproveitam melhor as rajadas")
                .isLessThan(servidores("um dobrado").esperaMedida());

        assertThat(servidores("quatro normais").esperaMedida())
                .as("e com quatro contra um quadruplicado e a mesma coisa")
                .isLessThan(servidores("um quadruplicado").esperaMedida());
    }

    @Test
    @DisplayName("e o rapido ganha na permanencia, que soma o atendimento")
    void oRapidoGanhaNaPermanencia() {
        assertThat(servidores("um dobrado").permanencia()).isCloseTo(4.915, within(0.001));
        assertThat(servidores("dois normais").permanencia()).isCloseTo(5.173, within(0.001));

        assertThat(servidores("um dobrado").permanencia())
                .isLessThan(servidores("dois normais").permanencia());

        assertThat(servidores("um quadruplicado").permanencia())
                .isLessThan(servidores("quatro normais").permanencia());

        assertThat(servidores("um dobrado").atendimentoMedio())
                .as("porque ele atende em metade do tempo")
                .isCloseTo(0.5, within(0.01));
    }

    @Test
    @DisplayName("e tanto dobrar quanto duplicar cortam a espera pela metade")
    void qualquerDosDoisCortaPelaMetade() {
        assertThat(servidores("um normal").esperaMedida() / servidores("dois normais").esperaMedida())
                .isCloseTo(2.1, within(0.1));

        assertThat(servidores("um normal").esperaMedida() / servidores("um dobrado").esperaMedida())
                .isCloseTo(2.0, within(0.1));
    }

    private static Medidas.LinhaServidores servidores(String arranjo) {
        return SERVIDORES.stream()
                .filter(l -> l.arranjo().equals(arranjo))
                .findFirst()
                .orElseThrow();
    }

    // ───────────────────── 5. a disciplina ─────────────────────

    /**
     * O MAIS CURTO PRIMEIRO TROCA MEDIA POR CAUDA, e a troca e brutal nos dois sentidos.
     */
    @Test
    @DisplayName("o mais curto primeiro corta a media por tres e multiplica a cauda por treze")
    void aTrocaDeMediaPorCauda() {
        assertThat(disciplina("por chegada", "exponencial").esperaMedia())
                .isCloseTo(8.831, within(0.001));

        assertThat(disciplina("mais curto", "exponencial").esperaMedia())
                .isCloseTo(3.165, within(0.001));

        assertThat(disciplina("por chegada", "exponencial").esperaMedia()
                / disciplina("mais curto", "exponencial").esperaMedia())
                .as("a media cai por quase tres")
                .isGreaterThan(2.7);

        assertThat(disciplina("mais curto", "exponencial").maiorEspera())
                .isCloseTo(1147.4, within(0.1));

        assertThat(disciplina("mais curto", "exponencial").maiorEspera()
                / disciplina("por chegada", "exponencial").maiorEspera())
                .as("e a maior espera sobe por treze")
                .isGreaterThan(13.0);
    }

    @Test
    @DisplayName("com cauda pesada a troca fica ainda maior nos dois lados")
    void comCaudaPesadaATrocaCresce() {
        assertThat(disciplina("por chegada", "1% cinquenta vezes").esperaMedia())
                .isCloseTo(55.314, within(0.001));

        assertThat(disciplina("mais curto", "1% cinquenta vezes").esperaMedia())
                .isCloseTo(14.320, within(0.001));

        assertThat(disciplina("por chegada", "1% cinquenta vezes").esperaMedia()
                / disciplina("mais curto", "1% cinquenta vezes").esperaMedia())
                .as("os pedidos curtos param de esperar atras dos longos")
                .isGreaterThan(3.8);

        assertThat(disciplina("mais curto", "1% cinquenta vezes").maiorEspera())
                .isGreaterThan(disciplina("por chegada", "1% cinquenta vezes").maiorEspera() * 2);
    }

    @Test
    @DisplayName("a lei de Little continua valendo com o mais curto primeiro")
    void littleValeComQualquerOrdem() {
        for (var linha : DISCIPLINA)
            assertThat(linha.naFila())
                    .as(linha.disciplina() + " em " + linha.atendimento())
                    .isCloseTo(linha.esperaMedia() * 0.9, within(linha.esperaMedia() * 0.03));
    }

    private static Medidas.LinhaDisciplina disciplina(String disciplina, String atendimento) {
        return DISCIPLINA.stream()
                .filter(l -> l.disciplina().equals(disciplina)
                        && l.atendimento().equals(atendimento))
                .findFirst()
                .orElseThrow();
    }
}
