package br.com.conde.espera;

import java.util.ArrayList;
import java.util.List;

/**
 * A FILA SIMULADA: clientes chegam, esperam, sao atendidos e saem.
 *
 * <p>A simulacao e DIRIGIDA A EVENTOS e nao a passos de tempo. O relogio pula direto para
 * o proximo acontecimento, que e ou uma chegada ou uma saida, e entre dois acontecimentos
 * nada muda. Um laco que andasse de tique em tique daria exatamente o mesmo resultado e
 * gastaria mil vezes mais, porque as duracoes daqui sao da ordem de mil tiques.
 *
 * <h2>O aquecimento</h2>
 *
 * <p>Uma fila comeca vazia, e uma fila vazia nao e uma fila em regime. Os primeiros
 * clientes esperam menos do que esperariam em regime permanente, e incluir essa parte na
 * media puxa tudo para baixo.
 *
 * <p>Por isso a simulacao roda um numero de clientes com o contador desligado e so entao
 * comeca a medir, com a fila ja no estado em que ela vive. Sem isso, os numeros nao bateriam
 * com formula nenhuma, e a culpa nao seria da formula.
 */
public final class Fila {

    /** Quem e atendido primeiro quando ha mais de um esperando. */
    public enum Disciplina {

        /** Por ordem de chegada. E a unica que nao precisa saber quanto o servico vai durar. */
        CHEGADA,

        /**
         * O servico mais curto primeiro.
         *
         * <p>Ela minimiza a espera MEDIA, e isso e um teorema. O que ela cobra esta na
         * cauda: quem tem um servico longo pode ser ultrapassado para sempre, e a tabela da
         * disciplina mede as duas coisas lado a lado.
         */
        MAIS_CURTO_PRIMEIRO
    }

    private record Cliente(long chegouEm, long servico) {
    }

    private final int servidores;
    private final Tempos entreChegadas;
    private final Tempos atendimento;
    private final Disciplina disciplina;

    public Fila(int servidores, Tempos entreChegadas, Tempos atendimento, Disciplina disciplina) {
        if (servidores < 1) throw new IllegalArgumentException("pelo menos um servidor");

        this.servidores = servidores;
        this.entreChegadas = entreChegadas;
        this.atendimento = atendimento;
        this.disciplina = disciplina;
    }

    public int servidores() {
        return servidores;
    }

    public Tempos atendimento() {
        return atendimento;
    }

    public Tempos entreChegadas() {
        return entreChegadas;
    }

    public Disciplina disciplina() {
        return disciplina;
    }

    /**
     * A utilizacao PREVISTA pelos parametros, antes de simular.
     *
     * <p>E a taxa de chegada dividida pela capacidade total de atendimento. Acima de um, a
     * fila cresce para sempre e nenhuma media existe.
     */
    public double utilizacaoPrevista() {
        return atendimento.media() / (entreChegadas.media() * servidores);
    }

    public String nome() {
        return servidores + " servidor" + (servidores > 1 ? "es" : "") + ", "
                + atendimento.nome() + ", "
                + (disciplina == Disciplina.CHEGADA ? "por chegada" : "mais curto primeiro");
    }

    /**
     * Roda a simulacao e enche o contador.
     *
     * @param quantos     clientes medidos
     * @param aquecimento clientes que passam antes, sem serem medidos
     */
    public void simular(int quantos, int aquecimento, long semente, Contador contador) {
        if (utilizacaoPrevista() >= 1)
            throw new IllegalArgumentException(
                    "utilizacao prevista de " + utilizacaoPrevista()
                            + ": a fila cresce para sempre e nao tem media");

        Tempos.Sorteio sorteio = new Tempos.Sorteio(semente);

        List<Cliente> esperando = new ArrayList<>();
        long[] fimDoServidor = new long[servidores];
        long[] esperaNoServidor = new long[servidores];
        long[] servicoNoServidor = new long[servidores];
        boolean[] ocupado = new boolean[servidores];

        long agora = 0;
        long proximaChegada = entreChegadas.proximo(sorteio);
        int chegados = 0;
        int saidos = 0;
        int total = quantos + aquecimento;

        while (saidos < total) {
            long proximaSaida = Long.MAX_VALUE;
            int quemSai = -1;

            for (int s = 0; s < servidores; s++)
                if (ocupado[s] && fimDoServidor[s] < proximaSaida) {
                    proximaSaida = fimDoServidor[s];
                    quemSai = s;
                }

            boolean vaiChegar = chegados < total && proximaChegada <= proximaSaida;
            long proximo = vaiChegar ? proximaChegada : proximaSaida;

            /*
             * O TRECHO E CONTADO ANTES DE O ESTADO MUDAR. A fila ficou como esta durante
             * todo o intervalo ate agora, e e essa area que a lei de Little compara com a
             * media das permanencias.
             */
            int ocupados = 0;

            for (boolean estaOcupado : ocupado) if (estaOcupado) ocupados++;

            contador.trecho(proximo - agora, esperando.size() + ocupados, esperando.size(),
                    ocupados);

            agora = proximo;

            if (vaiChegar) {
                chegados++;

                if (chegados > aquecimento) contador.chegou();

                esperando.add(new Cliente(agora, atendimento.proximo(sorteio)));
                proximaChegada = agora + entreChegadas.proximo(sorteio);
            } else {
                ocupado[quemSai] = false;
                saidos++;

                if (saidos > aquecimento)
                    contador.saiu(esperaNoServidor[quemSai], servicoNoServidor[quemSai]);

                if (saidos == aquecimento) contador.zerar();
            }

            for (int s = 0; s < servidores && !esperando.isEmpty(); s++) {
                if (ocupado[s]) continue;

                Cliente cliente = tirarDaFila(esperando);

                ocupado[s] = true;
                esperaNoServidor[s] = agora - cliente.chegouEm();
                servicoNoServidor[s] = cliente.servico();
                fimDoServidor[s] = agora + cliente.servico();
            }
        }
    }

    private Cliente tirarDaFila(List<Cliente> esperando) {
        if (disciplina == Disciplina.CHEGADA) return esperando.remove(0);

        int escolhido = 0;

        for (int i = 1; i < esperando.size(); i++)
            if (esperando.get(i).servico() < esperando.get(escolhido).servico()) escolhido = i;

        return esperando.remove(escolhido);
    }
}
