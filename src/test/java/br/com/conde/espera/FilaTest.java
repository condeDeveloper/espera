package br.com.conde.espera;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FilaTest {

    private static final int ATENDIMENTO = 1_000;

    // ───────────────────── o juiz contra toda configuracao ─────────────────────

    /**
     * A LEI DE LITTLE E O JUIZ, e ela nao supoe nada sobre a fila.
     *
     * <p>Os dois lados dela sao medidos por caminhos sem nada em comum: L sai da integral
     * da fila no tempo, W sai da media das permanencias. Um erro na contabilidade do tempo
     * simulado quebra um e nao o outro.
     */
    @Test
    @DisplayName("a lei de Little vale em toda configuracao, com erro abaixo de um por cento")
    void littleValeSempre() {
        List<String> falhas = new ArrayList<>();
        int conferidas = 0;

        for (int servidores : new int[] {1, 2, 5})
            for (double rho : new double[] {0.2, 0.6, 0.9})
                for (Tempos atendimento : List.of(
                        new Tempos.Fixo(ATENDIMENTO),
                        new Tempos.Exponencial(ATENDIMENTO),
                        new Tempos.PesadaNaCauda(ATENDIMENTO, 0.02, 30)))
                    for (Fila.Disciplina disciplina : Fila.Disciplina.values()) {
                        Fila fila = new Fila(servidores,
                                new Tempos.Exponencial(ATENDIMENTO / (rho * servidores)),
                                atendimento, disciplina);

                        Contador contador = new Contador();

                        fila.simular(40_000, 5_000, 7, contador);

                        for (Juiz.Veredito veredito
                                : new Juiz.Veredito[] {Juiz.little(contador, 0.01),
                                Juiz.littleNaFila(contador, 0.01)}) {

                            conferidas++;

                            if (!veredito.passou())
                                falhas.add(fila.nome() + " com rho " + rho + ", "
                                        + veredito.conta() + ": erro " + veredito.erro());
                        }
                    }

        assertThat(conferidas).isGreaterThan(100);
        assertThat(falhas).isEmpty();
    }

    @Test
    @DisplayName("a utilizacao medida bate com a prevista pelas medias")
    void aUtilizacaoBate() {
        for (int servidores : new int[] {1, 3})
            for (double rho : new double[] {0.3, 0.7, 0.9}) {
                Fila fila = new Fila(servidores,
                        new Tempos.Exponencial(ATENDIMENTO / (rho * servidores)),
                        new Tempos.Exponencial(ATENDIMENTO), Fila.Disciplina.CHEGADA);

                Contador contador = new Contador();

                fila.simular(100_000, 10_000, 3, contador);

                assertThat(contador.utilizacao(servidores))
                        .as(servidores + " servidores com rho " + rho)
                        .isCloseTo(rho, within(0.02));
            }
    }

    /**
     * A FORMULA DE POLLACZEK E KHINCHINE, conferida nas tres distribuicoes.
     *
     * <p>Ela diz que a espera depende do SEGUNDO MOMENTO do atendimento. Tres
     * distribuicoes com a mesma media e segundos momentos diferentes tem que dar tres
     * esperas diferentes, cada uma batendo com a propria previsao.
     */
    @Test
    @DisplayName("a espera prevista por Pollaczek-Khinchine bate em toda distribuicao")
    void pollaczekBate() {
        for (double rho : new double[] {0.3, 0.6, 0.8})
            for (Tempos atendimento : List.of(
                    new Tempos.Fixo(ATENDIMENTO),
                    new Tempos.Exponencial(ATENDIMENTO),
                    new Tempos.PesadaNaCauda(ATENDIMENTO, 0.01, 10))) {

                Fila fila = new Fila(1, new Tempos.Exponencial(ATENDIMENTO / rho),
                        atendimento, Fila.Disciplina.CHEGADA);

                Contador contador = new Contador();

                fila.simular(300_000, 20_000, 5, contador);

                assertThat(contador.esperaMedia())
                        .as(atendimento.nome() + " com rho " + rho)
                        .isCloseTo(Juiz.esperaPrevista(fila), within(Juiz.esperaPrevista(fila) * 0.1));
            }
    }

    @Test
    @DisplayName("a formula e recusada onde ela nao vale, em vez de dar numero errado")
    void aFormulaSabeOndeNaoVale() {
        Fila doisServidores = new Fila(2, new Tempos.Exponencial(1_000),
                new Tempos.Exponencial(1_000), Fila.Disciplina.CHEGADA);

        assertThatThrownBy(() -> Juiz.esperaPrevista(doisServidores))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("um servidor so");

        Fila maisCurto = new Fila(1, new Tempos.Exponencial(2_000),
                new Tempos.Exponencial(1_000), Fila.Disciplina.MAIS_CURTO_PRIMEIRO);

        assertThatThrownBy(() -> Juiz.esperaPrevista(maisCurto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duracao do servico");
    }

    // ───────────────────── o que a fila promete sobre si ─────────────────────

    @Test
    @DisplayName("utilizacao de um ou mais e recusada: a fila cresce para sempre")
    void utilizacaoImpossivel() {
        Fila cheia = new Fila(1, new Tempos.Fixo(1_000), new Tempos.Fixo(1_000),
                Fila.Disciplina.CHEGADA);

        assertThat(cheia.utilizacaoPrevista()).isEqualTo(1.0);

        assertThatThrownBy(() -> cheia.simular(100, 0, 1, new Contador()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cresce para sempre");

        assertThatThrownBy(() -> new Fila(0, new Tempos.Fixo(1), new Tempos.Fixo(1),
                Fila.Disciplina.CHEGADA))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * UMA FILA SEM ESPERA NENHUMA, que e o caso em que toda conta tem que dar zero.
     *
     * <p>Com atendimento fixo e chegadas espacadas o bastante, ninguem nunca espera. E um
     * teste de sanidade: se a espera medida nao for zero aqui, a contabilidade esta somando
     * coisa que nao existe.
     */
    @Test
    @DisplayName("com chegadas bem espacadas e atendimento fixo, ninguem espera")
    void filaQueNuncaEnche() {
        Fila fila = new Fila(1, new Tempos.Fixo(1_000), new Tempos.Fixo(100),
                Fila.Disciplina.CHEGADA);

        Contador contador = new Contador();

        fila.simular(5_000, 100, 1, contador);

        assertThat(contador.esperaMedia()).isZero();
        assertThat(contador.maiorEspera()).isZero();
        assertThat(contador.naFilaMedio()).isZero();
        assertThat(contador.atendimentoMedio()).isEqualTo(100.0);
        assertThat(contador.permanenciaMedia()).isEqualTo(100.0);

        assertThat(contador.utilizacao(1))
                .as("e o servidor fica ocupado um decimo do tempo")
                .isCloseTo(0.1, within(0.001));

        assertThat(contador.noSistemaMedio()).isCloseTo(0.1, within(0.001));
    }

    /**
     * O AQUECIMENTO IMPORTA, E SO APARECE SE FOR PROCURADO NO LUGAR CERTO.
     *
     * <p>A primeira versao deste teste comparava cinco mil clientes sem aquecimento com
     * cinco mil depois de aquecer, com uma semente so, e deu o CONTRARIO do esperado: 9.221
     * contra 8.091. Nao porque o aquecimento nao importa, e sim porque com rho de 0,9 a
     * media de cinco mil clientes varia mais do que o efeito que se queria medir. Duas
     * janelas quaisquer da mesma fila ja diferem assim.
     *
     * <p>O efeito esta nos PRIMEIROS clientes, e para ve-lo e preciso olhar poucos deles e
     * media-los sobre varias sementes. Ai ele aparece sem ambiguidade.
     */
    @Test
    @DisplayName("os primeiros clientes de uma fila vazia esperam muito menos que os de regime")
    void oAquecimentoImporta() {
        double somaDoComeco = 0;
        double somaDoRegime = 0;
        int sementes = 20;

        for (int semente = 1; semente <= sementes; semente++) {
            Fila fila = new Fila(1, new Tempos.Exponencial(ATENDIMENTO / 0.9),
                    new Tempos.Exponencial(ATENDIMENTO), Fila.Disciplina.CHEGADA);

            Contador doComeco = new Contador();
            Contador doRegime = new Contador();

            fila.simular(200, 0, semente, doComeco);
            fila.simular(200, 100_000, semente, doRegime);

            somaDoComeco += doComeco.esperaMedia();
            somaDoRegime += doRegime.esperaMedia();
        }

        assertThat(somaDoComeco / sementes)
                .as("comecar com a fila vazia puxa a media para baixo")
                .isLessThan(somaDoRegime / sementes * 0.8);
    }

    @Test
    @DisplayName("a lei de Little vale com e sem aquecimento, porque ela nao supoe regime")
    void littleNaoSupoeRegime() {
        Fila fila = new Fila(1, new Tempos.Exponencial(ATENDIMENTO / 0.9),
                new Tempos.Exponencial(ATENDIMENTO), Fila.Disciplina.CHEGADA);

        Contador semAquecimento = new Contador();
        Contador comAquecimento = new Contador();

        fila.simular(5_000, 0, 11, semAquecimento);
        fila.simular(5_000, 50_000, 11, comAquecimento);

        assertThat(Juiz.little(comAquecimento, 0.02).passou()).isTrue();
        assertThat(Juiz.little(semAquecimento, 0.02).passou()).isTrue();

        assertThat(semAquecimento.esperaMedia())
                .as("e as duas janelas dao medias bem diferentes, que e o ruido em rho alto")
                .isNotCloseTo(comAquecimento.esperaMedia(),
                        within(comAquecimento.esperaMedia() * 0.1));
    }

    @Test
    @DisplayName("o mais curto primeiro reduz a media e estica a cauda")
    void oMaisCurtoTrocaMediaPorCauda() {
        Tempos atendimento = new Tempos.PesadaNaCauda(ATENDIMENTO, 0.01, 50);

        Contador porChegada = new Contador();
        Contador maisCurto = new Contador();

        new Fila(1, new Tempos.Exponencial(ATENDIMENTO / 0.9), atendimento,
                Fila.Disciplina.CHEGADA).simular(100_000, 10_000, 13, porChegada);

        new Fila(1, new Tempos.Exponencial(ATENDIMENTO / 0.9), atendimento,
                Fila.Disciplina.MAIS_CURTO_PRIMEIRO).simular(100_000, 10_000, 13, maisCurto);

        assertThat(maisCurto.esperaMedia())
                .as("a media cai de verdade, e isso e um teorema")
                .isLessThan(porChegada.esperaMedia() / 2);

        assertThat(maisCurto.maiorEspera())
                .as("e a maior espera piora, que e o preco")
                .isGreaterThan(porChegada.maiorEspera());

        assertThat(maisCurto.atendimentoMedio())
                .as("o atendimento medio nao muda: e o mesmo conjunto de clientes")
                .isCloseTo(porChegada.atendimentoMedio(), within(1.0));
    }

    // ───────────────────── as distribuicoes ─────────────────────

    @Test
    @DisplayName("cada distribuicao tem a media e o segundo momento que promete")
    void asDistribuicoesSaoOQueDizem() {
        Tempos.Sorteio sorteio = new Tempos.Sorteio(1);

        for (Tempos tempos : List.of(
                new Tempos.Fixo(1_000),
                new Tempos.Exponencial(1_000),
                new Tempos.PesadaNaCauda(1_000, 0.01, 50))) {

            double soma = 0;
            double somaDosQuadrados = 0;
            int quantos = 500_000;

            for (int i = 0; i < quantos; i++) {
                long duracao = tempos.proximo(sorteio);

                soma += duracao;
                somaDosQuadrados += (double) duracao * duracao;
            }

            assertThat(soma / quantos).as(tempos.nome() + ": a media")
                    .isCloseTo(tempos.media(), within(tempos.media() * 0.03));

            assertThat(somaDosQuadrados / quantos).as(tempos.nome() + ": o segundo momento")
                    .isCloseTo(tempos.segundoMomento(), within(tempos.segundoMomento() * 0.1));
        }
    }

    @Test
    @DisplayName("o segundo momento do fixo e um, o do exponencial e dois")
    void osSegundosMomentosConhecidos() {
        Tempos fixo = new Tempos.Fixo(1_000);
        Tempos exponencial = new Tempos.Exponencial(1_000);

        assertThat(fixo.segundoMomento() / (fixo.media() * fixo.media())).isEqualTo(1.0);

        assertThat(exponencial.segundoMomento()
                / (exponencial.media() * exponencial.media())).isEqualTo(2.0);

        Tempos.PesadaNaCauda pesada = new Tempos.PesadaNaCauda(1_000, 0.01, 50);

        assertThat(pesada.segundoMomento() / (pesada.media() * pesada.media()))
                .as("e a cauda pesada tem onze vezes o do fixo, com a mesma media")
                .isCloseTo(11.71, within(0.01));

        assertThat(pesada.curta() * 0.99 + pesada.longa() * 0.01)
                .as("as duas duracoes dao a media pedida")
                .isCloseTo(1_000.0, within(0.01));
    }

    @Test
    @DisplayName("nenhuma duracao e zero, porque zero quebraria a fila")
    void nenhumaDuracaoEZero() {
        Tempos.Sorteio sorteio = new Tempos.Sorteio(1);
        Tempos curto = new Tempos.Exponencial(1.0);

        for (int i = 0; i < 100_000; i++)
            assertThat(curto.proximo(sorteio)).isGreaterThanOrEqualTo(1);
    }

    // ───────────────────── o sorteio da casa ─────────────────────

    /**
     * O TESTE DO DESLOCAMENTO DE 32.
     *
     * <p>Aqui ele tem uma consequencia propria: uma sequencia de chegadas com estrutura
     * deixa de ser um processo sem memoria. A lei de Little continuaria valendo, porque ela
     * nao supoe nada, e as formulas de fila parariam de bater. O repositorio teria dois
     * juizes discordando por causa do gerador.
     */
    @Test
    @DisplayName("o deslocamento de 32 e o que faz as chegadas nao terem memoria")
    void oDeslocamentoDeTrintaEDois() {
        Tempos.Sorteio sorteio = new Tempos.Sorteio(1);
        int[] quantos = new int[10];

        for (int i = 0; i < 100_000; i++) quantos[(int) (sorteio.fracao() * 10)]++;

        for (int faixa = 0; faixa < 10; faixa++)
            assertThat(quantos[faixa]).as("faixa " + faixa).isBetween(9_600, 10_400);

        long estado = 1;
        int alternancias = 0;
        int anterior = -1;

        for (int i = 0; i < 1_000; i++) {
            estado = estado * 6364136223846793005L + 1442695040888963407L;

            int baixo = (int) (estado & 1);

            if (anterior >= 0 && baixo != anterior) alternancias++;

            anterior = baixo;
        }

        assertThat(alternancias)
                .as("o bit baixo do estado cru alterna em TODAS: isso e memoria")
                .isEqualTo(999);
    }

    @Test
    @DisplayName("a fracao nunca e zero nem um, porque o logaritmo de zero nao existe")
    void aFracaoEAberta() {
        Tempos.Sorteio sorteio = new Tempos.Sorteio(7);

        for (int i = 0; i < 200_000; i++) {
            double fracao = sorteio.fracao();

            assertThat(fracao).isGreaterThan(0.0).isLessThan(1.0);
        }
    }
}
