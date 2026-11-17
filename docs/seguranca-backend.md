# Revisão de segurança do backend

Esta revisão cobre as rotas REST, autenticação, autorização, entrada de dados, persistência e configuração do backend. As proteções abaixo foram aplicadas no código; os pontos em “Limites e trabalho restante” ainda dependem da implantação ou de integrações futuras.

## Riscos avaliados e controles aplicados

| Área | Controle no projeto |
| --- | --- |
| Acesso a dados de outro setor (BOLA/IDOR) | Consultas e alterações recebem o setor da conta autenticada e filtram recursos pelo setor; IDs enviados pelo cliente não concedem acesso por si só. |
| Senhas e enumeração de contas | Senhas usam PBKDF2-HMAC-SHA256, salt aleatório e 600.000 iterações; hashes legados de menor custo são atualizados após autenticação válida. Falhas de matrícula, senha, bloqueio ou conta inativa retornam a mesma resposta `401`, sem desafio Basic. Contas já bloqueadas são rejeitadas sem repetir o cálculo caro do hash. |
| Tentativas repetidas | Cinco falhas em cinco minutos bloqueiam temporariamente a conta por cinco minutos. Os contadores de login e PIN são persistidos. |
| PIN de recebedor | PIN de seis dígitos nunca é devolvido pela API nem salvo em texto puro; tentativas incorretas usam a mesma política temporária de bloqueio. |
| CSRF e CORS | CSRF continua habilitado, com token em cookie e cabeçalho. CORS permite apenas origens configuradas em `SICOMAT_CORS_ALLOWED_ORIGINS`; não há origem curinga. |
| Permissões por função | Os endpoints separam administrador do sistema, administrador do estoque e operador do estoque, além da validação de perfil nos serviços que registram movimentações. |
| Alteração de estado e concorrência | Empenho, confirmação, cancelamento, devolução e extravio são transacionais. Itens são bloqueados durante a movimentação e reservas impedem que o mesmo item seja empenhado simultaneamente. Devolução duplicada é impedida. |
| Atribuição indevida de propriedades | A API recebe DTOs específicos em vez de entidades JPA; setor, perfil e situação do item não são aceitos como autoridade do cliente. As respostas não incluem hash de senha nem de PIN. |
| Entrada e consumo de recursos | Validações de campos, lotes de até 50 itens, páginas de até 100 resultados, cabeçalhos HTTP limitados a 8 KB e pool de banco limitado. |
| Configuração e vazamento de erros | Banco não possui credenciais padrão no arquivo principal; segredos vêm de variáveis de ambiente. Mensagens internas, erros de validação do servidor e stack traces não são expostos como resposta padrão. PostgreSQL no Compose publica a porta somente em `127.0.0.1`. |
| Biometria | O provedor facial não foi integrado. A API não coleta, armazena nem afirma validar imagens faciais; a confirmação atual usa PIN e justificativa. |

## Limites e trabalho restante

- HTTP Basic envia credenciais em cada requisição e não substitui HTTPS. A publicação deve usar HTTPS em todas as rotas e encerrar TLS em um servidor ou proxy confiável.
- O bloqueio por conta reduz tentativas, mas também pode ser abusado para bloquear um usuário legítimo. Antes de expor a API à internet, configure limitação de requisições por IP no gateway ou proxy reverso, sem confiar em `X-Forwarded-For` vindo diretamente da internet.
- A autenticação do frontend usa credenciais Basic por chamada nesta versão. O frontend deve mantê-las apenas em memória e nunca gravá-las em armazenamento persistente do navegador. Para produção, prefira um backend-for-frontend com sessão segura ou um provedor de identidade OAuth/OIDC.
- As credenciais de banco e o administrador inicial precisam ser fornecidos por secret manager ou variáveis de ambiente protegidas em produção. `.env` é somente para desenvolvimento local e não deve ser versionado.
- A política de senha exige no mínimo 15 caracteres nas rotas de criação e redefinição de conta. Os limites de frequência e de tamanho de requisição devem ser revistos quando a API for publicada atrás de gateway.
- O teste de integração usa H2 e exercita autenticação, CSRF, CORS, isolamento por setor e o ciclo de empenho/devolução. Ele não substitui uma execução de implantação com PostgreSQL e as migrações Flyway antes de publicar uma versão.
- Não existe trilha de auditoria inviolável nem provedor facial nesta etapa. As operações são persistidas no banco, mas a retenção, backup, acesso de administradores do banco e proteção contra alteração externa precisam ser definidas na implantação.

## Referências de segurança

- [OWASP API Security Top 10 2023](https://api-security.owasp.org/editions/2023/en/0x11-t10/)
- [OWASP Password Storage Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html)
- [OWASP Authentication Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html)
- [OWASP REST Security Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/REST_Security_Cheat_Sheet.html)
- [Spring Security CSRF](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html)
- [Spring Security CORS](https://docs.spring.io/spring-security/reference/servlet/integrations/cors.html)
