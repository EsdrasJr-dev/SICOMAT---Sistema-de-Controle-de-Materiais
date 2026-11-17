# Arquitetura do backend

## Escolha

O SICOMAT usa um monólito modular em Spring Boot. A API, as regras e a persistência são implantadas juntas, mas ficam separadas por responsabilidade e módulo de negócio. Essa estrutura mantém o MVP simples e permite evoluir sem introduzir a complexidade operacional de microserviços.

## Fluxo de uma requisição

Cliente HTTP → Controller REST → Serviço de aplicação → Repositório JPA e entidade de persistência → PostgreSQL

Os modelos de domínio continuam separados das entidades JPA. As entidades ficam em infrastructure.persistence para evitar acoplamento direto com o Hibernate. Controllers expõem DTOs de entrada e saída; serviços de aplicação concentram validações de negócio, autorização por setor e transações.

## Módulos

- usuario: perfis, criação de contas e autenticação.
- estoque: materiais, itens físicos e disponibilidade.
- movimentacao: empenhos pendentes e confirmados, devoluções parciais, cancelamentos, extravios e histórico.
- equipe: equipe e recebedor.
- seguranca: hash de senha/PIN, bloqueio de tentativas e contrato do verificador facial.

Cada módulo separa api, application e infrastructure.persistence quando essa camada já está implementada.

## Segurança

A API usa Spring Security com autenticação HTTP Basic, CSRF por cookie e token em cabeçalho, CORS por lista explícita de origens e autorização por perfil. A conta inicial de administrador do sistema é criada somente quando ainda não existe e seus dados vêm de SICOMAT_ADMIN_*; o banco também garante que exista no máximo uma conta desse perfil. Essa conta não pode ser inativada pela API e pode cadastrar administradores e operadores do estoque.

No login da API, cinco senhas incorretas em uma janela de cinco minutos bloqueiam a conta por cinco minutos, com controle salvo no PostgreSQL. O PIN dos recebedores usa a mesma política e também é persistido somente como hash. Senhas legadas com custo menor são atualizadas após uma autenticação bem-sucedida. HTTP Basic é o mecanismo de autenticação desta etapa; qualquer implantação compartilhada deve usar HTTPS.

## Persistência e migrações

Flyway cria o esquema PostgreSQL em src/main/resources/db/migration. O Hibernate valida o esquema (ddl-auto=validate) e não altera tabelas automaticamente. As migrações persistem usuários, materiais, itens físicos, equipes, recebedores, empenhos e devoluções, com restrições de unicidade e índices por setor.

## Endpoints implementados

- Conta: GET /api/v1/status e GET /api/v1/csrf são públicos; GET /api/v1/conta retorna o perfil autenticado sem credenciais.
- Acesso: GET, POST e PATCH em /api/v1/usuarios são restritos ao administrador do sistema.
- Estoque: leitura para usuários autenticados; cadastro, edição, inativação, liberação de itens avariados e inativação individual de itens para administrador do estoque.
- Equipe: operadores e administradores consultam equipes e recebedores; apenas o administrador do estoque pode cadastrá-los, inativá-los ou redefinir PIN.
- Movimentação: o operador registra, confirma e cancela empenhos e registra devoluções; operadores e administradores consultam pendências e histórico; o administrador do estoque registra extravios.
- A lista completa de rotas e exemplos está em [api.md](api.md).

## Próximas integrações

Não há telas frontend nesta etapa. VerificadorFacial permanece como contrato sem implementação de fornecedor: a aplicação não simula uma aprovação facial nem guarda dados biométricos. A confirmação por PIN com justificativa está disponível no backend enquanto a integração facial permanece futura. A implantação também precisa aplicar limites de tentativas por origem no gateway e armazenar os segredos fora do repositório.
