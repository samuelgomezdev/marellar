# Marellar — MVP do simulador de estaleiro

Simulador de gestão de um estaleiro de iates em Osasco-SP (2027–2031), em pixel art. Este README define o **MVP**: a menor partida jogável. Ele é a especificação para programar o projeto.

> Projeto de portfólio do Samuel Gomez da Silva. As regras completas estão no **Documento de Visão** e o jogo no **GDD**; esses documentos ficam fora do repositório (veja a seção 13). Quando este README e o Documento de Visão discordarem, vale o Documento de Visão. Decisões tomadas durante a programação ficam em [`docs/decisoes.md`](docs/decisoes.md).

**Plataforma:** PC (Windows) primeiro; celular depois.

## Status

| # | Entrega | Status |
| --- | --- | --- |
| M1 | Maven multi-módulo, `data/*.json`, carregamento e validação | ✅ |
| M2 | Motor: relógio, barcos, gates e produção | ✅ |
| M3 | Qualidade e inspetor | ✅ |
| M4 | Pessoas, fornecedores e eventos | ✅ |
| M5 | Finanças, DRE, valor da empresa, conselho | ✅ |
| M6 | `marellar-cli` | ✅ roda N meses com jogador automático e imprime o relatório do conselho |
| M7 | `marellar-api` | 🚧 criar, estado, avançar, ordens, barco, eventos e conselho; falta salvar/carregar |
| M8 | `client-godot`: telas Fábrica, Barco, Conselho | 🚧 Fábrica, ficha do barco, Equipe, Fornecedores e Conselho (arte provisória) |

---

## 1. Objetivo do MVP

Jogar **1 mês** de fábrica com a **linha de 33 pés**:

1. Barcos passam pelos gates, gastando horas-homem (HH) e material.
2. NCs são sorteadas nos gates. O inspetor (único agente com personalidade) detecta ou deixa passar.
3. O jogador decide: prioridade dos barcos, liberar ou segurar gate, RNC ou concessão, contratar, treinar e fazer 1:1.
4. As cotas do cliente entram no caixa quando os gates são aprovados.
5. No fim do mês acontece a **reunião do conselho**: metas, DRE simples, indicadores de qualidade e o **valor da empresa**.

O MVP está pronto quando uma partida de 1 mês roda do começo ao fim:
- primeiro pela linha de comando;
- depois pela API;
- por fim nas 3 telas do cliente.

O resultado tem de ser o mesmo para a mesma semente.

---

## 2. Arquitetura

```
┌──────────────────────────┐      HTTP/JSON      ┌─────────────────────────┐
│  client-godot (Godot 4)  │  ─────────────────▶ │  marellar-api (Java 21) │
│  telas em pixel art      │  ◀───────────────── │  Spring Boot 3, REST    │
└──────────────────────────┘                     └────────────┬────────────┘
                                                              │ usa
                                                 ┌────────────▼────────────┐
                                                 │ marellar-engine (Java)  │
                                                 │ simulação pura, sem     │
                                                 │ framework, 100% testável│
                                                 └────────────┬────────────┘
                                                              │ lê
                                                 ┌────────────▼────────────┐
                                                 │ data/*.json parâmetros  │
                                                 │ (somente sintéticos)    │
                                                 └─────────────────────────┘
```

| Camada | Tecnologia | Responsabilidade |
| --- | --- | --- |
| `marellar-engine` | **Java 21**, sem dependências além de Jackson | Regras: tempo, gates, NC, pessoas, caixa, eventos, conselho, valuation |
| `marellar-cli` | **Java 21** | Roda uma partida pelo terminal e imprime o relatório do conselho |
| `marellar-api` | **Java 21 + Spring Boot 3** | Expõe a partida por REST; guarda partidas em memória e salva em JSON |
| `client-godot` | **Godot 4** (GDScript) | Telas em pixel art que chamam a API |
| `data/` | JSON | Parâmetros de custo, tempo, probabilidades e textos |

Decisões técnicas:

- **Java no núcleo.** Toda regra de negócio fica no `marellar-engine`. Nem a API nem o Godot calculam regra; eles só mostram o estado e enviam ordens.
- **Determinismo.** Toda aleatoriedade usa um `java.util.random.RandomGenerator` criado a partir da semente da partida. Mesma semente e mesmas ordens dão o mesmo resultado, o que permite testes e replays.
- **Tempo discreto.** O motor avança em **ticks de 1 hora de turno**. O dia útil tem 8 ticks (1 turno no MVP). O mês tem 21 dias úteis.
- **Dinheiro** em `BigDecimal`, escala 2, `RoundingMode.HALF_EVEN`. Nunca `double` para dinheiro.
- **Build:** Maven multi-módulo (`pom.xml` raiz). Testes com JUnit 5 e AssertJ.

---

## 3. Estrutura do repositório

```
marellar/
├── README.md
├── pom.xml                         # Maven multi-módulo (Java 21)
├── mvnw, mvnw.cmd                  # Maven Wrapper (não precisa instalar Maven)
├── docs/decisoes.md                # decisões tomadas na programação
├── data/
│   ├── linha-33.json               # gates, HH, material, preço, cotas
│   ├── nc-catalogo.json            # tipos de NC, gravidade, probabilidade por gate
│   ├── pessoas.json                # equipe inicial (nomes fictícios), salários, níveis
│   ├── fornecedores.json           # 4 perfis escondidos, PPM, prazo
│   ├── eventos.json                # eventos do MVP
│   ├── financeiro.json             # caixa inicial, despesas fixas, múltiplo, descontos
│   └── textos-ptbr.json            # falas do conselho e dos personagens
├── marellar-engine/
│   └── src/main/java/br/marellar/engine/
│       ├── Simulacao.java          # fachada: novaPartida, avancar, aplicarOrdem, estado
│       ├── Relogio.java            # ticks, dias úteis, mês
│       ├── dominio/                # Barco, Gate, NaoConformidade, Rnc, Pessoa, Fornecedor,
│       │                           # Pedido, Cota, Caixa, Lancamento, Evento, Conselho
│       ├── regras/                 # ProducaoRegra, QualidadeRegra, PessoasRegra,
│       │                           # FornecedorRegra, FinanceiroRegra, EventoRegra
│       ├── agentes/                # Inspetor (personalidade), interface Agente
│       ├── ordens/                 # Ordem (sealed interface) e implementações
│       ├── relatorio/              # RelatorioConselho, Dre, IndicadoresQualidade
│       └── dados/                  # carregamento dos JSON de data/
├── marellar-cli/
│   └── src/main/java/br/marellar/cli/Main.java
├── marellar-api/
│   └── src/main/java/br/marellar/api/  # controllers, DTOs, PartidaService
└── client-godot/
    ├── project.godot
    ├── scenes/ (Fabrica.tscn, Barco.tscn, Conselho.tscn)
    ├── scripts/ (api.gd, estado.gd, ...)
    └── assets/ (sprites 16 px, paleta Endesga 32)
```

---

## 4. Escopo do MVP

### Dentro

| Sistema | O que entra no MVP |
| --- | --- |
| Tempo | Pausa, ×1, ×2, ×4; 1 turno; mês de 21 dias úteis; começa em **mar/2027** |
| Produção | Só a **linha de 33 pés**; 3 a 4 barcos em produção ao mesmo tempo; gates em sequência |
| Qualidade | Sorteio de NC por gate; inspetor detecta ou não; RNC, retrabalho ou concessão; NC escapada vira evento de garantia depois |
| Pessoas | Equipe de produção com nível (1–5) e moral; contratar, treinar, 1:1; produtividade = HH × nível × moral |
| Fornecedores | 3 fornecedores sintéticos; PPM, prazo, scorecard A–F; escada até **chargeback** |
| Finanças | Caixa, cotas do cliente, salários, material, DRE do mês, valor da empresa |
| Eventos | Quebra de peça, atraso de fornecedor, acidente de trabalho, pedido de demissão |
| Conselho | Reunião no fim do mês: metas, falas (sério + humor leve), 3 a 4 decisões para o mês seguinte |
| Telas | Fábrica, Ficha do barco, Conselho |

### Fora do MVP (próximas versões)

- Linhas de 47 e 59 pés, segundo jogo de moldes.
- Obras e terreno. No MVP o layout já está pronto.
- Telas de Pessoas, Obras e Fornecedores. No MVP essas ações ficam em menus simples.
- CS1, CS2, bloqueio e troca de fornecedor. O MVP vai até o chargeback.
- ISO 9001, salões náuticos, pós-venda como negócio, Escola Marellar, tributos detalhados.
- Os 3 objetivos completos e a venda da empresa em 2031.

---

## 5. Regras do MVP

### 5.1 Gates da linha de 33 pés

Os barcos passam pelos gates abaixo, em ordem. A lista completa de 15 gates está no Documento de Visão; o MVP usa estes 10.

| Ordem | Código | Gate | Peso de HH | Cota do cliente ao aprovar |
| --- | --- | --- | --- | --- |
| 0 | — | Contrato assinado | — | **30%** |
| 1 | RE | Recebimento de materiais | 2% | — |
| 2 | DM | Desmoldagem (casco laminado) | 28% | **10%** |
| 3 | BA | Base / montagem inicial | 20% | — |
| 4 | BF | Fechamento do barco | 18% | **10%** |
| 5 | ST | Teste de estanqueidade | 4% | **10%** |
| 6 | CT | Comissionamento e testes | 10% | — |
| 7 | PI | Pré-inspeção | 6% | **10%** |
| 8 | TM | Teste de mar | 6% | — |
| 9 | TR | Transporte | 3% | — |
| 10 | ET | Entrega técnica | 3% | **30%** |

A cota só entra no caixa quando o gate é **aprovado pela qualidade**, e entra após `prazoRecebimentoDias` (parâmetro, padrão 5 dias úteis).

### 5.2 Parâmetros da linha de 33 pés (`data/linha-33.json`)

| Parâmetro | Valor inicial | Observação |
| --- | --- | --- |
| HH por barco | 1.900 HH | Estimativa: 19.422 HH do Fairline Squadron 78 (referência pública) × (33/78)^2,7. O expoente 2,7 é premissa, a validar com dados reais de HH por linha |
| Custo da HH | R$ 57,00 | Inclui encargos |
| Material por barco | R$ 400.000 | Premissa: 50% do preço |
| Preço de venda | R$ 800.000 | |
| Consumíveis | 3% do material | Parâmetro |
| Itens importados | 15% do material | Só motores, geradores e itens caros ou complexos; sofre câmbio |

> Todos os números são **parâmetros sintéticos**, fáceis de mudar no JSON. **Nenhum dado real de empresa entra no repositório.**

### 5.3 Produção

- Cada barco tem `hhRestantesNoGate`. A cada tick, as pessoas alocadas ao barco produzem `1 h × fatorNivel × fatorMoral`.
- `fatorNivel` = {1: 0,6; 2: 0,75; 3: 0,9; 4: 1,0; 5: 1,1}.
- `fatorMoral` = 0,7 + 0,3 × (moral / 100), com a moral entre 0 e 100.
- Quando `hhRestantesNoGate` chega a 0, o gate vai para **inspeção**.
- O material de cada gate é consumido do estoque quando o gate começa. Sem material, o barco fica **parado esperando material**, e a espera é registrada como hora improdutiva.
- Prioridade: o jogador ordena os barcos e as pessoas livres vão para o barco de maior prioridade.

### 5.4 Qualidade

- Ao terminar um gate, sorteia-se de 0 a N NCs, com `probabilidade[gate] × fatorNivelEquipe × fatorFornecedor`, a partir do catálogo em `nc-catalogo.json`. Cada tipo tem gravidade (leve, média, crítica), HH de retrabalho e custo de material.
- O **inspetor** detecta cada NC com probabilidade `deteccaoBase[gravidade] × fatorPersonalidade × fatorCarga`. `fatorCarga` cai quando há mais de 2 gates esperando inspeção.
- NC detectada: o jogador escolhe uma destas três ações.
  - **Retrabalho:** HH extra no barco e custo.
  - **Concessão:** libera sem consertar, com risco de garantia depois.
  - **RNC contra fornecedor**, quando a origem é material comprado. Gera 8D e chargeback.
- NC não detectada **escapa**: vira chance de evento de garantia após a entrega e reduz a nota de qualidade.
- Gate só é aprovado quando não há NC crítica aberta.

### 5.5 Inspetor (único agente do MVP)

Personalidade definida por 3 atributos (0–100): **rigor**, **velocidade** e **paciência**.
- Mais rigor aumenta a detecção e o tempo de inspeção.
- Mais velocidade diminui o tempo de inspeção e um pouco a detecção.
- Paciência baixa: quando o jogador libera muitas concessões, a moral dele cai e a fala no conselho fica mais dura.

O inspetor tem falas curtas em `textos-ptbr.json`, com tom misto: sério sobre segurança e qualidade, humor leve no resto.

### 5.6 Pessoas

- Equipe inicial do MVP: **12 produtivos** da linha de 33 pés, com nomes fictícios, mais 1 inspetor.
- **Contratar:** a pessoa chega em 10 dias úteis, no nível 1, e sobe 1 nível a cada `diasParaSubirNivel` (padrão 30) até o nível 3.
- **Treinar:** a pessoa sai da linha por X dias, sobe 1 nível (máximo 5) e reduz a chance de acidente.
- **1:1:** +5 de moral, gasta 0,5 h do líder. Sem 1:1 por mais de 30 dias, a moral cai 1 por dia.
- **Pedido de demissão:** chance diária maior com moral abaixo de 40.

### 5.7 Fornecedores (modelo automotivo, até o chargeback)

- **3 fornecedores sintéticos**, cada um com um perfil escondido: premium confiável, barato instável, grande indiferente ou pequeno dedicado.
- PPM = peças rejeitadas ÷ peças recebidas × 1.000.000.
- Scorecard mensal: entrega 40%, qualidade (PPM) 30%, custo 20%, resposta 10%. Nota de A a F.
- **Chargeback:** custo documentado (HH × R$ 57 + material + frete + horas de linha parada) **+ 10% de taxa administrativa**, descontado na próxima compra.
- **8D:** prazo de 10 dias úteis. 8D vencido em NC crítica gera chargeback das horas paradas.

### 5.8 Finanças

- **Caixa inicial:** parâmetro (padrão R$ 3.000.000).
- **Entradas:** cotas do cliente.
- **Saídas:** salários (pagos no último dia útil), material (na entrega do fornecedor) e custos de retrabalho.
- **DRE do mês:** receita reconhecida por gate aprovado, custo de material, custo de HH, despesas fixas e **EBITDA**.
- **Valor da empresa** = EBITDA anualizado (mês × 12) × **múltiplo 6**.
  - Desconto de 5% por acidente com afastamento no mês.
  - Desconto de 3% por NC escapada.
  - Os descontos são parâmetros.
- **Caixa negativo:** aparece alerta no conselho. Derrota após 3 meses seguidos sem crédito; isso não acontece num MVP de 1 mês, mas a regra deve estar implementada.

### 5.9 Eventos do MVP

| Evento | Gatilho | Efeito |
| --- | --- | --- |
| Quebra de peça | Por gate, 3% (mais com pressa ou moral baixa) | HH de retrabalho e material |
| Atraso de fornecedor | Por pedido, de acordo com o perfil | Barco parado esperando material |
| Acidente de trabalho | Por HH trabalhada; maior com treino vencido e sobrecarga | Dias perdidos; taxa de frequência e de gravidade; moral da equipe cai |
| Pedido de demissão | Por pessoa por dia, maior com moral baixa | Vaga aberta |

Indicadores de acidente (NBR 14280):
- `TF = acidentes × 1.000.000 / HHT`
- `TG = (dias perdidos + debitados) × 1.000.000 / HHT`

### 5.10 Conselho do fim do mês

- **Metas do mês** (de `textos-ptbr.json`), por exemplo:
  - concluir o gate DM em 2 cascos;
  - caixa acima de R$ 2 mi;
  - zero acidente com afastamento;
  - RNC por barco abaixo de 4.
- **Falas** de 2 conselheiros fictícios. A fala escolhida depende do resultado: sério quando há acidente ou caixa ruim, humor leve quando há algo estranho (ex.: compra de material muito acima do uso).
- **Paciência do conselho:** de 0 a 5. Cada meta perdida tira 1; cada meta cumprida devolve 1, até o máximo.
- **Decisões para o mês seguinte:** 3 a 4 opções com custo e efeito (treinar NR-35, contratar, antecipar compra, negociar com fornecedor). A escolha vira uma ordem aplicada no início do mês seguinte.

---

## 6. Modelo de dados (núcleo)

Use `record` do Java 21 para objetos de valor e classes para entidades com estado.

```java
// identificação
record BarcoId(String valor) {}          // ex.: "33-007"
enum Linha { L33 }                        // L47, L59 depois do MVP
enum CodigoGate { RE, DM, BA, BF, ST, CT, PI, TM, TR, ET }
enum Gravidade { LEVE, MEDIA, CRITICA }
enum DecisaoNc { RETRABALHO, CONCESSAO, RNC_FORNECEDOR }

class Barco {
  BarcoId id; Linha linha; String cliente;
  CodigoGate gateAtual; StatusGate status;   // EM_PRODUCAO, AGUARDANDO_INSPECAO, APROVADO, PARADO_MATERIAL
  BigDecimal hhRestantesNoGate; int prioridade;
  List<NaoConformidade> ncs; List<Cota> cotas;
}
record NaoConformidade(String id, BarcoId barco, CodigoGate gate, String tipo,
                       Gravidade gravidade, boolean detectada, Origem origem,
                       Optional<String> fornecedorId, BigDecimal hhRetrabalho) {}
class Pessoa { String id; String nome; Funcao funcao; int nivel; int moral;
               LocalDate ultimo1a1; Set<Treinamento> treinamentos; Optional<BarcoId> alocadoEm; }
class Fornecedor { String id; String nome; PerfilFornecedor perfil /* escondido do cliente */;
                   long pecasRecebidas, pecasRejeitadas; NotaScorecard nota; }
record Lancamento(LocalDate data, TipoLancamento tipo, BigDecimal valor, String descricao) {}
class EstadoPartida { long semente; LocalDateTime agora; Velocidade velocidade;
                      List<Barco> barcos; List<Pessoa> pessoas; List<Fornecedor> fornecedores;
                      Caixa caixa; List<Evento> eventos; Optional<RelatorioConselho> conselhoPendente; }
```

**Ordens** do jogador (`sealed interface Ordem`):
- `DefinirPrioridade(barcoId, posicao)`
- `LiberarGate(barcoId)`
- `SegurarGate(barcoId)`
- `DecidirNc(ncId, DecisaoNc)`
- `Contratar(funcao)`
- `Treinar(pessoaId, Treinamento)`
- `FazerUmAUm(pessoaId)`
- `EscolherDecisaoConselho(opcaoId)`
- `MudarVelocidade(Velocidade)`

---

## 7. API REST (`marellar-api`)

Base: `/api/v1`. JSON em camelCase. Valores em reais como string decimal (`"3200000.00"`).

| Método | Rota | Faz |
| --- | --- | --- |
| POST | `/partidas` | Cria partida `{ "semente": 42 }` → `{ "id": "..." }` |
| GET | `/partidas/{id}` | Estado completo para a tela da fábrica |
| POST | `/partidas/{id}/avancar` | Avança `{ "ticks": 8 }` (o cliente chama em loop conforme a velocidade) |
| POST | `/partidas/{id}/ordens` | Aplica uma ordem `{ "tipo": "DecidirNc", "ncId": "...", "decisao": "RETRABALHO" }` |
| GET | `/partidas/{id}/barcos/{barcoId}` | Dados da ficha do barco |
| GET | `/partidas/{id}/conselho` | Relatório do conselho, quando houver |
| GET | `/partidas/{id}/eventos?desde=...` | Eventos novos para alertas e toasts |
| POST | `/partidas/{id}/salvar` | Grava `saves/{id}.json` |
| POST | `/partidas/carregar` | Carrega um save |

- O motor **pausa sozinho**, devolvendo `"pausado": true` e o motivo, quando acontece acidente, NC crítica, conselho ou evento com escolha.
- Erros no formato `application/problem+json`.

---

## 8. Cliente Godot (MVP)

- **Resolução base 640×360**, escala inteira ×2 ou ×3, sem suavização (filtro *Nearest*).
- **Tile 16×16 px = 2×2 m.** Personagens com 10×16 a 16×24 px.
- **Paleta Endesga 32**, com papéis fixos:

| Papel | Cor |
| --- | --- |
| Produção suja | `#b86f50` |
| Montagem limpa | `#63c74d` |
| Água | `#0099db` |
| Armazém | `#5a6988` |
| Não conformes | `#e43b44` |
| Fundo da interface | `#262b44` e `#181425` |
| Texto | `#ead4aa` |
| Alerta | `#feae34` |

- **Fonte:** pixel, com acentos do português (ex.: VT323 para texto e Press Start 2P para títulos, ambas com licença OFL).
- **Telas do MVP** (mockups fora do repositório, veja a seção 13):
  1. **Fábrica:** baias da montagem com os barcos de 33 pés, bonequinhos andando e trabalhando, barra de topo (data, velocidade, caixa, valor, moral, NCs) e barra de baixo com os menus.
  2. **Ficha do barco:** gates, HH e material, NCs com botões de decisão, cotas.
  3. **Conselho:** metas, falas, decisões.
- **Arte provisória:** retângulos e sprites simples na paleta. A arte final vem depois.

---

## 9. Ordem de construção (milestones)

| # | Entrega | Pronto quando |
| --- | --- | --- |
| M1 | Maven multi-módulo, `data/*.json`, carregamento e validação dos parâmetros | `mvn verify` passa; JSON inválido gera erro claro |
| M2 | Motor: relógio, barcos, gates e produção | Teste: 1 barco sem NC passa de RE a ET com HH ≈ 1.900 |
| M3 | Qualidade e inspetor | Teste com semente fixa: mesmas NCs e detecções a cada rodada |
| M4 | Pessoas, fornecedores e eventos | Teste: moral baixa aumenta pedidos de demissão; chargeback = custo + 10% |
| M5 | Finanças, DRE, valor da empresa, conselho | Teste: cotas somam 100% do preço; EBITDA × 6 bate com o relatório |
| M6 | `marellar-cli` | `java -jar marellar-cli.jar --semente 42 --meses 1` imprime o relatório do conselho |
| M7 | `marellar-api` | Testes de integração dos endpoints; partida de 1 mês jogável via HTTP |
| M8 | `client-godot`: telas Fábrica, Barco, Conselho | Uma pessoa joga 1 mês só com mouse |

---

## 10. Testes obrigatórios

- **Determinismo:** duas partidas com a mesma semente e as mesmas ordens têm estados iguais (comparar o JSON).
- **Soma das cotas** = 100% do preço do barco.
- **Dinheiro:** nenhum `double` em valores monetários. Um teste de arquitetura com ArchUnit verifica isso.
- **Gate não aprova** com NC crítica aberta.
- **Chargeback** = custo documentado × 1,10.
- **Taxa de frequência e de gravidade** batem com as fórmulas da NBR 14280.
- **Sem material:** o barco fica parado e a hora improdutiva é registrada.
- **Valor da empresa** = EBITDA anualizado × múltiplo − descontos.
- **Cobertura** do `marellar-engine` acima de 80%.

---

## 11. Regras para quem programa (Claude Code)

1. **Java 21** em todos os módulos Java; Maven; JUnit 5 + AssertJ; Spring Boot 3 só no `marellar-api`.
2. O `marellar-engine` **não depende** de Spring, banco ou Godot.
3. Código, nomes de classes e comentários **em português**, sem acentos nos identificadores. Mensagens para o jogador em `textos-ptbr.json`.
4. **Nenhum dado real** de empresa (planilhas, custos, nomes, fornecedores) entra no repositório. Só parâmetros agregados e dados sintéticos. Documentos de referência ficam fora do repo (`.gitignore`).
5. Cada milestone em um branch e um PR pequeno, com testes.
6. **Commits e PRs sem coautoria de IA:** não adicionar "Co-Authored-By: Claude", "Generated with Claude Code" nem link de sessão em mensagens de commit ou descrições de PR.
7. Quando uma regra estiver ambígua, seguir o Documento de Visão. Se ainda faltar informação, criar um parâmetro em `data/` com um valor razoável e anotar em `docs/decisoes.md`.

---

## 12. Como rodar (meta)

Pré-requisitos: **JDK 21** e **Godot 4** (o Maven vem pelo wrapper).

```bash
./mvnw verify                                # build e testes (Windows: mvnw.cmd verify)
java -jar marellar-cli/target/marellar-cli.jar --semente 42 --meses 1
./mvnw -pl marellar-api spring-boot:run      # API em http://localhost:8080/api/v1
# abrir client-godot/project.godot no Godot 4 e rodar (com a API ligada)
godot --path client-godot -- --rodar          # opcional: abre o jogo já em x4
godot --path client-godot -- --dias 12 --selecionar 33-002   # opcional: pula dias e abre a ficha de um barco
godot --path client-godot -- --dias 40 --painel equipe       # opcional: abre direto no painel equipe ou fornecedores
```

A CLI tem um jogador automático: NC de material vira RNC, as outras vão para retrabalho, e ele faz 1:1 com quem está com moral abaixo de 70. Use `--sem-decisoes` para desligar.

Controles da tela da Fábrica: **Espaço** pausa, **1/2/3** mudam a velocidade (x1, x2, x4), **F1** abre a Equipe (contratar, 1:1, treinar), **F2** os Fornecedores (scorecard, estoque, pedidos), **Esc** volta. Clique num barco para dar **Prioridade 1**, **Segurar/Liberar gate** e decidir as NCs (**Retrabalho**, **Concessão** ou **RNC**) na ficha à direita.

---

## 13. Documentos de referência

Os documentos de design ficam **fora do repositório**, na pasta local do projeto, porque o Documento de Visão cita dados reais:

- `Marellar_Documento_de_Visao.docx`: regras e números (fonte da verdade).
- `Marellar_GDD.docx`: game design.
- `Marellar_GDD_visual.pptx`: apresentação visual.
- `telas/*.png`: mockups das telas (fábrica, barco, pessoas, conselho, obras, fornecedores).

## Licença

Código sob [MIT](LICENSE). Fontes [VT323](https://fonts.google.com/specimen/VT323) e [Press Start 2P](https://fonts.google.com/specimen/Press+Start+2P) sob SIL Open Font License 1.1 (licenças em `client-godot/assets/fonts/`).
