class_name Paleta
extends RefCounted
## Paleta Endesga 32 com papéis fixos (GDD, Direção de arte).
## O jogador lê a fábrica pela cor, como na planta do layout.

const PRODUCAO_SUJA := Color("#b86f50")
const PRODUCAO_SUJA_ESCURA := Color("#733e39")
const MONTAGEM_LIMPA := Color("#63c74d")
const MONTAGEM_LIMPA_ESCURA := Color("#3e8948")
const AGUA := Color("#0099db")
const AGUA_ESCURA := Color("#124e89")
const ARMAZEM := Color("#5a6988")
const ARMAZEM_ESCURO := Color("#3a4466")
const NAO_CONFORME := Color("#e43b44")
const NAO_CONFORME_ESCURO := Color("#a22633")
const CASCO := Color("#ffffff")
const CASCO_SOMBRA := Color("#c0cbdc")
const PISO := Color("#c0cbdc")
const PISO_ESCURO := Color("#8b9bb4")
const FUNDO := Color("#262b44")
const FUNDO_ESCURO := Color("#181425")
const TEXTO := Color("#ead4aa")
const TEXTO_CLARO := Color("#ffffff")
const ALERTA := Color("#feae34")
const ALERTA_ESCURO := Color("#f77622")
const CRITICO := Color("#ff0044")
const PELE := Color("#e8b796")
const PELE_ESCURA := Color("#c28569")
const CABELO := Color("#3f2832")
const MADEIRA := Color("#be4a2f")
const METAL := Color("#8b9bb4")
const TECA := Color("#e4a672")
const TECA_ESCURA := Color("#b86f50")
const MADEIRA_ESCURA := Color("#3e2731")
const TIJOLO := Color("#be4a2f")
const TIJOLO_ESCURO := Color("#733e39")
const FAIXA := Color("#fee761")

## As 32 cores da Endesga 32, na ordem da paleta original. A inspeção 3D reduz cada pixel a uma delas.
const ENDESGA_32: Array[Color] = [
	Color("#be4a2f"), Color("#d77643"), Color("#ead4aa"), Color("#e4a672"), Color("#b86f50"), Color("#733e39"), Color("#3e2731"), Color("#a22633"),
	Color("#e43b44"), Color("#f77622"), Color("#feae34"), Color("#fee761"), Color("#63c74d"), Color("#3e8948"), Color("#265c42"), Color("#193c3e"),
	Color("#124e89"), Color("#0099db"), Color("#2ce8f5"), Color("#ffffff"), Color("#c0cbdc"), Color("#8b9bb4"), Color("#5a6988"), Color("#3a4466"),
	Color("#262b44"), Color("#181425"), Color("#ff0044"), Color("#68386c"), Color("#b55088"), Color("#f6757a"), Color("#e8b796"), Color("#c28569"),
]
