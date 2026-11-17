# Gera data/estrutura-produto.json da Marellar (linhas 33, 47 e 59) - versao 2.
# Modelo do SD4: barco -> escopos STD/OPC -> itens. Codigos proprios (nenhum codigo ou fornecedor real).
# Tipos: MOLDE (peca grande de fibra), FRP (peca pequena de fibra), INOX, TECA, ESTOFADO, VIDRO, COMPRADO,
#        MOVEL (marcenaria propria), ELETRICA (eletrica e eletronica, terceirizada).
# 'modelo3d' liga o item ao desenho na pagina; 'peca3d' diz em qual grupo da montagem ele entra.
# v3 (16/11/2026): 'Cockpit' vira 'Praca de popa'; paiol de popa com itens; arco/mastro do radar e antenas; itens basicos da 33
#     (bombas, baterias, carregador, agua doce, WC no console, flaps) pela referencia de 33 pes do mercado;
#     47/59: ar-condicionado por ambiente e sistemas tirados dos checklists FM-CQ013 e FM-CQ034 da pasta de referencias.
# v2: 'compartimento' (onde o item fica no barco), 'verificar' (pontos de inspecao) e 'ref' (checklist de referencia
#     da pasta 01_Referencias_Externas); estrutura do fundo, sistemas da sala de maquinas e interiores.
#     Os codigos da v1 nao mudam: fibra nova usa VTR-<linha>-1xx; estrutura STR-, marcenaria MOV-, eletrica ELE-, hidraulica HID-.
import json, sys

FONTES = {
    "IPS950": "Volvo Penta D11-IPS950: 6 cil., 10,84 L, 725 hp; pacote ~1.800 kg (darthaven.co.uk; pubs.volvopenta.com 47709250)",
    "IPS800": "Volvo Penta D8-IPS800: 6 cil., 7,7 L, 441 kW/600 hp; D8 1.261 mm sem reversor, 987 mm de largura (pubs.volvopenta.com 47708360; yachtbuyer.com)",
    "V10": "Mercury Verado V10 350: espacamento de 26 pol (660 mm); 709 lb (~322 kg) com rabeta de 25 pol (soundingsonline.com; magzter.com)",
    "GER20": "Classe Kohler 20 kW com cabine acustica: 1.137 x 635 x 690 mm, ~382 kg (ficha G2-87 20EOZD; tdxltd.co.nz)",
    "GER13": "Classe Kohler 13 kW com cabine acustica: 992 x 601 mm de base, ~395 kg (ficha G2-171 13EKOZD; tdxltd.co.nz); altura aproximada",
    "SK9": "Seakeeper 9: 0,85 x 0,90 x 0,72 m, 550 kg, barcos de ~50-59 pes ate ~30 t (seakeeper.com/seakeeper_products/seakeeper-9)",
    "SK6": "Seakeeper 6: barcos de 44-52 pes ate ~24 t (twindisc.com.au); envelope aproximado",
    "MC2": "Quick MC2 X19: 18.700 N.m, 553 kg, refrigerado a ar (ficha CTDX19ACEN00); envelope ~0,61 x 0,61 x 0,67 m lido do desenho, a confirmar",
    "MC2P": "Quick MC2 X13: 12.850 N.m (boattest.com/quick-gyro-stabilizers); envelope aproximado",
    "SK2": "Seakeeper 2: barcos de ~30-36 pes ate ~7,5 t; 188 kg; 25,5 x 24,6 x 20,4 pol (seakeeper.com)",
    "BW330": "Referencia de mercado: Boston Whaler 330 Outrage traz bomba de porao automatica a vante e 2 a re, 3 bandejas de bateria com chaves, separadores agua/combustivel, agua doce de 40 gal (~150 L) e carregador com tomada de cais e isolador galvanico (bostonwhaler.com, ficha 2024)",
    "FIN": "Aletas eletricas: CMC Waveless STAB 25 usa aletas de 0,65 ou 0,80 m2 em iates de 18-24 m (nauticexpo.com); Humphree oferece 0,60 a 1,50 m2",
}
REF = {
    "motor": "REF-FM-QUA-019 Checklist de Motores e Linha de Eixo (FM-CQ031)",
    "gerador": "REF-FM-QUA-020 Checklist de Gerador (FM-CQ032)",
    "giro": "REF-FM-QUA-021 Checklist de Seakeeper (FM-CQ033)",
    "incendio": "REF-FM-QUA-022 Checklist do Sistema de Incendio (FM-CQ037)",
    "tanque": "REF-FM-QUA-023 Teste de Estanqueidade de Tanques de Diesel (FM-CQ038)",
    "ar": "REF-FM-QUA-005 Checklist de Ar Condicionado",
    "movel": "REF-IT-QUA-001 Criterios de Inspecao de Movelaria",
    "pintura": "REF-IT-QUA-002 Criterios de Inspecao de Pintura",
    "gel": "REF-IT-QUA-003 Lista de Defeitos de Gelcoat em Produtos VTR",
    "casco": "REF-PC-QUA-002 Plano de Controle Casco",
    "conves": "REF-PC-QUA-003 Plano de Controle Conves",
    "casaria": "REF-PC-QUA-001 Plano de Controle Casaria",
    "eletrica": "REF-TR-PRD-001 Carregador de Baterias e Bomba de Porao",
    "geral": "REF-FM-QUA-007 Checklist Geral de Embarcacoes (FM-CQ013)",
    "sistemas": "REF-FM-QUA-007 Checklist Geral (FM-CQ013), fase Sistemas",
    "collaudo": "REF-FM-QUA-007 Checklist Geral (FM-CQ013), fase Collaudo Terra",
    "mar": "REF-FM-QUA-009 Checklist de Seatrial (FM-CQ017)",
}
# modelo3d -> (compartimento, [pontos a verificar], ref)
V = {
 "casco": ("CASCO", ["Espessura e cura do laminado", "Gelcoat: bolhas, trincas, porosidade", "Simetria BB/BE e linha d'agua"], "casco"),
 "frisos": ("CASCO", ["Frisos sem falha de laminacao", "Alinhamento com a quilha"], "casco"),
 "vidrosCasco": ("CASCO", ["Vedacao no teste de mangueira", "Serigrafia sem falha", "Folga uniforme no rebaixo"], "geral"),
 "saidaAgua": ("CASCO", ["Vedacao da tampa", "Altura acima da linha d'agua"], "geral"),
 "tomadaAr": ("CASCO", ["Fixacao da grelha", "Area livre de passagem de ar"], "geral"),
 "aletas": ("CASCO", ["Vedacao do passa-casco", "Folga e curso da aleta", "Reforco do fundo na base do atuador"], "giro"),
 "longarinas": ("SM", ["Colagem ao fundo sem vazios (bater com martelo)", "Delaminacao na base", "Alinhamento dos calcos do motor"], "casco"),
 "hastilhas": ("SM", ["Colagem ao fundo e as longarinas", "Furos de drenagem (bueiros) livres"], "casco"),
 "anteparas": ("SM", ["Laminacao de uniao ao casco dos dois lados", "Passagens de cabos e tubos vedadas", "Prumo e posicao (dimensional)"], "casco"),
 "motorDiesel": ("SM", ["Alinhamento e torque dos coxins", "Vazamentos de oleo, agua e diesel", "Vibracao e ruido em prova de mar"], "motor"),
 "pod": ("SM", ["Vedacao do anel do pod no fundo", "Anodos e helices", "Alinhamento do eixo intermediario"], "motor"),
 "motorPopa": ("POPA", ["Torque dos parafusos do espelho", "Altura de montagem", "Curso de trim e direcao"], "motor"),
 "tanque": ("TQ", ["Teste de estanqueidade", "Fixacao e respiros", "Sensor de nivel"], "tanque"),
 "tanqueAgua": ("TQ", ["Estanqueidade e fixacao", "Sensor de nivel"], "geral"),
 "gerador": ("SM", ["Fixacao nos coxins", "Escape, sifao e agua de refrigeracao", "Partida, carga e ruido"], "gerador"),
 "giro": ("SM", ["Torque de fixacao na base", "Refrigeracao e alimentacao", "Tempo de giro e reducao de balanco"], "giro"),
 "baterias": ("SM", ["Fixacao e ventilacao da caixa", "Bornes protegidos e identificados", "Tensao e carregador"], "eletrica"),
 "quadroEletrico": ("SM", ["Identificacao de disjuntores e cabos", "Aperto dos bornes (termografia)", "Protecao contra respingo"], "eletrica"),
 "chiller": ("SM", ["Vazamento de agua gelada e dreno", "Vibracao do compressor", "Temperatura de insuflamento nas cabines"], "ar"),
 "boiler": ("SM", ["Vazamento nas conexoes", "Valvula de alivio"], "geral"),
 "bombasPorao": ("SM", ["Acionamento da boia automatica", "Alarme de porao no painel", "Saida de agua no costado"], "eletrica"),
 "incendio": ("SM", ["Carga e validade do cilindro", "Acionamento manual e automatico", "Corte de ventilacao e motores"], "incendio"),
 "conves": ("CONVES", ["Colagem casco-conves", "Antiderrapante e gelcoat", "Caimento para os drenos"], "conves"),
 "casaria": ("SAL", ["Uniao casaria-conves", "Rebaixos dos vidros (dimensional)", "Gelcoat externo e interno"], "casaria"),
 "paraBrisa": ("CMD", ["Vedacao no teste de mangueira", "Distorcao optica na visao do piloto", "Limpadores"], "geral"),
 "vidrosLaterais": ("SAL", ["Vedacao no teste de mangueira", "Riscos e delaminacao do vidro"], "geral"),
 "portaSalao": ("SAL", ["Corre livre e trava", "Vedacao e dreno do trilho"], "geral"),
 "hardtop": ("FLY", ["Fixacao e uniao", "Gelcoat e drenos"], "gel"),
 "colunasHardtop": ("CPT", ["Fixacao e solda", "Polimento do inox"], "geral"),
 "tampaMaquinas": ("CPT", ["Vedacao e dreno da calha", "Abertura (pistao) e trava"], "geral"),
 "cunho": ("CONVES", ["Contraplaca e torque", "Vedacao do furo"], "conves"),
 "degrausSalao": ("CPT", ["Fixacao", "Calafeto e infiltracao sob a teca"], "geral"),
 "cozinhaExterna": ("CPT", ["Fixacao do movel", "Grill, cuba e geladeira funcionando"], "geral"),
 "sofaPopaBase": ("CPT", ["Fixacao e gelcoat"], "gel"), "sofaPopaEstofado": ("CPT", ["Costura, manchas e encaixe"], "geral"),
 "mesaCockpit": ("CPT", ["Verniz/oleo da teca", "Pedestal eletrico sobe e desce"], "geral"),
 "tampaPaiol": ("PROA", ["Vedacao e dobradica"], "geral"), "tampaGuincho": ("PROA", ["Vedacao e dobradica"], "geral"),
 "guincho": ("PROA", ["Funciona nos dois sentidos", "Contraplaca e vedacao"], "geral"),
 "ancora": ("PROA", ["Assenta no rolete sem bater no casco"], "geral"), "rolete": ("PROA", ["Fixacao e alinhamento"], "geral"),
 "solarioBase": ("PROA", ["Fixacao e gelcoat"], "gel"), "solarioEstofado": ("PROA", ["Costura, manchas e encaixe"], "geral"),
 "mesaProa": ("PROA", ["Verniz/oleo da teca", "Pedestal firme"], "geral"),
 "plataforma": ("PLAT", ["Fixacao ao espelho", "Teca: calafeto e infiltracao"], "geral"),
 "escada": ("PLAT", ["Abre, trava e recolhe"], "geral"), "guarda": ("CONVES", ["Fixacao das bases", "Polimento e soldas"], "geral"),
 "painelFly": ("FLY", ["Gelcoat e furacoes dos instrumentos"], "gel"), "telasFly": ("FLY", ["Liga, toque e rede NMEA"], "mar"),
 "defletorFly": ("FLY", ["Riscos e trincas no acrilico", "Fixacao e vedacao da base"], "geral"),
 "barFly": ("FLY", ["Fixacao, cuba e geladeira"], "geral"), "sofaFly": ("FLY", ["Costura, manchas e encaixe"], "geral"),
 "mesaFly": ("FLY", ["Verniz/oleo da teca", "Pedestal firme"], "geral"),
 "guardaFly": ("FLY", ["Fixacao das bases", "Polimento e soldas"], "geral"),
 "jacuzzi": ("FLY", ["Estanqueidade cheia por 24 h", "Bomba, aquecedor e dreno", "Reforco do piso do fly"], "geral"),
 "console": ("CMD", ["Gelcoat e furacoes"], "gel"), "paraBrisaConsole": ("CMD", ["Riscos e trincas", "Fixacao"], "geral"),
 "telas": ("CMD", ["Liga, toque e rede NMEA"], "mar"), "volante": ("CMD", ["Folga e curso da direcao"], "mar"),
 "manete": ("CMD", ["Curso e neutro", "Sincronismo"], "mar"),
 "leaningPost": ("CMD", ["Fixacao e gelcoat"], "gel"), "leaningEstofado": ("CMD", ["Costura e encaixe"], "geral"),
 "hardtop33": ("CMD", ["Fixacao das colunas", "Gelcoat"], "gel"),
 # v2.1: paiol de popa, plataforma, antenas e itens basicos da 33
 "tampaPaiolPopa": ("POPA", ["Vedacao e dreno da calha", "Abertura e trava"], "geral"),
 "tanqueNegras": ("POPA", ["Estanqueidade e fixacao", "Respiro com filtro de carvao", "Sensor de nivel e alarme de tanque cheio"], "geral"),
 "maceradora": ("POPA", ["Acionamento pelo painel", "Valvula Y e descarga lacrada", "Vazamento nas conexoes"], "geral"),
 "enrolador": ("POPA", ["Recolhe e solta o cabo pelos comandos", "Aperto dos terminais e aterramento", "Vedacao da passagem no espelho"], "eletrica"),
 "transformador": ("POPA", ["Fixacao e ventilacao", "Isolamento entre terra e bordo (megometro)", "Aperto dos terminais"], "eletrica"),
 "chuveiro": ("PLAT", ["Agua quente e fria sem vazamento", "Mangueira recolhe sem prender"], "geral"),
 "interceptores": ("PLAT", ["Vedacao dos passa-cascos dos atuadores", "Curso e indicacao no painel", "Fixacao e selante no espelho"], "mar"),
 "flaps": ("PLAT", ["Curso e indicador no painel", "Vedacao dos parafusos no espelho"], "mar"),
 "luzesSub": ("PLAT", ["Acendem e mudam de cor", "Vedacao do passa-casco"], "eletrica"),
 "mastroRadar": ("FLY", ["Fixacao e contraplacas", "Passagem de cabos vedada", "Gelcoat e alinhamento"], "gel"),
 "radar": ("FLY", ["Fixacao no mastro", "Imagem e alcance na tela", "Alinhamento de proa (heading)"], "mar"),
 "antenaVhf": ("FLY", ["Base reclinavel trava", "ROE da antena (estacionaria)", "Teste de chamada no canal 16"], "mar"),
 "luzesNav": ("FLY", ["Navegacao e fundeio acendem", "Setor e cor corretos", "Vedacao das luminarias"], "eletrica"),
 "buzina": ("FLY", ["Toca pelos comandos", "Fixacao e dreno"], "geral"),
 "holofote": ("FLY", ["Gira e acende pelo controle", "Vedacao"], "eletrica"),
 "antenaSat": ("FLY", ["Fixacao e vedacao da base", "Rastreia o satelite e entrega sinal na TV"], "geral"),
 "spotsHardtop": ("CPT", ["Todos acendem com o mesmo tom", "Vedacao e fixacao no teto"], "eletrica"),
 "carregador": ("PORAO", ["Carrega todos os bancos", "Tomada de cais e isolador galvanico", "Fixacao e ventilacao"], "eletrica"),
 "separadores": ("PORAO", ["Drenar a agua do copo", "Vazamento nas conexoes", "Fixacao"], "motor"),
 "washdown": ("PORAO", ["Pressao na mangueira", "Filtro de entrada limpo"], "geral"),
 "viveiro": ("CPT", ["Bomba enche e recircula", "Dreno e ladrao escoam", "Tampa veda e trava"], "geral"),
 "vasoConsole": ("CONS", ["Descarga e valvulas", "Tanque de aguas negras sem odor", "Ventilacao do console"], "geral"),
 "portaConsole": ("CONS", ["Fecha e tranca", "Riscos no acrilico"], "geral"),
 "propulsorProa": ("PROA", ["Empuxo nos dois sentidos", "Vedacao do tunel e da base do motor", "Protecao termica"], "mar"),
 # v3: sistemas tirados dos checklists FM-CQ013 (fases Sistemas e Collaudo) e FM-CQ034 (ar-condicionado)
 "evaporadora": ("SAL", ["Fixacao e coxins", "Dreno da bandeja com sifao", "Fluxo de ar na grelha e temperatura no termostato"], "ar"),
 "acBombaMar": ("SM", ["Bomba abaixo da linha d'agua", "Valvula de fundo abre e fecha", "Filtro na vertical e sem ar"], "ar"),
 "acCircuito": ("SM", ["Pressao da linha de 15 a 25 psi", "Tanque de expansao com valvula", "Manifold com registro por maquina"], "ar"),
 "exaustor": ("SM", ["Helice gira livre e no sentido certo", "Fixacao na base", "Corte pelo sistema de incendio"], "incendio"),
 "filtroMar": ("SM", ["Copo limpo e sem ar", "Valvula de fundo abre e fecha", "Duas abracadeiras contrapostas"], "motor"),
 "anodos": ("CASCO", ["Posicao e quantidade de projeto", "Espacamento minimo de 5 mm do casco", "Continuidade eletrica"], "sistemas"),
 "chavesBateria": ("SM", ["Identificacao de cada chave", "Paralela e bypass funcionando", "Acionamento pelo keypad e manual"], "collaudo"),
 "displaysSistemas": ("CMD", ["Todos ligam e mostram leitura", "Alarmes de incendio e de porao", "Niveis de tanque corretos"], "collaudo"),
 "limpadores": ("CMD", ["Funcionamento sincronizado", "Nao batem na moldura", "Esguicho molha toda a palheta"], "collaudo"),
 "bombaAgua": ("SM", ["Pressostato liga e desliga", "Sem vazamento no acumulador"], "sistemas"),
 "aguasCinzas": ("ARM", ["Automatico liga pela boia", "Sem retorno de odor", "Descarga no costado sem vazamento"], "sistemas"),
 "lavaCorrente": ("PROA", ["Joga agua na corrente", "Sem vazamento na bomba"], "sistemas"),
 "agulheiros": ("CONVES", ["Tampa identificada (DIESEL, WATER, WASTE)", "Vedacao e aterramento do bocal de diesel"], "sistemas"),
 "estacaoPopa": ("CPT", ["Joystick responde nos dois sentidos", "Tampa abre, fecha e veda"], "mar"),
 "centralHidraulica": ("POPA", ["Sem vazamento de oleo", "Plataforma desce 350 mm abaixo da agua", "Fim de curso nos dois sentidos"], "sistemas"),
 # interiores
 "pisoInferior": ("ARM", ["Colagem ao casco e as hastilhas", "Nivel e planicidade", "Tampas de inspecao do porao"], "geral"),
 "forros": ("ARM", ["Emendas e alinhamento", "Fixacao sem ruido"], "movel"),
 "camaArmador": ("ARM", ["Fixacao e gavetas", "Acabamento do verniz"], "movel"), "colchaoArmador": ("ARM", ["Costura, manchas e medidas"], "geral"),
 "criados": ("ARM", ["Gavetas correm e travam", "Alinhamento das frentes"], "movel"),
 "roupeiroArmador": ("ARM", ["Portas alinhadas, folga uniforme", "Fecho e dobradicas", "Verniz sem escorrido"], "movel"),
 "tvArmador": ("ARM", ["Liga e recebe sinal", "Suporte firme"], "geral"),
 "divisoriasWcArm": ("WCA", ["Prumo e esquadro", "Vedacao no pe da parede"], "movel"),
 "vasoArm": ("WCA", ["Descarga e valvulas", "Vazamento e odor"], "geral"), "bancadaWcArm": ("WCA", ["Torneira, sifao e vazamentos", "Tampo e cuba sem trinca"], "movel"),
 "boxWcArm": ("WCA", ["Vedacao do box", "Dreno do piso escoa"], "geral"),
 "escadaInterna": ("HALL", ["Fixacao e rangido", "Altura uniforme dos degraus", "Corrimao"], "movel"),
 "divisoriasHall": ("HALL", ["Prumo e esquadro", "Portas fecham sem bater"], "movel"),
 "beliche": ("HOSP", ["Fixacao e escada do beliche", "Acabamento"], "movel"), "colchoesHosp": ("HOSP", ["Costura, manchas e medidas"], "geral"),
 "roupeiroHosp": ("HOSP", ["Portas alinhadas", "Fecho e dobradicas"], "movel"),
 "vasoHosp": ("WCH", ["Descarga e valvulas", "Vazamento e odor"], "geral"), "bancadaWcHosp": ("WCH", ["Torneira, sifao e vazamentos"], "movel"),
 "boxWcHosp": ("WCH", ["Vedacao do box", "Dreno do piso escoa"], "geral"),
 "pisoVip": ("VIP", ["Colagem ao casco", "Nivel e planicidade"], "geral"),
 "camaVip": ("VIP", ["Fixacao e paiol sob a cama", "Acabamento do verniz"], "movel"), "colchaoVip": ("VIP", ["Costura, manchas e medidas"], "geral"),
 "roupeirosVip": ("VIP", ["Portas alinhadas", "Fecho e dobradicas"], "movel"),
 "pisoSalao": ("SAL", ["Nivel e rangido", "Emendas do piso", "Tampas de acesso"], "geral"),
 "sofaSalaoBase": ("SAL", ["Fixacao e paiois", "Acabamento"], "movel"), "sofaSalaoEstofado": ("SAL", ["Costura, manchas e encaixe"], "geral"),
 "mesaSalao": ("SAL", ["Verniz sem risco", "Pedestal firme"], "movel"),
 "movelTv": ("SAL", ["Elevador da TV sobe e desce", "Frentes alinhadas"], "movel"), "tvSalao": ("SAL", ["Liga e recebe sinal"], "geral"),
 "bancadaCozinha": ("COZ", ["Gavetas e portas com trava de mar", "Alinhamento das frentes", "Verniz"], "movel"),
 "tampoCozinha": ("COZ", ["Emendas e trincas", "Vedacao junto a parede"], "geral"),
 "cooktop": ("COZ", ["Aquece em todas as bocas", "Fixacao"], "geral"), "cubaCozinha": ("COZ", ["Torneira, sifao e vazamentos"], "geral"),
 "geladeira": ("COZ", ["Gela e trava a porta", "Ventilacao do nicho"], "geral"),
 "consoleInterno": ("CMD", ["Furacoes dos instrumentos", "Acabamento do painel"], "movel"),
 "telasInternas": ("CMD", ["Liga, toque e rede NMEA", "Alarmes de motor e porao"], "mar"),
 "volanteInterno": ("CMD", ["Folga e curso da direcao"], "mar"), "maneteInterno": ("CMD", ["Curso, neutro e joystick"], "mar"),
 "bancoPiloto": ("CMD", ["Regulagem e trava", "Costura"], "geral"),
 "painelDisjuntores": ("CMD", ["Identificacao dos circuitos", "Cada disjuntor desliga o circuito certo"], "eletrica"),
}

def item(cod, desc, tipo, gate, modelo3d=None, peca3d="acabamento", qtd=1, un="UN", lado=None, medidas=None, fonte=None, comp=None):
    d = {"codigo": cod, "descricao": desc, "tipo": tipo, "gate": gate, "qtd": qtd, "un": un}
    if lado: d["lado"] = lado
    if modelo3d: d["modelo3d"] = modelo3d; d["peca3d"] = peca3d
    if medidas: d["medidasM"] = medidas
    if fonte: d["fonte"] = FONTES[fonte]
    if modelo3d in V:
        c, ver, ref = V[modelo3d]
        d["compartimento"] = comp or c; d["verificar"] = ver; d["ref"] = REF[ref]
    elif comp: d["compartimento"] = comp
    return d

def linha(n):
    L = str(n)
    seq = {"v": 0, "w": 100}
    def v():
        seq["v"] += 1
        return f"VTR-{L}-{seq['v']:03d}"
    def w():                      # fibra nova da v2: numeracao a partir de 101, para nao mexer nos codigos antigos
        seq["w"] += 1
        return f"VTR-{L}-{seq['w']:03d}"
    esc = []
    def escopo(nome, std, gate, itens, exclusivo=None):
        k = len(esc) + 1
        e = {"codigo": f"E{L}-{'STD' if std else 'OPC'}-{k:02d}",
             "descricao": f"ESCOPO {L}-{'STD' if std else 'OPC'} {nome}", "padrao": std, "gate": gate, "itens": itens}
        if exclusivo: e["exclusivo"] = exclusivo
        esc.append(e)
    I = "internos"

    escopo("CASCO", True, "DM", [
        item(v(), f"CASCO VTR {L}", "MOLDE", "DM", "casco", "casco"),
        item(v(), f"VARAS D'AGUA (FRISOS) DO FUNDO - LAMINADAS NO CASCO", "FRP", "DM", "frisos", "casco"),
        item(v(), "TAMPA SAIDA DE AGUA CASCO BB VTR", "FRP", "ST", "saidaAgua", "casco", lado="BB"),
        item(v(), "TAMPA SAIDA DE AGUA CASCO BE VTR", "FRP", "ST", "saidaAgua", "casco", lado="BE"),
    ] + ([] if n == 33 else [
        item(v(), "GRELHA TOMADA DE AR CASA DE MAQUINAS BB", "FRP", "ST", "tomadaAr", "casco", lado="BB"),
        item(v(), "GRELHA TOMADA DE AR CASA DE MAQUINAS BE", "FRP", "ST", "tomadaAr", "casco", lado="BE"),
    ]))
    if n == 33:
        escopo("MOTORIZACAO 2X POPA V10 350HP", True, "BA", [
            item("CMP-MOT-V10-350", "MOTOR DE POPA V10 350 HP RABETA 25 POL", "COMPRADO", "BA", "motorPopa", I, qtd=2,
                 medidas=[0.85, 0.75, 1.95], fonte="V10"),
            item("CMP-TQ-0400", "TANQUE DE COMBUSTIVEL 400 L", "COMPRADO", "BA", "tanque", I),
        ])
    else:
        mot = "IPS950" if n == 59 else "IPS800"
        med = [1.75, 1.00, 1.00] if n == 59 else [1.55, 0.99, 0.70]
        escopo(f"MOTORIZACAO 2X {'D11-IPS950 725HP' if n == 59 else 'D8-IPS800 600HP'}", True, "BA", [
            item(f"CMP-MOT-{mot}", f"MOTOR DIESEL {'D11 725 HP' if n == 59 else 'D8 600 HP'} PARA IPS", "COMPRADO", "BA", "motorDiesel", I,
                 qtd=2, medidas=med, fonte=mot),
            item(f"CMP-POD-{mot}", f"POD {mot} COM HELICES DUPLAS", "COMPRADO", "BA", "pod", I, qtd=2, fonte=mot),
            item(f"CMP-TQ-{'2400' if n == 59 else '1600'}", f"TANQUE DE COMBUSTIVEL {'2.400' if n == 59 else '1.600'} L", "COMPRADO", "BA", "tanque", I),
        ])
    escopo("CONVES", True, "BF", [
        item(v(), f"CONVES VTR {L}", "MOLDE", "BF", "conves", "conves"),
        item(v(), "TAMPA CASA DE MAQUINAS PRACA DE POPA VTR" if n != 33 else "TAMPA COMPARTIMENTO DE POPA VTR", "FRP", "ST", "tampaMaquinas"),
        item("INX-CUNHO-300", "CUNHO DE AMARRACAO 300 MM ACO INOX 316", "INOX", "ST", "cunho", qtd=6),
    ])
    if n != 33:
        escopo("CASARIA", True, "BF", [item(v(), f"CASARIA VTR {L}", "MOLDE", "BF", "casaria", "casaria")])
    escopo("PAIOL DE ANCORA PROA", True, "ST", ([
        item(v(), "TAMPA COMPARTIMENTO ANCORA BB VTR", "FRP", "ST", "tampaPaiol", lado="BB"),
        item(v(), "TAMPA COMPARTIMENTO ANCORA BE VTR", "FRP", "ST", "tampaPaiol", lado="BE"),
    ] if n != 33 else [item(v(), "TAMPA PAIOL DE ANCORA VTR", "FRP", "ST", "tampaPaiol")]) + [
        item(v(), "TAMPA GUINCHO DE AMARRACAO VTR", "FRP", "ST", "tampaGuincho"),
        item("CMP-GUI-1500", f"GUINCHO DE ANCORA {'1500' if n == 59 else '1000' if n == 47 else '700'} W", "COMPRADO", "ST", "guincho"),
        item("CMP-ANC-XR", f"ANCORA {'30' if n == 59 else '20' if n == 47 else '10'} KG ACO INOX 316L", "COMPRADO", "ST", "ancora"),
        item("INX-ROLETE", "ROLETE DE PROA ACO INOX", "INOX", "ST", "rolete"),
    ])
    escopo("SOLARIO DE PROA", True, "CT", [
        item(v(), "BASE SOLARIO DE PROA VTR", "FRP", "ST", "solarioBase"),
        item("EST-SOL-PROA", "ESTOFADOS SOLARIO DE PROA (KIT)", "ESTOFADO", "CT", "solarioEstofado"),
    ])
    escopo("PLATAFORMA DE POPA", True, "ST", [
        item(v(), "PLATAFORMA DE POPA VTR", "FRP", "ST", "plataforma"),
        item("INX-ESCADA-TEL", "ESCADA TELESCOPICA EMBUTIVEL ACO INOX", "INOX", "ST", "escada"),
        item("HID-CHUV-POPA", "CHUVEIRO DE POPA AGUA QUENTE E FRIA" if n != 33 else "CHUVEIRO DE POPA AGUA DOCE", "INOX", "ST", "chuveiro"),
    ] + ([item("CMP-FLAPS-33", "FLAPS DE TRIM ELETRICOS COM INDICADOR (PAR)", "COMPRADO", "BA", "flaps", "casco", fonte="BW330")] if n == 33 else
         [item("CMP-INTERCEPT", "INTERCEPTORES DE TRIM COM ATUADORES ELETRICOS (PAR)", "COMPRADO", "BA", "interceptores", "casco")]))
    escopo("LUZES SUBAQUATICAS LED", False, "CT", [item("ELE-LUZ-SUB", "LUZ SUBAQUATICA LED RGB NO ESPELHO DE POPA", "ELETRICA", "CT", "luzesSub", "casco", qtd=2)])
    escopo("GUARDA-MANCEBO", True, "ST", [item("INX-GM-25", "GUARDA-MANCEBO TUBO 25 MM ACO INOX 316 (KIT)", "INOX", "ST", "guarda")])
    if n == 33:
        escopo("COMANDO CENTRAL", True, "CT", [
            item(v(), "CONSOLE DE COMANDO VTR", "FRP", "ST", "console"),
            item("VID-PB-33", "PARA-BRISA CURVO DO CONSOLE ACRILICO 8 MM", "VIDRO", "ST", "paraBrisaConsole"),
            item("CMP-TELA-12", "TELA MULTIFUNCAO 12 POL", "ELETRICA", "CT", "telas", qtd=2),
            item("CMP-VOLANTE", "VOLANTE ACO INOX COM DIRECAO ELETRO-HIDRAULICA", "COMPRADO", "CT", "volante"),
            item("CMP-MANETE", "MANETE DIGITAL DUPLA", "COMPRADO", "CT", "manete"),
            item(v(), "LEANING POST COM 2 BANCOS VTR", "FRP", "ST", "leaningPost"),
            item("EST-LP-33", "ESTOFADOS LEANING POST", "ESTOFADO", "CT", "leaningEstofado"),
        ])
        escopo("HARDTOP (COMPRADO)", True, "ST", [item("CMP-HT-33", "HARDTOP COMPLETO COM COLUNAS INOX (FORNECEDOR)", "COMPRADO", "ST", "hardtop33")])
        escopo("PRACA DE POPA", True, "CT", [
            item(v(), "BASE SOFA DE POPA VTR", "FRP", "ST", "sofaPopaBase", comp="POPA"),
            item("EST-SOFA-POPA", "ESTOFADOS SOFA DE POPA", "ESTOFADO", "CT", "sofaPopaEstofado", comp="POPA"),
        ])
        escopo("MESA TECA PROA COM PEDESTAL", False, "CT", [item("TEC-MESA-PROA", "TAMPO TECA MESA PROA + PEDESTAL", "TECA", "CT", "mesaProa")])
        esc[-3]["itens"].append(item(w(), "VIVEIRO DE ISCA 150 L NA RE DO LEANING POST COM TAMPA", "FRP", "ST", "viveiro"))    # escopo PRACA DE POPA
        escopo("SISTEMA ELETRICO", True, "BA", [
            item("ELE-BAT-33", "BATERIA AGM GRUPO 31 EM CAIXA (2 PARTIDA + 1 SERVICO) COM CHAVES GERAIS", "ELETRICA", "BA", "baterias", I, qtd=3, fonte="BW330"),
            item("ELE-CARREG-33", "CARREGADOR DE BATERIAS 3 BANCOS COM TOMADA DE CAIS E ISOLADOR GALVANICO", "ELETRICA", "BA", "carregador", I, fonte="BW330"),
            item("ELE-PAINEL-33", "PAINEL DE DISJUNTORES E CHAVES NO CONSOLE", "ELETRICA", "CT", "painelDisjuntores"),
            item("ELE-NAV-33", "LUZES DE NAVEGACAO LED E LUZ DE FUNDEIO (KIT)", "ELETRICA", "CT", "luzesNav"),
            item("ELE-VHF-33", "RADIO VHF COM ANTENA 2,4 M NO HARDTOP", "ELETRICA", "CT", "antenaVhf"),
            item("CMP-BUZINA", "BUZINA ELETRICA INOX", "COMPRADO", "ST", "buzina"),
            item("ELE-SPOTS-33", "ILUMINACAO LED SOB O HARDTOP", "ELETRICA", "CT", "spotsHardtop"),
        ])
        escopo("HIDRAULICA E BOMBAS", True, "BA", [
            item("HID-BP-33", "BOMBA DE PORAO AUTOMATICA COM BOIA (1 A VANTE, 2 A RE)", "COMPRADO", "BA", "bombasPorao", I, qtd=3, fonte="BW330"),
            item("HID-SEP-33", "FILTRO SEPARADOR DE AGUA E COMBUSTIVEL", "COMPRADO", "BA", "separadores", I, qtd=2, fonte="BW330"),
            item("HID-AGUA-33", "TANQUE DE AGUA DOCE 150 L COM BOMBA PRESSURIZADORA", "COMPRADO", "BA", "tanqueAgua", I, fonte="BW330"),
            item("HID-WASH-33", "BOMBA DE LAVAGEM COM AGUA SALGADA (WASHDOWN)", "COMPRADO", "BA", "washdown", I),
        ])
        escopo("WC NO CONSOLE", True, "CT", [
            item("VID-PORTA-CONS", "PORTA DO CONSOLE EM ACRILICO COM FECHADURA", "VIDRO", "ST", "portaConsole"),
            item("HID-VASO-33", "VASO SANITARIO ELETRICO COM TANQUE DE AGUAS NEGRAS 26 L", "COMPRADO", "CT", "vasoConsole"),
        ])
        escopo("ESTABILIZADOR GIROSCOPICO SEAKEEPER 2", False, "BA", [
            item("CMP-SK-2", "ESTABILIZADOR GIROSCOPICO SEAKEEPER 2 (12 V)", "COMPRADO", "BA", "giro", I, medidas=[0.65, 0.62, 0.52], fonte="SK2"),
        ])
        escopo("PROPULSOR DE PROA 12 V", False, "BA", [item("CMP-THR-12V", "PROPULSOR DE PROA 12 V COM TUNEL LAMINADO", "COMPRADO", "BA", "propulsorProa", I)])
        escopo("ESTRUTURA DO FUNDO", True, "BA", [
            item("STR-33-001", "LONGARINAS DO FUNDO VTR (KIT 4)", "FRP", "BA", "longarinas", I, comp="POPA"),
            item("STR-33-002", "HASTILHAS E CAVERNAS DO FUNDO VTR (KIT)", "FRP", "BA", "hastilhas", I, comp="POPA"),
        ])
    else:
        escopo("VIDROS E HARDTOP", True, "ST", [
            item("VID-PB", "PARA-BRISA CURVO LAMINADO DA CASARIA", "VIDRO", "ST", "paraBrisa"),
            item(v(), "HARD TOP VTR", "FRP", "ST", "hardtop", comp="FLY"),
            item("INX-COL-HT", "PILARES DE POPA DO HARDTOP ACO INOX", "INOX", "ST", "colunasHardtop"),
            item("VID-LAT", "VIDROS LATERAIS DA CASARIA TEMPERADOS (KIT BB/BE)", "VIDRO", "ST", "vidrosLaterais"),
            item("VID-PORTA", "PORTA DE CORRER DO SALAO VIDRO TEMPERADO COM QUADRO INOX", "VIDRO", "ST", "portaSalao"),
            item("VID-CASCO", "VIDROS DO CASCO COM CANTOS ARREDONDADOS (KIT BB/BE)", "VIDRO", "ST", "vidrosCasco", "casco"),
        ])
        escopo("PRACA DE POPA", True, "CT", [
            item(v(), "COZINHA EXTERNA VTR (MOVEL)", "FRP", "ST", "cozinhaExterna"),
            item(v(), "BASE SOFA DE POPA VTR", "FRP", "ST", "sofaPopaBase"),
            item("EST-SOFA-POPA", "ESTOFADOS SOFA DE POPA", "ESTOFADO", "CT", "sofaPopaEstofado"),
            item("TEC-DEG-SALAO", "DEGRAUS DE ACESSO AO SALAO REVESTIDOS EM TECA", "TECA", "ST", "degrausSalao"),
        ])
        escopo("MESA TECA PRACA DE POPA PEDESTAL ELETRICO", False, "CT", [item("TEC-MESA-CP", "TAMPO TECA MESA PRACA DE POPA + PEDESTAL ELETRICO", "TECA", "CT", "mesaCockpit")])
        escopo("MESA TECA PROA PEDESTAL ELETRICO", False, "CT", [item("TEC-MESA-PROA", "TAMPO TECA MESA PROA + PEDESTAL ELETRICO", "TECA", "CT", "mesaProa")])
        if n == 59:
            escopo("FLYBRIDGE", True, "ST", [
                item(v(), "PAINEL FLY VTR", "FRP", "ST", "painelFly"),
                item("CMP-TELA-16", "TELA MULTIFUNCAO 16 POL", "ELETRICA", "CT", "telasFly", qtd=2),
                item(v(), "MOVEL BAR FLY VTR", "FRP", "ST", "barFly"),
                item("EST-FLY", "ESTOFADOS SOFA DO FLY", "ESTOFADO", "CT", "sofaFly"),
                item("INX-GC-FLY", "GUARDA-CORPO DO FLY ACO INOX", "INOX", "ST", "guardaFly"),
                item("VID-DEF-FLY", "DEFLETOR CURVO DO FLY ACRILICO FUME 10 MM", "VIDRO", "ST", "defletorFly"),
                item("TEC-MESA-FLY", "TAMPO TECA MESA DO FLY + PEDESTAL", "TECA", "CT", "mesaFly"),
            ])
            escopo("JACUZZI NO FLY", False, "ST", [item("CMP-JAC-6P", "JACUZZI 6 PESSOAS COM AQUECIMENTO", "COMPRADO", "ST", "jacuzzi")])
        # ---------- v2.1: paiol de popa e arco do radar ----------
        escopo("PAIOL DE POPA", True, "BA", [
            item(w(), "TAMPA DO PAIOL DE POPA VTR", "FRP", "ST", "tampaPaiolPopa"),
            item(f"HID-TQ-NEGRAS-{'200' if n == 59 else '150'}", f"TANQUE DE AGUAS NEGRAS {'200' if n == 59 else '150'} L COM SENSOR DE NIVEL", "COMPRADO", "BA", "tanqueNegras", I),
            item("HID-MACERADORA", "BOMBA MACERADORA DE ESGOTO COM VALVULA Y", "COMPRADO", "BA", "maceradora", I),
            item("ELE-ENROLADOR", "ENROLADOR MOTORIZADO DO CABO DE ENERGIA DE TERRA 50 A", "ELETRICA", "CT", "enrolador", I),
        ] + ([item("ELE-TRAFO", "TRANSFORMADOR DE ISOLAMENTO DA ENERGIA DE TERRA", "ELETRICA", "BA", "transformador", I)] if n == 59 else []))
        escopo("ARCO DO RADAR E ANTENAS NO FLY" if n == 59 else "MASTRO E ANTENAS NO HARDTOP", True, "ST", [
            item(w(), "ARCO DO RADAR VTR" if n == 59 else "MASTRO DO RADAR VTR", "FRP", "ST", "mastroRadar"),
            item("ELE-RADAR-18", "RADAR DOMO 18 POL", "ELETRICA", "CT", "radar"),
            item("ELE-VHF-ANT", "ANTENA VHF 2,4 M COM BASE RECLINAVEL", "ELETRICA", "CT", "antenaVhf"),
            item("ELE-NAV-LUZ", "LUZES DE NAVEGACAO E LUZ DE FUNDEIO (KIT)", "ELETRICA", "CT", "luzesNav"),
            item("CMP-BUZINA", "BUZINA ELETRICA DUPLA INOX", "COMPRADO", "ST", "buzina"),
            item("ELE-HOLOFOTE", "HOLOFOTE COM CONTROLE REMOTO", "ELETRICA", "CT", "holofote"),
            item("ELE-SPOTS-HT", "SPOTS LED SOB O HARDTOP", "ELETRICA", "CT", "spotsHardtop"),
        ])
        escopo("ANTENA DE TV POR SATELITE", False, "CT", [item("ELE-SAT", "ANTENA DE TV POR SATELITE (DOMO)", "ELETRICA", "CT", "antenaSat")])
        # ---------- v2: estrutura, sistemas e interiores ----------
        escopo("ESTRUTURA DO FUNDO E ANTEPARAS", True, "BA", [
            item(f"STR-{L}-001", "LONGARINAS DO FUNDO VTR (KIT 4, COM CALCO DOS MOTORES)", "FRP", "BA", "longarinas", I),
            item(f"STR-{L}-002", "HASTILHAS E CAVERNAS DO FUNDO VTR (KIT)", "FRP", "BA", "hastilhas", I),
            item(f"STR-{L}-003", "ANTEPARAS ESTRUTURAIS COMPENSADO NAVAL LAMINADO (KIT 6)", "FRP", "BA", "anteparas", I),
        ])
        ger = ("GER20", [1.14, 0.64, 0.69], "20") if n == 59 else ("GER13", [0.99, 0.60, 0.62], "13")
        escopo("GERACAO E ELETRICA DA SALA DE MAQUINAS", True, "BA", [
            item(f"CMP-GER-{ger[2]}", f"GERADOR DIESEL {ger[2]} KW COM CABINE ACUSTICA", "COMPRADO", "BA", "gerador", I, medidas=ger[1], fonte=ger[0]),
            item("ELE-BAT-24", "BANCO DE BATERIAS 24 V COM CAIXA E CARREGADOR", "ELETRICA", "BA", "baterias", I),
            item("ELE-QDP", "QUADRO ELETRICO PRINCIPAL CA/CC", "ELETRICA", "CT", "quadroEletrico", I),
        ])
        escopo("HIDRAULICA, CLIMATIZACAO E SEGURANCA", True, "BA", [
            item("HID-AC-CHILLER", f"CHILLER DE AR CONDICIONADO {'72.000' if n == 59 else '54.000'} BTU", "COMPRADO", "BA", "chiller", I),
            item("HID-BOILER-60", "AQUECEDOR DE AGUA (BOILER) 60 L", "COMPRADO", "BA", "boiler", I),
            item("HID-BP-3700", "BOMBA DE PORAO AUTOMATICA COM BOIA", "COMPRADO", "BA", "bombasPorao", I, qtd=4),
            item(f"HID-TQ-AGUA-{'700' if n == 59 else '450'}", f"TANQUE DE AGUA DOCE {'700' if n == 59 else '450'} L", "COMPRADO", "BA", "tanqueAgua", I),
            item("SEG-FM200", "SISTEMA FIXO DE COMBATE A INCENDIO DA SALA DE MAQUINAS", "COMPRADO", "ST", "incendio", I),
        ])
        sk = ("SK9", [0.85, 0.90, 0.72], "9") if n == 59 else ("SK6", [0.77, 0.78, 0.63], "6")
        mc = ("MC2", [0.61, 0.61, 0.67], "X19") if n == 59 else ("MC2P", [0.61, 0.61, 0.67], "X13")
        btu = {"SAL": (24, 16), "COZ": (12, 9), "ARM": (16, 12), "VIP": (12, 9), "HOSP": (9, 7)}
        nomeAmb = {"SAL": "SALAO", "COZ": "COZINHA", "ARM": "CABINE DO ARMADOR", "VIP": "CABINE VIP", "HOSP": "CABINE DE HOSPEDES"}
        escopo("AR-CONDICIONADO - EVAPORADORAS E CIRCUITO DE AGUA", True, "BA", [
            item("HID-AC-BAS", "BOMBA DE AGUA SALGADA DO AR-CONDICIONADO COM FILTRO E VALVULA DE FUNDO", "COMPRADO", "BA", "acBombaMar", I),
            item("HID-AC-CIRC", "BOMBA DE CIRCULACAO, TANQUE DE EXPANSAO E MANIFOLD DO CHILLER", "COMPRADO", "BA", "acCircuito", I),
        ] + [item(f"HID-EV-{a}", f"EVAPORADORA (FAN COIL) {btu[a][0 if n == 59 else 1]}.000 BTU COM TERMOSTATO - {nomeAmb[a]}", "COMPRADO", "CT",
                  "evaporadora", I, comp=a) for a in ("SAL", "COZ", "ARM", "VIP", "HOSP")])
        escopo("SALA DE MAQUINAS - VENTILACAO, COMBUSTIVEL E AGUA DO MAR", True, "BA", [
            item("CMP-EXAUSTOR", "EXAUSTOR E VENTILADOR DA SALA DE MAQUINAS COM DUTOS", "COMPRADO", "BA", "exaustor", I),
            item("HID-RACOR", "FILTRO RACOR SEPARADOR AGUA/DIESEL - MOTORES E GERADOR", "COMPRADO", "BA", "separadores", I, qtd=3, comp="SM"),
            item("HID-FILTRO-MAR", "FILTRO DE AGUA DO MAR COM VALVULA DE FUNDO - MOTORES E GERADOR", "COMPRADO", "BA", "filtroMar", I, qtd=3),
            item("CMP-ANODOS", "ANODOS DE ZINCO DO CASCO E ANODO ELETRONICO (KIT)", "COMPRADO", "BA", "anodos", "casco"),
        ])
        escopo("ELETRICA - CHAVES, MONITORAMENTO E COMANDO", True, "CT", [
            item("ELE-CHAVES-BAT", "PAINEL DE CHAVES GERAIS DE BATERIA (MOTORES, SERVICO, GERADOR, PARALELA, THRUSTER)", "ELETRICA", "BA", "chavesBateria", I),
            item("ELE-DISPLAYS", "DISPLAYS DE SISTEMAS NO COMANDO (GERADOR, AR, INCENDIO, BATERIAS, ESTABILIZADOR)", "ELETRICA", "CT", "displaysSistemas"),
            item("CMP-LIMPADOR", "LIMPADORES DE PARA-BRISA COM ESGUICHO (KIT 3)", "COMPRADO", "ST", "limpadores"),
        ])
        escopo("HIDRAULICA - AGUA DOCE, AGUAS CINZAS E CONVES", True, "BA", [
            item("HID-BOMBA-AD", "BOMBA PRESSURIZADORA DE AGUA DOCE COM ACUMULADOR", "COMPRADO", "BA", "bombaAgua", I),
            item("HID-CINZAS", "CAIXA DE AGUAS CINZAS COM BOMBA AUTOMATICA", "COMPRADO", "BA", "aguasCinzas", I),
            item("HID-LAVA-CORR", "BOMBA LAVA-CORRENTE DO PAIOL DE ANCORA", "COMPRADO", "BA", "lavaCorrente", I),
            item("INX-AGULHEIROS", "AGULHEIROS DE ABASTECIMENTO DIESEL, WATER E WASTE (KIT)", "INOX", "ST", "agulheiros"),
        ])
        escopo("PROPULSOR DE PROA", True, "BA", [item("CMP-THR-24V", "PROPULSOR DE PROA (BOW THRUSTER) 24 V COM TUNEL LAMINADO", "COMPRADO", "BA", "propulsorProa", I)])
        escopo("ESTACAO DE MANOBRA NA PRACA DE POPA", False, "CT", [item("ELE-JOY-POPA", "ESTACAO DE MANOBRA COM JOYSTICK NA PRACA DE POPA", "ELETRICA", "CT", "estacaoPopa")])
        escopo("PLATAFORMA DE POPA HIDRAULICA SUBMERSIVEL", False, "BA", [item("CMP-LIFT", "CENTRAL HIDRAULICA E CILINDROS DA PLATAFORMA SUBMERSIVEL", "COMPRADO", "BA", "centralHidraulica", I)])
        escopo(f"ESTABILIZADOR GIROSCOPICO SEAKEEPER {sk[2]}", False, "BA", [
            item(f"CMP-SK-{sk[2]}", f"ESTABILIZADOR GIROSCOPICO SEAKEEPER {sk[2]} COM BASE LAMINADA", "COMPRADO", "BA", "giro", I, medidas=sk[1], fonte=sk[0]),
        ], exclusivo="ESTABILIZADOR")
        escopo(f"ESTABILIZADOR GIROSCOPICO QUICK MC2 {mc[2]}", False, "BA", [
            item(f"CMP-MC2-{mc[2]}", f"ESTABILIZADOR GIROSCOPICO QUICK MC2 {mc[2]} (REFRIGERADO A AR)", "COMPRADO", "BA", "giro", I, medidas=mc[1], fonte=mc[0]),
        ], exclusivo="ESTABILIZADOR")
        escopo("ESTABILIZADOR DE ALETAS (PINAS) ELETRICAS", False, "BA", [
            item(f"CMP-FIN-{'080' if n == 59 else '065'}", f"ALETA ESTABILIZADORA {'0,80' if n == 59 else '0,65'} M2 COM ATUADOR ELETRICO", "COMPRADO", "BA", "aletas", "casco", qtd=2, fonte="FIN"),
        ], exclusivo="ESTABILIZADOR")
        escopo("INTERIOR - PISOS E FORROS DO CONVES INFERIOR", True, "BA", [
            item(w(), "PISO ESTRUTURAL CABINES LOWERDECK VTR", "FRP", "BA", "pisoInferior", I),
            item(w(), "PISO ESTRUTURAL CABINE VIP VTR", "FRP", "BA", "pisoVip", I),
            item("MOV-FORRO-LAT", "PAINEIS DE FORRO LATERAL DAS CABINES (KIT)", "MOVEL", "CT", "forros", I),
        ])
        escopo("INTERIOR - CABINE DO ARMADOR", True, "BA", [
            item("MOV-ARM-CAMA", "CAMA DE CASAL COM CABECEIRA E GAVETAS", "MOVEL", "BA", "camaArmador", I),
            item("EST-ARM-COLCHAO", "COLCHAO E TRAVESSEIROS CABINE ARMADOR", "ESTOFADO", "CT", "colchaoArmador", I),
            item("MOV-ARM-CRIADO", "CRIADO-MUDO CABINE ARMADOR", "MOVEL", "BA", "criados", I, qtd=2),
            item("MOV-ARM-ROUP", "GUARDA-ROUPA CABINE ARMADOR", "MOVEL", "BA", "roupeiroArmador", I),
            item("ELE-TV-43", "TV 43 POL CABINE ARMADOR", "ELETRICA", "CT", "tvArmador", I),
        ])
        escopo("INTERIOR - WC DO ARMADOR", True, "BA", [
            item("MOV-WCA-DIV", "DIVISORIAS E PORTA DO WC ARMADOR", "MOVEL", "BA", "divisoriasWcArm", I),
            item("HID-VASO-EL", "VASO SANITARIO ELETRICO", "COMPRADO", "CT", "vasoArm", I),
            item("MOV-WCA-BANC", "BANCADA COM CUBA WC ARMADOR", "MOVEL", "BA", "bancadaWcArm", I),
            item("VID-BOX-ARM", "VIDROS DO BOX WC ARMADOR (PRINCIPAL E LATERAL)", "VIDRO", "CT", "boxWcArm", I),
        ])
        escopo("INTERIOR - HALL, CABINE DE HOSPEDES E WC", True, "BA", [
            item("MOV-ESCADA", "ESCADA DO SALAO AO CONVES INFERIOR", "MOVEL", "BA", "escadaInterna", I),
            item("MOV-HALL-DIV", "DIVISORIAS E PORTAS DO HALL (KIT)", "MOVEL", "BA", "divisoriasHall", I),
            item("MOV-HOSP-BEL", "BELICHE CABINE DE HOSPEDES", "MOVEL", "BA", "beliche", I),
            item("EST-HOSP-COLCHAO", "COLCHOES CABINE DE HOSPEDES", "ESTOFADO", "CT", "colchoesHosp", I, qtd=2),
            item("MOV-HOSP-ROUP", "GUARDA-ROUPA CABINE DE HOSPEDES", "MOVEL", "BA", "roupeiroHosp", I),
            item("HID-VASO-EL", "VASO SANITARIO ELETRICO", "COMPRADO", "CT", "vasoHosp", I),
            item("MOV-WCH-BANC", "BANCADA COM CUBA WC HOSPEDES", "MOVEL", "BA", "bancadaWcHosp", I),
            item("VID-BOX-HOSP", "VIDRO DO BOX WC HOSPEDES", "VIDRO", "CT", "boxWcHosp", I),
        ])
        escopo("INTERIOR - CABINE VIP DE PROA", True, "BA", [
            item("MOV-VIP-CAMA", "CAMA DE CASAL EM ILHA CABINE VIP", "MOVEL", "BA", "camaVip", I),
            item("EST-VIP-COLCHAO", "COLCHAO E TRAVESSEIROS CABINE VIP", "ESTOFADO", "CT", "colchaoVip", I),
            item("MOV-VIP-ROUP", "GUARDA-ROUPA CABINE VIP", "MOVEL", "BA", "roupeirosVip", I, qtd=2),
        ])
        A = "acabamento"
        escopo("INTERIOR - SALAO", True, "CT", [
            item(w(), "PISO DO SALAO VTR COM REVESTIMENTO", "FRP", "BF", "pisoSalao", I),
            item("MOV-SAL-SOFA", "BASE DO SOFA EM L DO SALAO", "MOVEL", "CT", "sofaSalaoBase", A),
            item("EST-SAL-SOFA", "ESTOFADOS DO SOFA DO SALAO", "ESTOFADO", "CT", "sofaSalaoEstofado", A),
            item("MOV-SAL-MESA", "MESA DE JANTAR DO SALAO", "MOVEL", "CT", "mesaSalao", A),
            item("MOV-SAL-TV", "MOVEL DA TV COM ELEVADOR", "MOVEL", "CT", "movelTv", A),
            item("ELE-TV-55", "TV 55 POL DO SALAO", "ELETRICA", "CT", "tvSalao", A),
        ])
        escopo("INTERIOR - COZINHA", True, "CT", [
            item("MOV-COZ-BANC", "BANCADA EM L DA COZINHA COM GAVETEIROS", "MOVEL", "CT", "bancadaCozinha", A),
            item("CMP-COZ-TAMPO", "TAMPO DA COZINHA EM SUPERFICIE SOLIDA", "COMPRADO", "CT", "tampoCozinha", A),
            item("CMP-COOKTOP-4", "COOKTOP DE INDUCAO 4 BOCAS", "ELETRICA", "CT", "cooktop", A),
            item("INX-CUBA-COZ", "CUBA DA COZINHA ACO INOX COM TORNEIRA", "INOX", "CT", "cubaCozinha", A),
            item("CMP-GELADEIRA", "GELADEIRA DE EMBUTIR DUPLA", "ELETRICA", "CT", "geladeira", A),
        ])
        escopo("INTERIOR - COMANDO INTERNO", True, "CT", [
            item("MOV-CMD-CONSOLE", "CONSOLE DO COMANDO INTERNO", "MOVEL", "CT", "consoleInterno", A),
            item("CMP-TELA-16", "TELA MULTIFUNCAO 16 POL", "ELETRICA", "CT", "telasInternas", A, qtd=2),
            item("CMP-VOLANTE", "VOLANTE ACO INOX COM DIRECAO ELETRONICA", "COMPRADO", "CT", "volanteInterno", A),
            item("CMP-MANETE-JOY", "MANETE DIGITAL DUPLA COM JOYSTICK IPS", "ELETRICA", "CT", "maneteInterno", A),
            item("EST-CMD-BANCO", "BANCO DUPLO DO PILOTO", "ESTOFADO", "CT", "bancoPiloto", A),
            item("ELE-PAINEL-DISJ", "PAINEL DE DISJUNTORES E CHAVES DO SALAO", "ELETRICA", "CT", "painelDisjuntores", A),
        ])
    if n == 33:
        M33 = {"motorPopa": "PLAT", "tanque": "PORAO", "longarinas": "PORAO", "hastilhas": "PORAO", "sofaPopaBase": "CPT", "sofaPopaEstofado": "CPT",
               "tampaMaquinas": "CPT", "leaningPost": "CMD", "leaningEstofado": "CMD", "hardtop33": "CMD", "luzesNav": "CMD", "antenaVhf": "CMD",
               "buzina": "CMD", "spotsHardtop": "CMD", "baterias": "PORAO", "bombasPorao": "PORAO", "tanqueAgua": "PORAO", "giro": "PORAO",
               "painelDisjuntores": "CMD", "cunho": "CONVES", "guarda": "CONVES"}
        for e in esc:
            for i in e["itens"]:
                if i.get("modelo3d") in M33: i["compartimento"] = M33[i["modelo3d"]]
    return {"linha": n, "codigo": f"MRL{L}", "descricao": f"EMBARCACAO MARELLAR {L}", "escopos": esc}

dados = {
    "versao": 3,
    "observacao": "Estrutura sintetica no modelo do SD4 (barco -> escopos STD/OPC -> itens). Codigos proprios da Marellar. v2: compartimento, pontos de inspecao, estrutura, sistemas e interiores. v3: praca de popa, paiol de popa, antenas e itens basicos da 33.",
    "linhas": [linha(33), linha(47), linha(59)],
}
destino = sys.argv[1] if len(sys.argv) > 1 else "data/estrutura-produto.json"
json.dump(dados, open(destino, "w", encoding="utf-8"), ensure_ascii=False, indent=2)
for l in dados["linhas"]:
    n_it = sum(len(e["itens"]) for e in l["escopos"])
    print(l["codigo"], len(l["escopos"]), "escopos,", n_it, "itens,", sum(1 for e in l["escopos"] if not e["padrao"]), "opcionais")
