document.addEventListener("DOMContentLoaded", function () {

  // ELEMENTOS DE LOGIN
  const telaLogin = document.getElementById("login");
  const telaSistema = document.getElementById("app");
  const campoLogin = document.getElementById("login-login");
  const campoSenha = document.getElementById("login-senha");
  const mensagemErro = document.getElementById("login-erro");
  const botaoEntrar = document.querySelector("[data-action='entrar']");
  const botaoSair = document.querySelector("[data-action='sair']");
  const nomeUsuario = document.getElementById("usuario-nome");
  const perfilUsuario = document.getElementById("usuario-perfil");

  // ELEMENTOS DO PAINEL
  const inicioNome = document.getElementById("inicio-nome");
  const inicioAviso = document.getElementById("inicio-aviso");

  // NAVEGAÇÃO PRINCIPAL
  function mostrarTela(nome) {
    const alvo = document.getElementById("tela-" + nome);

    if (!alvo) {
      console.warn("Tela ainda não criada: tela-" + nome);
      return;
    }

    document.querySelectorAll(".tela").forEach(t => (t.hidden = true));
    alvo.hidden = false;

    document.querySelectorAll("[data-go]").forEach(l => {
      l.classList.toggle("active", l.getAttribute("data-go") === nome);
    });
  }

  document.querySelectorAll("[data-go]").forEach(link => {
    link.addEventListener("click", function (e) {
      e.preventDefault();
      mostrarTela(this.getAttribute("data-go"));

      const menu = document.getElementById("menu");
      if (menu && menu.classList.contains("show") && window.bootstrap) {
        bootstrap.Collapse.getOrCreateInstance(menu).hide();
      }
    });
  });

  // === LOGIN ===
  function fazerLogin() {
    const login = campoLogin.value.trim();
    const senha = campoSenha.value.trim();

    mensagemErro.textContent = "";

    if (login === "" || senha === "") {
      mensagemErro.textContent = "Preencha todos os campos.";
      return;
    }

    if (login === "operador" && senha === "1234") {
      logarUsuario("Vinicius Rodrigo", " (Operador)", false);
    } else if (login === "admin" && senha === "1234") {
      logarUsuario("Esdras Batista", " (Adm do Estoque)", false);
    } else if (login === "sistema" && senha === "1234") {
      logarUsuario("Administrador do Sistema", " (Adm do Sistema)", true);
    } else {
      mensagemErro.textContent = "Usuário ou senha incorretos.";
    }
  }

  function logarUsuario(nome, perfil, isAdminSistema) {
    telaLogin.hidden = true;
    telaSistema.hidden = false;

    if (nomeUsuario) nomeUsuario.textContent = nome;
    if (perfilUsuario) perfilUsuario.textContent = perfil;
    if (inicioNome) inicioNome.textContent = nome;

    if (inicioAviso) inicioAviso.hidden = !isAdminSistema;

    mostrarTela("inicio");
  }

  if (botaoEntrar) botaoEntrar.addEventListener("click", fazerLogin);

  [campoLogin, campoSenha].forEach(campo => {
    if (campo) {
      campo.addEventListener("keydown", function (evento) {
        if (evento.key === "Enter") fazerLogin();
      });
    }
  });

  if (botaoSair) {
    botaoSair.addEventListener("click", function () {
      telaSistema.hidden = true;
      telaLogin.hidden = false;
      campoLogin.value = "";
      campoSenha.value = "";
      mensagemErro.textContent = "";
    });
  }

  // ABAS (REGISTRAR MOVIMENTAÇÃO)
  const botoesAbas = document.querySelectorAll("#tela-registrar .nav-tabs .nav-link");
  const conteudosAbas = document.querySelectorAll("#tela-registrar .aba");

  botoesAbas.forEach(botao => {
    botao.addEventListener("click", function () {
      botoesAbas.forEach(b => b.classList.remove("active"));
      this.classList.add("active");

      conteudosAbas.forEach(aba => (aba.hidden = true));

      const abaAlvo = document.getElementById(this.getAttribute("data-aba"));
      if (abaAlvo) abaAlvo.hidden = false;
    });
  });

  // DADOS (começam vazios e ficam salvos no navegador)
  const CHAVE = "movimentacoes_estoque";

  function carregarDados() {
    try {
      return JSON.parse(localStorage.getItem(CHAVE)) || [];
    } catch (e) {
      return [];
    }
  }

  function salvarDados() {
    try {
      localStorage.setItem(CHAVE, JSON.stringify(movimentacoes));
    } catch (e) {
      console.warn("Não foi possível salvar os dados.");
    }
  }

  const movimentacoes = carregarDados();

  // FUNÇÕES AUXILIARES
  function hojeISO() {
    const d = new Date();
    const mes = String(d.getMonth() + 1).padStart(2, "0");
    const dia = String(d.getDate()).padStart(2, "0");
    return d.getFullYear() + "-" + mes + "-" + dia;
  }

  function formatarData(iso) {
    const [a, m, d] = iso.split("-");
    return d + "/" + m + "/" + a;
  }

  function escapar(texto) {
    const div = document.createElement("div");
    div.textContent = texto;
    return div.innerHTML;
  }

  function montarTabela(tabela, colunas, linhas) {
    const cabecalho = "<thead><tr>" +
      colunas.map(c => "<th>" + c.titulo + "</th>").join("") +
      "</tr></thead>";

    const corpo = linhas.length
      ? "<tbody>" + linhas.map(l =>
          "<tr>" + colunas.map(c => "<td>" + escapar(String(c.valor(l))) + "</td>").join("") + "</tr>"
        ).join("") + "</tbody>"
      : '<tbody><tr><td colspan="' + colunas.length + '" class="text-center text-muted">Nenhuma ação realizada ainda.</td></tr></tbody>';

    tabela.innerHTML = cabecalho + corpo;
  }

  // Quantidade que o recebedor ainda tem em mãos de um material
  function quantidadeEmAberto(recebedor, material) {
    let total = 0;
    movimentacoes.forEach(m => {
      if (m.recebedor.toLowerCase() === recebedor.toLowerCase() &&
          m.material.toLowerCase() === material.toLowerCase()) {
        total += m.tipo === "Empenho" ? m.qtd : -m.qtd;
      }
    });
    return total;
  }

  // HISTÓRICO 
  const histTipo = document.getElementById("hist-tipo");
  const histRecebedor = document.getElementById("hist-recebedor");
  const tabelaHistorico = document.getElementById("tabela-historico");

  function preencherRecebedores() {
    if (!histRecebedor) return;
    const selecionado = histRecebedor.value;
    const nomes = [...new Set(movimentacoes.map(m => m.recebedor))].sort();
    histRecebedor.innerHTML = '<option value="">Todos os recebedores</option>' +
      nomes.map(n => '<option value="' + escapar(n) + '">' + escapar(n) + "</option>").join("");
    if (nomes.includes(selecionado)) histRecebedor.value = selecionado;
  }

  function renderizarHistorico() {
    if (!tabelaHistorico || !histTipo || !histRecebedor) return;

    const tipo = histTipo.value;
    const recebedor = histRecebedor.value;

    const filtradas = movimentacoes
      .filter(m =>
        (tipo === "" || m.tipo === tipo) &&
        (recebedor === "" || m.recebedor === recebedor)
      )
      .slice()
      .reverse();

    montarTabela(tabelaHistorico, [
      { titulo: "Data",      valor: m => formatarData(m.data) },
      { titulo: "Tipo",      valor: m => m.tipo },
      { titulo: "Material",  valor: m => m.material },
      { titulo: "Qtd",       valor: m => m.qtd },
      { titulo: "Recebedor", valor: m => m.recebedor },
      { titulo: "Situação",  valor: m => m.status }
    ], filtradas);
  }

  if (histTipo) histTipo.addEventListener("change", renderizarHistorico);
  if (histRecebedor) histRecebedor.addEventListener("change", renderizarHistorico);

  // RELATÓRIOS
  const relTipo = document.getElementById("rel-tipo");
  const tabelaRelatorio = document.getElementById("tabela-relatorio");
  const botaoGerar = document.querySelector("[data-action='gerar-relatorio']");
  const botaoImprimir = document.querySelector("[data-action='imprimir']");

  const colunasDevolucao = [
    { titulo: "Data",      valor: m => formatarData(m.data) },
    { titulo: "Material",  valor: m => m.material },
    { titulo: "Qtd",       valor: m => m.qtd },
    { titulo: "Recebedor", valor: m => m.recebedor },
    { titulo: "Situação",  valor: m => m.status }
  ];

  function gerarRelatorio() {
    if (!tabelaRelatorio || !relTipo) return;

    switch (relTipo.value) {

      case "abertos": {
        const mapa = {};
        movimentacoes.forEach(m => {
          const chave = m.recebedor.toLowerCase() + "||" + m.material.toLowerCase();
          if (!mapa[chave]) mapa[chave] = { recebedor: m.recebedor, material: m.material, qtd: 0 };
          mapa[chave].qtd += m.tipo === "Empenho" ? m.qtd : -m.qtd;
        });
        const abertos = Object.values(mapa).filter(x => x.qtd > 0);

        montarTabela(tabelaRelatorio, [
          { titulo: "Recebedor",     valor: x => x.recebedor },
          { titulo: "Material",      valor: x => x.material },
          { titulo: "Qtd em aberto", valor: x => x.qtd }
        ], abertos);
        break;
      }

      case "extraviados":
        montarTabela(tabelaRelatorio, colunasDevolucao,
          movimentacoes.filter(m => m.tipo === "Devolução" && m.status === "Extraviado"));
        break;

      case "avariados":
        montarTabela(tabelaRelatorio, colunasDevolucao,
          movimentacoes.filter(m => m.tipo === "Devolução" && m.status === "Avariado"));
        break;

      case "estoque": {
        const mapa = {};
        movimentacoes.forEach(m => {
          const chave = m.material.toLowerCase();
          if (!mapa[chave]) mapa[chave] = { material: m.material, empenhado: 0, devolvido: 0, avariado: 0, extraviado: 0 };
          const item = mapa[chave];
          if (m.tipo === "Empenho") {
            item.empenhado += m.qtd;
          } else {
            item.devolvido += m.qtd;
            if (m.status === "Avariado") item.avariado += m.qtd;
            if (m.status === "Extraviado") item.extraviado += m.qtd;
          }
        });

        montarTabela(tabelaRelatorio, [
          { titulo: "Material",        valor: x => x.material },
          { titulo: "Total empenhado", valor: x => x.empenhado },
          { titulo: "Total devolvido", valor: x => x.devolvido },
          { titulo: "Em aberto",       valor: x => x.empenhado - x.devolvido },
          { titulo: "Avariados",       valor: x => x.avariado },
          { titulo: "Extraviados",     valor: x => x.extraviado }
        ], Object.values(mapa));
        break;
      }
    }
  }

  if (botaoGerar) botaoGerar.addEventListener("click", gerarRelatorio);
  if (botaoImprimir) botaoImprimir.addEventListener("click", function () { window.print(); });

  // REGISTRAR MOVIMENTAÇÃO
  const registrarMsg = document.getElementById("registrar-msg");

  function mostrarMensagem(texto, tipo) {
    if (!registrarMsg) return;
    registrarMsg.innerHTML = '<div class="alert alert-' + tipo + ' py-2 mb-0">' + escapar(texto) + "</div>";
  }

  function atualizarTelas() {
    salvarDados();
    preencherRecebedores();
    renderizarHistorico();
    if (tabelaRelatorio && tabelaRelatorio.innerHTML.trim() !== "") gerarRelatorio();
  }

  function registrarEmpenho() {
    const material = document.getElementById("emp-material").value.trim();
    const qtd = parseInt(document.getElementById("emp-qtd").value, 10);
    const recebedor = document.getElementById("emp-recebedor").value.trim();

    if (!material || !recebedor || !qtd || qtd < 1) {
      mostrarMensagem("Preencha material, quantidade e recebedor.", "warning");
      return;
    }

    movimentacoes.push({
      data: hojeISO(),
      tipo: "Empenho",
      material: material,
      qtd: qtd,
      recebedor: recebedor,
      status: "Registrado"
    });

    atualizarTelas();
    mostrarMensagem("Empenho registrado com sucesso.", "success");

    document.getElementById("emp-material").value = "";
    document.getElementById("emp-qtd").value = "";
    document.getElementById("emp-recebedor").value = "";
  }

  function registrarDevolucao() {
    const material = document.getElementById("dev-material").value.trim();
    const qtd = parseInt(document.getElementById("dev-qtd").value, 10);
    const recebedor = document.getElementById("dev-recebedor").value.trim();
    const condicao = document.getElementById("dev-condicao").value;

    if (!material || !recebedor || !qtd || qtd < 1) {
      mostrarMensagem("Preencha material, quantidade e recebedor.", "warning");
      return;
    }

    const emAberto = quantidadeEmAberto(recebedor, material);
    if (qtd > emAberto) {
      mostrarMensagem(
        emAberto > 0
          ? "Esse recebedor só tem " + emAberto + " unidade(s) em aberto desse material."
          : "Não há empenho em aberto desse material para esse recebedor.",
        "danger"
      );
      return;
    }

    movimentacoes.push({
      data: hojeISO(),
      tipo: "Devolução",
      material: material,
      qtd: qtd,
      recebedor: recebedor,
      status: condicao
    });

    atualizarTelas();
    mostrarMensagem("Devolução registrada com sucesso.", "success");

    document.getElementById("dev-material").value = "";
    document.getElementById("dev-qtd").value = "";
    document.getElementById("dev-recebedor").value = "";
    document.getElementById("dev-condicao").value = "Bom estado";
  }

  const botaoEmpenho = document.querySelector("[data-action='registrar-empenho']");
  const botaoDevolucao = document.querySelector("[data-action='registrar-devolucao']");
  if (botaoEmpenho) botaoEmpenho.addEventListener("click", registrarEmpenho);
  if (botaoDevolucao) botaoDevolucao.addEventListener("click", registrarDevolucao);

  // INICIALIZAÇÃO 
  preencherRecebedores();
  renderizarHistorico();

});