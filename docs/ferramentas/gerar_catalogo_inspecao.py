# Gera data/nc-catalogo-inspecao.json: catalogo ampliado de defeitos da inspecao 3D.
# Nao altera data/nc-catalogo.json (o do motor do jogo, com probabilidades). Cada defeito daqui aponta para um
# defeito-base do motor ('base'), usado para validar a gravidade na API; a gravidade minima nunca fica abaixo da do base.
# categoria: ESTRUTURAL, ESTANQUEIDADE, ESTETICO, DIMENSIONAL, FUNCIONAL, ELETRICO, MOVELARIA, MONTAGEM
# aplica: tipos de item (MOLDE, FRP, TECA, VIDRO, INOX, ESTOFADO, MOVEL, ELETRICA, COMPRADO) em que o defeito e sugerido primeiro
import json, sys
T = []
def t(cod, desc, cat, grav, gates, base, aplica):
    T.append({"codigo": cod, "descricao": desc, "categoria": cat, "gravidade": grav, "gates": gates.split(), "base": base, "aplica": aplica.split()})
L, M, C = "LEVE", "MEDIA", "CRITICA"
# --- estrutural (laminacao) ---
t("LAM-DELAM", "Delaminação (camadas do laminado soltas)", "ESTRUTURAL", C, "DM BA ST TM", "LAM-SECO", "MOLDE FRP")
t("LAM-BOLHA", "Bolha de ar na laminação", "ESTRUTURAL", M, "DM", "LAM-BOLHA", "MOLDE FRP")
t("LAM-SECO", "Fibra seca (resina insuficiente)", "ESTRUTURAL", C, "DM", "LAM-SECO", "MOLDE FRP")
t("LAM-ESPESSURA", "Espessura do laminado abaixo do plano", "ESTRUTURAL", C, "DM", "LAM-SECO", "MOLDE FRP")
t("LAM-CURA", "Cura incompleta (dureza Barcol baixa)", "ESTRUTURAL", M, "DM", "LAM-BOLHA", "MOLDE FRP")
t("LAM-NUCLEO", "Núcleo (espuma/balsa) descolado ou com vazio", "ESTRUTURAL", C, "DM BA", "LAM-SECO", "MOLDE FRP")
t("EST-COLAGEM", "Colagem estrutural com vazio (longarina, antepara, casco-convés)", "ESTRUTURAL", C, "BA BF", "LAM-SECO", "MOLDE FRP")
t("EST-TRINCA", "Trinca estrutural (passa do gelcoat para o laminado)", "ESTRUTURAL", C, "DM BA ST TM", "LAM-SECO", "MOLDE FRP")
t("EST-REFORCO", "Reforço ou inserto faltando ou fora de posição", "ESTRUTURAL", M, "DM BA", "LAM-BOLHA", "MOLDE FRP INOX")
# --- estanqueidade / infiltracao ---
t("TEC-INFILTRA", "Infiltração sob a teca (água entre a teca e a fibra)", "ESTANQUEIDADE", C, "ST PI TM", "EST-INFILTRA", "TECA MOLDE")
t("TEC-CALAFETO", "Calafeto da teca solto, falhado ou com bolha", "ESTANQUEIDADE", M, "ST PI", "VID-VEDACAO", "TECA")
t("TEC-DESCOLA", "Régua de teca descolada ou soando oca", "ESTANQUEIDADE", M, "ST PI", "VID-VEDACAO", "TECA")
t("EST-INFILTRA", "Infiltração no teste de mangueira (convés, casaria, tampas)", "ESTANQUEIDADE", C, "ST", "EST-INFILTRA", "MOLDE FRP VIDRO")
t("VID-VEDACAO", "Vidro com falha de vedação", "ESTANQUEIDADE", M, "ST PI", "VID-VEDACAO", "VIDRO")
t("EST-PASSACASCO", "Passa-casco ou válvula de fundo com vazamento", "ESTANQUEIDADE", C, "ST PI", "EST-INFILTRA", "MOLDE COMPRADO")
t("EST-TANQUE", "Tanque reprovado no teste de estanqueidade", "ESTANQUEIDADE", C, "BA ST", "EST-INFILTRA", "COMPRADO")
t("EST-DRENO", "Dreno entupido ou caimento invertido (água empoçada)", "ESTANQUEIDADE", M, "ST PI", "VID-VEDACAO", "MOLDE FRP")
t("EST-TAMPA", "Tampa ou escotilha sem vedação (borracha, calha)", "ESTANQUEIDADE", M, "ST", "VID-VEDACAO", "FRP")
t("HID-VAZAMENTO", "Vazamento em tubulação ou conexão hidráulica", "ESTANQUEIDADE", M, "ST CT", "VID-VEDACAO", "COMPRADO MOVEL")
# --- estetico ---
t("GEL-TRINCA", "Trinca fina no gelcoat (aranha)", "ESTETICO", L, "DM BA ST", "GEL-TRINCA", "MOLDE FRP")
t("GEL-LOTE", "Gelcoat com cor fora do padrão", "ESTETICO", M, "DM", "GEL-LOTE", "MOLDE FRP")
t("GEL-POROSIDADE", "Porosidade ou furinhos no gelcoat", "ESTETICO", L, "DM", "GEL-TRINCA", "MOLDE FRP")
t("GEL-MARCA", "Marca de fibra ou de molde aparente (print-through)", "ESTETICO", L, "DM PI", "GEL-TRINCA", "MOLDE FRP")
t("GEL-REPARO", "Reparo de gelcoat aparente (diferença de cor ou brilho)", "ESTETICO", L, "PI ET", "ACA-RISCO", "MOLDE FRP")
t("PIN-ESCORRIDO", "Pintura ou verniz escorrido", "ESTETICO", L, "CT PI", "ACA-RISCO", "MOLDE FRP MOVEL")
t("PIN-CASCA", "Casca de laranja ou sujeira na pintura", "ESTETICO", L, "CT PI", "ACA-RISCO", "MOLDE FRP MOVEL")
t("POL-HOLOGRAMA", "Polimento com hologramas ou queimado", "ESTETICO", L, "PI ET", "ACA-RISCO", "MOLDE FRP INOX")
t("ACA-RISCO", "Risco de acabamento", "ESTETICO", L, "PI ET", "ACA-RISCO", "MOLDE FRP VIDRO INOX MOVEL")
t("ACA-MANCHA", "Mancha, cola ou silicone aparente", "ESTETICO", L, "CT PI ET", "ACA-RISCO", "MOLDE FRP VIDRO ESTOFADO MOVEL TECA")
t("INX-OXIDACAO", "Inox com oxidação, solda sem acabamento ou sem polimento", "ESTETICO", L, "ST PI", "ACA-RISCO", "INOX")
t("ESF-COSTURA", "Estofado com costura torta, ruga ou mancha", "ESTETICO", L, "CT ET", "ACA-RISCO", "ESTOFADO")
t("TEC-COR", "Teca com diferença de tom ou veio fora do padrão", "ESTETICO", L, "ST PI", "ACA-RISCO", "TECA")
t("VID-RISCO", "Vidro riscado, lascado ou com serigrafia falhada", "ESTETICO", M, "ST PI", "VID-VEDACAO", "VIDRO")
t("TRA-AVARIA", "Avaria de manuseio ou transporte", "ESTETICO", M, "TR ET", "TRA-AVARIA", "MOLDE FRP VIDRO INOX MOVEL")
# --- dimensional ---
t("DIM-FORA", "Medida fora da tolerância do desenho", "DIMENSIONAL", M, "DM BA BF ST", "LAM-BOLHA", "MOLDE FRP VIDRO INOX MOVEL")
t("DIM-SIMETRIA", "Assimetria entre bombordo e boreste", "DIMENSIONAL", M, "DM BF", "LAM-BOLHA", "MOLDE FRP")
t("DIM-EMPENO", "Peça empenada ou torcida", "DIMENSIONAL", M, "DM BA", "LAM-BOLHA", "MOLDE FRP MOVEL")
t("DIM-FOLGA", "Folga ou vão irregular entre peças (gap)", "DIMENSIONAL", L, "BF ST CT", "MON-FIXACAO", "FRP VIDRO MOVEL")
t("DIM-FURACAO", "Furação ou recorte fora de posição", "DIMENSIONAL", M, "BA BF ST", "LAM-BOLHA", "MOLDE FRP INOX")
t("DIM-NIVEL", "Piso ou tampo fora de nível ou de prumo", "DIMENSIONAL", L, "BA CT", "MON-FIXACAO", "FRP MOVEL")
t("DIM-ALINHA", "Desalinhamento de linha de eixo ou de motor", "DIMENSIONAL", C, "BA TM", "MOT-VIBRACAO", "COMPRADO")
t("DIM-LINHADAGUA", "Linha d'água ou linha de beleza fora da marcação", "DIMENSIONAL", L, "PI", "ACA-RISCO", "MOLDE")
# --- funcional (mecanica e hidraulica) ---
t("FUN-NAOOPERA", "Equipamento não funciona", "FUNCIONAL", M, "CT TM", "ELE-PAINEL", "COMPRADO ELETRICA")
t("FUN-INTERMITENTE", "Equipamento funciona de forma intermitente", "FUNCIONAL", M, "CT TM", "ELE-PAINEL", "COMPRADO ELETRICA")
t("FUN-RUIDO", "Ruído ou vibração anormal em equipamento", "FUNCIONAL", M, "CT TM", "ELE-PAINEL", "COMPRADO")
t("MOT-VIBRACAO", "Vibração anormal no motor", "FUNCIONAL", C, "TM", "MOT-VIBRACAO", "COMPRADO")
t("MOT-VAZAMENTO", "Vazamento de óleo, diesel ou água no motor ou gerador", "FUNCIONAL", C, "BA TM", "MOT-VIBRACAO", "COMPRADO")
t("FUN-PORTA", "Porta, tampa ou gaveta não fecha, não trava ou raspa", "FUNCIONAL", L, "CT ET", "MON-FIXACAO", "MOVEL FRP VIDRO")
t("FUN-CURSO", "Curso incompleto (pedestal, elevador de TV, escada, pistão)", "FUNCIONAL", M, "CT", "ELE-PAINEL", "COMPRADO MOVEL INOX TECA")
t("FUN-AR", "Ar-condicionado não atinge a temperatura", "FUNCIONAL", M, "CT TM", "ELE-PAINEL", "COMPRADO")
t("FUN-BOMBA", "Bomba (porão, água doce, esgoto) sem vazão ou sem automático", "FUNCIONAL", C, "CT TM", "EST-INFILTRA", "COMPRADO")
t("FUN-DIRECAO", "Direção, manete ou joystick com folga ou sem resposta", "FUNCIONAL", C, "TM", "MOT-VIBRACAO", "COMPRADO ELETRICA")
t("FUN-ESTABILIZADOR", "Estabilizador com alarme ou sem redução de balanço", "FUNCIONAL", M, "TM", "ELE-PAINEL", "COMPRADO")
# --- eletrico e eletronico ---
t("ELE-COMPONENTE", "Falha em componente eletrônico (tela, módulo, placa, sensor)", "ELETRICO", M, "CT TM", "ELE-PAINEL", "ELETRICA COMPRADO")
t("ELE-PAINEL", "Painel elétrico com defeito", "ELETRICO", M, "CT", "ELE-PAINEL", "ELETRICA")
t("ELE-CHICOTE", "Chicote elétrico sem identificação", "ELETRICO", M, "BF CT", "ELE-CHICOTE", "ELETRICA")
t("ELE-CABO", "Cabo sem proteção, mal fixado ou com raio de curva apertado", "ELETRICO", M, "BA BF CT", "ELE-CHICOTE", "ELETRICA")
t("ELE-CONEXAO", "Conexão frouxa, oxidada ou aquecendo", "ELETRICO", C, "CT TM", "EST-INFILTRA", "ELETRICA")
t("ELE-CURTO", "Curto, fuga de corrente ou disjuntor desarmando", "ELETRICO", C, "CT TM", "EST-INFILTRA", "ELETRICA")
t("ELE-REDE", "Falha de comunicação (NMEA 2000, CAN, sensores sem leitura)", "ELETRICO", M, "CT TM", "ELE-PAINEL", "ELETRICA")
t("ELE-ILUMINA", "Luminária ou LED queimado, piscando ou com tom diferente", "ELETRICO", L, "CT ET", "MON-FIXACAO", "ELETRICA MOVEL")
t("ELE-BATERIA", "Bateria ou carregador fora da tensão", "ELETRICO", M, "CT TM", "ELE-PAINEL", "ELETRICA")
t("ELE-SOFTWARE", "Software ou configuração de equipamento desatualizada", "ELETRICO", L, "CT TM", "MON-FIXACAO", "ELETRICA")
# --- movelaria ---
t("MOV-ALINHA", "Frentes de móvel desalinhadas ou com folga desigual", "MOVELARIA", L, "BA CT", "MON-FIXACAO", "MOVEL")
t("MOV-VERNIZ", "Verniz com falha, bolha ou diferença de brilho", "MOVELARIA", L, "CT", "ACA-RISCO", "MOVEL")
t("MOV-LAMINA", "Lâmina de madeira descolada ou lascada", "MOVELARIA", M, "BA CT", "ELE-CHICOTE", "MOVEL")
t("MOV-RANGIDO", "Móvel ou piso rangendo ou solto", "MOVELARIA", L, "CT TM", "MON-FIXACAO", "MOVEL FRP")
t("MON-FIXACAO", "Fixação solta em móvel", "MOVELARIA", L, "BA BF", "MON-FIXACAO", "MOVEL")
t("MOV-FERRAGEM", "Ferragem (dobradiça, corrediça, fecho) com defeito", "MOVELARIA", L, "CT", "MON-FIXACAO", "MOVEL")
# --- montagem ---
t("MON-FALTA", "Item faltando em relação à estrutura de produto", "MONTAGEM", M, "BA BF ST CT", "ELE-CHICOTE", "MOLDE FRP VIDRO INOX ESTOFADO MOVEL ELETRICA COMPRADO TECA")
t("MON-ERRADO", "Item diferente do especificado (modelo, cor, lado)", "MONTAGEM", M, "BA BF ST CT", "ELE-CHICOTE", "FRP VIDRO INOX ESTOFADO MOVEL ELETRICA COMPRADO TECA")
t("MON-TORQUE", "Parafuso sem torque, sem trava ou sem contraplaca", "MONTAGEM", M, "BA ST", "ELE-CHICOTE", "INOX COMPRADO")
t("MON-SELANTE", "Selante faltando ou aplicado em excesso", "MONTAGEM", L, "ST CT", "MON-FIXACAO", "INOX VIDRO FRP TECA")
ordem = {"LEVE": 0, "MEDIA": 1, "CRITICA": 2}
BASE = {"LAM-BOLHA": M, "LAM-SECO": C, "GEL-TRINCA": L, "GEL-LOTE": M, "MON-FIXACAO": L, "ELE-CHICOTE": M, "ELE-PAINEL": M,
        "EST-INFILTRA": C, "VID-VEDACAO": M, "ACA-RISCO": L, "MOT-VIBRACAO": C, "TRA-AVARIA": M}
for x in T:
    assert x["base"] in BASE, x
    assert ordem[x["gravidade"]] >= ordem[BASE[x["base"]]], ("gravidade abaixo do base", x["codigo"])
assert len({x["codigo"] for x in T}) == len(T)
dados = {"versao": 1,
         "observacao": "Catalogo ampliado da inspecao 3D. 'base' e o defeito do data/nc-catalogo.json usado para validar a gravidade na API.",
         "categorias": {"ESTRUTURAL": "Estrutural (laminação)", "ESTANQUEIDADE": "Estanqueidade e infiltração", "ESTETICO": "Estético",
                        "DIMENSIONAL": "Dimensional", "FUNCIONAL": "Funcional", "ELETRICO": "Elétrico e eletrônico",
                        "MOVELARIA": "Movelaria", "MONTAGEM": "Montagem"},
         "tipos": T}
json.dump(dados, open(sys.argv[1] if len(sys.argv) > 1 else "data/nc-catalogo-inspecao.json", "w", encoding="utf-8"), ensure_ascii=False, indent=1)
from collections import Counter
print(len(T), "defeitos", dict(Counter(x["categoria"] for x in T)))
