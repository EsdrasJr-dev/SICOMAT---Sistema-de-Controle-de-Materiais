# SICOMAT

### Sistema de Controle de Materiais

Aplicação para registrar e acompanhar materiais e itens físicos sob responsabilidade de equipes durante um serviço.

## 💡 Motivação e problema

A ideia do SICOMAT surgiu da experiência de um integrante da equipe no serviço de dia da Polícia Militar. Ao assumir o turno, ele perguntou ao cabo que estava transmitindo o serviço qual era a quantidade correta de cada material disponível, mas essa informação não pôde ser confirmada com clareza, pois o controle era feito em caderno.

O operador é dedicado ao controle do estoque e acompanha um fluxo contínuo de materiais: empenha itens para as equipes, recebe devoluções em horários diferentes, registra trocas durante o serviço e prepara os materiais para a equipe que assumirá o próximo turno. Com registros manuais, pode ser difícil consultar a situação atual de cada item e manter o histórico das movimentações organizado. Divergências ou extravios também podem exigir apuração administrativa, inclusive por meio de sindicância.

## 🎯 Objetivo

O SICOMAT tem como objetivo centralizar o cadastro dos materiais e o controle de cada item físico, separando a administração dos acessos, a gestão do estoque e o registro das movimentações. O sistema permite acompanhar empenhos para as equipes, devoluções totais ou parciais e trocas durante o turno. Cada movimentação fica associada à data, ao horário e aos responsáveis, facilitando a consulta da situação dos itens, a identificação de pendências e a preparação do estoque para a equipe seguinte.

## 👤 Perfis de acesso

| Perfil | Responsabilidades |
| --- | --- |
| Administrador do sistema | Conta única criada na carga inicial; cadastra administradores e operadores do estoque; não pode ser inativada. |
| Administrador do estoque | Cadastra, edita e inativa materiais e itens físicos. |
| Operador do estoque | Registra empenhos, devoluções e trocas, e acompanha a situação dos itens. Não pode cadastrar, editar ou inativar materiais e itens. |

A conta única do administrador do sistema não pode ser substituída pela aplicação. Se for necessário trocá-la, a alteração deve ser feita na carga inicial ou diretamente no banco de dados.

## ⚙️ Funcionalidades previstas

- Cadastro, inativação e redefinição de senha de administradores e operadores do estoque pelo administrador do sistema.
- Cadastro, edição e inativação de materiais e itens físicos pelo administrador do estoque.
- Cadastro de equipes e recebedores do estoque pelo administrador do estoque.
- Registro de empenho de itens para uma equipe pelo operador do estoque, com identificação dos responsáveis e da movimentação.
- Confirmação do recebimento por PIN, com bloqueio temporário após tentativas inválidas e justificativa registrada.
- Registro de devoluções totais ou parciais, inclusive quando ocorrem em horários diferentes.
- Registro de avarias e extravios, com controle de itens disponíveis, empenhados, avariados e inativos.
- Bloqueio temporário após cinco falhas de senha ou PIN em cinco minutos; a credencial é liberada automaticamente após cinco minutos.
- Consulta de empenhos pendentes, abertos e do histórico de devoluções.
- Histórico das movimentações para consulta e apoio a conferências e auditorias.

O provedor de verificação facial ainda não foi integrado. No backend atual, a confirmação do recebedor usa PIN; o sistema não coleta nem simula reconhecimento facial.

## 🌐 Possíveis aplicações em outros setores

Embora tenha surgido para atender ao serviço de dia da Polícia Militar, a proposta pode ser adaptada a organizações civis que entregam equipamentos compartilhados a equipes externas. Empresas de manutenção elétrica, telecomunicações, construção e outros serviços de campo também precisam acompanhar ferramentas e materiais retirados, devolvidos ou substituídos ao longo do trabalho. Nesses contextos, o controle poderia ser associado, por exemplo, a uma ordem de serviço, veículo ou local de atendimento.

## 🧰 Tecnologias

| Área | Tecnologias |
| --- | --- |
| Backend | Java 21, Spring Boot 3.5.16, API REST, Spring Security, Spring Data JPA e Flyway |
| Interface | A interface Thymeleaf + Bootstrap permanece planejada; o backend expõe API REST |
| Banco de dados | PostgreSQL 17 |

A arquitetura está descrita em [docs/arquitetura.md](docs/arquitetura.md). As rotas, formatos de requisição e regras para integração do frontend estão em [docs/api.md](docs/api.md); a revisão de segurança está em [docs/seguranca-backend.md](docs/seguranca-backend.md).

## 📐 Diagrama de classes

[Ver o diagrama em tamanho original (JPG)](docs/diagramas/SICOMAT_diagrama_de_classes%20.jpg) · [Abrir arquivo editável (draw.io)](docs/diagramas/SICOMAT_diagrama_de_classes%20.drawio)

![Diagrama de classes do SICOMAT](docs/diagramas/SICOMAT_diagrama_de_classes%20.jpg)

## 👥 Equipe

| Integrante | Área |
| --- | --- |
| Rodrigo Felipe | Frontend |
| Vinicius Rodrigo | Frontend |
| Thiago Domingos | Frontend |
| Esdras Batista | Backend e banco de dados |
| Gabriel Rodrigues | Backend e banco de dados |
| Midiane S. Victorino | Backend e banco de dados |
| Jonas Napoleão da Silva Farias | Backend e banco de dados |

## 🚀 Como executar

Requisitos: JDK 21 e Docker com Compose.

1. Crie um arquivo local `.env` na raiz do projeto e configure `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `POSTGRES_DB` e as variáveis `SICOMAT_ADMIN_*`. O arquivo `.env` é ignorado pelo Git e não deve ser enviado ao repositório.
2. Inicie o PostgreSQL: docker compose up -d database.
3. Inicie a aplicação: .\mvnw.cmd spring-boot:run.

Na primeira inicialização, a aplicação cria a conta única de administrador do sistema a partir das variáveis SICOMAT_ADMIN_* do arquivo .env. Não use os valores de exemplo em ambientes compartilhados. As rotas privadas exigem autenticação HTTP Basic com matrícula e senha. Para chamadas que alteram dados, consulte o token CSRF em `GET /api/v1/csrf` e envie o token no cabeçalho indicado pela resposta. Consulte [docs/api.md](docs/api.md) para os detalhes de integração.

Para executar os testes automatizados: `.\mvnw.cmd test`.

## 📊 Status do projeto

🟡 **Backend Spring Boot em implementação** — a API REST, autenticação, inventário, equipes, recebedores, empenhos e devoluções estão implementados. A interface frontend e o provedor de verificação facial continuam planejados.
