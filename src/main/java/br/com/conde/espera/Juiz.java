package br.com.conde.espera;

/**
 * O JUIZ: dois TEOREMAS.
 *
 * <p>Este repositorio nao tem como ter um juiz do jeito dos outros. Nao existe uma "fila de
 * forca bruta" que de a resposta certa e devagar: a fila simulada E a resposta, e comparar
 * uma simulacao com outra simulacao nao prova nada.
 *
 * <p>Entao o juiz e a matematica que vale para qualquer fila, independente de como ela foi
 * escrita.
 *
 * <h2>A lei de Little</h2>
 *
 * <p>L = lambda vezes W. Quantos clientes ha em media no sistema e igual a quantos chegam
 * por unidade de tempo vezes quanto tempo cada um fica.
 *
 * <p>Ela e extraordinariamente geral: vale para qualquer distribuicao de chegada, qualquer
 * distribuicao de atendimento, qualquer numero de servidores e qualquer ordem de
 * atendimento. Ela nao supoe NADA sobre a fila, e e por isso que ela serve de juiz aqui.
 *
 * <p>E os dois lados dela sao medidos por caminhos que nao tem nada em comum. L sai da
 * integral da fila no tempo; W sai da media das permanencias dos clientes. Um erro na
 * contabilidade do tempo quebraria um e nao o outro.
 *
 * <h2>Pollaczek e Khinchine</h2>
 *
 * <p>Para um servidor so e chegadas exponenciais, a espera media na fila e lambda vezes o
 * segundo momento do atendimento, dividido por dois vezes um menos rho.
 *
 * <p>Ela e menos geral que a de Little e diz muito mais: diz que a espera depende do
 * SEGUNDO MOMENTO do atendimento, e nao da media dele. Essa formula e o juiz da tabela da
 * variabilidade, e e tambem a explicacao dela.
 */
public final class Juiz {

    private Juiz() {
    }

    public record Veredito(String conta, double medido, double previsto, double erro,
                           boolean passou) {

        public static Veredito de(String conta, double medido, double previsto, double tolerancia) {
            double erro = previsto == 0
                    ? Math.abs(medido)
                    : Math.abs(medido - previsto) / Math.abs(previsto);

            return new Veredito(conta, medido, previsto, erro, erro <= tolerancia);
        }
    }

    /**
     * A LEI DE LITTLE, conferida nos dois lados.
     *
     * <p>Ela vale para toda fila deste repositorio, sem excecao, e um desvio aqui quer
     * dizer que a contabilidade do tempo simulado esta errada.
     */
    public static Veredito little(Contador contador, double tolerancia) {
        double lambda = contador.taxaDeChegada();
        double w = contador.permanenciaMedia();

        return Veredito.de("L = lambda W", contador.noSistemaMedio(), lambda * w, tolerancia);
    }

    /** A mesma lei aplicada so a fila de espera, sem o atendimento: Lq = lambda Wq. */
    public static Veredito littleNaFila(Contador contador, double tolerancia) {
        double lambda = contador.taxaDeChegada();

        return Veredito.de("Lq = lambda Wq", contador.naFilaMedio(),
                lambda * contador.esperaMedia(), tolerancia);
    }

    /**
     * A espera media na fila prevista por Pollaczek e Khinchine.
     *
     * <p>So vale com UM servidor e chegadas exponenciais. Com mais servidores nao ha
     * formula fechada simples, e e por isso que a tabela dos servidores e julgada so pela
     * lei de Little.
     */
    public static double esperaPrevista(Fila fila) {
        if (fila.servidores() != 1)
            throw new IllegalArgumentException("a formula vale para um servidor so");

        /*
         * E VALE SO PARA A ORDEM DE CHEGADA, por um motivo que e o assunto da tabela da
         * disciplina: a formula supoe que a ordem de atendimento nao olha a duracao do
         * servico. Atender o mais curto primeiro olha, e por isso consegue uma espera media
         * MENOR do que a formula preve. Nao e a formula que erra, e a hipotese que mudou.
         */
        if (fila.disciplina() != Fila.Disciplina.CHEGADA)
            throw new IllegalArgumentException(
                    "a formula supoe uma ordem que nao olha a duracao do servico");

        double lambda = 1.0 / fila.entreChegadas().media();
        double rho = fila.utilizacaoPrevista();

        return lambda * fila.atendimento().segundoMomento() / (2 * (1 - rho));
    }

    /**
     * A espera media numa fila exponencial de um servidor, que e o caso classico.
     *
     * <p>E a mesma formula acima com o segundo momento da exponencial, que e dois vezes o
     * quadrado da media. Fica aqui com nome proprio porque e o numero que todo mundo
     * conhece: rho sobre um menos rho, vezes o tempo de atendimento.
     */
    public static double esperaClassica(double media, double rho) {
        return media * rho / (1 - rho);
    }

    public static Veredito pollaczek(Fila fila, Contador contador, double tolerancia) {
        return Veredito.de("Wq de Pollaczek-Khinchine", contador.esperaMedia(),
                esperaPrevista(fila), tolerancia);
    }

    /**
     * Confere tudo que se pode conferir sobre uma fila, e devolve o pior erro.
     *
     * <p>A utilizacao tambem entra: ela foi medida integrando o tempo ocupado dos
     * servidores e prevista pela razao entre as medias, e os dois caminhos nao tem nada em
     * comum.
     */
    public static Veredito[] tudoQueDaParaConferir(Fila fila, Contador contador,
                                                   double tolerancia) {

        Veredito utilizacao = Veredito.de("rho", contador.utilizacao(fila.servidores()),
                fila.utilizacaoPrevista(), tolerancia);

        Veredito atendimento = Veredito.de("atendimento medio", contador.atendimentoMedio(),
                fila.atendimento().media(), tolerancia);

        if (fila.servidores() != 1 || fila.disciplina() != Fila.Disciplina.CHEGADA)
            return new Veredito[] {little(contador, tolerancia),
                    littleNaFila(contador, tolerancia), utilizacao, atendimento};

        return new Veredito[] {little(contador, tolerancia), littleNaFila(contador, tolerancia),
                utilizacao, atendimento, pollaczek(fila, contador, tolerancia)};
    }
}
