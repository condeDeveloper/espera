package br.com.conde.espera;

import java.util.Locale;

/**
 * O MEDIDOR: imprime as tabelas que o README mostra e que os testes conferem.
 *
 * <p>Toda tabela daqui tem um teste do lado que afirma os mesmos numeros. Texto nao quebra
 * quando deixa de ser verdade; teste quebra.
 */
public final class Medidor {

    private Medidor() {
    }

    public static void main(String[] args) {
        String comando = args.length > 0 ? args[0] : "tudo";

        if (comando.equals("correcao") || comando.equals("tudo")) correcao();
        if (comando.equals("utilizacao") || comando.equals("tudo")) utilizacao();
        if (comando.equals("variabilidade") || comando.equals("tudo")) variabilidade();
        if (comando.equals("servidores") || comando.equals("tudo")) servidores();
        if (comando.equals("disciplina") || comando.equals("tudo")) disciplina();
    }

    private static void correcao() {
        System.out.println();
        System.out.println("OS DOIS TEOREMAS VALEM EM TODA CONFIGURACAO.");
        System.out.println();

        var linhas = Medidas.aCorrecao();
        long erradas = linhas.stream().filter(l -> l.erro() > 0.05).count();

        System.out.printf("  %-22s %-26s %16s %16s %12s%n",
                "fila", "conta", "medido", "previsto", "erro");

        for (var l : linhas)
            System.out.printf(Locale.ROOT, "  %-22s %-26s %16.4f %16.4f %12.5f%n",
                    l.fila(), l.conta(), l.medido(), l.previsto(), l.erro());

        System.out.println();
        System.out.printf("  %d conferencias, %d acima de 5%% de erro.%n", linhas.size(), erradas);
        System.out.println();
    }

    private static void utilizacao() {
        System.out.println();
        System.out.println("A FILA POR UTILIZACAO, um servidor, tudo exponencial.");
        System.out.println("Os tempos sao em atendimentos medios.");
        System.out.println();
        System.out.printf("  %10s %18s %18s %18s %18s%n",
                "rho", "espera medida", "espera prevista", "clientes na fila", "maior espera");

        for (var l : Medidas.aUtilizacao())
            System.out.printf(Locale.ROOT, "  %10.2f %18.3f %18.3f %18.3f %18.1f%n",
                    l.rho(), l.esperaMedida(), l.esperaPrevista(), l.naFila(), l.maiorEspera());

        System.out.println();
    }

    private static void variabilidade() {
        System.out.println();
        System.out.println("A FILA POR VARIABILIDADE, mesma media e mesma utilizacao de 0,8.");
        System.out.println();
        System.out.printf("  %-22s %22s %18s %18s %16s%n",
                "atendimento", "segundo momento rel.", "espera medida", "espera prevista",
                "vezes o fixo");

        for (var l : Medidas.aVariabilidade())
            System.out.printf(Locale.ROOT, "  %-22s %22.2f %18.3f %18.3f %16.2f%n",
                    l.atendimento(), l.segundoMomentoRelativo(), l.esperaMedida(),
                    l.esperaPrevista(), l.vezesOFixo());

        System.out.println();
    }

    private static void servidores() {
        System.out.println();
        System.out.println("UM RAPIDO CONTRA VARIOS LENTOS, mesma capacidade e rho de 0,9.");
        System.out.println();
        System.out.printf("  %-20s %12s %18s %16s %16s %16s%n",
                "arranjo", "servidores", "atendimento", "espera", "permanencia", "maior espera");

        for (var l : Medidas.osServidores())
            System.out.printf(Locale.ROOT, "  %-20s %12d %18.3f %16.3f %16.3f %16.1f%n",
                    l.arranjo(), l.servidores(), l.atendimentoMedio(), l.esperaMedida(),
                    l.permanencia(), l.maiorEspera());

        System.out.println();
    }

    private static void disciplina() {
        System.out.println();
        System.out.println("A ORDEM DE ATENDIMENTO, um servidor e rho de 0,9.");
        System.out.println();
        System.out.printf("  %-14s %-22s %16s %18s %18s%n",
                "disciplina", "atendimento", "espera media", "maior espera", "na fila");

        for (var l : Medidas.aDisciplina())
            System.out.printf(Locale.ROOT, "  %-14s %-22s %16.3f %18.1f %18.3f%n",
                    l.disciplina(), l.atendimento(), l.esperaMedia(), l.maiorEspera(),
                    l.naFila());

        System.out.println();
    }
}
