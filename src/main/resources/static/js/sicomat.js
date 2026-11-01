document.addEventListener("DOMContentLoaded", function () {

  // IDs e Atributos originais mantidos
  const telaLogin = document.getElementById("login");
  const telaSistema = document.getElementById("app");
  const campoLogin = document.getElementById("login-login");
  const campoSenha = document.getElementById("login-senha");
  const mensagemErro = document.getElementById("login-erro");
  
  // Mapeamento usando os seletores [data-action] originais do seu HTML
  const botaoEntrar = document.querySelector("[data-action='entrar']");
  const botaoSair = document.querySelector("[data-action='sair']");
  
  // IDs corretos do HTML para injetar o texto
  const nomeUsuario = document.getElementById("usuario-nome");
  const perfilUsuario = document.getElementById("usuario-perfil");

  function fazerLogin() {
    const login = campoLogin.value.trim();
    const senha = campoSenha.value.trim();

    mensagemErro.textContent = "";

    if (login === "" || senha === "") {
      mensagemErro.textContent = "Preencha todos os campos.";
      return;
    }

    // Condicionais simples de iniciante (If/Else)
    if (login === "operador" && senha === "1234") {
      telaLogin.hidden = true;
      telaSistema.hidden = false;
      if (nomeUsuario) nomeUsuario.textContent = "Vinicius Rodrigo";
      if (perfilUsuario) perfilUsuario.textContent = " (Operador)";
    } 
    else if (login === "admin" && senha === "1234") {
      telaLogin.hidden = true;
      telaSistema.hidden = false;
      if (nomeUsuario) nomeUsuario.textContent = "Esdras Batista";
      if (perfilUsuario) perfilUsuario.textContent = " (Administrador de Estoque)";
    } 
    else if (login === "sistema" && senha === "1234") {
      telaLogin.hidden = true;
      telaSistema.hidden = false;
      if (nomeUsuario) nomeUsuario.textContent = "Administrador do Sistema";
      if (perfilUsuario) perfilUsuario.textContent = " (Administrador do Sistema)";
    } 
    else {
      mensagemErro.textContent = "Usuário ou senha incorretos.";
    }
  }

  // Evento de clique
  if (botaoEntrar) {
    botaoEntrar.addEventListener("click", fazerLogin);
  }

  // Evento de teclado (Tecla Enter) nos dois campos de entrada
  if (campoLogin) {
    campoLogin.addEventListener("keydown", function (evento) {
      if (evento.key === "Enter") {
        fazerLogin();
      }
    });
  }

  if (campoSenha) {
    campoSenha.addEventListener("keydown", function (evento) {
      if (evento.key === "Enter") {
        fazerLogin();
      }
    });
  }

  // Evento do botão sair
  if (botaoSair) {
    botaoSair.addEventListener("click", function () {
      telaSistema.hidden = true;
      telaLogin.hidden = false;
      campoLogin.value = "";
      campoSenha.value = "";
    });
  }
});
