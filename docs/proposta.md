# SICOMAT — Proposta de Projeto

## 📋 Informações Básicas

### Nome do Projeto
SICOMAT — Sistema de Controle de Materiais

### Equipe

| Integrante | Área de atuação |
|---|---|
| Rodrigo Felipe | Frontend |
| Vinicius Rodrigo | Frontend |
| Thiago Domingos | Frontend |
| Esdras Batista | Backend e banco de dados |
| Gabriel Rodrigues | Backend e banco de dados |
| Midiane S. Victorino | Backend e banco de dados |
| Jonas Napoleão da Silva Farias | Backend e banco de dados |

### Link do Repositório
https://github.com/EsdrasJr-dev/SICOMAT---Sistema-de-Controle-de-Materiais

---

## 🎯 Identificação do Problema

### 1. Descrição do Problema
A motivação para o SICOMAT surgiu da experiência de um integrante da equipe no serviço de dia da Polícia Militar. Ao assumir o turno, ele perguntou ao cabo que estava transmitindo o serviço qual era a quantidade correta de cada material disponível, mas não foi possível confirmar essa informação com clareza, pois o controle era feito em caderno.

O operador do estoque é responsável por acompanhar continuamente a movimentação dos materiais: empenha itens para as equipes, recebe devoluções em horários diferentes, registra substituições durante o turno e prepara o estoque para a equipe seguinte. O acervo inclui materiais como tonfas, rádios HT, TDPs, suportes e câmeras. Os registros manuais dificultam a consulta da situação de cada item e a reconstrução do histórico. Divergências ou extravios podem exigir apuração administrativa, inclusive por meio de sindicância.

- **Quem enfrenta esse problema?** O Operador do estoque, que registra as entregas e devoluções; o Administrador do estoque, que mantém o cadastro dos materiais; e as equipes que recebem os itens.
- **Quando ocorre?** A cada empenho, devolução parcial ou total, troca de item e passagem de turno.
- **Por que é importante?** É necessário saber quais unidades físicas estão disponíveis, empenhadas ou pendentes de devolução e quem responde por cada movimentação.
- **Impacto negativo:** Registros incompletos ou divergentes podem dificultar a localização de materiais e levar a apurações administrativas, além de gerar prejuízo para a instituição.

### 2. Alternativas de Controle Consideradas

A comparação abaixo é preliminar e considera a adequação das alternativas ao fluxo observado na companhia. Ela não representa uma avaliação de todos os produtos disponíveis no mercado.

| Alternativa | Limitações para o fluxo observado | Como o SICOMAT pretende atender |
|---|---|---|
| Sistemas de controle patrimonial | Podem atender ao cadastro de bens, mas é necessário verificar se também representam o fluxo de empenho, devolução parcial e passagem de turno usado pela companhia. | Priorizar o registro por unidade física e por movimentação, com perfis e painel voltados ao serviço de dia. |
| Planilhas | Podem organizar registros, mas a atualização de disponibilidade e o histórico dependem dos controles e procedimentos configurados pelos usuários. | Centralizar os registros e atualizar a situação dos itens a partir das movimentações registradas. |
| Caderno físico (situação atual) | A consulta e a consolidação do histórico dependem de buscas e conferências manuais. | Oferecer consulta digital de itens disponíveis e pendentes, com histórico de movimentações. |

### 3. Público-Alvo
- **Usuários diretos:** Administrador do sistema, Administrador do estoque e Operador do estoque no serviço de dia.
- **Pessoas impactadas:** integrantes das equipes que recebem e devolvem materiais.
- **Necessidades específicas:**
  - Registrar empenhos, devoluções parciais e trocas com poucos passos.
  - Consultar rapidamente quais itens estão disponíveis e quais permanecem empenhados.
  - Identificar quem registrou a movimentação e quem recebeu ou devolveu cada item.
  - Restringir alterações no cadastro de materiais ao Administrador do estoque.
- **Conhecimento tecnológico:** Iniciante a intermediário — a interface precisa ser simples e direta

---

## 💡 Solução Proposta

### 1. Descrição da Solução
O SICOMAT é um sistema web que digitaliza o controle de materiais do serviço de dia, substituindo os registros em caderno por um histórico consultável. O modelo prevê três perfis: **Administrador do sistema**, que cadastra administradores do estoque; **Administrador do estoque**, que gerencia os materiais e as unidades físicas; e **Operador do estoque**, que registra empenhos, devoluções e trocas, sem alterar o cadastro de itens. Cada movimentação registra a unidade física, a data e a hora, o Operador do estoque responsável pelo registro e a pessoa ou equipe que recebeu ou devolveu o material. Um painel mostra a situação atual dos itens e as pendências de devolução.

| Perfil | Responsabilidade principal |
|---|---|
| Administrador do sistema | Cadastrar usuários com perfil de administrador do estoque. |
| Administrador do estoque | Cadastrar, editar e inativar materiais e unidades físicas. |
| Operador do estoque | Registrar empenhos, devoluções e trocas; consultar a situação dos itens. |

O procedimento para criação das contas de operador será definido durante o detalhamento da autenticação e dos perfis de acesso.

### 2. Funcionalidades Principais (MVP)

- [ ] **Controle de acesso e perfis**
  - Descrição: autenticar usuários e separar as permissões do administrador do sistema, do administrador do estoque e do operador.
  - Valor para usuário: limita cada ação à responsabilidade correspondente.

- [ ] **Cadastro de materiais e unidades físicas** (Administrador do estoque)
  - Descrição: cadastrar e editar tipos de materiais e suas unidades físicas identificadas por patrimônio ou outro identificador; inativar registros quando necessário, preservando o histórico.
  - Valor para usuário: mantém o inventário organizado sem apagar o vínculo com movimentações anteriores.

- [ ] **Empenho, devolução e troca de materiais** (Operador do estoque)
  - Descrição: registrar os itens entregues à equipe, quem os recebeu, quem efetuou o registro e a data e hora da operação. Vincular cada devolução à unidade física e ao empenho original; permitir devoluções em etapas, cada uma com seu próprio horário e responsável. Uma substituição será registrada vinculando a devolução do item antigo ao empenho do item que o substitui.
  - Valor para usuário: acompanha o fluxo real do turno e mantém rastreabilidade por unidade física.

- [ ] **Painel de Conferência**
  - Descrição: mostrar itens disponíveis, empenhados e ainda não devolvidos, com consulta por equipe ou responsável.
  - Valor para usuário: permite conferir pendências e preparar o estoque para a próxima equipe sem buscar registros manualmente.

### 3. Funcionalidades Futuras (Pós-MVP)
- [ ] Relatórios de uso por período/equipe
- [ ] Alertas automáticos para materiais em aberto há muito tempo
- [ ] Controle de manutenção/estado de conservação dos materiais

---

## 🛠️ Especificações Técnicas

### 1. Arquitetura do Sistema
```
┌───────────────────────────────┐
│ Usuário em um navegador       │
└──────────────┬────────────────┘
               │ HTTP
               ▼
┌────────────────────────────────────────┐
│ Aplicação Java + Spring Boot           │
│ Spring MVC + Spring Security           │
│ Thymeleaf (telas) + Bootstrap (estilo) │
└───────────────────┬────────────────────┘
                    │ JPA / SQL
                    ▼
┌───────────────────────────────┐
│ PostgreSQL                    │
└───────────────────────────────┘
```

### 2. Stack Tecnológica

| Camada | Tecnologia | Justificativa |
|---|---|---|
| Interface web | Thymeleaf + Bootstrap | Thymeleaf renderiza as páginas no servidor Spring Boot, e Bootstrap fornece componentes e estilos responsivos; não é necessária uma aplicação frontend separada para o MVP. |
| Aplicação | Java + Spring Boot | Alinhado ao foco de estudo da equipe e permite organizar rotas, regras de negócio e integração com o banco. |
| Persistência | Spring Data JPA / Hibernate | Facilita o mapeamento entre as entidades do domínio e as tabelas relacionais; as consultas devem ser parametrizadas. |
| Segurança | Spring Security | Fornece mecanismos de autenticação e autorização por perfil, a serem configurados para as três responsabilidades do sistema. |
| Banco de Dados | PostgreSQL | Banco relacional adequado ao cadastro de materiais, unidades físicas, usuários e movimentações. |
| Hospedagem | Railway / Render (a definir) | Opções gratuitas para hospedar aplicações Java, facilitam a demonstração |

### 3. Requisitos Não-Funcionais
- **Performance:** buscar resposta de até 2 segundos para as operações principais no ambiente de demonstração, sob condições de teste documentadas.
- **Segurança:** exigir autenticação, armazenar senhas com hash e aplicar autorização por perfil; não incluir credenciais, dados pessoais reais ou identificadores operacionais no repositório público.
- **Rastreabilidade:** registrar autor, data e hora e os itens envolvidos em cada empenho, devolução e substituição.
- **Usabilidade:** oferecer uma interface simples, com poucos passos para as operações frequentes do operador.
- **Escalabilidade:** atender ao volume previsto para uma companhia, com dezenas de itens e múltiplos registros diários.

---

## 📅 Planejamento

### Cronograma de 3 Meses

**Mês 1: Fundação (Semanas 1-4)**

*Semana 1-2 — Setup e Planejamento*
- [x] Configurar repositório GitHub
- [x] Definir estrutura de pastas
- [ ] Configurar ambiente de desenvolvimento (Java + Spring Boot + PostgreSQL)
- [ ] Definir os três perfis, as permissões e quem poderá cadastrar cada tipo de usuário
- [ ] Criar wireframes das telas do administrador do sistema, administrador do estoque e operador
- [ ] Definir o modelo de dados (Material, ItemMaterial, Usuario, Equipe, Empenho e Devolucao)

*Semana 3-4 — Funcionalidade Base*
- [ ] Implementar autenticação e autorização dos três perfis (Spring Security)
- [ ] Criar a estrutura do banco de dados e as relações de histórico
- [ ] Desenvolver telas principais com Thymeleaf + Bootstrap (sem funcionalidades ainda)

**Mês 2: Desenvolvimento (Semanas 5-8)**

*Semana 5-6 — Funcionalidades Core*
- [ ] Implementar cadastro, edição e inativação de materiais e unidades físicas (Administrador do estoque)
- [ ] Implementar empenhos, devoluções parciais e substituições (Operador do estoque)
- [ ] Verificar autorização por perfil e rastreabilidade das operações

*Semana 7-8 — Funcionalidades Adicionais*
- [ ] Implementar Painel de Conferência
- [ ] Refinamento de interface (Bootstrap)
- [ ] Tratamento de erros e validações
- [ ] Testes dos fluxos de empenho, devolução em etapas e troca de item

**Mês 3: Finalização (Semanas 9-12)**

*Semana 9-10 — Refinamento*
- [ ] Correção de bugs
- [ ] Testes com usuários reais (colegas que atuam como Operador do estoque e Administrador do estoque)
- [ ] Ajustes baseados em feedback

*Semana 11 — Documentação*
- [ ] README completo
- [ ] Guia de instalação
- [ ] Manual do usuário

*Semana 12 — Apresentação*
- [ ] Preparar slides
- [ ] Gravar demo de backup
- [ ] Ensaiar apresentação
- [ ] Deploy final 

### Divisão de Responsabilidades

| Membro | Área de atuação |
|---|---|
| Rodrigo Felipe | Frontend (Thymeleaf + Bootstrap) |
| Vinicius Rodrigo | Frontend (Thymeleaf + Bootstrap) |
| Thiago Domingos | Frontend (Thymeleaf + Bootstrap) |
| Esdras Batista | Backend e banco de dados |
| Gabriel Rodrigues | Backend e banco de dados |
| Midiane S. Victorino | Backend e banco de dados |
| Jonas Napoleão da Silva Farias | Backend e banco de dados |

---

## 📊 Métricas de Sucesso

- [ ] **MVP:** perfis de acesso, cadastro de materiais/unidades físicas, empenhos, devoluções parciais, substituições e painel funcionando no fluxo demonstrado.
- [ ] **Rastreabilidade:** cada movimentação demonstra o item, quem registrou, quem recebeu ou devolveu e a data e hora.
- [ ] **Usabilidade:** validar as tarefas principais com 2 a 3 usuários que atuem como Operador do estoque e Administrador do estoque e registrar os ajustes necessários.
- [ ] **Performance:** medir o tempo das operações principais no ambiente de demonstração e comparar com a meta de até 2 segundos.
- [ ] **Qualidade:** verificar os fluxos principais e as permissões de cada perfil, sem falhas críticas conhecidas.
- [ ] **Documentação:** README e guia de instalação permitem que outra pessoa configure e execute o projeto.

---

## 🎨 Design e Experiência do Usuário

### 1. Fluxo Principal do Usuário

**Administrador do sistema:**
1. Login no sistema
2. Cadastra usuários com perfil de administrador do estoque

**Administrador do estoque:**
1. Login no sistema
2. Cadastra e edita tipos de materiais e unidades físicas identificadas
3. Inativa registros que não devem mais ser usados, preservando o histórico

**Operador do estoque:**
1. Login no sistema
2. Registra o empenho dos itens para uma equipe e identifica quem os recebe
3. Registra cada devolução quando ocorre, inclusive devoluções parciais em horários diferentes
4. Em caso de substituição, registra a devolução do item antigo e o empenho do substituto
5. Consulta o painel para conferir itens disponíveis e pendências antes da passagem de turno

Em cada movimentação, o sistema distingue o operador autenticado que registra a ação da pessoa ou equipe que recebe ou devolve os materiais. A forma de criação das contas de operador será definida antes da implementação dos perfis.

### 2. Wireframes/Protótipos
`docs/wireframes/` 

### 3. Princípios de Design
- **Simplicidade:** telas objetivas, sem etapas desnecessárias para registrar uma movimentação rápida durante o plantão
- **Acessibilidade:** contraste adequado (aproveitando os componentes padrão do Bootstrap), textos claros
- **Responsividade:** funcional em desktop e tablet (uso na sala do serviço de dia), com o grid responsivo do Bootstrap
- **Feedback visual:** confirmação clara ao registrar retirada/devolução (alerts do Bootstrap)

---

## 🔒 Considerações de Segurança

- A autenticação será obrigatória, com senhas armazenadas por meio de hash seguro.
- A autorização será aplicada de acordo com os três perfis. O operador não poderá cadastrar, editar ou inativar materiais e unidades físicas.
- As consultas ao banco serão parametrizadas e os dados recebidos serão validados.
- O ambiente de produção usará HTTPS, caso o sistema seja hospedado publicamente.
- A demonstração e o repositório público usarão dados fictícios ou anonimizados. Não serão publicados nomes reais, patrimônios, identificadores de armamentos ou outros detalhes operacionais da instituição.

---

## 🌍 Impacto Social Esperado

### 1. Benefícios Diretos
Espera-se melhorar a rastreabilidade dos materiais, facilitar a localização de pendências e dar mais agilidade à passagem de turno. Um histórico organizado pode ajudar a reduzir divergências e apoiar apurações administrativas quando necessárias; o sistema, por si só, não garante a eliminação de extravios ou procedimentos.

### 2. Potencial de Escala
O escopo inicial é o serviço de dia/reserva de armas de uma companhia. Em uma etapa posterior, o sistema poderia ser adaptado a outras companhias com fluxo semelhante e, futuramente, integrado a sistemas administrativos da corporação. Como possibilidade de expansão além da instituição, o modelo de controle também poderia ser adaptado a empresas civis com equipes de campo — por exemplo, manutenção elétrica, telecomunicações ou construção — associando os materiais a uma ordem de serviço, veículo ou local. Essa adaptação não faz parte do MVP.

### 3. Sustentabilidade
- [ ] Manutenção pela própria equipe após a disciplina
- [ ] Possível adoção institucional formal, caso validado pela companhia

---

## 📚 Referências

### Pesquisa do Problema
- Vivência direta do problema no serviço de dia/reserva de armas da companhia
- Conversas com colegas que atuam como Operador do estoque e Administrador do estoque

### Referências Técnicas
- Documentação oficial do Spring Boot
- Documentação oficial do Spring Security
- Documentação oficial do Bootstrap

### Literatura Acadêmica
- SOMMERVILLE, Ian. **Engenharia de Software**. 10ª ed. Pearson, 2018.
- MARTIN, Robert C. **Código Limpo: Habilidades Práticas do Agile Software**. Alta Books, 2009.

---

## ✅ Aprovação

### Checklist de Validação da Proposta
- [x] Problema claramente definido e justificado
- [x] Solução viável tecnicamente em 3 meses
- [x] Público-alvo identificado
- [x] Funcionalidades MVP bem definidas
- [x] Stack tecnológica escolhida e justificada
- [x] Cronograma realista
- [x] Divisão de tarefas entre membros
- [x] Repositório GitHub criado
- [x] Impacto social claro
- [x] Referências incluídas

### Revisão do Professor
- [ ] Proposta aprovada
- [ ] Proposta aprovada com ressalvas (ver comentários)
- [ ] Proposta necessita revisão

**Comentários do Professor:**
_[feedback]_

## 📝 Notas e Observações
_[]_
