package br.com.conde.espera;

/**
 * O CONTADOR: tempo SIMULADO e as somas que a lei de Little precisa. Nunca segundos de
 * relogio.
 *
 * <p>Este repositorio e diferente dos outros: a medida dele nao e trabalho de processador, e
 * TEMPO DE ESPERA. Mas o tempo medido e o do mundo simulado, em tiques inteiros, e nao o do
 * relogio da maquina.
 *
 * <p>Essa distincao e tudo. O tempo simulado e uma funcao da semente: a mesma semente da a
 * mesma fila, na mesma maquina e em qualquer outra, hoje e daqui a dez anos. O tempo de
 * relogio mediria quao rapido o simulador roda, que nao e assunto nenhum.
 *
 * <h2>A integral da fila</h2>
 *
 * <p>Para medir "quantos clientes havia em media no sistema" nao basta somar e dividir: o
 * numero muda a cada evento e fica parado entre eles. O certo e integrar no tempo, ou seja,
 * somar quantidade vezes duracao a cada trecho, e dividir pelo tempo total.
 *
 * <p>Sem isso, uma fila que fica vazia nove decimos do tempo e tem cem clientes no decimo
 * restante daria media dez pela contagem de eventos e dez pela integral tambem, mas uma fila
 * com eventos de duracoes bem diferentes daria numeros errados. E e justamente a lei de
 * Little que a integral deixa conferir.
 */
public final class Contador {

    private long clientesChegados;
    private long clientesAtendidos;
    private long somaDeEspera;
    private long somaDeAtendimento;
    private long somaDePermanencia;
    private long maiorEspera;

    private long areaNoSistema;
    private long areaNaFila;
    private long tempoOcupado;
    private long tempoTotal;

    public void chegou() {
        clientesChegados++;
    }

    /**
     * Um cliente saiu, com as tres duracoes dele.
     *
     * <p>Espera e o tempo na fila, atendimento e o tempo sendo servido, e permanencia e a
     * soma dos dois. Guardar as tres separadas e o que permite distinguir "a fila esta
     * grande" de "o servico esta lento", que e a confusao mais comum sobre fila.
     */
    public void saiu(long espera, long atendimento) {
        clientesAtendidos++;
        somaDeEspera += espera;
        somaDeAtendimento += atendimento;
        somaDePermanencia += espera + atendimento;
        maiorEspera = Math.max(maiorEspera, espera);
    }

    /** Um trecho de tempo em que a fila e os servidores ficaram parados como estavam. */
    public void trecho(long duracao, int noSistema, int naFila, int servidoresOcupados) {
        areaNoSistema += duracao * noSistema;
        areaNaFila += duracao * naFila;
        tempoOcupado += duracao * servidoresOcupados;
        tempoTotal += duracao;
    }

    public long clientesChegados() {
        return clientesChegados;
    }

    public long clientesAtendidos() {
        return clientesAtendidos;
    }

    public long tempoTotal() {
        return tempoTotal;
    }

    public long maiorEspera() {
        return maiorEspera;
    }

    /** W: o tempo medio que um cliente passa no sistema, esperando mais sendo atendido. */
    public double permanenciaMedia() {
        return clientesAtendidos == 0 ? 0 : (double) somaDePermanencia / clientesAtendidos;
    }

    /** O tempo medio so na fila, sem o atendimento. */
    public double esperaMedia() {
        return clientesAtendidos == 0 ? 0 : (double) somaDeEspera / clientesAtendidos;
    }

    public double atendimentoMedio() {
        return clientesAtendidos == 0 ? 0 : (double) somaDeAtendimento / clientesAtendidos;
    }

    /** L: quantos clientes havia em media no sistema, integrado no tempo. */
    public double noSistemaMedio() {
        return tempoTotal == 0 ? 0 : (double) areaNoSistema / tempoTotal;
    }

    public double naFilaMedio() {
        return tempoTotal == 0 ? 0 : (double) areaNaFila / tempoTotal;
    }

    /** Lambda: quantos clientes chegam por unidade de tempo. */
    public double taxaDeChegada() {
        return tempoTotal == 0 ? 0 : (double) clientesAtendidos / tempoTotal;
    }

    /** Rho: a fracao do tempo em que um servidor esteve ocupado. */
    public double utilizacao(int servidores) {
        return tempoTotal == 0 ? 0 : (double) tempoOcupado / (tempoTotal * (long) servidores);
    }

    public void zerar() {
        clientesChegados = 0;
        clientesAtendidos = 0;
        somaDeEspera = 0;
        somaDeAtendimento = 0;
        somaDePermanencia = 0;
        maiorEspera = 0;
        areaNoSistema = 0;
        areaNaFila = 0;
        tempoOcupado = 0;
        tempoTotal = 0;
    }
}
