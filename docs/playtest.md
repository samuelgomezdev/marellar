# Playtest do portão A — "achar a diversão"

**O portão A passa quando 3 de 5 testadores que nunca viram o jogo pedem "mais um mês"**, sem arte nova.
Este roteiro é para quem aplica o teste. Uma sessão leva de 20 a 30 minutos.

## Antes

- Link: https://samuelgomezdev.github.io/marellar/ (a versão da `main`). Navegador no computador, com mouse; som ligado.
- Testador que **nunca viu o jogo** e não conhece este projeto por dentro.
- Tenha esta página aberta e a ficha do testador (no fim) para anotar.
- O log de playtest é gravado sozinho no navegador do testador. Nada é enviado pela rede; não há nome nem dado pessoal no arquivo.

## O que dizer (só isto)

> "Você é o CEO de um estaleiro que faz barcos de 33 pés. Jogue como achar melhor. Pense em voz alta, e me avise quando quiser parar."

**Não diga:** o que é gate, NC, rodada, capacidade, multa ou conselho; onde clicar; o que é uma marca falsa; que o jogo pausa sozinho.
Se perguntarem algo, devolva: "O que você acha?".

## Quando ajudar

- **Só se o testador ficar travado por mais de 2 minutos** (sem saber o que fazer, sem avançar).
- Ajude com **uma frase** que aponta, sem explicar ("Olhe o canto direito.").
- **Anote o momento e o que travou.** Com qualquer ajuda antes do fim do mês 1, o testador **não** conta como "fecha o mês 1 sem ajuda externa".

## O que observar

| Momento | O que olhar |
|---|---|
| Primeiros 60 s | Ele toma uma decisão (aceita proposta, decide NC, faz rodada)? Ou só olha? |
| Primeira pausa da rodada | Entende o aviso? Escolhe inspecionar ou deixa com o inspetor? |
| Rodada | Hesita entre marcas? Clica em sujeira, reflexo ou fita? Lê o resultado? |
| Primeira proposta | Lê os dois lados (desistência x multa)? Qual prazo escolhe e por quê? |
| Fechamento do mês | Entende "mês ruim"? Mexe na capacidade? Escolhe uma decisão do conselho? |
| Depois do fechamento | **Clica Continuar e segue?** Pede mais um mês? Em que momento quer parar? |
| O tempo todo | Onde se irrita, onde sorri, o que comenta em voz alta. |

No fim, pergunte só: **"Por que você levou essa multa / essa NC?"** (se levou) e **"Quer jogar mais um mês?"**.

## As 6 métricas

| Métrica | Meta | De onde vem |
|---|---|---|
| Tempo até a 1ª decisão | menos de 60 s | log: `primeira_decisao.segundos` (resumo: `tempo_ate_primeira_decisao_s`) |
| Fecha o mês 1 sem ajuda externa | 4 de 5 | observação (ajuda) + log: `meses_fechados` ≥ 1 |
| Explica por que levou multa ou NC | 4 de 5 | observação (pergunta no fim) |
| Acerto na 1ª rodada de inspeção | 60% a 80% | log: `acerto_primeira_rodada` (marcas classificadas certo ÷ marcas) |
| Cliques até a ação principal | no máximo 2 | log: `cliques_medios_ate_acao` e `acoes_com_ate_2_cliques` de `acoes` |
| Pede "mais um mês" | **3 de 5 (portão A)** | observação (pergunta no fim) + log: `continuou_depois_do_fechamento` |

## Como pegar o log

No fim da sessão, com o jogo aberto, aperte **F8** (ou **Gestão → Baixar log (F8)**). O navegador baixa um arquivo
`AAAAMMDD_HHMMSS_<id>.jsonl`. Renomeie para `testador-N.jsonl` e guarde junto da ficha.

No jogo instalado (desktop), o F8 abre a pasta dos logs (`%APPDATA%/Godot/app_userdata/Marellar/playtest` no Windows).

### Como ler

Uma linha por acontecimento, em JSON, com `t` (segundos desde o início), data e hora do jogo e `cliques` (total até ali).
A **última linha** (`"tipo":"resumo"`) já traz as métricas do log. As outras linhas contam a história:

| `tipo` | O que é |
|---|---|
| `oportunidade` | Algo que o jogador pode responder: `NC:…`, `PROPOSTA:…`, `RODADA:…` |
| `ordem` | O que o jogador fez, com `cliques_desde_oportunidade` |
| `primeira_decisao` | A primeira ordem que não é mudar a velocidade |
| `rodada` | Resultado de cada rodada (achou, escapou, falso alarme, ignorou, acerto) |
| `rodada_com_inspetor` | Rodada que o jogador deixou com o inspetor |
| `perda` | Multa, desistência, proposta expirada, garantia |
| `pausa`, `fechamento` | Pausas automáticas e o resumo de cada mês fechado |
| `continuou_depois_do_fechamento` | O jogador apertou Continuar no fechamento |
| `tela` | Tela em que o jogador estava (a última mostra onde ele parou) |
| `sessao_fim` | Fechou a janela (no navegador, fechar a aba nem sempre grava esta linha: vale a última `t`) |

## Ficha do testador

Copie uma por testador.

```
Testador N:            Data:           Duração:
Já jogou simulador/gestão? (sim/não)

Ajuda externa (momento e o quê):
Fechou o mês 1 sem ajuda? (sim/não)
1ª rodada: inspecionou ou deixou com o inspetor? O que achou?
1ª proposta: qual prazo escolheu e por quê?
Levou multa ou NC? Explicou a causa? (sim/não/não levou)
Pediu mais um mês? (sim/não)       Em que momento quis parar:
Frases marcantes:
Chato / confuso:
Divertido:

Do log (resumo): 1ª decisão __ s · acerto 1ª rodada __ · cliques médios __ · meses fechados __
```

## Resultado do portão A

| Testador | 1ª decisão < 60 s | Mês 1 sem ajuda | Explica multa/NC | Acerto 1ª rodada | Cliques ≤ 2 | Mais um mês |
|---|---|---|---|---|---|---|
| 1 | | | | | | |
| 2 | | | | | | |
| 3 | | | | | | |
| 4 | | | | | | |
| 5 | | | | | | |

**Passa se a última coluna tiver 3 "sim".** Se não passar, a próxima tarefa é ajustar o loop do mês, não a sprint 3.
