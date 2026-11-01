# Arquitetura do backend

## Escolha

O SICOMAT usa um monólito modular em Spring Boot. A API, as regras e a persistência são implantadas juntas, mas ficam separadas por responsabilidade e módulo de negócio. Essa estrutura mantém o MVP simples e permite evoluir sem introduzir a complexidade operacional de microserviços.

## Fluxo de uma requisição

Cliente HTTP → Controller REST → Serviço de aplicação → Repositório JPA e entidade de persistência → PostgreSQL

Os modelos Java que já existiam continuam separados das entidades JPA. As entidades ficam em infrastructure.persistence para evitar acoplamento direto com o Hibernate. A fatia atual de usuários e inventário usa serviços de aplicação e entidades JPA; os fluxos de movimentação existentes ainda não foram ligados ao banco.

## Módulos

- usuario: perfis, criação de contas e autenticação.
- estoque: materiais, itens físicos e disponibilidade.
- movimentacao: empenhos, devoluções e histórico; o domínio existente será conectado à persistência na próxima etapa.
- equipe: equipe e recebedor.
- seguranca: hash de senha/PIN, bloqueio de tentativas e contrato do verificador facial.

Cada módulo separa api, application e infrastructure.persistence quando essa camada já está implementada.

## Segurança

A API usa Spring Security com autenticação HTTP Basic e autorização por perfil. A conta inicial de administrador do sistema é criada somente quando ainda não existe e seus dados vêm de SICOMAT_ADMIN_*; o banco também garante que exista no máximo uma conta desse perfil. Essa conta não pode ser inativada pela API e pode cadastrar administradores e operadores do estoque.

No login da API, cinco senhas incorretas em uma janela de cinco minutos bloqueiam a conta por cinco minutos, com controle salvo no PostgreSQL. O verificador de PIN do domínio já aplica a mesma janela e duração enquanto a instância do recebedor está em memória; persistir recebedores e tentativas fica para a integração de movimentações. Senhas e PINs usam o hash PBKDF2 existente. HTTP Basic serve para desenvolvimento local; a implantação deve usar HTTPS.

## Persistência e migrações

Flyway cria o esquema PostgreSQL em src/main/resources/db/migration. O Hibernate valida o esquema (ddl-auto=validate) e não altera tabelas automaticamente. As primeiras tabelas persistem usuários, materiais e itens, com restrições de unicidade para matrículas, códigos por setor e patrimônios.

## Endpoints implementados

- GET /api/v1/status: estado da aplicação, sem autenticação.
- GET /api/v1/usuarios, POST /api/v1/usuarios e PATCH /api/v1/usuarios/{id}/inativacao: somente administrador do sistema.
- GET /api/v1/materiais e GET /api/v1/itens/disponiveis: usuário autenticado, restrito ao setor associado à conta.
- POST /api/v1/materiais, PUT /api/v1/materiais/{id}, PATCH /api/v1/materiais/{id}/inativacao e POST /api/v1/materiais/{id}/itens: somente administrador do estoque.

## Próximas integrações

A API atual cobre o cadastro de usuários e o inventário. Empenhos e devoluções ainda não foram conectados ao PostgreSQL e não há telas frontend nesta etapa. VerificadorFacial permanece como contrato sem implementação de fornecedor: a aplicação não simula uma aprovação facial. O PIN segue como alternativa no domínio, mediante justificativa, até a integração facial do MVP.