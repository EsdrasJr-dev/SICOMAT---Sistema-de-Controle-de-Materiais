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

    // Esconde todas as telas e mostra só a escolhida
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

      // Fecha o menu no celular depois do clique
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

    // Aviso só para o Administrador do Sistema
    if (inicioAviso) inicioAviso.hidden = !isAdminSistema;

    // Abre a tela inicial ao logar
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

});