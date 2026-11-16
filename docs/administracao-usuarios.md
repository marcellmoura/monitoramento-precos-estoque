# Administração de usuários

Incremento de US-31/US-33 e da revogação administrativa prevista em US-30. A autorização dos futuros módulos de negócio continua transversal na US-34.

## Uso

Crie o primeiro Administrador e entre com senha definitiva conforme [acesso](acesso.md). No início, selecione **Administrar usuários**. A página lista nome, e-mail, perfil, estado e necessidade de troca de senha, com paginação. Use **Cadastrar usuário** para informar nome, e-mail, perfil e senha temporária de pelo menos 12 caracteres; confirme a senha.

Em **Administrar**, altere perfil/estado ou redefina a senha temporária. Alterações reais de perfil/estado e redefinição encerram todas as sessões da conta afetada. Se a conta afetada for a própria, será necessário entrar novamente. Repetir os mesmos valores de acesso preserva a sessão. Uma conta inativa permanece inativa após redefinir a senha; a ativação é uma operação explícita.

É obrigatório manter ao menos um Administrador ativo. Tentativas de desativar ou rebaixar o último são recusadas, inclusive quando dois administradores tentam alterar seus acessos simultaneamente. O frontend explica a restrição; a API aplica a regra.

A senha temporária deve ser comunicada ao titular por um meio apropriado, fora do sistema. Não há envio por e-mail ou cadastro público. Ela não é devolvida pela API, exibida após a gravação ou armazenada pela aplicação no navegador. Após usar a temporária, a conta precisa trocar a senha antes de acessar funções de negócio.

## Contratos HTTP

Todas as rotas abaixo exigem Administrador com senha definitiva. Escritas também exigem CSRF.

| Método | Rota | Entrada / resultado |
|---|---|---|
| GET | `/api/usuarios?pagina=0&tamanho=20` | `itens`, `pagina`, `tamanho`, `total`, `paginas`; ordenação estável por ID |
| GET | `/api/usuarios/{id}` | Conta para administração |
| POST | `/api/usuarios` | `nome`, `email`, `perfil`, `senhaTemporaria`; 201 e Location da conta |
| PATCH | `/api/usuarios/{id}/acesso` | `perfil`, `ativo`; 200 com conta atualizada |
| POST | `/api/usuarios/{id}/redefinir-senha` | `senhaTemporaria`; 204 |

A resposta de conta inclui somente `id`, `nome`, `email`, `perfil`, `ativo` e `trocaSenhaObrigatoria`. Nome: até 120 caracteres, obrigatório após retirar espaços nas extremidades. E-mail: válido, até 254 caracteres e único sem diferenciar caixa; salvo normalizado. Um dos quatro perfis AP15 é obrigatório. Contas novas começam ativas e com troca de senha obrigatória. Página começa em zero; tamanho padrão 20 e máximo 100; parâmetros inválidos retornam 400.

Erros: 400 dados inválidos; 401 sessão ausente/revogada; 403 perfil sem permissão ou troca de senha pendente; 404 conta inexistente; 409 `EMAIL_DUPLICADO` ou `ULTIMO_ADMINISTRADOR`; 500 falha inesperada com identificador. Validação de corpo também informa `campos`, sem repetir valores recebidos. Detalhes de implementação/SQL e credenciais não são enviados ao cliente.

## Transações e histórico

- A autorização HTTP restringe o módulo a Administrador. Dentro das escritas, o ator autenticado é novamente conferido no banco, incluindo estado, perfil, troca pendente e versão de credencial.
- As escritas administrativas compartilham um advisory lock transacional no PostgreSQL, chave interna `170031`, antes de verificar ator, alvo e quantidade de administradores. A decisão sobre o último Administrador fica serializada entre conexões e instâncias. O banco libera o bloqueio ao encerrar a transação.
- Linhas de usuário são bloqueadas para alteração. Mudanças de acesso/reset incrementam a versão da credencial; exclusão das sessões e alteração pertencem à mesma transação. A conferência de versão nas requisições também rejeita uma sessão antiga eventualmente regravada por requisição concorrente.
- A migration V3 cria `historico_acesso_usuario`: identificador, ator, usuário afetado, evento `SENHA_REDEFINIDA` e instante. Não há coluna para senha/hash ou corpo da requisição. O evento é gravado na mesma transação do reset; falha ao registrá-lo reverte senha e revogação de sessões. V1/V2 e dados existentes são preservados.
- A interface adapta navegação, mas a autorização e a filtragem dos campos permanecem no servidor. Compras, Estoque e Consulta não administram contas.

## Verificação

`./mvnw clean verify` no backend executa testes HTTP com servidor real, cookies e PostgreSQL Testcontainers: quatro perfis, criação/consulta sem senha/hash, duplicidade/validação/paginação, CSRF, ativação/desativação, perfil, revogação, reset e troca obrigatória, histórico e rollback. O caso concorrente inicia duas alterações de administradores em clientes distintos e verifica uma aprovação, uma recusa e a preservação de um Administrador ativo.

Os testes React verificam listagem/paginação, formulários, confirmação/limpeza de senha, erros, navegação por perfil e retorno ao login após alteração da própria conta. Comandos em [ambiente](ambiente.md). O smoke Chromium continua uma verificação da interface; a integração real com API/banco é verificada separadamente.

Referências: [autorização HTTP do Spring Security](https://docs.spring.io/spring-security/reference/servlet/authorization/authorize-http-requests.html) e [bloqueios do PostgreSQL](https://www.postgresql.org/docs/18/explicit-locking.html#ADVISORY-LOCKS).

Validação local em 16/11/2026: 6 testes unitários e 48 de integração backend, 33 testes frontend, lint/build e smoke Chromium aprovados. Fluxo React/API/PostgreSQL real exercitado com banco temporário e credenciais sintéticas: cadastro, restrições por perfil, mudança/revogação, reset/troca, ativação/desativação e bloqueio do último Administrador. Telas desktop/móvel conferidas. CI remota e revisão do incremento pendentes.
