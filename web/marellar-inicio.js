// Sobe o motor do Marellar (Java compilado para JavaScript pelo TeaVM) antes do jogo pedir a
// primeira partida. O cliente Godot espera window.marellarStatus virar "pronto" (ver api.gd).
(function () {
  "use strict";
  var ARQUIVOS = ["linha-33.json", "nc-catalogo.json", "pessoas.json", "fornecedores.json",
    "eventos.json", "financeiro.json", "textos-ptbr.json"];
  window.marellarStatus = "carregando";
  try {
    main([]); // registra window.marellarIniciar e window.marellarRequisitar
  } catch (e) {
    window.marellarStatus = "erro ao iniciar o motor: " + e;
    return;
  }
  Promise.all(ARQUIVOS.map(function (nome) {
    return fetch("data/" + nome, { cache: "no-cache" }).then(function (r) {
      if (!r.ok) { throw new Error(nome + ": HTTP " + r.status); }
      return r.text();
    });
  })).then(function (conteudos) {
    var arquivos = {};
    ARQUIVOS.forEach(function (nome, i) { arquivos[nome] = conteudos[i]; });
    var erro = window.marellarIniciar(arquivos);
    window.marellarStatus = erro === "" ? "pronto" : "dados invalidos: " + erro;
  }).catch(function (e) {
    window.marellarStatus = "erro ao baixar os dados: " + e.message;
  });
})();
