# Persistência PostgreSQL

Primeiro incremento da TT-04: banco local, evolução do schema e persistência de usuários que será utilizada pela autenticação. Login, administração de usuários e sessões entram nos incrementos funcionais seguintes. Não há conta padrão nem API pública de usuários nesta etapa.

## Ambiente

PostgreSQL 18.7-bookworm, imagem oficial fixada pelo digest multi-arquitetura `sha256:ece55f7868da684e400f82f1030a050718adbdd65c23aa500c486f6e2950dc0f`. Compose e Testcontainers usam a mesma imagem; funciona em ARM64 e AMD64. Spring Boot 4.1.2 gerencia Hibernate 7.4.13.Final, Flyway 12.4.0 e JDBC PostgreSQL 42.7.14.

[Compose](../compose.yaml) inicia somente PostgreSQL, acessível em `127.0.0.1`, com healthcheck e volume `monitoramento_postgres_data`. PostgreSQL 18 armazena seus dados sob `/var/lib/postgresql`; o volume usa esse caminho. A aplicação continua executada pelo Maven e o frontend pelo npm. Dockerfiles e Compose da aplicação completa pertencem à etapa P10.

## Execução local

1. Na raiz, crie `.env` a partir de [.env.example](../.env.example), **somente se ainda não existir**. Preencha `DB_PASSWORD` com uma senha exclusiva local. Preserve arquivos e credenciais existentes.
2. Se a porta 5432 estiver ocupada, escolha outra em `DB_PORT` (por exemplo, 5433); não é necessário parar outro banco.
3. Exporte as variáveis de `.env` antes de iniciar o Compose e o backend. Variáveis já exportadas no terminal têm precedência sobre o arquivo lido pelo Compose; esta ordem mantém os dois processos com a mesma configuração.

No macOS/Linux, a partir da raiz:

```sh
set -a
source .env
set +a
docker compose up -d --wait postgres
docker compose ps
cd backend
./mvnw spring-boot:run
```

No PowerShell, a partir da raiz:

```powershell
Get-Content .env | Where-Object { $_ -match '^[A-Z_]+=' } | ForEach-Object {
    $nome, $valor = $_ -split '=', 2
    [Environment]::SetEnvironmentVariable($nome, $valor, 'Process')
}
docker compose up -d --wait postgres
docker compose ps
Set-Location backend
.\mvnw.cmd spring-boot:run
```

O arquivo deve conter somente atribuições das variáveis previstas. `.env` não é versionado. `.env.example` não contém credencial real. Não publique saídas de comandos que mostrem o ambiente resolvido, como `docker compose config` sem `--quiet`.

| Variável | Uso |
|---|---|
| DB_NAME | Nome do banco local; padrão `monitoramento` |
| DB_USER | Usuário PostgreSQL; necessário para Compose e backend |
| DB_PASSWORD | Senha PostgreSQL; necessária, sem valor público padrão |
| DB_PORT | Porta publicada localmente; padrão 5432 |
| DB_URL | Opcional: URL JDBC completa, substitui a URL formada por host/porta/nome no backend |
| SERVER_PORT / SERVER_ADDRESS | Bind do backend; padrão 8080 / 127.0.0.1 |

Os testes não usam o banco nem as credenciais de `.env`: Spring Boot recebe conexões de containers descartáveis via `@ServiceConnection`. Um Docker indisponível reprova os testes de integração; não há substituição por H2 nem skip silencioso.

Para parar preservando dados: `docker compose stop postgres`. Para retomar: `docker compose up -d --wait postgres`. Não executar `down -v` para resolver problemas: isso remove o volume. Alterar credenciais em `.env` não altera automaticamente as de um volume já inicializado.

## Schema e modelo inicial

Flyway executa `backend/src/main/resources/db/migration/V1__criar_usuarios.sql`. Hibernate usa `ddl-auto=validate` e não cria ou atualiza tabelas. Inicialização SQL paralela está desativada; Flyway clean está desabilitado. Nunca editar uma migration já aplicada: alterações seguintes devem usar uma nova migration versionada.

| Coluna de usuarios | Tipo / regra |
|---|---|
| id | BIGINT identity, chave primária gerada |
| nome | VARCHAR(120), obrigatório e não vazio |
| email | VARCHAR(254), obrigatório e não vazio; índice único em `lower(email)` |
| senha_hash | VARCHAR(255), obrigatório e não vazio |
| perfil | VARCHAR(20), somente ADMINISTRADOR, COMPRAS, ESTOQUE ou CONSULTA |
| ativo | BOOLEAN obrigatório |
| troca_senha_obrigatoria | BOOLEAN obrigatório |
| criado_em / atualizado_em | TIMESTAMPTZ obrigatórios |

A entidade `Usuario` normaliza espaços nas extremidades de nome/e-mail e armazena e-mail em minúsculas. Novos registros começam ativos e com troca de senha obrigatória. Uma alteração de nome preserva identidade/criação e atualiza o instante de alteração. Os instantes usam `Instant` e o JDBC usa UTC. O repositório consulta por e-mail sem diferenciar caixa.

A entidade recebe **hash**, não senha em texto puro. O cálculo Argon2id e o procedimento do primeiro administrador pertencem ao incremento de autenticação AP16; nenhuma credencial ou usuário é inserido pelas migrations. Entidades de persistência não devem ser expostas diretamente em respostas HTTP futuras.

## Verificação

Com Docker disponível:

```sh
cd backend
./mvnw clean verify
```

No Docker Desktop para macOS, se o cliente Java não descobrir o socket, exporte antes `DOCKER_HOST=unix://$HOME/.docker/run/docker.sock`. Use isso somente se esse for o socket do Docker Desktop instalado. A CI Linux usa a descoberta normal do Docker do runner.

A suíte comprova:

- Aplicação inicializa com Flyway e Hibernate validando o schema, mantendo 404 em rotas de negócio ainda não implementadas.
- Os quatro perfis persistem e são consultados após limpar o contexto JPA, incluindo hash, flags e timestamps.
- Atualização preserva identidade e data de criação.
- PostgreSQL rejeita e-mail duplicado mesmo com caixa diferente, perfil inválido e nome vazio, inclusive por SQL direto.
- Uma migration aplica uma vez, reexecução preserva dados e alteração do arquivo aplicado é detectada pelo checksum. Esse teste altera somente uma cópia temporária em schema isolado do container descartável; não modifica a migration original ou o banco local.

Validação local: 10 casos de integração aprovados em 16/11/2026. CI e gates são executados automaticamente pelo fluxo existente; [PR #60](https://github.com/marcellmoura/monitoramento-precos-estoque/pull/60) em revisão. A TT-04 permanece em andamento para a evolução de produtos, fornecedores, importações e histórico.

Fontes: [imagem oficial PostgreSQL](https://hub.docker.com/_/postgres), [inicialização de dados no Spring Boot](https://docs.spring.io/spring-boot/how-to/data-initialization.html), [suporte PostgreSQL no Flyway](https://documentation.red-gate.com/flyway/reference/database-driver-reference/postgresql-database).

Inicialização real do backend também verificada com o banco do Compose: migration V1 aplicada, Hibernate validou o modelo e a rota inexistente manteve HTTP 404. Nenhum usuário foi criado no banco local. A porta local usada nesta validação foi 5433, preservando o serviço que já ocupava a 5432.

O container local foi recriado mantendo o volume nomeado: a migration V1 continuou registrada e a tabela de usuários permaneceu vazia. O processo de backend usado na validação foi encerrado; PostgreSQL permanece disponível para o desenvolvimento.

Validação remota: [CI 43868438423](https://github.com/marcellmoura/monitoramento-precos-estoque/actions/runs/43868438423) aprovada para `e58967c`, incluindo backend/PostgreSQL, frontend, navegador, ambos os gates e artefatos. Conferir também o check do último commit do PR antes de integrar. A TT-04 segue aberta, Em andamento, por abranger a evolução dos demais modelos.

## Histórico de redefinição de senha

Migration V3: histórico transacional com ator/usuário/evento/instante, sem credenciais. Operação e testes em [administração de usuários](administracao-usuarios.md). As migrations anteriores permanecem intactas.
