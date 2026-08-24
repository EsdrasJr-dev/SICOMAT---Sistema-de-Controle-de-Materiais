# SICOMAT — Proposta de Projeto

## 📋 Informações Básicas

### Nome do Projeto
SICOMAT — Sistema de Controle de Materiais

### Equipe

| Nome     | GitHub | Papel Principal |
|----------|--------|------------------|
| [Esdras] | @EsdrasJr-dev | Backend |
| [Nome 2] | @username2 | Banco de Dados |
| [Nome 3] | @username3 | Telas / Front |

### Link do Repositório
https://github.com/EsdrasJr-dev/SICOMAT---Sistema-de-Controle-de-Materiais

---

## 🎯 Identificação do Problema

### 1. Descrição do Problema
No serviço de dia/reserva de armas da companhia, o policial escalado é responsável por distribuir e receber materiais das equipes — tonfas, HT (rádio), TDP (terminal portátil de dados), suporte de câmeras, câmeras, tasers e armamentos, entre outros. Todo esse controle é feito manualmente em cadernos.

- **Quem enfrenta esse problema?** O policial do serviço de dia, responsável por controlar a entrada e saída de materiais, e a seção P4, responsável pela gestão desses materiais.
- **Quando ocorre?** Em toda troca de plantão e em toda movimentação de equipamento entre equipes.
- **Por que é importante?** O controle manual não garante confiabilidade sobre a quantidade e localização real dos materiais.
- **Impacto negativo:** Quando um material é extraviado, isso resulta em sindicâncias e processos administrativos para os envolvidos, além do prejuízo material para a instituição.

### 2. Pesquisa de Soluções Existentes

| Solução Existente | Limitações | Como o SICOMAT é diferente |
|---|---|---|
| Sistemas genéricos de controle de patrimônio | Não contemplam o fluxo específico de retirada/devolução por plantão, nem a separação de perfis (quem cadastra x quem movimenta) | Modelado especificamente para o fluxo do serviço de dia, com perfis de acesso dedicados |
| Controle em planilhas (Excel) | Sem histórico auditável nem controle de acesso, fácil de haver erro humano ou perda do arquivo | Registro estruturado em banco de dados, com histórico de quem retirou/devolveu cada item |
| Caderno físico (situação atual) | Sem busca, sem rastreabilidade, sujeito a rasura/perda | Digital, consultável, com painel do que está em aberto em tempo real |

### 3. Público-Alvo
- **Idade:** 18-45 anos (faixa etária dos policiais em serviço ativo)
- **Perfil:** Policiais militares em serviço na companhia — tanto o efetivo do serviço de dia quanto o pessoal da seção P4
- **Necessidades específicas:**
  - Registrar retirada e devolução de material rapidamente, sem burocracia
  - Consultar o que está em aberto (não devolvido) a qualquer momento
  - Garantir que só a P4 possa alterar o cadastro de materiais
- **Conhecimento tecnológico:** Iniciante a intermediário — a interface precisa ser simples e direta

---

## 💡 Solução Proposta

### 1. Descrição da Solução
O SICOMAT é um sistema web que digitaliza o controle de materiais do serviço de dia, substituindo o caderno físico. Ele define dois perfis de acesso: a **seção P4**, responsável por cadastrar, editar e excluir materiais do sistema; e o **policial do serviço de dia**, responsável por registrar as movimentações — retirada e devolução de cada item, associadas a data, hora e responsável. Um painel de conferência mostra, em tempo real, quais materiais estão em aberto (retirados e ainda não devolvidos), permitindo identificar rapidamente pendências antes que se tornem um problema.

### 2. Funcionalidades Principais (MVP)

- [ ] **Cadastro de Materiais** (perfil P4)
  - Descrição: cadastrar, editar e excluir materiais (tipo, identificador/patrimônio, status)
  - Valor para usuário: mantém o inventário de materiais sempre atualizado e confiável

- [ ] **Registro de Movimentação** (perfil Policial de Serviço)
  - Descrição: registrar a retirada de um material (quem retirou, quando) e sua devolução (quem devolveu, quando)
  - Valor para usuário: substitui o caderno físico com um registro estruturado e rastreável

- [ ] **Painel de Conferência**
  - Descrição: lista de materiais atualmente "em aberto" (retirados e não devolvidos)
  - Valor para usuário: permite conferência rápida do que está pendente, sem precisar vasculhar páginas de caderno

### 3. Funcionalidades Futuras (Pós-MVP)
- [ ] Relatórios de uso por período/equipe
- [ ] Alertas automáticos para materiais em aberto há muito tempo
- [ ] Controle de manutenção/estado de conservação dos materiais

---

## 🛠️ Especificações Técnicas

### 1. Arquitetura do Sistema
```
┌─────────────────────────┐
│      FRONTEND            │
│   Thymeleaf + Bootstrap  │
└───────────┬──────────────┘
            │
            ↓ HTTP
┌─────────────────────────┐
│      BACKEND              │
│   Java + Spring Boot     │
└───────────┬──────────────┘
            │
            ↓ SQL
┌─────────────────────────┐
│      DATABASE             │
│   PostgreSQL              │
└─────────────────────────┘
```

### 2. Stack Tecnológica

| Camada | Tecnologia | Justificativa |
|---|---|---|
| Frontend | Thymeleaf + Bootstrap | Integra nativamente com Spring Boot (sem API separada nem build próprio) e permite telas com visual profissional usando classes prontas — equilíbrio entre simplicidade e um resultado visual decente para uma equipe iniciante |
| Backend | Java + Spring Boot | Alinhado com o foco de estudo da equipe em Java/back-end; framework maduro, com grande documentação e suporte a autenticação pronta (Spring Security) |
| Banco de Dados | PostgreSQL | Banco relacional robusto e gratuito, adequado ao modelo de dados (materiais, movimentações, usuários) |
| Hospedagem | Railway / Render (a definir) | Opções gratuitas para hospedar aplicações Java, facilitam a demonstração |
| Outras ferramentas | Spring Security | Controle de autenticação e dos dois perfis de acesso (P4 x Policial de serviço) |

### 3. Requisitos Não-Funcionais
- **Performance:** operações de registro de movimentação devem responder em menos de 2 segundos
- **Segurança:** autenticação obrigatória, senhas armazenadas com hash, controle de acesso por perfil (P4 x Operador)
- **Usabilidade:** interface simples, adequada a usuários com conhecimento tecnológico básico/intermediário
- **Escalabilidade:** suportar o volume de materiais e movimentações de uma companhia (dezenas de itens, múltiplos registros diários)

---

## 📅 Planejamento

### Cronograma de 3 Meses

**Mês 1: Fundação (Semanas 1-4)**

*Semana 1-2 — Setup e Planejamento*
- [x] Configurar repositório GitHub
- [x] Definir estrutura de pastas
- [ ] Configurar ambiente de desenvolvimento (Java + Spring Boot + PostgreSQL)
- [ ] Criar wireframes das telas por perfil (P4 x Operador)
- [ ] Definir schema do banco de dados (Material, Movimentacao, Usuario)

*Semana 3-4 — Funcionalidade Base*
- [ ] Implementar autenticação e os dois perfis de acesso (Spring Security)
- [ ] Criar estrutura do banco de dados
- [ ] Desenvolver telas principais com Thymeleaf + Bootstrap (sem funcionalidades ainda)

**Mês 2: Desenvolvimento (Semanas 5-8)**

*Semana 5-6 — Funcionalidades Core*
- [ ] Implementar Cadastro de Materiais (perfil P4)
- [ ] Implementar Registro de Movimentação (perfil Operador)
- [ ] Testes básicos

*Semana 7-8 — Funcionalidades Adicionais*
- [ ] Implementar Painel de Conferência
- [ ] Refinamento de interface (Bootstrap)
- [ ] Tratamento de erros e validações

**Mês 3: Finalização (Semanas 9-12)**

*Semana 9-10 — Refinamento*
- [ ] Correção de bugs
- [ ] Testes com usuários reais (colegas do serviço de dia / P4)
- [ ] Ajustes baseados em feedback

*Semana 11 — Documentação*
- [ ] README completo
- [ ] Guia de instalação
- [ ] Manual do usuário

*Semana 12/15 — Apresentação*
- [ ] Preparar slides
- [ ] Gravar demo de backup
- [ ] Ensaiar apresentação
- [ ] Deploy final 

### Divisão de Responsabilidades

| Membro   | Responsabilidade Principal | Responsabilidade Secundária |
|----------|---|---|
| [Esdras] | Backend (Spring Boot) | Autenticação e perfis de acesso |
| [Nome 2] | Banco de Dados | Testes |
| [Nome 3] | Telas / Front (Thymeleaf + Bootstrap) | Documentação |

---

## 📊 Métricas de Sucesso

- [ ] **Funcional:** as 3 funcionalidades do MVP implementadas e funcionando (cadastro, movimentação, painel de conferência)
- [ ] **Usabilidade:** validado com pelo menos 2-3 colegas reais do serviço de dia/P4
- [ ] **Performance:** tempo de resposta abaixo de 2 segundos nas operações principais
- [ ] **Código:** sem bugs críticos, organizado
- [ ] **Documentação:** README completo, permitindo que outra pessoa configure o projeto
- [ ] **GitHub:** 20+ commits bem distribuídos ao longo dos 3 meses, issues organizadas por perfil

---

## 🎨 Design e Experiência do Usuário

### 1. Fluxo Principal do Usuário

**Fluxo P4:**
1. Login no sistema
2. Acessa o cadastro de materiais
3. Adiciona/edita/exclui um item (tipo, identificador, status)

**Fluxo Policial do Serviço de Dia:**
1. Login no sistema
2. Registra a retirada de um material por uma equipe
3. Ao final do turno, registra a devolução
4. Consulta o painel de conferência para ver o que ainda está em aberto

### 2. Wireframes/Protótipos
`docs/wireframes/` 

### 3. Princípios de Design
- **Simplicidade:** telas objetivas, sem etapas desnecessárias para registrar uma movimentação rápida durante o plantão
- **Acessibilidade:** contraste adequado (aproveitando os componentes padrão do Bootstrap), textos claros
- **Responsividade:** funcional em desktop e tablet (uso na sala do serviço de dia), com o grid responsivo do Bootstrap
- **Feedback visual:** confirmação clara ao registrar retirada/devolução (alerts do Bootstrap)

---

## 🔒 Considerações de Segurança

- [x] Autenticação segura (senhas hasheadas via Spring Security)
- [x] Controle de acesso por perfil (P4 x Operador) — nenhum dos dois perfis acessa funções do outro
- [ ] Proteção contra SQL Injection (uso de JPA/Hibernate com queries parametrizadas)
- [ ] Validação de dados de entrada
- [ ] HTTPS obrigatório em produção (se hospedado)
- [x] Uso de dados fictícios/anonimizados na demonstração e no repositório público, evitando expor nomes reais de policiais ou identificação real de armamentos

---

## 🌍 Impacto Social Esperado

### 1. Benefícios Diretos
Redução de extravios de material por falta de controle, diminuição do número de sindicâncias e processos administrativos decorrentes de divergências no controle manual, e mais agilidade na troca de plantão.

### 2. Potencial de Escala
Inicialmente pensado para uma companhia, o sistema poderia ser adaptado para outras companhias da Polícia Militar com o mesmo tipo de serviço de dia/reserva de armas, e futuramente integrado a sistemas administrativos maiores da corporação.

### 3. Sustentabilidade
- [ ] Manutenção pela própria equipe após a disciplina
- [ ] Possível adoção institucional formal, caso validado pela companhia

---

## 📚 Referências

### Pesquisa do Problema
- Vivência direta do problema no serviço de dia/reserva de armas da companhia
- Conversas com colegas que atuam no serviço de dia e na seção P4

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
