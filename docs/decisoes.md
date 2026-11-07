# Decisões de implementação

Registro das decisões tomadas durante a programação, quando a especificação (README) não dizia o suficiente. Cada decisão vira um parâmetro em `data/` sempre que possível.

| Data | Decisão | Por quê | Onde |
| --- | --- | --- | --- |
| 2026-11-07 | Arquitetura mantida: motor e API em Java 21, cliente em Godot 4. No celular, o jogo vai precisar de um servidor rodando a API (ou de um port do motor). | Escolha do Samuel: o motor em Java é o centro do portfólio. | README §2 |
| 2026-11-07 | Documentos de design (Visão, GDD, mockups) ficam **fora** do repositório público. | O Documento de Visão cita empresa e dados reais. | `.gitignore` |
| 2026-11-07 | Parâmetros financeiros em `data/financeiro.json` (arquivo extra, não listado na spec). | Caixa inicial, despesas fixas, múltiplo e descontos são parâmetros e precisavam de um lugar. | `data/financeiro.json` |
| 2026-11-07 | Limite de `maxPessoasPorBarco` = 6. | A spec manda as pessoas livres para o barco de maior prioridade, mas sem limite os 12 iriam para um só barco. | `data/linha-33.json` |
| 2026-11-07 | Inspeção dura `ticksInspecao` = 2 ticks por gate e o inspetor atende um barco por vez, em ordem de chegada. | A spec fala de fila de inspeção (`fatorCarga`) mas não do tempo de inspeção. No M3 o tempo passa a depender da personalidade. | `data/linha-33.json` |
| 2026-11-07 | Identificadores (barco, pessoa, NC) são `String` nas ordens e na API, em vez de `record BarcoId`. | JSON mais simples para o cliente Godot; o formato do id (ex.: `33-007`) é validado no motor. | `ordens/` |
| 2026-11-07 | A pausa automática é um aviso: o motor para o `avancar` no tick do evento e devolve `pausado: true`; a próxima chamada de `avancar` continua. | O cliente controla o relógio; quem decide parar o loop é ele. | `Simulacao` |
| 2026-11-07 | Maven Wrapper (`mvnw`) no repositório. | Ninguém precisa instalar Maven para rodar o projeto. | raiz |
