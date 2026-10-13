package br.com.conde.espera;

/**
 * DE ONDE VEM CADA DURACAO. As distribuicoes de tempo entre chegadas e de atendimento.
 *
 * <p>E a escolha da distribuicao, e nao a da carga, que decide o tamanho da fila. Essa
 * frase e o repositorio inteiro, e a tabela da variabilidade existe para mostra-la.
 *
 * <p>Todas as duracoes sao inteiras, em TIQUES. O tempo simulado precisa ser exato para que
 * a mesma semente de a mesma fila, e um tique e pequeno o bastante para a granularidade nao
 * aparecer: as medias deste repositorio sao da ordem de mil tiques.
 */
public interface Tempos {

    long proximo(Sorteio sorteio);

    double media();

    /**
     * O segundo momento, que e a media do QUADRADO da duracao.
     *
     * <p>Ele esta aqui porque e o que a formula de Pollaczek e Khinchine usa, e essa
     * formula e um dos juizes deste repositorio: a espera na fila e lambda vezes o segundo
     * momento, dividido por dois vezes um menos rho.
     *
     * <p>Repare no que ela diz: a espera NAO depende da media do atendimento sozinha, ela
     * depende do segundo momento. Duas distribuicoes com a mesma media e variancias
     * diferentes dao filas de tamanhos diferentes, e e isso que a tabela da variabilidade
     * mede.
     */
    double segundoMomento();

    String nome();

    /**
     * O SORTEIO DA CASA: um gerador congruente linear escrito aqui, com constantes fixas.
     *
     * <p>Nao e {@link java.util.Random} de proposito. Uma medida que depende de uma
     * biblioteca nao e reproduzivel: a implementacao pode mudar entre versoes da maquina
     * virtual e as tabelas do README deixariam de bater sem que nada aqui tivesse mudado.
     */
    final class Sorteio {

        private long estado;

        public Sorteio(long semente) {
            this.estado = semente;
        }

        /**
         * O proximo inteiro.
         *
         * <p>O DESLOCAMENTO DE 32 CARREGA O METODO. Os bits BAIXOS de um gerador congruente
         * linear de modulo potencia de dois sao pessimos: o ultimo bit alterna zero e um
         * para sempre.
         *
         * <p>Numa simulacao de fila isso seria devastador de um jeito proprio: uma
         * sequencia de chegadas com estrutura deixa de ser um processo sem memoria, e a lei
         * de Little continuaria valendo enquanto as formulas da fila parariam de bater. O
         * repositorio teria dois juizes discordando por causa do gerador. Ha um teste so
         * para isto.
         */
        public int proximo() {
            estado = estado * 6364136223846793005L + 1442695040888963407L;
            return (int) (estado >>> 32);
        }

        /** Uma fracao em (0, 1), aberta nos dois lados: o logaritmo de zero nao existe. */
        public double fracao() {
            return ((proximo() & 0x7FFFFFFF) + 0.5) / 0x80000000L;
        }
    }

    /**
     * Duracao SEMPRE IGUAL.
     *
     * <p>E o melhor caso possivel de variabilidade, e a formula de Pollaczek e Khinchine
     * diz exatamente quanto: com segundo momento igual ao quadrado da media, a espera e
     * METADE da exponencial. Nao e uma melhora marginal; e um fator dois, de graca, sem
     * acrescentar servidor nenhum.
     */
    record Fixo(long duracao) implements Tempos {

        @Override
        public long proximo(Sorteio sorteio) {
            return duracao;
        }

        @Override
        public double media() {
            return duracao;
        }

        @Override
        public double segundoMomento() {
            return (double) duracao * duracao;
        }

        @Override
        public String nome() {
            return "fixo";
        }
    }

    /**
     * Duracao exponencial, que e a distribuicao SEM MEMORIA.
     *
     * <p>Ela e o caso padrao de toda a teoria de filas por um motivo: um processo de
     * chegadas exponencial e o unico em que "quanto falta para a proxima chegada" nao
     * depende de quanto tempo ja se esperou. Essa propriedade e o que torna as formulas
     * fechadas possiveis.
     *
     * <p>O segundo momento dela e DOIS vezes o quadrado da media, e e dai que sai o fator
     * dois em relacao ao fixo.
     */
    record Exponencial(double media) implements Tempos {

        @Override
        public long proximo(Sorteio sorteio) {
            return Math.max(1, Math.round(-media * Math.log(sorteio.fracao())));
        }

        @Override
        public double segundoMomento() {
            return 2 * media * media;
        }

        @Override
        public String nome() {
            return "exponencial";
        }
    }

    /**
     * Duracao quase sempre curta e as vezes enorme, com a mesma media.
     *
     * <p>E como atendimento de verdade se distribui: quase todo pedido e rapido e um em cem
     * e o pedido do cliente que tem dez mil itens. A media pode ser a mesma da exponencial,
     * e a fila nao e.
     *
     * <p>Esse e o ponto que a tabela da variabilidade existe para fazer: a media do
     * atendimento nao diz o tamanho da fila. O segundo momento diz.
     */
    record PesadaNaCauda(double media, double fracaoLonga, double fator) implements Tempos {

        /** A duracao curta, escolhida para a media total ficar na media pedida. */
        public double curta() {
            return media / (1 - fracaoLonga + fracaoLonga * fator);
        }

        public double longa() {
            return curta() * fator;
        }

        @Override
        public long proximo(Sorteio sorteio) {
            boolean longa = sorteio.fracao() < fracaoLonga;

            return Math.max(1, Math.round(longa ? longa() : curta()));
        }

        @Override
        public double segundoMomento() {
            return (1 - fracaoLonga) * curta() * curta() + fracaoLonga * longa() * longa();
        }

        @Override
        public String nome() {
            return "pesada na cauda";
        }
    }
}
