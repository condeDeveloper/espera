# espera

Teoria de filas do zero em Java 21: simulação dirigida a eventos, julgada por **dois
teoremas**.

Este repositório não tem como ter um juiz do jeito dos outros. Não existe uma "fila de força
bruta" que dê a resposta certa e devagar: a fila simulada **é** a resposta, e comparar uma
simulação com outra simulação não prova nada.

Então o juiz é a matemática que vale para qualquer fila, independente de como ela foi escrita.

A **lei de Little**, L = λW, não supõe nada: vale para qualquer distribuição de chegada,
qualquer atendimento, qualquer número de servidores e qualquer ordem. E os dois lados dela são
medidos por caminhos sem nada em comum — L sai da integral da fila no tempo, W sai da média das
permanências. Nas 28 conferências do medidor o erro dela é **zero até a quinta casa**.

A fórmula de **Pollaczek e Khinchine** é menos geral e diz muito mais: a espera é proporcional
ao **segundo momento** do atendimento, não à média dele. Ela é o juiz da tabela da
variabilidade e também a explicação dela.

O tempo é **simulado**, em tiques inteiros, e nunca o relógio da máquina: a mesma semente dá a
mesma fila em qualquer lugar. Todas as colunas de tempo são divididas pelo atendimento médio,
então **1,0 quer dizer "esperou o tempo de um atendimento"**.

## A fila explode por variabilidade, não por carga

```
$ mvn exec:java -Dexec.args="variabilidade"

  atendimento              segundo momento rel.      espera medida    espera prevista     vezes o fixo
  fixo                                     1.00              2.011              2.000             1.00
  exponencial                              2.00              3.967              4.000             1.97
  1% dez vezes                             1.67              3.322              3.350             1.65
  1% cinquenta vezes                      11.71             23.971             23.413            11.92
```

Quatro filas com a **mesma média de atendimento** e a **mesma utilização de 0,8**. A espera
varia por **doze vezes**.

Tornar o serviço mais previsível, sem torná-lo mais rápido, corta a fila pela metade: é o que
a linha do fixo diz. E um serviço que às vezes demora cinquenta vezes mais faz a fila explodir
sem que a média tenha mudado um tique.

Repare na terceira linha: a distribuição com 1% dez vezes mais longo tem segundo momento
**1,67**, que é **menor** que o 2,00 do exponencial, e a fila dela é menor também. Ter cauda
não basta. O que conta é quanto ela pesa no quadrado, e a ordem das quatro esperas segue
exatamente a ordem dos quatro segundos momentos.

## Como rodar

```
mvn test
mvn exec:java -Dexec.args="tudo"
```

Os comandos do medidor são `correcao`, `utilizacao`, `variabilidade`, `servidores`,
`disciplina` e `tudo`.

## O que as medidas me corrigiram

**Um servidor rápido contra dois lentos: não há um vencedor, há dois.**

Eu escrevi que o servidor único e mais rápido ganha, porque ele nunca fica parado enquanto
alguém espera.

```
$ mvn exec:java -Dexec.args="servidores"

  arranjo                servidores        atendimento           espera      permanencia     maior espera
  um normal                       1              0.999            8.831            9.830             82.7
  dois normais                    2              0.999            4.174            5.173             40.5
  um dobrado                      1              0.500            4.415            4.915             41.4
  quatro normais                  4              0.999            1.923            2.922             20.5
  um quadruplicado                1              0.250            2.208            2.458             20.7
```

Mesma capacidade total, mesma utilização de 0,9. Na **espera na fila**, dois servidores normais
ganham: 4,174 contra 4,415. Na **permanência**, que soma o atendimento, o rápido ganha: 4,915
contra 5,173.

E isso não é artefato da simulação. Para M/M/2 com ρ de 0,9 a fórmula de Erlang dá 4,263
atendimentos de espera, e para M/M/1 com o dobro da velocidade a clássica dá 4,500: a teoria
concorda que dois servidores esperam menos. Dois servidores aproveitam melhor as rajadas,
porque dois clientes que chegam juntos são atendidos em paralelo; o rápido perde na fila e
recupera com juros no atendimento.

Qual das duas colunas importa depende da pergunta. *"Quanto tempo até ser atendido"* é a
primeira; *"quanto tempo até ir embora"* é a segunda. Quase toda discussão sobre isso confunde
as duas.

**E o aquecimento importa, mas não onde eu fui procurar.**

Uma fila começa vazia, e uma fila vazia não é uma fila em regime. Por isso a simulação roda um
número de clientes com o contador desligado antes de medir.

O teste que eu escrevi comparava cinco mil clientes sem aquecimento com cinco mil depois de
aquecer, com uma semente só, e deu o **contrário** do esperado: 9.221 contra 8.091.

Não porque o aquecimento não importa. Porque com ρ de 0,9 a média de cinco mil clientes varia
mais do que o efeito que eu queria medir: duas janelas quaisquer da mesma fila já diferem
assim. O efeito está nos **primeiros** clientes, e para vê-lo é preciso olhar poucos deles e
mediá-los sobre várias sementes.

Isso tem uma consequência que vale para a tabela inteira: a simulação precisa de **um milhão**
de clientes por ponto para a espera ficar a 1% da fórmula em ρ = 0,95. Com duzentos mil ela
errava por 8%, e errava **para baixo**, porque os raros períodos longos de ocupação é que
carregam a média.

## A curva que todo mundo conhece, e a coluna que quase ninguém olha

```
$ mvn exec:java -Dexec.args="utilizacao"

         rho      espera medida    espera prevista   clientes na fila       maior espera
        0.10              0.111              0.111              0.011               12.2
        0.50              1.001              1.000              0.500               22.0
        0.80              3.967              4.000              3.175               49.4
        0.90              8.831              9.000              7.951               82.7
        0.95             18.821             19.000             17.888              160.6
```

De 50% para 90% de ocupação a carga quase dobra e a espera fica **nove vezes** maior. De 90%
para 95%, cinco pontos de carga **dobram** a espera.

A coluna da direita existe porque média não é experiência. Com ρ de 0,9 a média é nove
atendimentos e o pior cliente espera **oitenta e três**. E mesmo com **dez por cento** de
carga, alguém espera doze atendimentos.

## Atender o mais curto primeiro

```
$ mvn exec:java -Dexec.args="disciplina"

  disciplina     atendimento                espera media       maior espera            na fila
  por chegada    exponencial                       8.831               82.7              7.951
  mais curto     exponencial                       3.165             1147.4              2.850
  por chegada    1% cinquenta vezes               55.314              542.2             49.805
  mais curto     1% cinquenta vezes               14.320             1297.1             12.894
```

Atender o serviço mais curto primeiro minimiza a espera média, e isso é um teorema: ela cai de
8,831 para 3,165.

O que ela cobra está na cauda. A maior espera vai de 82,7 para **1.147,4**: treze vezes pior.
Quem tem um serviço longo é ultrapassado por todo mundo que chega depois.

Com atendimento de cauda pesada a troca fica ainda maior nos dois lados, e é exatamente onde a
tentação de usar essa disciplina é maior.

## O que não tem aqui

Uma fila só, sem rede de filas, sem abandono, sem prioridades e sem capacidade limitada. A
disciplina do mais curto primeiro supõe que a duração do serviço é conhecida antes de começar,
o que quase nunca é verdade.

E não há fórmula fechada para a fila de cauda pesada com vários servidores, que é por que a
tabela dos servidores é julgada só pela lei de Little: ela é o único juiz que vale ali.

## Licença

MIT.
