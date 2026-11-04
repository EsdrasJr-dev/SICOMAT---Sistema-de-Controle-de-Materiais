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

    // Atualiza as telas toda vez que elas são abertas
    if (nome === "gerenciar") {
      atualizarGerenciar();
    }
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
      logarUsuario("Vinicius Rodrigo", " (Operador)", false, false);
    } else if (login === "admin" && senha === "1234") {
      logarUsuario("Esdras Batista", " (Adm do Estoque)", false, true);
    } else if (login === "sistema" && senha === "1234") {
      logarUsuario("Administrador do Sistema", " (Adm do Sistema)", true, false);
    } else {
      mensagemErro.textContent = "Usuário ou senha incorretos.";
    }
  }

  function logarUsuario(nome, perfil, isAdminSistema, isAdminEstoque) {
    telaLogin.hidden = true;
    telaSistema.hidden = false;

    if (nomeUsuario) nomeUsuario.textContent = nome;
    if (perfilUsuario) perfilUsuario.textContent = perfil;
    if (inicioNome) inicioNome.textContent = nome;

    if (inicioAviso) inicioAviso.hidden = !isAdminSistema;

    // O menu "Gerenciar Materiais" só aparece para o Adm do Estoque
    const linkGerenciar = document.querySelector("[data-go='gerenciar']");
    if (linkGerenciar) linkGerenciar.parentElement.hidden = !isAdminEstoque;

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

  // ABAS Registrar Movimentação e Gerenciar Materiais
  const todosBotoesAbas = document.querySelectorAll(".nav-tabs .nav-link");

  todosBotoesAbas.forEach(botao => {
    botao.addEventListener("click", function () {
      const listaAbas = this.parentElement.parentElement;
      const tela = listaAbas.parentElement;              

      listaAbas.querySelectorAll(".nav-link").forEach(b => b.classList.remove("active"));
      this.classList.add("active");

      tela.querySelectorAll(".aba").forEach(aba => (aba.hidden = true));

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

  // GERENCIAR MATERIAIS
  const CH_MATERIAIS = "sicomat_materiais";
  const CH_ITENS = "sicomat_itens";
  const CH_RECEBEDORES = "sicomat_recebedores";

  function lerLista(chave) {
    try {
      return JSON.parse(localStorage.getItem(chave)) || [];
    } catch (e) {
      return [];
    }
  }
  function gravarLista(chave, lista) {
    try {
      localStorage.setItem(chave, JSON.stringify(lista));
    } catch (e) {
      console.warn("Não foi possível salvar.");
    }
  }
  const materiais = lerLista(CH_MATERIAIS);
  const itens = lerLista(CH_ITENS);
  const recebedores = lerLista(CH_RECEBEDORES);

  // Funções auxiliares desta tela
  function escaparAttr(texto) {
    return escapar(String(texto)).replace(/"/g, "&quot;");
  }
  // tabela onde já é HTML pronto (para poder ter botões)
  function montarTabelaGerenciar(tabela, titulos, linhas) {
    let html = "<thead><tr>";
    for (let i = 0; i < titulos.length; i++) {
      html += "<th>" + titulos[i] + "</th>";
    }
    html += "</tr></thead><tbody>";

    if (linhas.length === 0) {
      html += '<tr><td colspan="' + titulos.length + '" class="text-center text-muted">Nenhum registro cadastrado.</td></tr>';
    }
    for (let j = 0; j < linhas.length; j++) {
      html += "<tr>";
      for (let k = 0; k < linhas[j].length; k++) {
        html += "<td>" + linhas[j][k] + "</td>";
      }
      html += "</tr>";
    }
    tabela.innerHTML = html + "</tbody>";
  }
  // Cria um botão pequeno com a ação e o id que ele vai usar
  function botaoAcao(texto, classe, acao, id) {
    return '<button type="button" class="btn btn-sm ' + classe + ' me-1" data-acao="' + acao +
      '" data-id="' + escaparAttr(id) + '">' + texto + "</button>";
  }
  function acharMaterial(codigo) {
    for (let i = 0; i < materiais.length; i++) {
      if (materiais[i].codigo === codigo) return materiais[i];
    }
    return null;
  }
  function nomeDoMaterial(codigo) {
    const m = acharMaterial(codigo);
    return m ? m.nome : "(material removido)";
  }
  // ABA MATERIAIS
  const matCodigo = document.getElementById("mat-codigo");
  const matNome = document.getElementById("mat-nome");
  const matSecao = document.getElementById("mat-secao");
  const matErro = document.getElementById("mat-erro");
  const tabelaGerMateriais = document.getElementById("tabela-gerenciar-materiais");

  let codigoEmEdicao = "";

  function mostrarMateriais() {
    if (!tabelaGerMateriais) return;

    const linhas = [];
    for (let i = 0; i < materiais.length; i++) {
      const m = materiais[i];
      const situacao = m.ativo ? "Ativo" : "Inativo";
      const textoInativar = m.ativo ? "Inativar" : "Reativar";

      linhas.push([
        escapar(m.codigo),
        escapar(m.nome),
        escapar(m.secao),
        situacao,
        botaoAcao("Editar", "btn-outline-primary", "editar-material", m.codigo) +
        botaoAcao(textoInativar, "btn-outline-secondary", "ativar-material", m.codigo)
      ]);
    }
    montarTabelaGerenciar(tabelaGerMateriais, ["Código", "Material", "Seção", "Situação", "Ações"], linhas);
  }
  function salvarMaterial() {
    matErro.textContent = "";

    const codigo = matCodigo.value.trim();
    const nome = matNome.value.trim();
    const secao = matSecao.value.trim();

    if (codigo === "" || nome === "" || secao === "") {
      matErro.textContent = "Preencha código, nome e seção.";
      return;
    }
    if (codigoEmEdicao !== "") {
      // edita nome e ação
      const m = acharMaterial(codigoEmEdicao);
      if (m) {
        m.nome = nome;
        m.secao = secao;
      }
    } else {
      // Não deixa repetir o código
      if (acharMaterial(codigo)) {
        matErro.textContent = "Já existe um material com esse código.";
        return;
      }
      materiais.push({ codigo: codigo, nome: nome, secao: secao, ativo: true });
    }
    gravarLista(CH_MATERIAIS, materiais);

    // Limpa o formulário
    codigoEmEdicao = "";
    matCodigo.value = "";
    matCodigo.readOnly = false;
    matNome.value = "";
    matSecao.value = "";

    atualizarGerenciar();
  }
  if (tabelaGerMateriais) {
    tabelaGerMateriais.addEventListener("click", function (e) {
      const acao = e.target.getAttribute("data-acao");
      const codigo = e.target.getAttribute("data-id");
      if (!acao) return;

      const m = acharMaterial(codigo);
      if (!m) return;

      if (acao === "editar-material") {
        // coloca osdados nos campos para editar
        codigoEmEdicao = m.codigo;
        matCodigo.value = m.codigo;
        matCodigo.readOnly = true;
        matNome.value = m.nome;
        matSecao.value = m.secao;
        matErro.textContent = "";
        matNome.focus();
      }
      if (acao === "ativar-material") {
        m.ativo = !m.ativo;
        gravarLista(CH_MATERIAIS, materiais);
        atualizarGerenciar();
      }
    });
  }
  const botaoSalvarMaterial = document.querySelector("[data-action='salvar-material']");
  if (botaoSalvarMaterial) botaoSalvarMaterial.addEventListener("click", salvarMaterial);

  // ABA ITENS 
  const itemMaterial = document.getElementById("item-material");
  const itemPatrimonio = document.getElementById("item-patrimonio");
  const itemErro = document.getElementById("item-erro");
  const tabelaGerItens = document.getElementById("tabela-gerenciar-itens");

  // Preenche o select só com materiais ativos
  function preencherSelectMateriais() {
    if (!itemMaterial) return;

    const selecionado = itemMaterial.value;
    let html = "";

    for (let i = 0; i < materiais.length; i++) {
      if (materiais[i].ativo) {
        html += '<option value="' + escaparAttr(materiais[i].codigo) + '">' +
          escapar(materiais[i].codigo + " – " + materiais[i].nome) + "</option>";
      }
    }
    if (html === "") {
      html = '<option value="">Cadastre um material primeiro</option>';
    }
    itemMaterial.innerHTML = html;
    if (selecionado !== "") itemMaterial.value = selecionado;
  }
  function mostrarItens() {
    if (!tabelaGerItens) return;

    const linhas = [];
    for (let i = 0; i < itens.length; i++) {
      const it = itens[i];
      const situacao = it.ativo ? "Disponível" : "Inativo";
      const textoBotao = it.ativo ? "Inativar" : "Liberar";

      linhas.push([
        escapar(it.patrimonio),
        escapar(nomeDoMaterial(it.codigo)),
        situacao,
        botaoAcao(textoBotao, "btn-outline-secondary", "ativar-item", it.patrimonio)
      ]);
    }
    montarTabelaGerenciar(tabelaGerItens, ["Patrimônio", "Material", "Situação", "Ações"], linhas);
  }
  function salvarItem() {
    itemErro.textContent = "";

    const codigo = itemMaterial.value;
    const patrimonio = itemPatrimonio.value.trim();

    if (codigo === "") {
      itemErro.textContent = "Escolha um material (cadastre um na aba Materiais).";
      return;
    }
    if (patrimonio === "") {
      itemErro.textContent = "Informe o número de patrimônio.";
      return;
    }
    // Não deixa repetir o patrimônio
    for (let i = 0; i < itens.length; i++) {
      if (itens[i].patrimonio === patrimonio) {
        itemErro.textContent = "Esse patrimônio já está cadastrado.";
        return;
      }
    }
    itens.push({ patrimonio: patrimonio, codigo: codigo, ativo: true });
    gravarLista(CH_ITENS, itens);

    itemPatrimonio.value = "";
    atualizarGerenciar();
  }
  if (tabelaGerItens) {
    tabelaGerItens.addEventListener("click", function (e) {
      const acao = e.target.getAttribute("data-acao");
      const patrimonio = e.target.getAttribute("data-id");
      if (acao !== "ativar-item") return;

      for (let i = 0; i < itens.length; i++) {
        if (itens[i].patrimonio === patrimonio) {
          itens[i].ativo = !itens[i].ativo; // inativa ou libera
        }
      }
      gravarLista(CH_ITENS, itens);
      atualizarGerenciar();
    });
  }
  const botaoSalvarItem = document.querySelector("[data-action='salvar-item']");
  if (botaoSalvarItem) botaoSalvarItem.addEventListener("click", salvarItem);

  // ABA RECEBEDORES
  const recNome = document.getElementById("rec-nome");
  const recMatricula = document.getElementById("rec-matricula");
  const recSenha = document.getElementById("rec-senha");
  const recErro = document.getElementById("rec-erro");
  const tabelaRecebedores = document.getElementById("tabela-recebedores");

  function mostrarRecebedores() {
    if (!tabelaRecebedores) return;

    const linhas = [];
    for (let i = 0; i < recebedores.length; i++) {
      const r = recebedores[i];
      linhas.push([
        escapar(r.nome),
        escapar(r.matricula),
        botaoAcao("Remover", "btn-outline-danger", "remover-recebedor", r.matricula)
      ]);
    }
    montarTabelaGerenciar(tabelaRecebedores, ["Nome", "Matrícula", "Ações"], linhas);
  }
  function salvarRecebedor() {
    recErro.textContent = "";

    const nome = recNome.value.trim();
    const matricula = recMatricula.value.trim();
    const senha = recSenha.value.trim();

    if (nome === "" || matricula === "" || senha === "") {
      recErro.textContent = "Preencha nome, matrícula e senha.";
      return;
    }

    // Não deixa repetir a matrícula
    for (let i = 0; i < recebedores.length; i++) {
      if (recebedores[i].matricula === matricula) {
        recErro.textContent = "Essa matrícula já está cadastrada.";
        return;
      }
    }

    recebedores.push({ nome: nome, matricula: matricula, senha: senha });
    gravarLista(CH_RECEBEDORES, recebedores);

    recNome.value = "";
    recMatricula.value = "";
    recSenha.value = "";

    atualizarGerenciar();
  }

  if (tabelaRecebedores) {
    tabelaRecebedores.addEventListener("click", function (e) {
      const acao = e.target.getAttribute("data-acao");
      const matricula = e.target.getAttribute("data-id");
      if (acao !== "remover-recebedor") return;

      if (!confirm("Remover este recebedor?")) return;

      for (let i = 0; i < recebedores.length; i++) {
        if (recebedores[i].matricula === matricula) {
          recebedores.splice(i, 1);
          break;
        }
      }
      gravarLista(CH_RECEBEDORES, recebedores);
      atualizarGerenciar();
    });
  }
  const botaoSalvarRecebedor = document.querySelector("[data-action='salvar-recebedor']");
  if (botaoSalvarRecebedor) botaoSalvarRecebedor.addEventListener("click", salvarRecebedor);

  // ABA EXTRAVIOS
  const tabelaExtravios = document.getElementById("tabela-extravios");

  function mostrarExtravios() {
    if (!tabelaExtravios) return;

    // Soma empenhos e subtrai devoluções por recebedor + material
    const grupos = {};
    for (let i = 0; i < movimentacoes.length; i++) {
      const m = movimentacoes[i];
      const chave = m.recebedor + "||" + m.material;

      if (!grupos[chave]) {
        grupos[chave] = { recebedor: m.recebedor, material: m.material, qtd: 0 };
      }
      if (m.tipo === "Empenho") {
        grupos[chave].qtd += m.qtd;
      } else {
        grupos[chave].qtd -= m.qtd;
      }
    }
    const linhas = [];
    for (const chave in grupos) {
      if (grupos[chave].qtd > 0) {
        linhas.push([
          escapar(grupos[chave].recebedor),
          escapar(grupos[chave].material),
          grupos[chave].qtd
        ]);
      }
    }
    montarTabelaGerenciar(tabelaExtravios, ["Recebedor", "Material", "Qtd não devolvida"], linhas);
  }
  // Atualiza as 4 abas de uma vez
  function atualizarGerenciar() {
    mostrarMateriais();
    preencherSelectMateriais();
    mostrarItens();
    mostrarRecebedores();
    mostrarExtravios();
  }
  // INICIALIZAÇÃO 
  preencherRecebedores();
  renderizarHistorico();
  atualizarGerenciar();

});