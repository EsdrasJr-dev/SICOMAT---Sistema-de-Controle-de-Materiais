# SICOMAT

### Sistema de Controle de Materiais

Aplicação para registrar e acompanhar materiais sob responsabilidade de equipes durante o serviço de dia e na reserva de armas.

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

- Cadastro de administradores e operadores do estoque pelo administrador do sistema.
- Cadastro, edição e inativação de materiais e itens físicos pelo administrador do estoque.
- Registro de empenho de itens para uma equipe pelo operador do estoque, com identificação dos responsáveis e da movimentação.
- Registro de devoluções totais ou parciais, inclusive quando ocorrem em horários diferentes.
- Registro de trocas de itens durante o turno.
- Autenticação facial consentida do recebedor como regra do MVP; PIN como exceção quando a facial não puder ser concluída, com justificativa registrada.
- Bloqueio temporário após cinco falhas de senha ou PIN em cinco minutos; a credencial é liberada automaticamente após cinco minutos.
- Painel de conferência com itens disponíveis, empenhados e ainda não devolvidos.
- Histórico das movimentações para consulta e apoio a conferências e auditorias.

## 🌐 Possíveis aplicações em outros setores

Embora tenha surgido para atender ao serviço de dia da Polícia Militar, a proposta pode ser adaptada a organizações civis que entregam equipamentos compartilhados a equipes externas. Empresas de manutenção elétrica, telecomunicações, construção e outros serviços de campo também precisam acompanhar ferramentas e materiais retirados, devolvidos ou substituídos ao longo do trabalho. Nesses contextos, o controle poderia ser associado, por exemplo, a uma ordem de serviço, veículo ou local de atendimento.

## 🧰 Tecnologias

| Área | Tecnologias |
| --- | --- |
| Backend | Java 21, Spring Boot 3.5.16, API REST, Spring Security, Spring Data JPA e Flyway |
| Interface | A interface Thymeleaf + Bootstrap permanece planejada; o backend expõe API REST |
| Banco de dados | PostgreSQL 17 |

A arquitetura e o escopo atual da integração estão descritos em [docs/arquitetura.md](docs/arquitetura.md).

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

1. Copie .env.example para .env e defina uma senha forte em SICOMAT_ADMIN_SENHA.
2. Inicie o PostgreSQL: docker compose up -d database.
3. Inicie a aplicação: .\mvnw.cmd spring-boot:run.
4. Confira http://localhost:8080/api/v1/status.

Na primeira inicialização, a aplicação cria a conta única de administrador do sistema a partir das variáveis SICOMAT_ADMIN_* do arquivo .env. Não use os valores de exemplo em ambientes compartilhados. As demais rotas exigem autenticação HTTP Basic com matrícula e senha.

## 📊 Status do projeto

🟡 **Backend Spring Boot em implementação** — a base da API, autenticação e inventário está integrada; a persistência dos fluxos de movimentação, a interface e o provedor facial ainda serão implementados.
