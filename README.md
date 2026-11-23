# Marellar — MVP do simulador de estaleiro

Simulador de gestão de um estaleiro de iates em Osasco-SP (2027–2031), em pixel art. Este README define o **MVP**: a menor partida jogável.

> Projeto de portfólio do Samuel Gomez da Silva. As regras completas estão no **Documento de Visão** e o jogo no **GDD**; esses documentos ficam fora do repositório (veja a seção 11). Quando este README e o Documento de Visão discordarem, vale o Documento de Visão. Decisões tomadas durante a programação ficam em [`docs/decisoes.md`](docs/decisoes.md).

**Plataforma:** PC (Windows) primeiro; celular depois.

**Jogar no navegador:** https://samuelgomezdev.github.io/marellar/ — sem instalar nada e sem servidor: o motor Java roda compilado para JavaScript dentro da página (veja a seção 10). **Inspeção 3D:** https://samuelgomezdev.github.io/marellar/inspecao/ (modo demonstração: as NCs ficam no navegador).

## Status e roadmap

### Feito

| # | Entrega | O que tem |
| --- | --- | --- |
| M1–M5 | Motor (`marellar-engine`) | Relógio, barcos, gates e produção; qualidade e inspetor; pessoas, fornecedores e eventos; finanças, DRE, valor da empresa e conselho |
| M6 | `marellar-cli` | Roda N meses com jogador automático e imprime o relatório do conselho |
| M7 | `marellar-api` | Criar, estado, avançar, ordens, barco, eventos, conselho, salvar/carregar |
| M8 | `client-godot` | Fábrica, ficha completa do barco, Equipe, Fornecedores, Conselho e saves, tudo pelo mouse; arte v1 da planta vista de cima |
| Web | `marellar-web` + GitHub Pages | Motor compilado para JavaScript (TeaVM), cliente Godot exportado para Web, saves no navegador |
| M9 | Inspeção 3D (three.js) na `marellar-api` | Página `/inspecao/` com os 3 moldes montados pelos gates; NC no ponto clicado gravada em `inspecao/ncs.json` |
| M10 | Estrutura de produto e exterior 3D | `data/estrutura-produto.json` com as linhas 33/47/59 (barco → escopos STD/OPC → itens); cada item vira uma peça 3D clicável; a NC grava o código do item |
| M11 | Interiores, sistemas e inspeção por compartimento | Estrutura v3 (51/126/135 itens) com `compartimento`, `verificar` e `ref`; casaria, vidros, longarinas, anteparas, sala de máquinas e cabines; cortes, raio-X e primeira pessoa; catálogo ampliado `data/nc-catalogo-inspecao.json` (73 defeitos em 8 categorias) |
| 3D no jogo | Inspeção 3D dentro do Godot, em pixel art | **Inspecionar 3D** na ficha abre o barco no gate atual; vistas exterior, raio-X, corte, plantas e, por compartimento, De cima e Entrar; clicar numa peça abre a NC, que vira a ordem `RegistrarNcInspecao`, vai no save e aparece como marcador pela gravidade. Por enquanto a NC só registra (não gera retrabalho nem custo) |
| Sprint 1 | Fundação visual (design system) | Tokens (`tokens.gd`) e tema gerado por script; Pixelify Sans e Jersey 10; foco e navegação por mouse, teclado e controle; todo painel fecha por X, Esc/B, clique fora e o mesmo botão; abas G1/G2; botões desativados com dica; nenhum rótulo cortado; sons por evento, hitstop e tremor; cena de amostra `scenes/amostra_tema.tscn` |
| Gates | Gates e conselho revistos | 9 gates (DM a ET) com pesos de HH pela equipe por fase; demissão do CEO após 3 meses ruins seguidos (seção 5) |
| M12a | Treinamento explicado | O estado traz o efeito de cada nível e dos treinos; o painel Equipe mostra o efeito em números na dica de cada botão e no fim do treino |

### Próximo

O que falta das milestones M12 a M15 foi encaixado em sprints. A M16 (inspeção 3D em pixel art fora do jogo) foi substituída pelo **3D no jogo**.

| Sprint | Tema | Entregas | Pronto quando |
| --- | --- | --- | --- |
| 2 | Achar a diversão | Aviso de pausa (M12-1); travadinhas (M12-3); cascos novos (M12-9); vendedor, propostas, prazo e multa (M14); painel de gestão (M15); rodada curta de inspeção 2D | 3 de 5 jogadores pedem mais um mês |
| 3 | O mês 1 ensina jogando | Cenário enxuto do mês 1; mecânicas que chegam em 4 atos; dicas que apontam (M12-4); cartão de causa → custo em toda perda. O treinamento explicado (M12-7) já foi feito | 4 de 5 jogadores fecham o mês 1 sem ajuda |
| 4 | Impacto e charme | Equipe junto do barco (M12-5); barco na piscina (M12-6); arte pendente: popa, empilhadeira e sombra (M13); mesa do conselho (M15) | — |
| 5 | Profundidade | Barra de risco nas propostas; álbum ISO; NC do 3D gerando retrabalho e custo | Passa nas 6 métricas de diversão |

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
- por fim no cliente Godot (PC e navegador).

O resultado tem de ser o mesmo para a mesma semente.

---

## 2. Arquitetura

```
┌──────────────────────────┐      HTTP/JSON      ┌─────────────────────────┐
│  client-godot (Godot 4)  │  ─────────────────▶ │  marellar-api (Java 21) │
│  telas em pixel art      │  ◀───────────────── │  Spring Boot 3, REST    │
└────────────┬─────────────┘                     └────────────┬────────────┘
             │ no navegador: a mesma API                      │ usa
             │ via JavaScriptBridge                           │
┌────────────▼─────────────┐                     ┌────────────▼────────────┐
│ marellar-web (TeaVM)     │ ──── compila ─────▶ │ marellar-engine (Java)  │
│ motor em JavaScript      │                     │ simulação pura, sem     │
└──────────────────────────┘                     │ framework, 100% testável│
                                                 └────────────┬────────────┘
                                                              │ lê
                                                 ┌────────────▼────────────┐
                                                 │ data/*.json parâmetros  │
                                                 │ (somente sintéticos)    │
                                                 └─────────────────────────┘
```

| Camada | Tecnologia | Responsabilidade |
| --- | --- | --- |
| `marellar-engine` | **Java 21**, sem dependências além de Jackson | Regras: tempo, gates, NC, pessoas, caixa, eventos, conselho, valuation, saves |
| `marellar-cli` | **Java 21** | Roda uma partida pelo terminal e imprime o relatório do conselho |
| `marellar-api` | **Java 21 + Spring Boot 3** | Expõe a partida por REST; guarda partidas em memória e salva em JSON |
| `marellar-web` | **Java 21 → JavaScript** (TeaVM) | O motor e a API dentro do navegador, para o jogo rodar no GitHub Pages sem servidor |
| `client-godot` | **Godot 4** (GDScript) | Telas em pixel art e inspeção 3D; chamam a API (no navegador, o `marellar-web`) |
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
├── docs/
│   ├── decisoes.md                 # decisões tomadas na programação
│   ├── capturas/                   # capturas de tela das entregas
│   └── ferramentas/                # exportar_barco.mjs (3D → Godot), gerar_catalogo_inspecao.py, gerar_estrutura_produto.py
├── data/
│   ├── linha-33.json               # gates, HH, material, preço, cotas
│   ├── nc-catalogo.json            # tipos de NC, gravidade, probabilidade por gate, fator de nível da equipe
│   ├── nc-catalogo-inspecao.json   # catálogo ampliado da inspeção 3D (73 defeitos, 8 categorias)
│   ├── estrutura-produto.json      # linhas 33/47/59: escopos STD/OPC, itens, compartimentos
│   ├── pessoas.json                # equipe inicial (nomes fictícios), salários, níveis, treinos
│   ├── fornecedores.json           # 3 fornecedores com perfil escondido, PPM, prazo
│   ├── eventos.json                # eventos do MVP
│   ├── financeiro.json             # caixa inicial, despesas fixas, múltiplo, descontos, conselho
│   └── textos-ptbr.json            # mensagens, falas do conselho e dos personagens
├── marellar-engine/
│   └── src/main/java/br/marellar/engine/
│       ├── Simulacao.java          # fachada: novaPartida, avancar, aplicarOrdem, estado
│       ├── Partida.java, Relogio.java, Aleatorio.java, MesContabil.java
│       ├── dominio/                # Barco, NaoConformidade, NcInspecao3d, Rnc, Pessoa, Fornecedor,
│       │                           # Pedido, CotaAReceber, Lancamento, Evento e os enums
│       ├── regras/                 # Producao, Qualidade, Inspecao, Inspecao3d, Pessoas,
│       │                           # Fornecedor, Financeiro, Evento, Conselho
│       ├── agentes/                # Inspetor (personalidade)
│       ├── ordens/                 # Ordem (sealed interface) e implementações
│       ├── estado/                 # EstadoPartida: a visão que a API devolve
│       ├── relatorio/              # RelatorioConselho, Dre, IndicadoresQualidade, Valuation
│       ├── save/                   # Save com verificação SHA-256
│       └── dados/                  # carregamento e validação dos JSON de data/
├── marellar-cli/
│   └── src/main/java/br/marellar/cli/Main.java
├── marellar-api/
│   └── src/main/java/br/marellar/api/  # controllers, DTOs, PartidaService; página /inspecao/ (three.js)
├── marellar-web/                   # o motor compilado para JavaScript (TeaVM): a API dentro do navegador
│   └── src/main/java/br/marellar/web/  # MotorWeb (entrada JS), ServicoWeb (rotas), json/ (JSON sem reflexão)
├── web/                            # montagem do site: montar_site.sh, marellar-inicio.js, template do Godot
└── client-godot/
    ├── project.godot
    ├── scenes/                     # Fabrica.tscn (o jogo) e amostra_tema.tscn (design system)
    ├── scripts/                    # fabrica.gd, ficha_barco.gd, inspecao_3d.gd, formulario_nc_3d.gd, planta.gd,
    │                               # pixel_art.gd, sprites.gd, api.gd, tokens.gd, movimento.gd, sons.gd, ...
    ├── ferramentas/                # gerar_tema.gd, gerar_sons.gd, conferir_barco.gd
    └── assets/                     # barcos (modelo 3D da linha 33), fontes, sons, tema
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
| Fornecedores | 3 fornecedores sintéticos; PPM, prazo, scorecard A–F; RNC, 8D e **chargeback** |
| Finanças | Caixa, cotas do cliente, salários, material, DRE do mês, valor da empresa |
| Eventos | Quebra de peça, atraso de fornecedor, acidente de trabalho, pedido de demissão |
| Conselho | Reunião no fim do mês: metas, falas (sério + humor leve), 3 a 4 decisões para o mês seguinte |
| Telas | Fábrica (galpões G1 e G2), ficha do barco, inspeção 3D, painéis Equipe, Fornecedores e Conselho, saves |

### Fora do MVP (próximas versões)

- Linhas de 47 e 59 pés, segundo jogo de moldes.
- Obras e terreno. No MVP o layout já está pronto.
- Tela de Obras.
- CS1, CS2, bloqueio e troca de fornecedor. O MVP vai até o chargeback.
- ISO 9001, salões náuticos, pós-venda como negócio, Escola Marellar, tributos detalhados.
- Os 3 objetivos completos e a venda da empresa em 2031.

---

## 5. Regras do MVP

### 5.1 Gates da linha de 33 pés

Os barcos passam pelos gates abaixo, em ordem. A lista completa de 15 gates está no Documento de Visão; o MVP usa estes 9. Recebimento e inspeção de material comprado não são etapa do barco.

| Ordem | Código | Gate | Peso de HH | Cota do cliente ao aprovar |
| --- | --- | --- | --- | --- |
| 0 | — | Contrato assinado | — | **30%** |
| 1 | DM | Desmolde (casco laminado) | 8% | **10%** |
| 2 | BA | Barco aberto | 25% | — |
| 3 | BF | Barco fechado | 3% | **10%** |
| 4 | ST | Sistemas (estanqueidade diesel, isolamento, pressão, incêndio) | 25% | **10%** |
| 5 | CT | Collaudo terra (funcional completo em seco) | 27% | — |
| 6 | PI | Collaudo piscina (flutuação, trim, bombas, infiltração) | 5% | **10%** |
| 7 | TR | Carreta (Osasco → marina) | 1% | — |
| 8 | TM | Teste de mar (marina) | 3% | — |
| 9 | ET | Entrega técnica | 3% | **30%** |

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
- **Treinar:** a pessoa sai da linha por `diasTreinamento` (padrão 3 dias). São dois treinos:
  - **Técnico:** sobe 1 nível (máximo 5), de forma permanente: mais velocidade, menos chance de NC (pelo nível médio de quem trabalhou no gate) e salário maior.
  - **NR (segurança):** põe o treino em dia; com o treino vencido, a chance de acidente dobra.
- **1:1:** +5 de moral, gasta 0,5 h do líder. Sem 1:1 por mais de 30 dias, a moral cai 1 por dia.
- **Pedido de demissão:** chance diária maior com moral abaixo de 40.

### 5.7 Fornecedores (modelo automotivo, até o chargeback)

- **3 fornecedores sintéticos**, cada um com um perfil escondido: premium confiável, barato instável e grande indiferente. O motor tem um quarto perfil, pequeno dedicado, ainda sem fornecedor no `data/`.
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
- **Caixa negativo:** aparece alerta no conselho. Derrota após `mesesCaixaNegativoParaDerrota` (padrão 3) meses seguidos sem crédito.

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
- **Paciência do conselho:** de 0 a 5. Cada meta perdida tira 1; cada meta cumprida devolve 1, até o máximo. Aparece no painel como termômetro e não causa derrota.
- **Demissão do CEO (derrota):** mês ruim é aquele com 3 ou mais das 4 metas perdidas. No 3º mês ruim seguido o conselho demite o CEO; um mês que não é ruim zera a contagem (`mesesRuinsParaDemissao` em `data/financeiro.json`).
- **Decisões para o mês seguinte:** 3 a 4 opções com custo e efeito (treinar NR-35, contratar, antecipar compra, negociar com fornecedor). A escolha vira uma ordem aplicada no início do mês seguinte.

---

## 6. Modelo de dados (núcleo)

Use `record` do Java 21 para objetos de valor e classes para entidades com estado.

```java
// identificação e enums
enum Linha { L33 }                        // L47, L59 depois do MVP
enum CodigoGate { DM, BA, BF, ST, CT, PI, TR, TM, ET }
enum StatusBarco { EM_PRODUCAO, AGUARDANDO_INSPECAO, EM_INSPECAO, AGUARDANDO_DECISAO,
                   EM_RETRABALHO, APROVADO, PARADO_MATERIAL, ENTREGUE }
enum Gravidade { LEVE, MEDIA, CRITICA }
enum Origem { PROCESSO, FORNECEDOR }
enum DecisaoNc { RETRABALHO, CONCESSAO, RNC_FORNECEDOR }
enum Funcao { PRODUCAO, INSPETOR }
enum SituacaoPessoa { ATIVA, A_CAMINHO, EM_TREINAMENTO, AFASTADA, DESLIGADA }
enum Treinamento { TECNICO, SEGURANCA }
enum PerfilFornecedor { PREMIUM_CONFIAVEL, BARATO_INSTAVEL, GRANDE_INDIFERENTE, PEQUENO_DEDICADO }
enum MotivoDerrota { CAIXA, CONSELHO }

class Barco { String id /* ex.: "33-007" */; Linha linha; String cliente;
              CodigoGate gateAtual; StatusBarco status; boolean retido; int prioridade;
              BigDecimal hhTotalDoGate, hhRestantesNoGate, hhRetrabalhoPendente;
              List<CodigoGate> gatesAprovados; }
class NaoConformidade { String id, barcoId; CodigoGate gate; String tipo, descricao;
                        Gravidade gravidade; Origem origem; String fornecedorId;
                        BigDecimal hhRetrabalho, custoMaterial; StatusNc status; DecisaoNc decisao; }
class Pessoa { String id, nome; Funcao funcao; int nivel, moral; boolean treinoSegurancaEmDia;
               SituacaoPessoa situacao; Treinamento treinamentoEmCurso; String alocadoEm; }
class Fornecedor { String id, nome; PerfilFornecedor perfil /* escondido do jogador */;
                   long pecasRecebidas, pecasRejeitadas; /* nota A–F pelo scorecard */ }
record Lancamento(LocalDate data, TipoLancamento tipo, BigDecimal valor, String descricao) {}

// o que a API devolve (estado/EstadoPartida.java), resumido
record EstadoPartida(long semente, LocalDate data, int hora, int mes, Velocidade velocidade,
                     boolean pausado, MotivoPausa motivoPausa,
                     List<BarcoVisao> barcos, List<PessoaVisao> pessoas, InspetorVisao inspetor,
                     List<FornecedorVisao> fornecedores, Map<String, Integer> estoque, List<RncVisao> rncs,
                     SegurancaVisao seguranca, FinanceiroVisao financeiro, RelatorioConselho conselho,
                     MotivoDerrota derrota, List<NcInspecao3d> ncsInspecao, TreinamentosVisao treinamentos) {}
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
- `RegistrarNcInspecao(barcoId, codigoDefeito, gravidade, item, compartimento, x, y, z, observacao)`: NC marcada no ponto clicado do barco em 3D, no gate atual

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
| POST | `/partidas/carregar` | Carrega um save (`{"id": "..."}`); a partida volta para a memória com o mesmo id |
| GET | `/partidas/saves` | Lista os saves, do mais recente para o mais antigo |
| GET | `/inspecao/defeitos` | Catálogo ampliado da inspeção 3D (`data/nc-catalogo-inspecao.json`): código, descrição, categoria, gravidade, gates, defeito-base do motor e tipos de item |
| GET | `/inspecao/ncs?linha=59` | NCs da inspeção 3D de uma linha (33, 47 ou 59) |
| POST | `/inspecao/ncs` | Registra uma NC no ponto clicado: peça, posição (m), baliza 0–15, bordo (BB/BE/LC), zona, compartimento, gate, defeito e gravidade |
| PUT | `/inspecao/ncs/{id}` | Edita uma NC aberta: defeito, gravidade, gate, observação e o ponto (peça e posição); número, linha e data de registro não mudam |
| POST | `/inspecao/ncs/{id}/fechar` | Fecha a NC |
| DELETE | `/inspecao/ncs/{id}` | Exclui a NC |

- O motor **pausa sozinho**, devolvendo `"pausado": true` e o motivo, quando acontece acidente, NC crítica, conselho ou evento com escolha.
- Erros no formato `application/problem+json`.
- Catálogo ampliado da inspeção (`data/nc-catalogo-inspecao.json`, gerado por `docs/ferramentas/gerar_catalogo_inspecao.py`): 73 defeitos em 8 categorias (estrutural, estanqueidade, estético, dimensional, funcional, elétrico, movelaria, montagem). A NC envia em `codigoDefeito` o defeito detalhado e, opcionalmente, o `compartimento` (código da estrutura de produto, ex.: `SAL`); o servidor preenche `descricaoDefeito`, `categoria` e `codigoBase` (o defeito do motor em `data/nc-catalogo.json`, que não mudou). Compartimento que não existe nos itens da linha dá 422. NCs antigas com `[CODIGO]` no começo da observação são migradas ao carregar.
- Inspeção 3D: a gravidade pode **subir** em relação ao catálogo, mas não pode ficar abaixo dele (422). As NCs ficam em `inspecao/ncs.json` (`marellar.inspecao.arquivo`). A página em `/inspecao/` traz o three.js r187 junto (`static/inspecao/vendor`, licença MIT), então funciona sem internet.

---

## 8. Cliente Godot (MVP)

- **Resolução base 640×360**, escala inteira ×2 ou ×3, sem suavização (filtro *Nearest*).
- **Tile 16×16 px = 2×2 m.** Personagens com 10×16 a 16×24 px.
- **Paleta Endesga 32.** O cenário (zonas, piso, casco, pessoas) usa `scripts/paleta.gd`; a interface usa só o **design system** (`scripts/tokens.gd`), em que cada cor tem um significado fixo:

| Token | Cor | Uso |
| --- | --- | --- |
| `FUNDO` | `#181425` | Fundo das telas |
| `PAINEL` | `#3a4466` | Painéis, janelas, cartões |
| `TEXTO` / `TEXTO_CALMO` | `#ffffff` / `#c0cbdc` | Texto principal / rótulos e valores secundários |
| `MARCA` / `MARCA_CLARA` | `#124e89` / `#0099db` | Identidade Marellar, barra de cima |
| `ACAO` | `#feae34` | A única ação principal do painel (`BotaoAcao`) |
| `OK` | `#63c74d` | Aprovado, entregue, meta batida |
| `ATENCAO` | `#f77622` | Prazo em risco, estoque baixo; NC média |
| `NC` | `#e43b44` | Não conformidade, multa, perda; NC crítica |
| `NC_LEVE` | `#fee761` | NC leve (marcador da inspeção) |
| `FOCO` | `#2ce8f5` | Contorno do item em foco |

- **Tema:** `client-godot/assets/tema/marellar_theme.tres`, gerado a partir do `Tokens` por `ferramentas/gerar_tema.gd` (cantos chanfrados de 2 px, grade de 8 px). Não edite o `.tres` à mão. No máximo um `BotaoAcao` por painel.
- **Fontes:** Pixelify Sans 16 px no texto; Jersey 10 a 20 px nos títulos de tela e nos números de valor (a Pixelify sem suavização desenha o "5" parecido com "S"). Ambas OFL, sem antialiasing.
- **Telas** (mockups fora do repositório, veja a seção 11):
  1. **Fábrica:** galpões G1 (laminação) e G2 (montagem) em abas, baias com os barcos de 33 pés, bonequinhos andando e trabalhando, almoxarifado, barra de topo (data, velocidade, caixa, valor, moral, NCs, conselho) e barra de baixo com os menus.
  2. **Ficha do barco:** caminho pelos gates, HH e material, NCs com botões de decisão, cotas e o botão **Inspecionar 3D**.
  3. **Inspeção 3D:** o barco no gate atual, em pixel art; clicar numa peça registra uma NC.
  4. **Painéis laterais:** Equipe (contratar, 1:1, treinar), Fornecedores (scorecard, estoque, pedidos), Conselho e saves.
- **Arte v1:** planta vista de cima e sprites gerados em código (`pixel_art.gd`, `planta.gd`, `sprites.gd`). A arte final vem depois.

---

## 9. Testes obrigatórios

- **Determinismo:** duas partidas com a mesma semente e as mesmas ordens têm estados iguais (comparar o JSON).
- **Soma das cotas** = 100% do preço do barco.
- **Gate não aprova** com NC crítica aberta.
- **Chargeback** = custo documentado × 1,10.
- **Taxa de frequência e de gravidade** batem com as fórmulas da NBR 14280.
- **Sem material:** o barco fica parado e a hora improdutiva é registrada.
- **Valor da empresa** = EBITDA anualizado × múltiplo − descontos.
- **Arquitetura** (ArchUnit): o motor não depende de Spring e não usa `double` em dinheiro.
- **Paridade:** o motor no navegador (`marellar-web`) lê os mesmos parâmetros e escreve o mesmo JSON que a API (`ParidadeJacksonTest`).

---

## 10. Como rodar

Pré-requisitos: **JDK 21** e **Godot 4** (o Maven vem pelo wrapper).

```bash
./mvnw verify                                # build e testes (Windows: mvnw.cmd verify)
java -jar marellar-cli/target/marellar-cli.jar --semente 42 --meses 1
./mvnw -pl marellar-api spring-boot:run      # API em http://localhost:8080/api/v1
# inspeção 3D: com a API ligada, abrir http://localhost:8080/inspecao/
node docs/ferramentas/exportar_barco.mjs 33       # regera o modelo 3D do jogo (precisa de um Playwright instalado; veja o cabeçalho do script)
# abrir client-godot/project.godot no Godot 4 e rodar (com a API ligada)
godot --path client-godot -- --rodar          # opcional: abre o jogo já em x4
godot --path client-godot -- --dias 12 --selecionar 33-002   # opcional: pula dias e abre a ficha de um barco
godot --path client-godot -- --dias 40 --painel equipe       # opcional: abre direto no painel equipe ou fornecedores
godot --path client-godot -- --dias 12 --salvar              # opcional: salva depois de avançar; --carregar abre o último save
godot --path client-godot -- --dias 15 --selecionar 33-001 --ficha   # opcional: abre a ficha completa; --saves abre a lista de saves
godot --path client-godot -- --dias 6 --selecionar 33-002 --inspecao  # opcional: abre a inspeção 3D; --gate PI mostra outro gate (só para capturas); --comp CONS --vista dentro abre uma vista (ext, raiox, corte, inf, pri, cima, dentro)
godot --path client-godot res://scenes/amostra_tema.tscn   # amostra do design system: componentes, estados, sons e impacto
godot --headless --path client-godot --script res://ferramentas/gerar_tema.gd   # regera o tema a partir do tokens.gd
godot --headless --path client-godot --script res://ferramentas/gerar_sons.gd   # regera os sons provisórios
```

### Jogar no navegador (GitHub Pages)

O site https://samuelgomezdev.github.io/marellar/ é publicado pelo workflow `.github/workflows/pages.yml` a cada push na `main`. Não há servidor:

1. O `marellar-web` compila o `marellar-engine` para JavaScript com o **TeaVM** e expõe a **mesma API** da seção 7 como uma função (`marellarRequisitar(metodo, caminho, corpo)`), com as mesmas respostas e erros.
2. O cliente Godot é exportado para **Web** (sem threads, então roda no GitHub Pages sem cabeçalhos especiais). No navegador, o `api.gd` troca o HTTP por essa função via `JavaScriptBridge`; o resto do cliente não muda.
3. Os saves ficam no `localStorage` do navegador.
4. A inspeção 3D vai junto em `/inspecao/`. Sem a `marellar-api` ela entra em **modo demonstração**: mesmas regras de catálogo e gravidade, mas as NCs ficam só no `localStorage` daquele navegador. Para gravar no servidor, rode a API e abra `localhost:8080/inspecao/`.

O JSON no navegador não usa Jackson (precisa de reflexão): `CodecGerado.java` é gerado a partir dos records. Mudou um record do motor? Rode `./mvnw -pl marellar-web test -Dtest=CodecEmDiaTest -Dmarellar.gerarCodec=true`. O `ParidadeJacksonTest` garante que o navegador lê os mesmos parâmetros e escreve o mesmo JSON que a API.

Para montar o site localmente (precisa do Godot 4.7.2 e do template web):

```bash
python web/baixar_template_web.py 4.7.2 "$APPDATA/Godot/export_templates"   # Linux: ~/.local/share/godot/export_templates
GODOT=/caminho/do/godot bash web/montar_site.sh
cd client-godot/export/web && python -m http.server 8000                   # abrir http://localhost:8000
```

A CLI tem um jogador automático: NC de material vira RNC, as outras vão para retrabalho, e ele faz 1:1 com quem está com moral abaixo de 70. Use `--sem-decisoes` para desligar.

Tudo se joga com o mouse pela barra de baixo: velocidade (**Pausa**, **x1**, **x2**, **x4**), **Ficha**, **Prior. 1**, **Segurar**, **Equipe** (contratar, 1:1, treinar), **Fornec.** (scorecard, estoque, pedidos), **Conselho** (última reunião), **Salvar** e **Carregar** (lista de saves). Clique num barco para selecioná-lo; **duplo clique** abre a ficha completa (caminho pelos gates, horas-homem, material, NCs com **Retrabalho**, **Concessão** ou **RNC**, e cotas do cliente). Na ficha, **Inspecionar 3D** abre o barco em 3D no estado do gate atual: arraste para girar, roda ou setas aproximam, Esc volta. Passe o mouse para ver o item; clique numa peça para marcar uma NC (defeito do catálogo ampliado, gravidade a partir da mínima, observação).

Também se joga só com teclado ou só com controle: setas ou D-pad movem o foco, Enter ou A confirmam, Esc ou B fecham o painel de cima. Atalhos de teclado: **Espaço** pausa, **1/2/3** velocidade, **Enter** ficha, **F1** Equipe, **F2** Fornecedores, **F5** salva, **F9** carrega o último save, **Esc** volta.

---

## 11. Documentos de referência

Os documentos de design ficam **fora do repositório**, na pasta local do projeto, porque o Documento de Visão cita dados reais:

- `Marellar_Documento_de_Visao.docx`: regras e números (fonte da verdade).
- `Marellar_GDD.docx`: game design.
- `Marellar_GDD_visual.pptx`: apresentação visual.
- `telas/*.png`: mockups das telas (fábrica, barco, pessoas, conselho, obras, fornecedores).

## Licença

Código sob [MIT](LICENSE). Fontes [Pixelify Sans](https://fonts.google.com/specimen/Pixelify+Sans) e [Jersey 10](https://fonts.google.com/specimen/Jersey+10) sob SIL Open Font License 1.1 (licenças em `client-godot/assets/fontes/`).
