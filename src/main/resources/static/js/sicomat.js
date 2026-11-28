document.addEventListener("DOMContentLoaded", function () {

  // =====================================================================
  // CONFIGURAÇÃO DO LOGIN  (é só aqui que você mexe)
  // =====================================================================
  // false = login local, para testar sem back-end (admin / operador / sistema, senha 1234)
  // true  = login feito pelo servidor (back-end Spring)
  const USAR_SERVIDOR = false;

  // Endereço do back-end:
  //   ""                      -> quando você abre o sistema pelo próprio Spring (http://localhost:8080/index.html)
  //   "http://localhost:8080" -> quando você abre pelo Live Server (o back-end precisa liberar o CORS)
  const URL_BASE = "";

  // ELEMENTOS DE LOGIN E PAINEL
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

  // USUÁRIOS (contas de acesso)
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
    sistema: ["inicio", "perfil", "usuarios", "funcionarios"]
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

  // Consulta a conta no servidor usando as credenciais informadas
  async function consultarConta(autorizacao) {
    const resposta = await fetch(URL_BASE + "/api/v1/conta", {
      method: "GET",
      headers: {
        "Authorization": autorizacao
      },
      credentials: "same-origin"
    });

    if (!resposta.ok) {
      throw new Error("HTTP " + resposta.status);
    }

    return await resposta.json();
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

  // DADOS DO ESTOQUE
  const CH_MATERIAIS = "sicomat_materiais";
  const CH_ITENS = "sicomat_itens";
  const CH_FUNCIONARIOS = "sicomat_funcionarios";
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

  const materiais = lerLista(CH_MATERIAIS);       // codigo, nome, secao, ativo 
  const itens = lerLista(CH_ITENS);               // patrimonio, codigo, ativo 
  const funcionarios = lerLista(CH_FUNCIONARIOS); // nome, matricula, pin, ativo 
  const equipes = lerLista(CH_EQUIPES);           // nome 
  const empenhos = lerLista(CH_EMPENHOS);

  // FUNÇÕES AUXILIARES
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

  // Tabela onde as células já são HTML pronto
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

  // Um item está "em empenho" se está num empenho confirmado e ainda não voltou
  function itemEmUso(patrimonio) {
    return empenhos.some(e =>
      e.status === "Confirmado" &&
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

  // Marca o empenho como encerrado quando todos os itens foram resolvidos
  function encerrarSeCompleto(emp) {
    if (emp.status === "Confirmado" && !emp.itens.some(i => i.situacao === "Em uso")) {
      emp.status = "Encerrado";
    }
  }

  // Registra o resultado de um item
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
    gravarLista(CH_FUNCIONARIOS, funcionarios);
    gravarLista(CH_EQUIPES, equipes);
    gravarLista(CH_EMPENHOS, empenhos);
  }

  // JANELA DE MENSAGENS (modal)
  const modalEl = document.getElementById("modal");
  const modalTitulo = document.getElementById("modal-titulo");
  const modalCorpo = document.getElementById("modal-corpo");
  const modalRodape = document.getElementById("modal-rodape");

  // botoes = lista de texto, classe, aoClicar, manter
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

  // NAVEGAÇÃO PRINCIPAL
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
    if (nome === "consultar") mostrarConsultar();
    if (nome === "registrar") mostrarRegistrar();
    if (nome === "historico") renderizarHistorico();
    if (nome === "gerenciar") atualizarGerenciar();
    if (nome === "usuarios") mostrarUsuarios();
    if (nome === "funcionarios") mostrarFuncionarios();
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

  // Atualiza todas as telas de uma vez depois de qualquer mudança
  function atualizarTudo() {
    gravarTudo();
    mostrarInicio();
    mostrarConsultar();
    mostrarRegistrar();
    renderizarHistorico();
    if (tabelaRelatorio && tabelaRelatorio.innerHTML.trim() !== "") gerarRelatorio();
    atualizarGerenciar();
  }

 
  // LOGIN  local ou pelo servidor, conforme USAR_SERVIDOR
  async function fazerLogin() {
    const login = campoLogin.value.trim();
    const senha = campoSenha.value;

    mensagemErro.textContent = "";

    if (login === "" || senha === "") {
      mensagemErro.textContent = "Preencha todos os campos.";
      return;
    }

    // MODO TESTE: confere na lista local, sem back-end
    if (!USAR_SERVIDOR) {
      const u = usuarios.find(x => x.login === login && x.senha === senha);

      if (!u) {
        mensagemErro.textContent = "Usuário ou senha incorretos.";
        return;
      }
      if (!u.ativo) {
        mensagemErro.textContent = "Este usuário está inativo.";
        return;
      }
      logarUsuario(u);
      return;
    }

    // MODO SERVIDOR: confere no back-end
    const credenciais = new TextEncoder().encode(login + ":" + senha);
    const autorizacao = "Basic " + btoa(
      String.fromCharCode(...credenciais)
    );

    let conta;

    try {
      conta = await consultarConta(autorizacao);
    } catch (erro) {
      console.error("Falha no login:", erro);

      if (erro.message === "HTTP 401") {
        mensagemErro.textContent = "Matrícula ou senha incorretas.";
      } else if (erro.message === "HTTP 404") {
        mensagemErro.textContent = "API não encontrada. Abra o sistema pelo servidor do back-end, não pelo Live Server.";
      } else {
        mensagemErro.textContent = "Não foi possível conectar ao servidor. Verifique se o back-end está rodando.";
      }
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
      rascunho = [];
      if (empMatricula) empMatricula.value = "";
      if (empPin) empPin.value = "";
      if (devMatricula) devMatricula.value = "";
      if (devPin) devPin.value = "";
      telaSistema.hidden = true;
      telaLogin.hidden = false;
      campoLogin.value = "";
      campoSenha.value = "";
      mensagemErro.textContent = "";
    });
  }

  // Abas Registrar Movimentação e Gerenciar Materiais
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

  // PAINEL DO INÍCIO
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
    inicioResumo.innerHTML =
      cartao("Itens em empenho aberto", listarItens("Em uso").length) +
      cartao("Itens disponíveis", itensDisponiveis().length) +
      cartao("Itens avariados", listarItens("Avariado").length) +
      cartao("Itens extraviados", listarItens("Extraviado").length);

    // Filtros
    preencherSelect(painelEquipe, "Todas as equipes",
      equipes.map(q => ({ valor: q.nome, texto: q.nome })));
    preencherSelect(painelRecebedor, "Todos os recebedores",
      funcionarios.map(f => ({ valor: f.matricula, texto: f.nome })));

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

  // CONSULTAR MATERIAIS
  const buscaTexto = document.getElementById("busca-texto");
  const buscaSecao = document.getElementById("busca-secao");
  const buscaStatus = document.getElementById("busca-status");
  const tabelaMateriais = document.getElementById("tabela-materiais");

  function mostrarConsultar() {
    if (!tabelaMateriais) return;

    const secoes = [...new Set(materiais.map(m => m.secao))].sort();
    preencherSelect(buscaSecao, "Todas as seções", secoes.map(s => ({ valor: s, texto: s })));

    const texto = buscaTexto.value.trim().toLowerCase();
    const secao = buscaSecao.value;
    const status = buscaStatus.value;
    const disponiveis = itensDisponiveis();

    const linhas = materiais.map(m => {
      const meus = itens.filter(i => i.codigo === m.codigo);
      return {
        codigo: m.codigo,
        nome: m.nome,
        secao: m.secao,
        ativo: m.ativo,
        total: meus.length,
        disp: disponiveis.filter(i => i.codigo === m.codigo).length,
        emUso: meus.filter(i => itemEmUso(i.patrimonio)).length
      };
    }).filter(l =>
      (texto === "" || l.nome.toLowerCase().includes(texto) || l.codigo.toLowerCase().includes(texto)) &&
      (secao === "" || l.secao === secao) &&
      (status === "" || (status === "disp" ? l.disp > 0 : l.disp === 0))
    );

    montarTabela(tabelaMateriais, [
      { titulo: "Código",       valor: l => l.codigo },
      { titulo: "Material",     valor: l => l.nome },
      { titulo: "Seção",        valor: l => l.secao },
      { titulo: "Total",        valor: l => l.total },
      { titulo: "Disponíveis",  valor: l => l.disp },
      { titulo: "Em empenho",   valor: l => l.emUso },
      { titulo: "Situação",     valor: l => (l.ativo ? "Ativo" : "Inativo") }
    ], linhas);
  }

  if (buscaTexto) buscaTexto.addEventListener("input", mostrarConsultar);
  if (buscaSecao) buscaSecao.addEventListener("change", mostrarConsultar);
  if (buscaStatus) buscaStatus.addEventListener("change", mostrarConsultar);

  // REGISTRAR MOVIMENTAÇÃO
  const empEquipe = document.getElementById("emp-equipe");
  const empItem = document.getElementById("emp-item");
  const empLista = document.getElementById("emp-lista");
  const empMatricula = document.getElementById("emp-matricula");
  const empPin = document.getElementById("emp-pin");
  const empErro = document.getElementById("emp-erro");
  const devEmpenho = document.getElementById("dev-empenho");
  const devItens = document.getElementById("dev-itens");
  const devErro = document.getElementById("dev-erro");
  const devMatricula = document.getElementById("dev-matricula");
  const devPin = document.getElementById("dev-pin");

  let rascunho = [];
  function mostrarMensagem(texto, tipo) {
    if (!registrarMsg) return;
    registrarMsg.innerHTML = '<div class="alert alert-' + tipo + ' py-2 mb-0">' + escapar(texto) + "</div>";
  }

  function mostrarRegistrar() {
    if (!empEquipe) return;

    preencherSelect(empEquipe, "Selecione a equipe",
      equipes.map(q => ({ valor: q.nome, texto: q.nome })));

    // Itens disponíveis que ainda não estão no rascunho
    const livres = itensDisponiveis().filter(i => !rascunho.includes(i.patrimonio));
    preencherSelect(empItem, livres.length ? "Selecione o item" : "Nenhum item disponível",
      livres.map(i => ({ valor: i.patrimonio, texto: nomeDoMaterial(i.codigo) + " – " + i.patrimonio })));

    mostrarRascunho();
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

  // O funcionário se identifica com matrícula e PIN se estiver certo o material é liberado
  function criarEmpenho() {
    empErro.textContent = "";

    const equipe = empEquipe.value;
    const matricula = empMatricula.value.trim();
    const pin = empPin.value.trim();

    if (rascunho.length === 0) {
      empErro.textContent = "Adicione pelo menos um item.";
      return;
    }
    if (equipe === "") {
      empErro.textContent = "Escolha a equipe.";
      return;
    }
    if (matricula === "" || pin === "") {
      empErro.textContent = "Informe a matrícula e o PIN do funcionário.";
      return;
    }

    const func = funcionarios.find(f => f.matricula === matricula && f.pin === pin);
    if (!func) {
      empPin.value = "";
      empErro.textContent = "Matrícula ou PIN incorretos.";
      return;
    }
    if (!func.ativo) {
      empPin.value = "";
      empErro.textContent = "Este funcionário está inativo. Procure o Adm do Sistema.";
      return;
    }

    const livres = itensDisponiveis().map(i => i.patrimonio);
    if (rascunho.some(p => !livres.includes(p))) {
      empErro.textContent = "Algum item escolhido não está mais disponível. Revise a lista.";
      return;
    }

    const novo = {
      id: proximoId(),
      data: hojeISO(),
      dataConf: hojeISO(),
      recebedor: func.matricula,
      recebedorNome: func.nome,
      equipe: equipe,
      status: "Confirmado",
      criadoPor: usuarioLogado ? usuarioLogado.nome : "",
      itens: rascunho.map(p => {
        const it = acharItem(p);
        return { patrimonio: p, codigo: it.codigo, material: nomeDoMaterial(it.codigo), situacao: "Em uso", dataDev: "" };
      })
    };
    empenhos.push(novo);

    rascunho = [];
    empMatricula.value = "";
    empPin.value = "";
    atualizarTudo();
    mostrarMensagem("Material liberado para " + func.nome + " (empenho nº " + novo.id + ").", "success");
  }

  // Devolução
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

  // O funcionário que retirou o material se identifica com matrícula e PIN para devolver
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

    const matricula = devMatricula.value.trim();
    const pin = devPin.value.trim();
    if (matricula === "" || pin === "") {
      devErro.textContent = "Informe a matrícula e o PIN do funcionário.";
      return;
    }

    const func = funcionarios.find(f => f.matricula === matricula && f.pin === pin);
    if (!func) {
      devPin.value = "";
      devErro.textContent = "Matrícula ou PIN incorretos.";
      return;
    }
    if (func.matricula !== emp.recebedor) {
      devPin.value = "";
      devErro.textContent = "Esta matrícula não é a do funcionário que retirou este empenho.";
      return;
    }

    escolhas.forEach(c => resolverItem(emp, c.pat, c.situacao));
    devMatricula.value = "";
    devPin.value = "";
    atualizarTudo();
    mostrarMensagem(
      emp.status === "Encerrado"
        ? "Devolução registrada por " + func.nome + ". O empenho nº " + emp.id + " foi encerrado."
        : "Devolução registrada por " + func.nome + ". Ainda há itens em aberto no empenho nº " + emp.id + ".",
      "success");
  }

  document.querySelector("[data-action='criar-empenho']").addEventListener("click", criarEmpenho);
  document.querySelector("[data-action='registrar-devolucao']").addEventListener("click", registrarDevolucao);


  // HISTÓRICO
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
    // mais recentes primeiro
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

  // ao clicar numa linha do histórico, mostra os dados dela
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


  // RELATÓRIOS

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

  // GERENCIAR MATERIAIS
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
      const m = acharMaterial(codigoEmEdicao);
      if (m) {
        m.nome = nome;
        m.secao = secao;
      }
    } else {
      if (acharMaterial(codigo)) {
        matErro.textContent = "Já existe um material com esse código.";
        return;
      }
      materiais.push({ codigo: codigo, nome: nome, secao: secao, ativo: true });
    }

    codigoEmEdicao = "";
    matCodigo.value = "";
    matCodigo.readOnly = false;
    matNome.value = "";
    matSecao.value = "";

    atualizarTudo();
  }

  if (tabelaGerMateriais) {
    tabelaGerMateriais.addEventListener("click", function (e) {
      const acao = e.target.getAttribute("data-acao");
      const codigo = e.target.getAttribute("data-id");
      if (!acao) return;

      const m = acharMaterial(codigo);
      if (!m) return;

      if (acao === "editar-material") {
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
        atualizarTudo();
      }
    });
  }
  document.querySelector("[data-action='salvar-material']").addEventListener("click", salvarMaterial);

  // ABA ITENS
  const itemMaterial = document.getElementById("item-material");
  const itemPatrimonio = document.getElementById("item-patrimonio");
  const itemErro = document.getElementById("item-erro");
  const tabelaGerItens = document.getElementById("tabela-gerenciar-itens");

  // Select só com materiais ativos
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
      const emUso = itemEmUso(it.patrimonio);
      const situacao = emUso ? "Em empenho" : (it.ativo ? "Disponível" : "Inativo");
      const acao = emUso
        ? '<span class="text-body-secondary small">—</span>'
        : botaoAcao(it.ativo ? "Inativar" : "Liberar", "btn-outline-secondary", "ativar-item", it.patrimonio);

      linhas.push([
        escapar(it.patrimonio),
        escapar(nomeDoMaterial(it.codigo)),
        situacao,
        acao
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
    if (acharItem(patrimonio)) {
      itemErro.textContent = "Esse patrimônio já está cadastrado.";
      return;
    }
    itens.push({ patrimonio: patrimonio, codigo: codigo, ativo: true });
    itemPatrimonio.value = "";
    atualizarTudo();
  }

  if (tabelaGerItens) {
    tabelaGerItens.addEventListener("click", function (e) {
      if (e.target.getAttribute("data-acao") !== "ativar-item") return;

      const it = acharItem(e.target.getAttribute("data-id"));
      if (!it) return;

      it.ativo = !it.ativo;
      atualizarTudo();
    });
  }
  document.querySelector("[data-action='salvar-item']").addEventListener("click", salvarItem);

  // ABA EQUIPES
  const eqNome = document.getElementById("eq-nome");
  const eqErro = document.getElementById("eq-erro");
  const tabelaEquipes = document.getElementById("tabela-equipes");

  function mostrarEquipes() {
    if (!tabelaEquipes) return;

    const linhas = equipes.map(q => [
      escapar(q.nome),
      botaoAcao("Remover", "btn-outline-danger", "remover-equipe", q.nome)
    ]);
    montarTabelaGerenciar(tabelaEquipes, ["Equipe", "Ações"], linhas);
  }

  function salvarEquipe() {
    eqErro.textContent = "";

    const nome = eqNome.value.trim();
    if (nome === "") {
      eqErro.textContent = "Informe o nome da equipe.";
      return;
    }
    if (equipes.some(q => q.nome.toLowerCase() === nome.toLowerCase())) {
      eqErro.textContent = "Essa equipe já está cadastrada.";
      return;
    }
    equipes.push({ nome: nome });
    eqNome.value = "";
    atualizarTudo();
  }

  if (tabelaEquipes) {
    tabelaEquipes.addEventListener("click", function (e) {
      if (e.target.getAttribute("data-acao") !== "remover-equipe") return;

      const nome = e.target.getAttribute("data-id");
      confirmarAcao("Remover equipe", "Remover a equipe " + nome + "? Empenhos antigos continuam com o nome dela.", function () {
        const pos = equipes.findIndex(q => q.nome === nome);
        if (pos >= 0) equipes.splice(pos, 1);
        atualizarTudo();
      });
    });
  }
  document.querySelector("[data-action='salvar-equipe']").addEventListener("click", salvarEquipe);

  // ABA EXTRAVIOS
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
    mostrarEquipes();
    mostrarExtravios();
  }

   // Tela usuarios só Adm ve do Sistema
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

  // tela funcionário matrícula e PIN
  const funcNome = document.getElementById("func-nome");
  const funcMatricula = document.getElementById("func-matricula");
  const funcPin = document.getElementById("func-pin");
  const funcErro = document.getElementById("func-erro");
  const tabelaFuncionarios = document.getElementById("tabela-funcionarios");

  // pin de 4 a 6 números
  function pinValido(pin) {
    return /^\d{4,6}$/.test(pin);
  }

  // Os campos de PIN só aceitam números
  [funcPin, empPin, devPin].forEach(campo => {
    if (campo) {
      campo.addEventListener("input", function () {
        campo.value = campo.value.replace(/\D/g, "");
      });
    }
  });

  function mostrarFuncionarios() {
    if (!tabelaFuncionarios) return;

    const linhas = funcionarios.map(f => [
      escapar(f.nome),
      escapar(f.matricula),
      f.ativo ? "Ativo" : "Inativo",
      botaoAcao("Redefinir PIN", "btn-outline-primary", "redefinir-pin", f.matricula) +
      botaoAcao(f.ativo ? "Inativar" : "Reativar", "btn-outline-secondary", "ativar-funcionario", f.matricula)
    ]);
    montarTabelaGerenciar(tabelaFuncionarios, ["Nome", "Matrícula", "Situação", "Ações"], linhas);
  }

  function salvarFuncionario() {
    funcErro.className = "small text-danger";
    funcErro.textContent = "";

    const nome = funcNome.value.trim();
    const matricula = funcMatricula.value.trim();
    const pin = funcPin.value.trim();

    if (nome === "" || matricula === "" || pin === "") {
      funcErro.textContent = "Preencha nome, matrícula e PIN.";
      return;
    }
    if (!pinValido(pin)) {
      funcErro.textContent = "O PIN deve ter de 4 a 6 números.";
      return;
    }
    if (funcionarios.some(f => f.matricula === matricula)) {
      funcErro.textContent = "Essa matrícula já está cadastrada.";
      return;
    }

    funcionarios.push({ nome: nome, matricula: matricula, pin: pin, ativo: true });
    gravarLista(CH_FUNCIONARIOS, funcionarios);

    funcNome.value = "";
    funcMatricula.value = "";
    funcPin.value = "";

    funcErro.className = "small text-success";
    funcErro.textContent = "Funcionário cadastrado com sucesso.";
    mostrarFuncionarios();
  }

  function pedirNovoPin(f) {
    const corpo =
      '<label class="form-label" for="novo-pin">Novo PIN (4 a 6 números)</label>' +
      '<input id="novo-pin" type="password" inputmode="numeric" maxlength="6" class="form-control" autocomplete="new-password">' +
      '<p id="novo-pin-erro" class="text-danger small mt-2 mb-0"></p>';

    abrirModal("Redefinir PIN de " + f.nome, corpo, [
      { texto: "Cancelar", classe: "btn-outline-secondary" },
      {
        texto: "Salvar PIN",
        classe: "btn-primary",
        manter: true,
        aoClicar: function () {
          const pin = document.getElementById("novo-pin").value.trim();
          if (!pinValido(pin)) {
            document.getElementById("novo-pin-erro").textContent = "O PIN deve ter de 4 a 6 números.";
            return;
          }
          f.pin = pin;
          gravarLista(CH_FUNCIONARIOS, funcionarios);
          fecharModal();
          funcErro.className = "small text-success";
          funcErro.textContent = "PIN de " + f.nome + " redefinido com sucesso.";
        }
      }
    ]);

    // Só aceita números no campo do modal
    const campo = document.getElementById("novo-pin");
    if (campo) {
      campo.addEventListener("input", function () {
        campo.value = campo.value.replace(/\D/g, "");
      });
    }
  }

  if (tabelaFuncionarios) {
    tabelaFuncionarios.addEventListener("click", function (e) {
      const acao = e.target.getAttribute("data-acao");
      const f = funcionarios.find(x => x.matricula === e.target.getAttribute("data-id"));
      if (!acao || !f) return;

      if (acao === "ativar-funcionario") {
        f.ativo = !f.ativo;
        gravarLista(CH_FUNCIONARIOS, funcionarios);
        mostrarFuncionarios();
      }
      if (acao === "redefinir-pin") {
        pedirNovoPin(f);
      }
    });
  }
  document.querySelector("[data-action='salvar-funcionario']").addEventListener("click", salvarFuncionario);

  // MEU PERFIL
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

    // No modo servidor a senha fica no back-end; aqui ainda não há rota para trocá-la
    if (USAR_SERVIDOR) {
      mensagemPerfil("A troca de senha pelo servidor ainda não está disponível.", "warning");
      return;
    }

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

  // INICIALIZAÇÃO

  // cancela e libera os itens
  empenhos.forEach(e => {
    if (e.status === "Pendente") {
      e.status = "Cancelado";
      e.itens.forEach(i => (i.situacao = "Cancelado"));
    }
  });

  atualizarTudo();

});