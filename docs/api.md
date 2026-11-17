# API REST do SICOMAT

## Endereço e autenticação

O endereço base local é `http://localhost:8080/api/v1`. Rotas privadas usam HTTP Basic: o frontend envia `Authorization: Basic ...` com matrícula e senha em cada chamada. Não existe uma rota de login que emita token nesta versão.

Como chamadas de escrita usam proteção CSRF, primeiro obtenha um token:

```http
GET /api/v1/csrf
```

Resposta:

```json
{
  "headerName": "X-XSRF-TOKEN",
  "token": "token-gerado-pelo-servidor"
}
```

Envie o valor no cabeçalho indicado em toda chamada `POST`, `PUT`, `PATCH` ou `DELETE`. O token fica associado à sessão HTTP; o navegador deve manter o cookie de sessão e usar `credentials: "include"`.

Exemplo de integração no navegador:

```javascript
const api = "http://localhost:8080/api/v1";
const bytes = new TextEncoder().encode(`${matricula}:${senha}`);
const basic = btoa(String.fromCharCode(...bytes));

const csrfResposta = await fetch(`${api}/csrf`, { credentials: "include" });
const csrf = await csrfResposta.json();

const resposta = await fetch(`${api}/materiais`, {
  method: "GET",
  credentials: "include",
  headers: { Authorization: `Basic ${basic}` }
});
```

Para uma chamada de escrita, inclua também `[csrf.headerName]: csrf.token` nos cabeçalhos. Mantenha a senha apenas em memória enquanto a aplicação estiver aberta; não a grave em `localStorage` ou `sessionStorage`. Em ambiente compartilhado, use HTTPS. Se o frontend estiver em outra origem, configure `SICOMAT_CORS_ALLOWED_ORIGINS` com a origem exata, por exemplo `http://localhost:5173`; não use `*`.

## Perfis

- `ADMINISTRADOR_SISTEMA`: administra contas de acesso.
- `ADMINISTRADOR_ESTOQUE`: administra materiais, itens, equipes e recebedores; consulta movimentações e registra extravios.
- `OPERADOR_ESTOQUE`: registra empenhos, confirma recebimentos e registra devoluções; consulta estoque e movimentações.

As consultas e alterações de estoque e movimentação ficam limitadas ao setor do usuário autenticado.

## Rotas

| Método e rota | Acesso | Descrição |
| --- | --- | --- |
| `GET /status` | Público | Verifica disponibilidade da aplicação. |
| `GET /csrf` | Público | Obtém token CSRF para chamadas de escrita. |
| `GET /conta` | Autenticado | Retorna os dados não sigilosos da conta atual. |
| `GET /usuarios` | Administrador do sistema | Lista contas de acesso. |
| `POST /usuarios` | Administrador do sistema | Cadastra administrador ou operador do estoque. |
| `PATCH /usuarios/{id}/inativacao` | Administrador do sistema | Inativa uma conta. |
| `PATCH /usuarios/{id}/senha` | Administrador do sistema | Redefine a senha de uma conta. |
| `GET /materiais` | Autenticado | Lista materiais do setor. |
| `POST /materiais` | Administrador do estoque | Cadastra um tipo de material. |
| `PUT /materiais/{id}` | Administrador do estoque | Edita um material do setor. |
| `PATCH /materiais/{id}/inativacao` | Administrador do estoque | Inativa material e itens disponíveis ou avariados sem reserva. |
| `POST /materiais/{id}/itens` | Administrador do estoque | Cadastra uma unidade física identificada por patrimônio. |
| `GET /itens/disponiveis` | Autenticado | Lista itens físicos disponíveis no setor. |
| `PATCH /itens/{id}/liberacao` | Administrador do estoque | Libera um item avariado após conferência. |
| `PATCH /itens/{id}/inativacao` | Administrador do estoque | Inativa item disponível ou avariado. |
| `GET /equipes` | Administrador ou operador do estoque | Lista equipes ativas do setor. |
| `POST /equipes` | Administrador do estoque | Cadastra equipe. |
| `PATCH /equipes/{id}/inativacao` | Administrador do estoque | Inativa equipe. |
| `GET /recebedores` | Administrador ou operador do estoque | Lista recebedores ativos, sem incluir PIN. |
| `POST /recebedores` | Administrador do estoque | Cadastra recebedor com PIN inicial. |
| `PATCH /recebedores/{id}/inativacao` | Administrador do estoque | Inativa recebedor. |
| `PATCH /recebedores/{id}/pin` | Administrador do estoque | Redefine PIN do recebedor. |
| `POST /empenhos` | Operador do estoque | Cria empenho pendente e reserva os itens selecionados. |
| `GET /empenhos/pendentes` | Administrador ou operador do estoque | Lista empenhos aguardando confirmação. |
| `GET /empenhos/abertos` | Administrador ou operador do estoque | Lista empenhos com itens aguardando devolução. |
| `GET /empenhos/{id}` | Administrador ou operador do estoque | Consulta empenho e itens associados. |
| `POST /empenhos/{id}/confirmacao` | Operador do estoque | Confirma recebedor por PIN. |
| `POST /empenhos/{id}/cancelamento` | Operador do estoque | Cancela empenho ainda pendente e libera as reservas. |
| `POST /empenhos/{id}/devolucoes` | Operador do estoque | Registra devolução total ou parcial. |
| `GET /empenhos/{id}/devolucoes` | Administrador ou operador do estoque | Lista devoluções registradas. |
| `POST /empenhos/{id}/itens/{itemEmpenhoId}/extravio` | Administrador do estoque | Registra extravio de item ainda empenhado. |

As listas de empenhos usam paginação. Parâmetros: `pagina` (começa em `0`) e `tamanho` (de `1` a `100`; padrão `20`). A resposta contém `conteudo`, `pagina`, `tamanho`, `totalElementos`, `totalPaginas` e `ultima`.

## Exemplos de requisição

Cadastrar material:

```json
{ "codigo": "HT", "nome": "Rádio HT" }
```

Cadastrar item físico:

```json
{ "numeroPatrimonio": "PAT-000123" }
```

Cadastrar equipe e recebedor:

```json
{ "nome": "Equipe Alfa" }
```

```json
{ "nome": "João da Silva", "matricula": "12345", "pinInicial": "482913" }
```

Criar empenho com itens físicos disponíveis:

```json
{
  "recebedorId": 12,
  "equipeId": 4,
  "itemMaterialIds": [31, 32],
  "observacao": "Início do serviço"
}
```

Confirmar recebimento. O PIN deve ter seis dígitos; a justificativa é mantida no histórico:

```json
{ "pin": "482913", "justificativa": "Recebedor conferido presencialmente" }
```

Devolver somente os itens recebidos até aquele horário. `itemEmpenhoId` é o ID da linha do item no empenho, não o ID do patrimônio:

```json
{
  "devolvidoPorId": 12,
  "itens": [
    { "itemEmpenhoId": 76, "condicao": "BOM" },
    { "itemEmpenhoId": 77, "condicao": "AVARIADO", "observacao": "Antena danificada" }
  ],
  "observacao": "Devolução parcial"
}
```

Uma confirmação com PIN inválido retorna HTTP `200` e `confirmado: false`; o PIN não é aceito e a tentativa fica contabilizada para bloqueio temporário. Um item só pode constar uma vez no empenho e só pode ser devolvido uma vez.

## Respostas e erros

- `200`: consulta ou ação concluída.
- `201`: registro criado; a resposta inclui o recurso criado e o cabeçalho `Location`.
- `204`: ação concluída sem conteúdo de resposta.
- `400`: corpo inválido ou parâmetro fora dos limites.
- `401`: credenciais ausentes ou inválidas.
- `403`: perfil sem permissão ou token CSRF ausente/inválido.
- `404`: recurso não encontrado ou não pertencente ao setor autenticado.
- `409`: conflito com o estado atual, por exemplo, item indisponível ou empenho já confirmado.

Erros de API usam `ProblemDetail` do Spring, com `title`, `status` e `detail`. A aplicação oculta mensagens internas e rastreamento de pilha de respostas de erro.
