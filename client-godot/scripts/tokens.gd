class_name Tokens
extends RefCounted
## Design system do Marellar (GDD, "Jogabilidade: princípios e design system", 20/11/2026).
## Única fonte das cores, medidas e tempos da interface. Cada cor tem UM significado e não é usada para outra coisa.
## As cores do cenário (zonas, piso, pele, casco...) ficam em Paleta.
## O tema (assets/tema/marellar_theme.tres) é gerado a partir daqui por ferramentas/gerar_tema.gd.

# ---------------------------------------------------------------- cores (Endesga 32)

const FUNDO := Color("#181425")         ## fundo das telas
const PAINEL := Color("#3a4466")        ## painéis, janelas, cartões
const TEXTO := Color("#ffffff")         ## texto principal
const TEXTO_CALMO := Color("#c0cbdc")   ## rótulos, valores secundários
const MARCA := Color("#124e89")         ## identidade Marellar, barra superior
const MARCA_CLARA := Color("#0099db")   ## identidade Marellar (destaque)
const ACAO := Color("#feae34")          ## a ÚNICA ação principal do painel (BotaoAcao)
const OK := Color("#63c74d")            ## aprovado, entregue, meta batida
const ATENCAO := Color("#f77622")       ## prazo em risco, estoque baixo
const NC := Color("#e43b44")            ## não conformidade, multa, perda
const FOCO := Color("#2ce8f5")          ## contorno do item em foco (mouse, teclado, controle)
const NC_LEVE := Color("#fee761")       ## gravidade leve de NC (marcador da inspeção); média = ATENCAO, crítica = NC

## Cor de cada gravidade de NC, na ordem leve, média, crítica.
const GRAVIDADE := {"LEVE": NC_LEVE, "MEDIA": ATENCAO, "CRITICA": NC}

# ---------------------------------------------------------------- forma

const CHANFRO := 2      ## canto chanfrado de 2 px (9-slice)
const GRADE := 8        ## espaçamentos são múltiplos de 8
const BORDA := 1        ## espessura de contorno (inclui o foco)

# ---------------------------------------------------------------- tela

const TELA_BASE := Vector2i(640, 360)   ## resolução base, escala inteira (project.godot)

# ---------------------------------------------------------------- fontes

const FONTE_UI := "res://assets/fontes/PixelifySans-Variavel.ttf"
const FONTE_TITULO := "res://assets/fontes/Jersey10-Regular.ttf"
const FONTE_NUMERO := "res://assets/fontes/Jersey10-Regular.ttf"   ## números de valor (a Pixelify desenha 5 ≈ S e 3 ≈ 8)
const TAMANHO_UI := 16
const TAMANHO_TITULO := 20
const TAMANHO_NUMERO := 20   ## Jersey 10 só fica com pixel exato em múltiplos de 10 px

# ---------------------------------------------------------------- movimento

const MOV_UI_MIN := 0.12        ## segundos
const MOV_UI_MAX := 0.20        ## segundos
const MOV_UI := 0.16            ## duração padrão de abrir painel / trocar aba
const MOV_UI_TRANSICAO := Tween.TRANS_CUBIC
const MOV_UI_SUAVIZACAO := Tween.EASE_OUT   ## saída suave
const IMPACTO_MIN := 3          ## quadros a 60 fps (hitstop)
const IMPACTO_MAX := 6


## Espaçamento em múltiplos da grade: grade(2) = 16 px.
static func grade(n: int) -> int:
	return n * GRADE
