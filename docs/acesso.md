# Autenticação e primeiro acesso

Fluxo implementado para US-29 e US-32; logout e expiração da US-30 implementados. A administração/reset de usuários e a matriz de permissões dos módulos de negócio seguem pendentes. Não há cadastro público.

## Criar o primeiro administrador

Prepare o PostgreSQL e exporte as variáveis conforme [persistência](persistencia.md). Na raiz do projeto, com Java 25 e Docker disponíveis:

```sh
cd backend
./mvnw clean verify
java -jar target/monitoramento-0.1.0.jar --inicializar-admin
```

Execute o JAR em terminal interativo. Informe nome, e-mail e senha temporária de pelo menos 12 caracteres; confirme a senha. A senha não aparece na tela e não é recebida por argumento, arquivo ou endpoint. O comando aplica as migrations, cria a conta e termina sem abrir servidor HTTP. Somente bases sem usuários aceitam essa operação; repetir não sobrescreve nem acrescenta contas. Um bloqueio no PostgreSQL protege contra inicializações simultâneas. Não executar builds sobre o JAR enquanto ele estiver em uso.

Em seguida, na mesma pasta/ambiente:

```sh
java -jar target/monitoramento-0.1.0.jar
```

Em outro terminal, execute `npm ci` e `npm run dev` em `frontend/`. Abra `http://127.0.0.1:5173`, entre com a credencial criada e troque a senha temporária. A nova senha deve ter no mínimo 12 caracteres e ser diferente da atual. Depois da troca, todas as sessões desse usuário são encerradas; entre novamente com a senha definitiva. A página inicial mostra nome, perfil e saída.

## API e sessão

| Método | Rota | Entrada / resultado |
|---|---|---|
| GET | `/api/auth/csrf` | Retorna `token` e `headerName`; enviar o token no cabeçalho indicado nas operações de escrita |
| POST | `/api/auth/login` | JSON `email`, `senha`; retorna identidade e cria sessão |
| GET | `/api/auth/me` | Retorna `id`, `nome`, `email`, `perfil`, `trocaSenhaObrigatoria` |
| PUT | `/api/auth/senha` | JSON `senhaAtual`, `novaSenha`; sucesso 204 e encerramento das sessões |
| POST | `/api/auth/logout` | Sucesso 204; invalida sessão e limpa cookie |

A API devolve erros JSON com `codigo`, `mensagem` e `identificador`. Login inválido ou inativo usa a mesma mensagem genérica. Dados inválidos retornam 400; ausência de sessão válida, 401; CSRF inválido/ausente e bloqueio de primeiro acesso, 403. Método e formato incorretos retornam 405/415. Falhas internas retornam mensagem genérica e identificador; não expõem stack trace ou entrada sensível.

- Spring Security 7.1.2 e Spring Session 4.1.1, gerenciados pelo Boot 4.1.2; Bouncy Castle `bcprov-jdk18on` 1.87 fixado no POM.
- Argon2id com salt aleatório de 16 bytes, hash de 32 bytes, memória 64 MiB, paralelismo 1 e 3 iterações. Medição local de três verificações: média aproximada de 135 ms; custo depende do hardware e deve ser reavaliado no ambiente de implantação. Nunca armazenar a senha em texto puro.
- Sessão no PostgreSQL via migration V2; não usar inicialização automática paralela do schema. V1 permanece intacta. Hibernate valida o modelo.
- Cookie `SESSION`, Path `/`, HttpOnly e SameSite=Lax. `SESSION_COOKIE_SECURE=true` é obrigatório ao servir por HTTPS; o padrão falso atende somente ao desenvolvimento HTTP local. Backend não habilita CORS: frontend usa `/api` na mesma origem, com proxy Vite local.
- CSRF é obtido antes de cada escrita, inclusive login/logout. Login troca o identificador da sessão e renova a proteção CSRF. Não armazenar senha ou token em localStorage.
- Expiração após 30 minutos sem requisição autenticada; não há polling para prolongar a sessão.
- A credencial temporária permite somente consulta da própria identidade, CSRF, troca e logout. A API aplica o bloqueio, além da navegação React.
- Troca de senha e exclusão das sessões participam da mesma transação. Uma versão de credencial por usuário é conferida nas requisições, impedindo reaproveitamento de sessão antiga mesmo em uma disputa com gravação de sessão. O principal serializado contém identificação/versão/perfil, sem senha ou hash.

## Verificação

`./mvnw test` verifica confirmação, entrada ausente e limpeza dos buffers de senha do comando local, inclusive em falhas. `./mvnw verify` inclui testes HTTP em servidor real com cookies e PostgreSQL Testcontainers: login, dados inválidos, usuário inativo, CSRF, primeiro acesso, troca, revogação de múltiplas sessões, logout, expiração, fixação de sessão e inicialização da primeira conta. Casos de expiração manipulam o horário da sessão de teste; não esperam 30 minutos.

Frontend: `npm run lint`, `npm run test:coverage`, `npm run build` e `npm run test:e2e`. Testes React cobrem formulários, confirmação, mensagens, redirecionamentos e saída. O smoke Playwright intercepta somente a consulta de sessão para verificar visitante sem depender de backend; não comprova integração completa. O fluxo completo React/API/PostgreSQL foi adicionalmente exercitado localmente com banco temporário e credenciais sintéticas, em 16/11/2026; telas desktop/móvel conferidas.

Referências: [sessões e persistência do contexto](https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html), [Spring Session JDBC e schema PostgreSQL](https://docs.spring.io/spring-session/reference/configuration/jdbc.html), [armazenamento de senhas](https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html) e [metadados oficiais do artefato Bouncy Castle](https://repo.maven.apache.org/maven2/org/bouncycastle/bcprov-jdk18on/maven-metadata.xml).

Implementação em revisão no [PR #61](https://github.com/marcellmoura/monitoramento-precos-estoque/pull/61).
