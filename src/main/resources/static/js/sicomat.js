document.addEventListener("DOMContentLoaded", function () {

  // =====================================================================
  // ELEMENTOS DE LOGIN E PAINEL
  // =====================================================================
  const telaLogin = document.getElementById("login");
  const telaSistema = document.getElementById("app");
  const campoLogin = document.getElementById("login-login");
  const campoSenha = document.getElementById("login-senha");
  const mensagemErro = document.getElementById("login-erro");
  const botaoEntrar = document.querySelector("[data-action='entrar']");
  const botaoSair = document.querySelector("[data-action='sair']");
  const nomeUsuario = document.getElementById("usuario-nome");
  const perfilUsuario = document.getElementById("usuario-perfil");
  const inicioNome = document.getElementById("inicio-nome");
  const inicioAviso = document.getElementById("inicio-aviso");

  // =====================================================================
  // USUÁRIOS (contas de acesso)
  // =====================================================================
  const CH_USUARIOS = "sicomat_usuarios";
  const PERFIS = {
    operador: " (Operador)",
    estoque: " (Adm do Estoque)",
    sistema: " (Adm do Sistema)"
  };

  // Telas que cada perfil pode abrir
  const TELAS_PERMITIDAS = {
    operador: ["inicio", "consultar", "registrar", "historico", "relatorios", "perfil"],
    estoque: ["inicio", "consultar", "registrar", "historico", "relatorios", "gerenciar", "perfil"],
    sistema: ["inicio", "perfil", "usuarios"]
  };

  function lerUsuarios() {
    try {
      const lista = JSON.parse(localStorage.getItem(CH_USUARIOS));
      if (Array.isArray(lista) && lista.length) return lista;
    } catch (e) {}
    return [
      { nome: "Vinicius Rodrigo", login: "operador", senha: "1234", perfil: "operador", ativo: true },
      { nome: "Esdras Batista", login: "admin", senha: "1234", perfil: "estoque", ativo: true },
      { nome: "Administrador do Sistema", login: "sistema", senha: "1234", perfil: "sistema", ativo: true }
    ];
  }
  function gravarUsuarios() {
    try {
      localStorage.setItem(CH_USUARIOS, JSON.stringify(usuarios));
    } catch (e) {
      console.warn("Não foi possível salvar os usuários.");
    }
  }
  const usuarios = lerUsuarios();
  let usuarioLogado = null;
  let autorizacaoAtual = null;
  let materiaisApi = [];
  let consultaMateriaisVersao = 0;
  let itensApi = [];
  let itensDisponiveisApi = [];
  let consultaItensVersao = 0;
  let salvandoItem = false;

  // Dados de equipes e recebedores recebidos da API
  let equipesApi = [];
  let recebedoresApi = [];

  let consultaEquipesVersao = 0;
  let consultaRecebedoresVersao = 0;

  let salvandoEquipe = false;
  let salvandoRecebedor = false;

    // Função central de comunicação com o backend
  async function requisitarApi(
    caminho,
    metodo = "GET",
    dados = null,
    autorizacao = autorizacaoAtual
  ) {
    if (!autorizacao) {
      throw new Error("Usuário não autenticado.");
    }

    const headers = {
      "Authorization": autorizacao,
      "Accept": "application/json"
    };

    const opcoes = {
      method: metodo,
      headers: headers,
      credentials: "same-origin"
    };

    // Obtém o token CSRF para operações de escrita
    if (!["GET", "HEAD", "OPTIONS"].includes(metodo)) {
      const csrf = await requisitarApi(
        "/api/v1/csrf",
        "GET",
        null,
        autorizacao
      );

      headers[csrf.headerName] = csrf.token;
    }

    // Converte os dados enviados para JSON
    if (dados !== null) {
      headers["Content-Type"] = "application/json";
      opcoes.body = JSON.stringify(dados);
    }

    const resposta = await fetch(caminho, opcoes);

    if (!resposta.ok) {
      throw new Error("HTTP " + resposta.status);
    }

    // Trata respostas sem conteúdo
    const conteudo = await resposta.text();

    return conteudo ? JSON.parse(conteudo) : null;
  }

  // Consulta os dados do usuário autenticado
  async function consultarConta(autorizacao) {
    return await requisitarApi(
      "/api/v1/conta",
      "GET",
      null,
      autorizacao
    );
  }

    // Busca os materiais cadastrados no backend
    async function carregarMateriais() {
    const dados = await requisitarApi("/api/v1/materiais");

    if (!Array.isArray(dados)) {
      throw new Error("Resposta de materiais inválida.");
    }

    return dados;
  }

    async function carregarItens() {
    const [todos, disponiveis] = await Promise.all([
      requisitarApi("/api/v1/itens"),
      requisitarApi("/api/v1/itens/disponiveis")
    ]);

    if (!Array.isArray(todos) ||
        !Array.isArray(disponiveis)) {
      throw new Error("Resposta de itens inválida.");
    }

    return { todos, disponiveis };
  }

  // Busca as equipes cadastradas no backend
async function carregarEquipes() {
  const dados = await requisitarApi("/api/v1/equipes");

  if (!Array.isArray(dados)) {
    throw new Error("Resposta de equipes inválida.");
  }

  return dados;
}

// Busca os recebedores cadastrados no backend
async function carregarRecebedores() {
  const dados = await requisitarApi("/api/v1/recebedores");

  if (!Array.isArray(dados)) {
    throw new Error("Resposta de recebedores inválida.");
  }

  return dados;
}

function converterPerfil(perfilBackend) {
  const perfis = {
    OPERADOR_ESTOQUE: "operador",
    ADMINISTRADOR_ESTOQUE: "estoque",
    ADMINISTRADOR_SISTEMA: "sistema"
  };

  return perfis[perfilBackend] || null;
}

function telaPermitida(nome) {
  return !!usuarioLogado &&
    TELAS_PERMITIDAS[usuarioLogado.perfil].includes(nome);
}

  // =====================================================================
  // DADOS DO ESTOQUE (ficam salvos no navegador)
  // =====================================================================
  const CH_MATERIAIS = "sicomat_materiais";
  const CH_ITENS = "sicomat_itens";
  const CH_RECEBEDORES = "sicomat_recebedores";
  const CH_EQUIPES = "sicomat_equipes";
  const CH_EMPENHOS = "sicomat_empenhos";

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

  const materiais = lerLista(CH_MATERIAIS);   // { codigo, nome, secao, ativo }
  const itens = lerLista(CH_ITENS);           // { patrimonio, codigo, ativo }
  const recebedores = lerLista(CH_RECEBEDORES); // { nome, matricula, senha }
  const equipes = lerLista(CH_EQUIPES);       // { nome }
  // empenho: { id, data, dataConf, recebedor(matrícula), recebedorNome, equipe,
  //            status: Pendente | Confirmado | Encerrado | Cancelado, criadoPor,
  //            itens: [{ patrimonio, codigo, material,
  //                      situacao: Em uso | Devolvido | Avariado | Extraviado | Cancelado, dataDev }] }
  const empenhos = lerLista(CH_EMPENHOS);

  // =====================================================================
  // FUNÇÕES AUXILIARES
  // =====================================================================
  function hojeISO() {
    const d = new Date();
    const mes = String(d.getMonth() + 1).padStart(2, "0");
    const dia = String(d.getDate()).padStart(2, "0");
    return d.getFullYear() + "-" + mes + "-" + dia;
  }

  function formatarData(iso) {
    if (!iso) return "";
    const [a, m, d] = iso.split("-");
    return d + "/" + m + "/" + a;
  }

  function escapar(texto) {
    const div = document.createElement("div");
    div.textContent = texto;
    return div.innerHTML;
  }

  function escaparAttr(texto) {
    return escapar(String(texto)).replace(/"/g, "&quot;");
  }

  // Tabela com colunas { titulo, valor(linha) } — os valores são escapados
  function montarTabela(tabela, colunas, linhas) {
    const cabecalho = "<thead><tr>" +
      colunas.map(c => "<th>" + c.titulo + "</th>").join("") +
      "</tr></thead>";

    const corpo = linhas.length
      ? "<tbody>" + linhas.map(l =>
          "<tr>" + colunas.map(c => "<td>" + escapar(String(c.valor(l))) + "</td>").join("") + "</tr>"
        ).join("") + "</tbody>"
      : '<tbody><tr><td colspan="' + colunas.length + '" class="text-center text-muted">Nenhum registro encontrado.</td></tr></tbody>';

    tabela.innerHTML = cabecalho + corpo;
  }

  // Tabela onde as células já são HTML pronto (para ter botões)
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

  // Preenche um select mantendo o valor escolhido, se ainda existir
  function preencherSelect(sel, rotuloVazio, opcoes) {
    if (!sel) return;
    const atual = sel.value;
    let html = '<option value="">' + escapar(rotuloVazio) + "</option>";
    opcoes.forEach(o => {
      html += '<option value="' + escaparAttr(o.valor) + '">' + escapar(o.texto) + "</option>";
    });
    sel.innerHTML = html;
    if (opcoes.some(o => o.valor === atual)) sel.value = atual;
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
  function acharItem(patrimonio) {
    return itens.find(i => i.patrimonio === patrimonio) || null;
  }
  function acharEmpenho(id) {
    return empenhos.find(e => String(e.id) === String(id)) || null;
  }

  // Um item está "em empenho" se está num empenho pendente ou confirmado
  function itemEmUso(patrimonio) {
    return empenhos.some(e =>
      (e.status === "Pendente" || e.status === "Confirmado") &&
      e.itens.some(i => i.patrimonio === patrimonio && i.situacao === "Em uso")
    );
  }

  // Itens que podem ser empenhados agora
  function itensDisponiveis() {
    return itens.filter(it => {
      const m = acharMaterial(it.codigo);
      return it.ativo && m && m.ativo && !itemEmUso(it.patrimonio);
    });
  }

  function textoItens(emp) {
    return emp.itens.map(i => i.material + " (" + i.patrimonio + ")").join(", ");
  }

  // Lista os itens de empenhos com determinada situação
  function listarItens(situacao) {
    const r = [];
    empenhos.forEach(e => {
      if (e.status === "Cancelado") return;
      if (situacao === "Em uso" && e.status !== "Confirmado") return;
      e.itens.forEach(i => {
        if (i.situacao === situacao) {
          r.push({
            data: situacao === "Em uso" ? (e.dataConf || e.data) : (i.dataDev || e.data),
            recebedor: e.recebedorNome,
            recebMat: e.recebedor,
            equipe: e.equipe,
            material: i.material,
            patrimonio: i.patrimonio,
            empenho: e.id
          });
        }
      });
    });
    return r;
  }

  function temEmpenhoAtivo(matricula) {
    return empenhos.some(e =>
      e.recebedor === matricula &&
      (e.status === "Pendente" || e.status === "Confirmado")
    );
  }

  // Marca o empenho como encerrado quando todos os itens foram resolvidos
  function encerrarSeCompleto(emp) {
    if (emp.status === "Confirmado" && !emp.itens.some(i => i.situacao === "Em uso")) {
      emp.status = "Encerrado";
    }
  }

  // Registra o resultado de um item (devolvido, avariado ou extraviado)
  function resolverItem(emp, patrimonio, situacao) {
    const it = emp.itens.find(i => i.patrimonio === patrimonio);
    if (!it) return;
    it.situacao = situacao;
    it.dataDev = hojeISO();
    if (situacao !== "Devolvido") {
      const cadastro = acharItem(patrimonio);
      if (cadastro) cadastro.ativo = false;   // avariado/extraviado fica inativo
    }
    encerrarSeCompleto(emp);
  }

  function proximoId() {
    return empenhos.reduce((m, e) => Math.max(m, e.id), 0) + 1;
  }

  function gravarTudo() {
    gravarLista(CH_MATERIAIS, materiais);
    gravarLista(CH_ITENS, itens);
    gravarLista(CH_RECEBEDORES, recebedores);
    gravarLista(CH_EQUIPES, equipes);
    gravarLista(CH_EMPENHOS, empenhos);
  }

  // =====================================================================
  // JANELA DE MENSAGENS (modal)
  // =====================================================================
  const modalEl = document.getElementById("modal");
  const modalTitulo = document.getElementById("modal-titulo");
  const modalCorpo = document.getElementById("modal-corpo");
  const modalRodape = document.getElementById("modal-rodape");

  // botoes = lista de { texto, classe, aoClicar, manter }
  // manter = true: o botão NÃO fecha a janela sozinho
  function abrirModal(titulo, corpoHTML, botoes) {
    if (!modalEl || !window.bootstrap) return;

    modalTitulo.textContent = titulo;
    modalCorpo.innerHTML = corpoHTML;
    modalRodape.innerHTML = "";

    botoes.forEach(b => {
      const btn = document.createElement("button");
      btn.type = "button";
      btn.className = "btn " + b.classe;
      btn.textContent = b.texto;
      if (!b.manter) btn.setAttribute("data-bs-dismiss", "modal");
      if (b.aoClicar) btn.addEventListener("click", b.aoClicar);
      modalRodape.appendChild(btn);
    });

    bootstrap.Modal.getOrCreateInstance(modalEl).show();
  }

  function fecharModal() {
    if (modalEl && window.bootstrap) bootstrap.Modal.getOrCreateInstance(modalEl).hide();
  }

  function mostrarAviso(titulo, texto) {
    abrirModal(titulo, '<p class="mb-0">' + escapar(texto) + "</p>", [
      { texto: "Fechar", classe: "btn-secondary" }
    ]);
  }

  function confirmarAcao(titulo, texto, aoConfirmar) {
    abrirModal(titulo, '<p class="mb-0">' + escapar(texto) + "</p>", [
      { texto: "Cancelar", classe: "btn-outline-secondary" },
      { texto: "Confirmar", classe: "btn-primary", aoClicar: aoConfirmar }
    ]);
  }

  // =====================================================================
  // NAVEGAÇÃO PRINCIPAL
  // =====================================================================
  function mostrarTela(nome) {
    const alvo = document.getElementById("tela-" + nome);

    if (!alvo) {
      console.warn("Tela ainda não criada: tela-" + nome);
      return;
    }
    if (!telaPermitida(nome)) return;

    document.querySelectorAll(".tela").forEach(t => (t.hidden = true));
    alvo.hidden = false;

    document.querySelectorAll("[data-go]").forEach(l => {
      l.classList.toggle("active", l.getAttribute("data-go") === nome);
    });

    // Atualiza a tela toda vez que ela é aberta
    if (nome === "inicio") mostrarInicio();
    if (nome === "consultar") atualizarConsultaMateriais();
    if (nome === "registrar") mostrarRegistrar();
    if (nome === "historico") renderizarHistorico();
    if (nome === "gerenciar") carregarGerenciamento();
    if (nome === "usuarios") mostrarUsuarios();
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

  // Atualiza todas as telas de uma vez (depois de qualquer mudança)
  function atualizarTudo() {
    gravarTudo();
    mostrarInicio();
    mostrarConsultar();
    mostrarRegistrar();
    renderizarHistorico();
    if (tabelaRelatorio && tabelaRelatorio.innerHTML.trim() !== "") gerarRelatorio();
    atualizarGerenciar();
  }

  // =====================================================================
  // LOGIN
  // =====================================================================
  async function fazerLogin() {
    const login = campoLogin.value.trim();
    const senha = campoSenha.value;

    mensagemErro.textContent = "";

    if (login === "" || senha === "") {
      mensagemErro.textContent = "Preencha todos os campos.";
      return;
    }

    const credenciais = new TextEncoder().encode(login + ":" + senha);
    const autorizacao = "Basic " + btoa(
      String.fromCharCode(...credenciais)
    );

    let conta;

    try {
      conta = await consultarConta(autorizacao);
    } catch (erro) {
    mensagemErro.textContent =
     erro.message === "HTTP 401"
       ? "Matrícula ou senha incorretas."
        : "Não foi possível autenticar. Verifique a conexão com o servidor.";
    return;
    }

    const perfil = converterPerfil(conta.perfil);

    if (!perfil) {
      mensagemErro.textContent = "Perfil de usuário não reconhecido.";
      return;
    }

    const u = {
       ...conta,
        login: conta.matricula,
        perfil: perfil
    };

    autorizacaoAtual = autorizacao;
    logarUsuario(u);
  }

  function logarUsuario(u) {
    usuarioLogado = u;
    const isAdminSistema = u.perfil === "sistema";

    telaLogin.hidden = true;
    telaSistema.hidden = false;

    if (nomeUsuario) nomeUsuario.textContent = u.nome;
    if (perfilUsuario) perfilUsuario.textContent = PERFIS[u.perfil];
    if (inicioNome) inicioNome.textContent = u.nome;

    // Aviso e painel do Início
    if (inicioAviso) inicioAviso.hidden = !isAdminSistema;
    const painel = document.getElementById("painel");
    if (painel) painel.hidden = isAdminSistema;

    // Cada perfil vê só os menus que pode usar
    document.querySelectorAll("[data-go]").forEach(l => {
      l.parentElement.hidden = !TELAS_PERMITIDAS[u.perfil].includes(l.getAttribute("data-go"));
    });

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
      usuarioLogado = null;
      autorizacaoAtual = null;

      // Limpa os dados recebidos da API
      materiaisApi = [];
      consultaMateriaisVersao++;
      mostrarConsultar();
      mostrarMateriais();
      limparFormularioMaterial();
      itensApi = [];
      itensDisponiveisApi = [];
      consultaItensVersao++;

      mostrarItens();

      preencherSelectMateriais();
      itemMaterial.value = "";
      itemPatrimonio.value = "";
      itemErro.textContent = "";

      equipesApi = [];
      consultaEquipesVersao++;
      mostrarEquipes();
      eqNome.value = "";
      eqErro.textContent = "";

      recebedoresApi = [];
      consultaRecebedoresVersao++;
      mostrarRecebedores();
      recNome.value = "";
      recMatricula.value = "";
      recPin.value = "";
      recErro.textContent = "";

      const campoNovoPin = document.getElementById("novo-pin");

      if (campoNovoPin) {
        campoNovoPin.value = "";
      }

      fecharModal();

      rascunho = [];
      telaSistema.hidden = true;
      telaLogin.hidden = false;
      campoLogin.value = "";
      campoSenha.value = "";
      mensagemErro.textContent = "";
    });
  }

  // =====================================================================
  // ABAS (Registrar Movimentação e Gerenciar Materiais)
  // =====================================================================
  const registrarMsg = document.getElementById("registrar-msg");

  document.querySelectorAll(".nav-tabs .nav-link").forEach(botao => {
    botao.addEventListener("click", function () {
      const listaAbas = this.parentElement.parentElement;
      const tela = listaAbas.parentElement;

      listaAbas.querySelectorAll(".nav-link").forEach(b => b.classList.remove("active"));
      this.classList.add("active");

      tela.querySelectorAll(".aba").forEach(aba => (aba.hidden = true));

      const abaAlvo = document.getElementById(this.getAttribute("data-aba"));
      if (abaAlvo) abaAlvo.hidden = false;

      if (registrarMsg) registrarMsg.innerHTML = "";
    });
  });

  // =====================================================================
  // PAINEL DO INÍCIO
  // =====================================================================
  const painelVisao = document.getElementById("painel-visao");
  const painelEquipe = document.getElementById("painel-equipe");
  const painelRecebedor = document.getElementById("painel-recebedor");
  const tabelaInicio = document.getElementById("tabela-inicio");
  const inicioResumo = document.getElementById("inicio-resumo");

  function cartao(titulo, valor) {
    return '<div class="col-6 col-md-3"><div class="card"><div class="card-body">' +
      '<div class="text-body-secondary small">' + titulo + "</div>" +
      '<div class="fs-3 fw-bold">' + valor + "</div></div></div></div>";
  }

  function mostrarInicio() {
    if (!tabelaInicio) return;

    // Cartões de resumo
    const pendentes = empenhos.filter(e => e.status === "Pendente").length;
    inicioResumo.innerHTML =
      cartao("Itens em empenho aberto", listarItens("Em uso").length) +
      cartao("Empenhos pendentes", pendentes) +
      cartao("Itens disponíveis", itensDisponiveis().length) +
      cartao("Itens extraviados", listarItens("Extraviado").length);

    // Filtros
    preencherSelect(painelEquipe, "Todas as equipes",
      equipes.map(q => ({ valor: q.nome, texto: q.nome })));
    preencherSelect(painelRecebedor, "Todos os recebedores",
      recebedores.map(r => ({ valor: r.matricula, texto: r.nome })));

    const situacao = painelVisao.value === "extraviados" ? "Extraviado" : "Em uso";
    const equipe = painelEquipe.value;
    const recebedor = painelRecebedor.value;

    const linhas = listarItens(situacao).filter(l =>
      (equipe === "" || l.equipe === equipe) &&
      (recebedor === "" || l.recebMat === recebedor)
    );

    montarTabela(tabelaInicio, [
      { titulo: "Data",       valor: l => formatarData(l.data) },
      { titulo: "Empenho",    valor: l => "Nº " + l.empenho },
      { titulo: "Recebedor",  valor: l => l.recebedor },
      { titulo: "Equipe",     valor: l => l.equipe },
      { titulo: "Material",   valor: l => l.material },
      { titulo: "Patrimônio", valor: l => l.patrimonio }
    ], linhas);
  }

  [painelVisao, painelEquipe, painelRecebedor].forEach(sel => {
    if (sel) sel.addEventListener("change", mostrarInicio);
  });

  // =====================================================================
  // CONSULTAR MATERIAIS
  // =====================================================================
  const buscaTexto = document.getElementById("busca-texto");
  const buscaSecao = document.getElementById("busca-secao");
  const buscaStatus = document.getElementById("busca-status");
  const tabelaMateriais = document.getElementById("tabela-materiais");

    function mostrarConsultar() {
    if (!tabelaMateriais) return;

    const secoes = [...new Set(
      materiaisApi.map(m => m.secao || "Não informada")
    )].sort();

    preencherSelect(
      buscaSecao,
      "Todas as seções",
      secoes.map(s => ({ valor: s, texto: s }))
    );

    const texto = buscaTexto.value.trim().toLowerCase();
    const secao = buscaSecao.value;
    const status = buscaStatus.value;

    const linhas = materiaisApi.map(m => ({
      codigo: m.codigo,
      nome: m.nome,
      secao: m.secao || "Não informada",
      ativo: m.ativo,
      total: m.quantidadeTotal,
      disp: m.quantidadeDisponivel,
      emUso: m.quantidadeEmpenhada ?? "—"
    })).filter(l =>
      (texto === "" ||
        l.nome.toLowerCase().includes(texto) ||
        l.codigo.toLowerCase().includes(texto)) &&
      (secao === "" || l.secao === secao) &&
      (status === "" ||
        (status === "disp" ? l.disp > 0 : l.disp === 0))
    );

    montarTabela(tabelaMateriais, [
      { titulo: "Código",      valor: l => l.codigo },
      { titulo: "Material",    valor: l => l.nome },
      { titulo: "Seção",       valor: l => l.secao },
      { titulo: "Total",       valor: l => l.total },
      { titulo: "Disponíveis", valor: l => l.disp },
      { titulo: "Em empenho",  valor: l => l.emUso },
      { titulo: "Situação",    valor: l => l.ativo ? "Ativo" : "Inativo" }
    ], linhas);
  }

    async function atualizarConsultaMateriais() {
    if (!autorizacaoAtual) return;

    const usuario = usuarioLogado;
    const versao = ++consultaMateriaisVersao;

    materiaisApi = [];
    mostrarConsultar();
    mostrarMateriais();
    preencherSelectMateriais();
    mostrarItens();

    try {
      const dados = await carregarMateriais();

      if (usuario !== usuarioLogado ||
          versao !== consultaMateriaisVersao) return;

      materiaisApi = dados;

      mostrarConsultar();
      mostrarMateriais();
      preencherSelectMateriais();
      mostrarItens();

    } catch (erro) {
      if (usuario !== usuarioLogado ||
          versao !== consultaMateriaisVersao) return;

      materiaisApi = [];

      mostrarConsultar();
      mostrarMateriais();
      preencherSelectMateriais();
      mostrarItens();

      console.error("Erro ao consultar materiais:", erro);

      mostrarAviso(
        "Erro na consulta",
        "Não foi possível carregar os materiais do servidor."
      );
    }
  }

  if (buscaTexto) buscaTexto.addEventListener("input", mostrarConsultar);
  if (buscaSecao) buscaSecao.addEventListener("change", mostrarConsultar);
  if (buscaStatus) buscaStatus.addEventListener("change", mostrarConsultar);

  // =====================================================================
  // REGISTRAR MOVIMENTAÇÃO
  // =====================================================================
  const empRecebedor = document.getElementById("emp-recebedor");
  const empEquipe = document.getElementById("emp-equipe");
  const empItem = document.getElementById("emp-item");
  const empLista = document.getElementById("emp-lista");
  const empErro = document.getElementById("emp-erro");
  const tabelaPendentes = document.getElementById("tabela-pendentes");
  const devEmpenho = document.getElementById("dev-empenho");
  const devItens = document.getElementById("dev-itens");
  const devErro = document.getElementById("dev-erro");

  let rascunho = []; // patrimônios já escolhidos para o novo empenho

  function mostrarMensagem(texto, tipo) {
    if (!registrarMsg) return;
    registrarMsg.innerHTML = '<div class="alert alert-' + tipo + ' py-2 mb-0">' + escapar(texto) + "</div>";
  }

  function mostrarRegistrar() {
    if (!empRecebedor) return;

    preencherSelect(empRecebedor, "Selecione o recebedor",
      recebedores.map(r => ({ valor: r.matricula, texto: r.nome + " (" + r.matricula + ")" })));
    preencherSelect(empEquipe, "Selecione a equipe",
      equipes.map(q => ({ valor: q.nome, texto: q.nome })));

    // Itens disponíveis que ainda não estão no rascunho
    const livres = itensDisponiveis().filter(i => !rascunho.includes(i.patrimonio));
    preencherSelect(empItem, livres.length ? "Selecione o item" : "Nenhum item disponível",
      livres.map(i => ({ valor: i.patrimonio, texto: nomeDoMaterial(i.codigo) + " – " + i.patrimonio })));

    mostrarRascunho();
    mostrarPendentes();
    mostrarSelectDevolucao();
  }

  function mostrarRascunho() {
    let html = "";
    rascunho.forEach(p => {
      const it = acharItem(p);
      html += '<li class="list-group-item d-flex justify-content-between align-items-center">' +
        "<span>" + escapar(nomeDoMaterial(it ? it.codigo : "") + " – " + p) + "</span>" +
        botaoAcao("Remover", "btn-outline-danger", "remover-rascunho", p) + "</li>";
    });
    empLista.innerHTML = html;
  }

  document.querySelector("[data-action='add-item-empenho']").addEventListener("click", function () {
    empErro.textContent = "";
    if (empItem.value === "") {
      empErro.textContent = "Escolha um item disponível.";
      return;
    }
    rascunho.push(empItem.value);
    mostrarRegistrar();
  });

  empLista.addEventListener("click", function (e) {
    if (e.target.getAttribute("data-acao") !== "remover-rascunho") return;
    rascunho = rascunho.filter(p => p !== e.target.getAttribute("data-id"));
    mostrarRegistrar();
  });

  function criarEmpenho() {
    empErro.textContent = "";

    const matricula = empRecebedor.value;
    const equipe = empEquipe.value;

    if (matricula === "" || equipe === "") {
      empErro.textContent = "Escolha o recebedor e a equipe.";
      return;
    }
    if (rascunho.length === 0) {
      empErro.textContent = "Adicione pelo menos um item.";
      return;
    }
    const livres = itensDisponiveis().map(i => i.patrimonio);
    if (rascunho.some(p => !livres.includes(p))) {
      empErro.textContent = "Algum item escolhido não está mais disponível. Revise a lista.";
      return;
    }

    const rec = recebedores.find(r => r.matricula === matricula);
    const novo = {
      id: proximoId(),
      data: hojeISO(),
      dataConf: "",
      recebedor: matricula,
      recebedorNome: rec ? rec.nome : matricula,
      equipe: equipe,
      status: "Pendente",
      criadoPor: usuarioLogado ? usuarioLogado.nome : "",
      itens: rascunho.map(p => {
        const it = acharItem(p);
        return { patrimonio: p, codigo: it.codigo, material: nomeDoMaterial(it.codigo), situacao: "Em uso", dataDev: "" };
      })
    };
    empenhos.push(novo);
    rascunho = [];
    atualizarTudo();
    mostrarMensagem("Empenho nº " + novo.id + " criado. Vá em \"Empenhos pendentes\" para o recebedor confirmar.", "success");
  }

  // --- Empenhos pendentes ---
  function mostrarPendentes() {
    if (!tabelaPendentes) return;

    const linhas = empenhos.filter(e => e.status === "Pendente").map(e => [
      escapar("Nº " + e.id),
      escapar(formatarData(e.data)),
      escapar(e.recebedorNome),
      escapar(e.equipe),
      escapar(textoItens(e)),
      botaoAcao("Confirmar", "btn-success", "confirmar-empenho", e.id) +
      botaoAcao("Cancelar", "btn-outline-danger", "cancelar-empenho", e.id)
    ]);
    montarTabelaGerenciar(tabelaPendentes, ["Empenho", "Data", "Recebedor", "Equipe", "Itens", "Ações"], linhas);
  }

  function pedirConfirmacao(emp) {
    const corpo =
      '<p class="mb-1">Recebedor: <strong>' + escapar(emp.recebedorNome) + "</strong></p>" +
      '<p class="mb-3">Itens: ' + escapar(textoItens(emp)) + "</p>" +
      '<div class="mb-2"><label class="form-label" for="conf-matricula">Matrícula</label>' +
      '<input id="conf-matricula" class="form-control" autocomplete="off"></div>' +
      '<div class="mb-2"><label class="form-label" for="conf-senha">Senha</label>' +
      '<input id="conf-senha" type="password" class="form-control" autocomplete="new-password"></div>' +
      '<p id="conf-erro" class="text-danger small mb-0"></p>';

    abrirModal("Confirmar empenho nº " + emp.id, corpo, [
      { texto: "Cancelar", classe: "btn-outline-secondary" },
      {
        texto: "Confirmar recebimento",
        classe: "btn-success",
        manter: true,
        aoClicar: function () {
          const matricula = document.getElementById("conf-matricula").value.trim();
          const senha = document.getElementById("conf-senha").value.trim();
          const erro = document.getElementById("conf-erro");
          const rec = recebedores.find(r => r.matricula === emp.recebedor);

          if (!rec || matricula !== rec.matricula || senha !== rec.senha) {
            erro.textContent = "Matrícula ou senha incorretas.";
            return;
          }
          emp.status = "Confirmado";
          emp.dataConf = hojeISO();
          fecharModal();
          atualizarTudo();
          mostrarMensagem("Empenho nº " + emp.id + " confirmado por " + emp.recebedorNome + ".", "success");
        }
      }
    ]);
  }

  tabelaPendentes.addEventListener("click", function (e) {
    const acao = e.target.getAttribute("data-acao");
    const emp = acharEmpenho(e.target.getAttribute("data-id"));
    if (!acao || !emp) return;

    if (acao === "confirmar-empenho") pedirConfirmacao(emp);

    if (acao === "cancelar-empenho") {
      confirmarAcao("Cancelar empenho",
        "Cancelar o empenho nº " + emp.id + "? Os itens voltam a ficar disponíveis.",
        function () {
          emp.status = "Cancelado";
          emp.itens.forEach(i => (i.situacao = "Cancelado"));
          atualizarTudo();
          mostrarMensagem("Empenho nº " + emp.id + " cancelado.", "warning");
        });
    }
  });

  // --- Devolução ---
  function empenhosComPendencia() {
    return empenhos.filter(e => e.status === "Confirmado" && e.itens.some(i => i.situacao === "Em uso"));
  }

  function mostrarSelectDevolucao() {
    preencherSelect(devEmpenho, "Selecione o empenho",
      empenhosComPendencia().map(e => ({
        valor: String(e.id),
        texto: "Nº " + e.id + " – " + e.recebedorNome + " – " + formatarData(e.dataConf || e.data) +
          " – " + e.itens.filter(i => i.situacao === "Em uso").length + " item(ns) pendente(s)"
      })));
    mostrarItensDevolucao();
  }

  function mostrarItensDevolucao() {
    const emp = acharEmpenho(devEmpenho.value);
    if (!emp) {
      devItens.innerHTML = "";
      return;
    }
    let html = '<p class="text-body-secondary small mb-2">Escolha a condição de cada item que voltou. Itens deixados como "Ainda não devolvido" continuam em aberto.</p>';
    emp.itens.filter(i => i.situacao === "Em uso").forEach(i => {
      html += '<div class="row g-2 align-items-center mb-2">' +
        '<div class="col-md-6">' + escapar(i.material + " – " + i.patrimonio) + "</div>" +
        '<div class="col-md-4"><select class="form-select form-select-sm dev-cond" data-pat="' + escaparAttr(i.patrimonio) + '">' +
        '<option value="">Ainda não devolvido</option>' +
        '<option value="Devolvido">Devolvido em bom estado</option>' +
        '<option value="Avariado">Avariado</option>' +
        '<option value="Extraviado">Extraviado</option>' +
        "</select></div></div>";
    });
    devItens.innerHTML = html;
  }

  if (devEmpenho) devEmpenho.addEventListener("change", mostrarItensDevolucao);

  function registrarDevolucao() {
    devErro.textContent = "";

    const emp = acharEmpenho(devEmpenho.value);
    if (!emp) {
      devErro.textContent = "Escolha um empenho.";
      return;
    }
    const escolhas = [];
    devItens.querySelectorAll(".dev-cond").forEach(sel => {
      if (sel.value !== "") escolhas.push({ pat: sel.getAttribute("data-pat"), situacao: sel.value });
    });
    if (escolhas.length === 0) {
      devErro.textContent = "Escolha a condição de pelo menos um item.";
      return;
    }
    escolhas.forEach(c => resolverItem(emp, c.pat, c.situacao));
    atualizarTudo();
    mostrarMensagem(
      emp.status === "Encerrado"
        ? "Devolução registrada. O empenho nº " + emp.id + " foi encerrado."
        : "Devolução registrada. Ainda há itens em aberto no empenho nº " + emp.id + ".",
      "success");
  }

  document.querySelector("[data-action='criar-empenho']").addEventListener("click", criarEmpenho);
  document.querySelector("[data-action='registrar-devolucao']").addEventListener("click", registrarDevolucao);

  // =====================================================================
  // HISTÓRICO
  // =====================================================================
  const histTipo = document.getElementById("hist-tipo");
  const histRecebedor = document.getElementById("hist-recebedor");
  const tabelaHistorico = document.getElementById("tabela-historico");

  function eventosHistorico() {
    const ev = [];
    empenhos.forEach(e => {
      ev.push({
        data: e.data, tipo: "Empenho", itens: textoItens(e), equipe: e.equipe,
        recebedor: e.recebedorNome, recebMat: e.recebedor, status: e.status
      });
      e.itens.forEach(i => {
        if (i.dataDev && ["Devolvido", "Avariado", "Extraviado"].includes(i.situacao)) {
          ev.push({
            data: i.dataDev, tipo: "Devolução", itens: i.material + " (" + i.patrimonio + ")",
            equipe: e.equipe, recebedor: e.recebedorNome, recebMat: e.recebedor,
            status: i.situacao === "Devolvido" ? "Bom estado" : i.situacao
          });
        }
      });
    });
    // mais recentes primeiro (o sort é estável, então a ordem do dia é mantida)
    return ev.reverse().sort((a, b) => b.data.localeCompare(a.data));
  }

  function renderizarHistorico() {
    if (!tabelaHistorico) return;

    const todos = eventosHistorico();

    const mapa = new Map();
    todos.forEach(ev => mapa.set(ev.recebMat, ev.recebedor));
    preencherSelect(histRecebedor, "Todos os recebedores",
      [...mapa.entries()].map(([valor, texto]) => ({ valor: valor, texto: texto })));

    const tipo = histTipo.value;
    const recebedor = histRecebedor.value;

    const filtradas = todos.filter(ev =>
      (tipo === "" || ev.tipo === tipo) &&
      (recebedor === "" || ev.recebMat === recebedor)
    );

    montarTabela(tabelaHistorico, [
      { titulo: "Data",      valor: ev => formatarData(ev.data) },
      { titulo: "Tipo",      valor: ev => ev.tipo },
      { titulo: "Itens",     valor: ev => ev.itens },
      { titulo: "Equipe",    valor: ev => ev.equipe },
      { titulo: "Recebedor", valor: ev => ev.recebedor },
      { titulo: "Situação",  valor: ev => ev.status }
    ], filtradas);
  }

  if (histTipo) histTipo.addEventListener("change", renderizarHistorico);
  if (histRecebedor) histRecebedor.addEventListener("change", renderizarHistorico);

  // Detalhes: ao clicar numa linha do histórico, mostra os dados dela
  if (tabelaHistorico) {
    tabelaHistorico.addEventListener("click", function (e) {
      const linha = e.target.closest("tbody tr");
      if (!linha) return;

      const celulas = linha.querySelectorAll("td");
      if (celulas.length < 6) return;

      const rotulos = ["Data", "Tipo", "Itens", "Equipe", "Recebedor", "Situação"];
      let html = '<dl class="row mb-0">';
      for (let i = 0; i < rotulos.length; i++) {
        html += '<dt class="col-4">' + rotulos[i] + '</dt><dd class="col-8">' +
          escapar(celulas[i].textContent) + "</dd>";
      }
      html += "</dl>";

      abrirModal("Detalhes da movimentação", html, [
        { texto: "Fechar", classe: "btn-secondary" }
      ]);
    });
  }

  // =====================================================================
  // RELATÓRIOS
  // =====================================================================
  const relTipo = document.getElementById("rel-tipo");
  const tabelaRelatorio = document.getElementById("tabela-relatorio");
  const botaoGerar = document.querySelector("[data-action='gerar-relatorio']");
  const botaoImprimir = document.querySelector("[data-action='imprimir']");

  const colunasItens = [
    { titulo: "Data",       valor: l => formatarData(l.data) },
    { titulo: "Empenho",    valor: l => "Nº " + l.empenho },
    { titulo: "Recebedor",  valor: l => l.recebedor },
    { titulo: "Equipe",     valor: l => l.equipe },
    { titulo: "Material",   valor: l => l.material },
    { titulo: "Patrimônio", valor: l => l.patrimonio }
  ];

  function contarOcorrencias(codigo, situacao) {
    let n = 0;
    empenhos.forEach(e => e.itens.forEach(i => {
      if (i.codigo === codigo && i.situacao === situacao) n++;
    }));
    return n;
  }

  function gerarRelatorio() {
    if (!tabelaRelatorio || !relTipo) return;

    switch (relTipo.value) {
      case "abertos":
        montarTabela(tabelaRelatorio, colunasItens, listarItens("Em uso"));
        break;

      case "extraviados":
        montarTabela(tabelaRelatorio, colunasItens, listarItens("Extraviado"));
        break;

      case "avariados":
        montarTabela(tabelaRelatorio, colunasItens, listarItens("Avariado"));
        break;

      case "estoque": {
        const disponiveis = itensDisponiveis();
        const linhas = materiais.map(m => {
          const meus = itens.filter(i => i.codigo === m.codigo);
          const disp = disponiveis.filter(i => i.codigo === m.codigo).length;
          const emUso = meus.filter(i => itemEmUso(i.patrimonio)).length;
          return {
            material: m.nome,
            total: meus.length,
            disp: disp,
            emUso: emUso,
            inativos: meus.length - disp - emUso,
            avariados: contarOcorrencias(m.codigo, "Avariado"),
            extraviados: contarOcorrencias(m.codigo, "Extraviado")
          };
        });
        montarTabela(tabelaRelatorio, [
          { titulo: "Material",     valor: x => x.material },
          { titulo: "Total",        valor: x => x.total },
          { titulo: "Disponíveis",  valor: x => x.disp },
          { titulo: "Em empenho",   valor: x => x.emUso },
          { titulo: "Inativos",     valor: x => x.inativos },
          { titulo: "Avariados",    valor: x => x.avariados },
          { titulo: "Extraviados",  valor: x => x.extraviados }
        ], linhas);
        break;
      }
    }
  }
  if (botaoGerar) botaoGerar.addEventListener("click", gerarRelatorio);
  if (botaoImprimir) botaoImprimir.addEventListener("click", function () { window.print(); });

  // =====================================================================
  // GERENCIAR MATERIAIS
  // =====================================================================

  // ---------- ABA MATERIAIS ----------
  const matCodigo = document.getElementById("mat-codigo");
  const matNome = document.getElementById("mat-nome");
  const matSecao = document.getElementById("mat-secao");
  const matErro = document.getElementById("mat-erro");
  const tabelaGerMateriais = document.getElementById("tabela-gerenciar-materiais");

  let idEmEdicao = null;
  let salvandoMaterial = false;

    function mostrarMateriais() {
    if (!tabelaGerMateriais) return;

    const linhas = materiaisApi.map(m => [
      escapar(m.codigo),
      escapar(m.nome),
      escapar(m.secao || "Não informada"),
      m.ativo ? "Ativo" : "Inativo",

      m.ativo
        ? botaoAcao(
            "Editar",
            "btn-outline-primary",
            "editar-material",
            m.id
          ) +
          botaoAcao(
            "Inativar",
            "btn-outline-secondary",
            "ativar-material",
            m.id
          )
        : "—"
    ]);

    montarTabelaGerenciar(
      tabelaGerMateriais,
      ["Código", "Material", "Seção", "Situação", "Ações"],
      linhas
    );
  }

    async function salvarMaterial() {
    if (!usuarioLogado ||
        usuarioLogado.perfil !== "estoque" ||
        salvandoMaterial) return;

    matErro.textContent = "";

    const codigo = matCodigo.value.trim();
    const nome = matNome.value.trim();
    const secao = matSecao.value.trim();

    if (!codigo || !nome || !secao) {
      matErro.textContent = "Preencha código, nome e seção.";
      return;
    }

    if (codigo.length > 40 ||
        nome.length > 120 ||
        secao.length > 80) {
      matErro.textContent = "Limite de caracteres excedido.";
      return;
    }

    const usuario = usuarioLogado;
    const dados = { codigo, nome, secao };
    const editando = idEmEdicao !== null;

    salvandoMaterial = true;

    try {
      if (editando) {
        await requisitarApi(
          "/api/v1/materiais/" + idEmEdicao,
          "PUT",
          dados
        );
      } else {
        await requisitarApi(
          "/api/v1/materiais",
          "POST",
          dados
        );
      }

      if (usuario !== usuarioLogado) return;

      limparFormularioMaterial();

      await atualizarConsultaMateriais();

    } catch (erro) {
      if (usuario !== usuarioLogado) return;

      if (erro.message === "HTTP 409") {
        matErro.textContent =
          "Já existe um material com esse código.";
      } else {
        matErro.textContent =
          "Não foi possível salvar o material.";
      }

      console.error("Erro ao salvar material:", erro);

    } finally {
      salvandoMaterial = false;
    }
  }

    function limparFormularioMaterial() {
    idEmEdicao = null;
    matCodigo.value = "";
    matCodigo.readOnly = false;
    matNome.value = "";
    matSecao.value = "";
    matErro.textContent = "";
  }

    if (tabelaGerMateriais) {
    tabelaGerMateriais.addEventListener(
      "click",
      async function (e) {

        if (!usuarioLogado ||
            usuarioLogado.perfil !== "estoque" ||
            salvandoMaterial) return;

        const acao = e.target.getAttribute("data-acao");
        const id = e.target.getAttribute("data-id");

        if (!acao || !id) return;

        const m = materiaisApi.find(
          material => String(material.id) === id
        );

        if (!m || !m.ativo) return;

        if (acao === "editar-material") {
          idEmEdicao = m.id;

          matCodigo.value = m.codigo;
          matCodigo.readOnly = true;
          matNome.value = m.nome;
          matSecao.value = m.secao || "";
          matErro.textContent = "";
          matNome.focus();
        }

        if (acao === "ativar-material") {
          const confirmou = window.confirm(
            "Deseja realmente inativar este material?"
          );

          if (!confirmou) return;

          const usuario = usuarioLogado;
          salvandoMaterial = true;

          try {
            await requisitarApi(
              "/api/v1/materiais/" + m.id + "/inativacao",
              "PATCH"
            );

            if (usuario !== usuarioLogado) return;

            limparFormularioMaterial();
            await atualizarConsultaMateriais();
            if (usuario !== usuarioLogado) return;
            await atualizarConsultaItens();

          } catch (erro) {
            if (usuario !== usuarioLogado) return;

            console.error("Erro ao inativar:", erro);

            mostrarAviso(
              "Erro na inativação",
              erro.message === "HTTP 409"
                ? "Verifique se existem empenhos pendentes."
                : "Não foi possível inativar o material."
            );

          } finally {
            salvandoMaterial = false;
          }
        }
      }
    );
  }
  document.querySelector("[data-action='salvar-material']").addEventListener("click", salvarMaterial);

  // ---------- ABA ITENS ----------
  const itemMaterial = document.getElementById("item-material");
  const itemPatrimonio = document.getElementById("item-patrimonio");
  const itemErro = document.getElementById("item-erro");
  const tabelaGerItens = document.getElementById("tabela-gerenciar-itens");

  // Select só com materiais ativos
    function preencherSelectMateriais() {
    if (!itemMaterial) return;

    const ativos = materiaisApi.filter(m => m.ativo);

    preencherSelect(
      itemMaterial,
      ativos.length
        ? "Selecione o material"
        : "Nenhum material ativo disponível",
      ativos.map(m => ({
        valor: String(m.id),
        texto: m.codigo + " – " + m.nome
      }))
    );
  }

    function mostrarItens() {
    if (!tabelaGerItens) return;

    const statusItens = {
      DISPONIVEL: "Disponível",
      EMPENHADO: "Em empenho",
      AVARIADO: "Avariado",
      EXTRAVIADO: "Extraviado",
      INATIVO: "Inativo"
    };

    const linhas = itensApi.map(it => {

      const livre = itensDisponiveisApi.some(
        i => i.id === it.id
      );

      const material = materiaisApi.find(
        m => m.codigo === it.codigoMaterial
      );

      let situacao = statusItens[it.status] || it.status;
      let acao = "—";

      if (it.status === "DISPONIVEL" && !livre) {
        situacao = "Indisponível";
      }

      if (it.status === "DISPONIVEL" && livre) {
        acao = botaoAcao(
          "Inativar",
          "btn-outline-secondary",
          "inativar-item",
          it.id
        );
      }

      if (it.status === "AVARIADO") {
        acao = botaoAcao(
          "Inativar",
          "btn-outline-secondary",
          "inativar-item",
          it.id
        );

        if (material && material.ativo) {
          acao += botaoAcao(
            "Liberar",
            "btn-outline-success",
            "liberar-item",
            it.id
          );
        }
      }

      return [
        escapar(it.numeroPatrimonio),
        escapar(it.nomeMaterial),
        escapar(situacao),
        acao
      ];
    });

    montarTabelaGerenciar(
      tabelaGerItens,
      ["Patrimônio", "Material", "Situação", "Ações"],
      linhas
    );
  }

    async function atualizarConsultaItens() {
    if (!usuarioLogado ||
        usuarioLogado.perfil !== "estoque") return;

    const usuario = usuarioLogado;
    const versao = ++consultaItensVersao;

    itensApi = [];
    itensDisponiveisApi = [];
    mostrarItens();

    try {
      const dados = await carregarItens();

      if (usuario !== usuarioLogado ||
          versao !== consultaItensVersao) return;

      itensApi = dados.todos;
      itensDisponiveisApi = dados.disponiveis;

      mostrarItens();

    } catch (erro) {
      if (usuario !== usuarioLogado ||
          versao !== consultaItensVersao) return;

      console.error("Erro ao consultar itens:", erro);

      mostrarAviso(
        "Erro na consulta",
        "Não foi possível carregar os itens."
      );
    }
  }

  async function salvarItem() {
    if (!usuarioLogado ||
        usuarioLogado.perfil !== "estoque" ||
        salvandoItem) return;

    itemErro.textContent = "";

    const materialId = itemMaterial.value;
    const patrimonio = itemPatrimonio.value.trim();

    const material = materiaisApi.find(
      m => String(m.id) === materialId && m.ativo
    );

    if (!material) {
      itemErro.textContent = "Escolha um material ativo.";
      return;
    }

    if (!patrimonio) {
      itemErro.textContent = "Informe o número de patrimônio.";
      return;
    }

    if (patrimonio.length > 80) {
      itemErro.textContent =
        "O patrimônio deve ter até 80 caracteres.";
      return;
    }

    const usuario = usuarioLogado;
    salvandoItem = true;

    try {
      await requisitarApi(
        "/api/v1/materiais/" + materialId + "/itens",
        "POST",
        { numeroPatrimonio: patrimonio }
      );

      if (usuario !== usuarioLogado) return;

      itemPatrimonio.value = "";

      await atualizarConsultaMateriais();

      if (usuario !== usuarioLogado) return;

      await atualizarConsultaItens();

    } catch (erro) {
      if (usuario !== usuarioLogado) return;

      itemErro.textContent =
        erro.message === "HTTP 409"
          ? "Esse patrimônio já está cadastrado."
          : "Não foi possível cadastrar o item.";

      console.error("Erro ao cadastrar item:", erro);

        } finally {
      salvandoItem = false;
    }
  }

  if (tabelaGerItens) {
    tabelaGerItens.addEventListener(
      "click",
      async function (e) {

        if (!usuarioLogado ||
            usuarioLogado.perfil !== "estoque" ||
            salvandoItem) return;

        const acao = e.target.getAttribute("data-acao");
        const id = e.target.getAttribute("data-id");

        if (!acao || !id) return;

        const item = itensApi.find(
          it => String(it.id) === id
        );

        if (!item) return;

        let operacao;
        let mensagem;

        if (acao === "inativar-item") {
          operacao = "inativacao";
          mensagem = "Deseja inativar este item?";

        } else if (acao === "liberar-item") {
          operacao = "liberacao";
          mensagem = "Deseja liberar este item avariado?";

        } else {
          return;
        }

        if (!window.confirm(mensagem)) return;

        const usuario = usuarioLogado;
        salvandoItem = true;

        try {
          await requisitarApi(
            "/api/v1/itens/" + item.id + "/" + operacao,
            "PATCH"
          );

          if (usuario !== usuarioLogado) return;

          await atualizarConsultaMateriais();

          if (usuario !== usuarioLogado) return;

          await atualizarConsultaItens();

        } catch (erro) {
          if (usuario !== usuarioLogado) return;

          console.error("Erro ao alterar item:", erro);

          mostrarAviso(
            "Erro na operação",
            erro.message === "HTTP 409"
              ? "A situação atual do item não permite essa operação."
              : "Não foi possível alterar o item."
          );

        } finally {
          salvandoItem = false;
        }
      }
    );
  }

  document.querySelector("[data-action='salvar-item']").addEventListener("click", salvarItem);

  // ---------- ABA RECEBEDORES ----------
  const recNome = document.getElementById("rec-nome");
  const recMatricula = document.getElementById("rec-matricula");
  const recPin = document.getElementById("rec-pin");
  const recErro = document.getElementById("rec-erro");
  const tabelaRecebedores = document.getElementById("tabela-recebedores");

  function mostrarRecebedores() {
  if (!tabelaRecebedores) return;

  const podeGerenciar =
    usuarioLogado?.perfil === "estoque";

  const linhas = recebedoresApi
    .filter(r => r.ativo)
    .map(r => [
      escapar(r.nome),
      escapar(r.matricula),

      podeGerenciar ? botaoAcao("Redefinir PIN", "btn-outline-primary", "redefinir-pin", r.id) +
          botaoAcao("Inativar", "btn-outline-danger", "inativar-recebedor", r.id)
        : "—"
    ]);

  montarTabelaGerenciar(
    tabelaRecebedores,
    ["Nome", "Matrícula", "Ações"],
    linhas
  );
}

async function atualizarConsultaRecebedores() {
  if (!usuarioLogado ||
      !["estoque", "operador"].includes(usuarioLogado.perfil)) return;

  const usuario = usuarioLogado;
  const versao = ++consultaRecebedoresVersao;

  recebedoresApi = [];
  mostrarRecebedores();

  try {
    const dados = await carregarRecebedores();

    if (usuario !== usuarioLogado ||
        versao !== consultaRecebedoresVersao) return;

    recebedoresApi = dados;
    mostrarRecebedores();

  } catch (erro) {
    if (usuario !== usuarioLogado ||
        versao !== consultaRecebedoresVersao) return;

    console.error("Erro ao consultar recebedores:", erro);

    mostrarAviso(
      "Erro na consulta",
      "Não foi possível carregar os recebedores."
    );
  }
}

  async function salvarRecebedor() {
  if (!usuarioLogado ||
      usuarioLogado.perfil !== "estoque" ||
      salvandoRecebedor) return;

  recErro.textContent = "";

  const nome = recNome.value.trim();
  const matricula = recMatricula.value.trim();
  const pinInicial = recPin.value;

  if (!nome || !matricula) {
    recErro.textContent = "Preencha nome e matrícula.";
    return;
  }

  if (nome.length > 120 || matricula.length > 40) {
    recErro.textContent = "Limite de caracteres excedido.";
    return;
  }

  if (!/^[0-9]{6}$/.test(pinInicial)) {
    recErro.textContent = "O PIN deve conter seis dígitos.";
    return;
  }

  const usuario = usuarioLogado;
  salvandoRecebedor = true;
  recPin.value = "";

  try {await requisitarApi(
      "/api/v1/recebedores",
      "POST",
      { nome, matricula, pinInicial }
    );

    if (usuario !== usuarioLogado) return;
    recNome.value = "";
    recMatricula.value = "";

    await atualizarConsultaRecebedores();

  } catch (erro) {
    if (usuario !== usuarioLogado) return;

    recErro.textContent =
      erro.message === "HTTP 409"
        ? "Essa matrícula já está cadastrada."
        : "Não foi possível cadastrar o recebedor.";

  } finally {
    salvandoRecebedor = false;
  }
}

  function redefinirPinRecebedor(recebedor) {
  if (!usuarioLogado || usuarioLogado.perfil !== "estoque" || salvandoRecebedor) return;

  const usuario = usuarioLogado;

  const corpo = `
    <p>Recebedor: <strong>${escapar(recebedor.nome)}</strong></p>
    <p>Confirme a identidade do recebedor antes de continuar.</p>
    <label for="novo-pin" class="form-label">
      Novo PIN
    </label><input id="novo-pin" type="password" class="form-control" placeholder="Digite seis dígitos" maxlength="6" autocomplete="new-password"><p id="novo-pin-erro"
       class="text-danger small mt-2"></p>
  `;

  abrirModal("Redefinir PIN", corpo, [
    {
      texto: "Cancelar",
      classe: "btn-outline-secondary"
    },
    {
      texto: "Salvar novo PIN",
      classe: "btn-primary",
      manter: true,

      aoClicar: async function () {
        if (usuario !== usuarioLogado || usuarioLogado.perfil !== "estoque" || salvandoRecebedor) return;

        const campo = document.getElementById("novo-pin");
        const erroPin = document.getElementById("novo-pin-erro");
        const novoPin = campo.value;

        erroPin.textContent = "";

        if (!/^[0-9]{6}$/.test(novoPin)) {
          erroPin.textContent = "O PIN deve conter seis dígitos.";
          return;
        }

        salvandoRecebedor = true;
        campo.value = "";

        try {
          await requisitarApi(
            "/api/v1/recebedores/" + recebedor.id + "/pin",
            "PATCH",
            { novoPin }
          );

          if (usuario !== usuarioLogado) return;

          mostrarAviso(
            "PIN redefinido",
            "O PIN foi redefinido com sucesso."
          );

        } catch (erro) {
          if (usuario !== usuarioLogado) return;

          erroPin.textContent =
            "Não foi possível redefinir o PIN.";

        } finally {
          salvandoRecebedor = false;
        }
      }
    }
  ]);

  const campoPin = document.getElementById("novo-pin");

  if (campoPin) {
    modalEl.addEventListener("hidden.bs.modal", function () {
      campoPin.value = "";
    }, { once: true });
  }
}

  if (tabelaRecebedores) {
  tabelaRecebedores.addEventListener("click",
    async function (e) {

      if (!usuarioLogado || usuarioLogado.perfil !== "estoque" || salvandoRecebedor) return;

      const acao = e.target.getAttribute("data-acao");
      const id = e.target.getAttribute("data-id");

      if (!acao || !id) return;

      const recebedor = recebedoresApi.find(
        r => String(r.id) === id
      );

      if (!recebedor || !recebedor.ativo) return;

      if (acao === "redefinir-pin") {
        redefinirPinRecebedor(recebedor);
        return;
      }

      if (acao !== "inativar-recebedor") return;

      const confirmou = window.confirm( "Deseja inativar o recebedor " +  recebedor.nome + "?");

      if (!confirmou) return;

      const usuario = usuarioLogado;
      salvandoRecebedor = true;

      try {
        await requisitarApi(
          "/api/v1/recebedores/" +
          recebedor.id + "/inativacao",
          "PATCH"
        );

        if (usuario !== usuarioLogado) return;

        await atualizarConsultaRecebedores();

      } catch (erro) {
        if (usuario !== usuarioLogado) return;

        mostrarAviso(
          "Erro na inativação",
          "Não foi possível inativar o recebedor.");

      } finally {
        salvandoRecebedor = false;
      }
    }
  );
}
  document.querySelector("[data-action='salvar-recebedor']").addEventListener("click", salvarRecebedor);

  // ---------- ABA EQUIPES ----------
  const eqNome = document.getElementById("eq-nome");
  const eqErro = document.getElementById("eq-erro");
  const tabelaEquipes = document.getElementById("tabela-equipes");

  function mostrarEquipes() {
  if (!tabelaEquipes) return;

  const linhas = equipesApi
    .filter(q => q.ativo)
    .map(q => [
      escapar(q.nome),
      botaoAcao(
        "Inativar",
        "btn-outline-danger",
        "inativar-equipe",
        q.id
      )
    ]);

  montarTabelaGerenciar(
    tabelaEquipes,
    ["Equipe", "Ações"],
    linhas
  );
}

async function atualizarConsultaEquipes() {
  if (!usuarioLogado ||
      !["estoque", "operador"].includes(usuarioLogado.perfil)) return;

  const usuario = usuarioLogado;
  const versao = ++consultaEquipesVersao;

  equipesApi = [];
  mostrarEquipes();

  try {
    const dados = await carregarEquipes();

    if (usuario !== usuarioLogado ||
        versao !== consultaEquipesVersao) return;

    equipesApi = dados;
    mostrarEquipes();

  } catch (erro) {
    if (usuario !== usuarioLogado ||
        versao !== consultaEquipesVersao) return;

    console.error("Erro ao consultar equipes:", erro);

    mostrarAviso(
      "Erro na consulta",
      "Não foi possível carregar as equipes."
    );
  }
}

  async function salvarEquipe() {
  if (!usuarioLogado ||
      usuarioLogado.perfil !== "estoque" ||
      salvandoEquipe) return;

  eqErro.textContent = "";

  const nome = eqNome.value.trim();

  if (!nome) {
    eqErro.textContent = "Informe o nome da equipe.";
    return;
  }

  if (nome.length > 120) {
    eqErro.textContent =
      "O nome deve ter até 120 caracteres.";
    return;
  }

  const usuario = usuarioLogado;
  salvandoEquipe = true;

  try {
    await requisitarApi(
      "/api/v1/equipes",
      "POST",
      { nome: nome }
    );

    if (usuario !== usuarioLogado) return;

    eqNome.value = "";

    await atualizarConsultaEquipes();

  } catch (erro) {
    if (usuario !== usuarioLogado) return;

    eqErro.textContent =
      erro.message === "HTTP 409"
        ? "Essa equipe já está cadastrada."
        : "Não foi possível cadastrar a equipe.";

    console.error("Erro ao cadastrar equipe:", erro);

  } finally {
    salvandoEquipe = false;
  }
}

  if (tabelaEquipes) {
  tabelaEquipes.addEventListener(
    "click",
    async function (e) {

      if (!usuarioLogado ||
          usuarioLogado.perfil !== "estoque" ||
          salvandoEquipe) return;

      const acao = e.target.getAttribute("data-acao");
      const id = e.target.getAttribute("data-id");

      if (acao !== "inativar-equipe" || !id) return;

      const equipe = equipesApi.find(
        q => String(q.id) === id
      );

      if (!equipe || !equipe.ativo) return;

      const confirmou = window.confirm(
        "Deseja inativar a equipe " + equipe.nome + "?"
      );

      if (!confirmou) return;

      const usuario = usuarioLogado;
      salvandoEquipe = true;

      try {
        await requisitarApi(
          "/api/v1/equipes/" + equipe.id + "/inativacao",
          "PATCH"
        );

        if (usuario !== usuarioLogado) return;

        await atualizarConsultaEquipes();

      } catch (erro) {
        if (usuario !== usuarioLogado) return;

        console.error("Erro ao inativar equipe:", erro);

        mostrarAviso(
          "Erro na inativação",
          "Não foi possível inativar a equipe."
        );

      } finally {
        salvandoEquipe = false;
      }
    }
  );
}
  document.querySelector("[data-action='salvar-equipe']").addEventListener("click", salvarEquipe);

  // ---------- ABA EXTRAVIOS ----------
  const tabelaExtravios = document.getElementById("tabela-extravios");

  function mostrarExtravios() {
    if (!tabelaExtravios) return;

    const linhas = [];
    empenhos.forEach(e => {
      if (e.status !== "Confirmado") return;
      e.itens.forEach(i => {
        if (i.situacao !== "Em uso") return;
        linhas.push([
          escapar(e.recebedorNome),
          escapar(e.equipe),
          escapar(i.material),
          escapar(i.patrimonio),
          escapar(formatarData(e.dataConf || e.data)),
          botaoAcao("Marcar extraviado", "btn-outline-danger", "marcar-extraviado", e.id + "|" + i.patrimonio)
        ]);
      });
    });
    montarTabelaGerenciar(tabelaExtravios,
      ["Recebedor", "Equipe", "Material", "Patrimônio", "Empenho em", "Ações"], linhas);
  }

  if (tabelaExtravios) {
    tabelaExtravios.addEventListener("click", function (e) {
      if (e.target.getAttribute("data-acao") !== "marcar-extraviado") return;

      const partes = e.target.getAttribute("data-id").split("|");
      const emp = acharEmpenho(partes[0]);
      const patrimonio = partes[1];
      if (!emp) return;

      confirmarAcao("Marcar como extraviado",
        "Confirma que o item " + patrimonio + " foi extraviado? Ele ficará inativo.",
        function () {
          resolverItem(emp, patrimonio, "Extraviado");
          atualizarTudo();
        });
    });
  }

  // Atualiza as abas de Gerenciar de uma vez
  function atualizarGerenciar() {
    mostrarMateriais();
    preencherSelectMateriais();
    mostrarItens();
    mostrarRecebedores();
    mostrarEquipes();
    mostrarExtravios();
  }

  async function carregarGerenciamento() {
  if (!usuarioLogado || usuarioLogado.perfil !== "estoque") return;

  const usuario = usuarioLogado;
  atualizarGerenciar();

  await atualizarConsultaMateriais();
  if (usuario !== usuarioLogado) return;

  await atualizarConsultaItens();
  if (usuario !== usuarioLogado) return;

  await atualizarConsultaEquipes();
  if (usuario !== usuarioLogado) return;

  await atualizarConsultaRecebedores();
}

  // =====================================================================
  // TELA USUÁRIOS (só Adm do Sistema)
  // =====================================================================
  const usrNome = document.getElementById("usr-nome");
  const usrLogin = document.getElementById("usr-login");
  const usrSenha = document.getElementById("usr-senha");
  const usrPerfil = document.getElementById("usr-perfil");
  const usrErro = document.getElementById("usr-erro");
  const tabelaUsuarios = document.getElementById("tabela-usuarios");

  function mostrarUsuarios() {
    if (!tabelaUsuarios) return;

    const linhas = [];
    usuarios.forEach(u => {
      const ehVoce = usuarioLogado && u.login === usuarioLogado.login;
      const acao = ehVoce
        ? '<span class="text-body-secondary small">Você</span>'
        : botaoAcao(u.ativo ? "Inativar" : "Reativar", "btn-outline-secondary", "ativar-usuario", u.login);

      linhas.push([
        escapar(u.nome),
        escapar(u.login),
        escapar(PERFIS[u.perfil].replace("(", "").replace(")", "").trim()),
        u.ativo ? "Ativo" : "Inativo",
        acao
      ]);
    });
    montarTabelaGerenciar(tabelaUsuarios, ["Nome", "Login", "Perfil", "Situação", "Ações"], linhas);
  }

  function salvarUsuario() {
    usrErro.className = "small text-danger";
    usrErro.textContent = "";

    const nome = usrNome.value.trim();
    const login = usrLogin.value.trim();
    const senha = usrSenha.value.trim();
    const perfil = usrPerfil.value;

    if (nome === "" || login === "" || senha === "") {
      usrErro.textContent = "Preencha nome, login e senha.";
      return;
    }
    if (senha.length < 4) {
      usrErro.textContent = "A senha precisa ter pelo menos 4 caracteres.";
      return;
    }
    if (usuarios.some(u => u.login.toLowerCase() === login.toLowerCase())) {
      usrErro.textContent = "Já existe um usuário com esse login.";
      return;
    }

    usuarios.push({ nome: nome, login: login, senha: senha, perfil: perfil, ativo: true });
    gravarUsuarios();

    usrNome.value = "";
    usrLogin.value = "";
    usrSenha.value = "";
    usrPerfil.value = "operador";

    usrErro.className = "small text-success";
    usrErro.textContent = "Usuário cadastrado com sucesso.";
    mostrarUsuarios();
  }

  if (tabelaUsuarios) {
    tabelaUsuarios.addEventListener("click", function (e) {
      if (e.target.getAttribute("data-acao") !== "ativar-usuario") return;

      const u = usuarios.find(x => x.login === e.target.getAttribute("data-id"));
      if (!u || (usuarioLogado && u.login === usuarioLogado.login)) return;

      u.ativo = !u.ativo;
      gravarUsuarios();
      mostrarUsuarios();
    });
  }
  document.querySelector("[data-action='salvar-usuario']").addEventListener("click", salvarUsuario);

  // =====================================================================
  // MEU PERFIL
  // =====================================================================
  const perfilNome = document.getElementById("perfil-nome");
  const perfilLogin = document.getElementById("perfil-login");
  const perfilCargo = document.getElementById("perfil-cargo");
  const senhaAtual = document.getElementById("senha-atual");
  const senhaNova = document.getElementById("senha-nova");
  const perfilMsg = document.getElementById("perfil-msg");

  function mostrarPerfil() {
    const u = usuarioLogado;
    if (!u) return;

    perfilNome.textContent = u.nome;
    perfilLogin.textContent = u.login;
    perfilCargo.textContent = PERFIS[u.perfil].replace("(", "").replace(")", "").trim();

    senhaAtual.value = "";
    senhaNova.value = "";
    perfilMsg.textContent = "";
  }

  function mensagemPerfil(texto, cor) {
    perfilMsg.className = "small text-" + cor;
    perfilMsg.textContent = texto;
  }

  function alterarSenha() {
    const u = usuarioLogado;
    if (!u) return;

    const atual = senhaAtual.value.trim();
    const nova = senhaNova.value.trim();

    perfilMsg.textContent = "";

    if (atual === "" || nova === "") {
      mensagemPerfil("Preencha a senha atual e a nova senha.", "danger");
      return;
    }
    if (atual !== u.senha) {
      mensagemPerfil("A senha atual está incorreta.", "danger");
      return;
    }
    if (nova.length < 4) {
      mensagemPerfil("A nova senha precisa ter pelo menos 4 caracteres.", "danger");
      return;
    }
    if (nova === atual) {
      mensagemPerfil("A nova senha deve ser diferente da atual.", "danger");
      return;
    }

    u.senha = nova;
    gravarUsuarios();

    senhaAtual.value = "";
    senhaNova.value = "";
    mostrarAviso("Senha alterada", "Sua senha foi alterada com sucesso.");
  }

  const botaoAlterarSenha = document.querySelector("[data-action='alterar-senha']");
  if (botaoAlterarSenha) botaoAlterarSenha.addEventListener("click", alterarSenha);

  document.querySelectorAll("[data-go='perfil']").forEach(link => {
    link.addEventListener("click", mostrarPerfil);
  });

  // =====================================================================
  // INICIALIZAÇÃO
  // =====================================================================
  atualizarTudo();

});