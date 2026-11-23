# Monitoramento de Preços, Estoque e Compras

Projeto da disciplina de DevOps para organizar produtos, fornecedores, estoque e preços a partir da importação de tabelas e orçamentos em PDF.

**Estado atual.** O sistema inclui login, gestão de usuários e perfis, cadastros, ajuste de estoque e importação de cotações em PDF. A importação cobre extração em segundo plano, conferência, associação ao catálogo, confirmação parcial e comparação com o histórico de preços. A CI executa os testes e os Quality Gates do SonarQube Cloud.

Repositório: [marcellmoura/monitoramento-precos-estoque](https://github.com/marcellmoura/monitoramento-precos-estoque).

Acompanhamento: [AV2 DevOps — Product Backlog](https://github.com/users/marcellmoura/projects/2).

## Documentação

- [Ambiente, versões, execução e testes](docs/ambiente.md)
- [Integração contínua](docs/ci.md)
- [Banco e persistência](docs/persistencia.md)
- [Login e criação do primeiro administrador](docs/acesso.md)
- [Administração de usuários](docs/administracao-usuarios.md)
- [Cadastro, consulta e edição de produtos](docs/produtos.md)
- [Cadastro e consulta de fornecedores](docs/fornecedores.md)
- [Envio, prévia e conferência de PDFs de fornecedores](docs/importacoes.md)
- [Product Backlog](docs/backlog.md)
- [Planejamento funcional e técnico](docs/planejamento.md)
- [Decisões de arquitetura e negócio](docs/decisoes.md)
- [Roadmap e entregas](docs/roadmap.md)
- [Arquivos de referência para testes](fixtures/pdfs/README.md)

## Versionamento

A branch principal é `main`. Cada incremento utiliza uma branch por tarefa e um Pull Request relacionado à issue correspondente. A documentação inicial está registrada no [PR #54](https://github.com/marcellmoura/monitoramento-precos-estoque/pull/54); a estrutura executável foi integrada pelo [PR #56](https://github.com/marcellmoura/monitoramento-precos-estoque/pull/56). A CI inicial foi integrada pelo [PR #57](https://github.com/marcellmoura/monitoramento-precos-estoque/pull/57).

## Executar com Docker Compose

Use Docker com o plugin Compose e execute os comandos na raiz do projeto.

1. Se ainda não existir, crie `.env` a partir de `.env.example` com `cp .env.example .env` no macOS/Linux ou `Copy-Item .env.example .env` no PowerShell. Preencha `DB_PASSWORD` com uma senha local exclusiva. Se uma porta estiver ocupada, ajuste `APP_PORT` (padrão `5173`) ou `DB_PORT` (padrão `5432`). O Compose lê esse arquivo automaticamente.
2. Em uma base nova, inicie o PostgreSQL e crie o primeiro administrador em um terminal interativo.

   ```sh
   docker compose up -d --wait postgres
   docker compose build backend
   docker compose run --rm backend --inicializar-admin
   ```

   Informe o nome, o e-mail e uma senha temporária de pelo menos 12 caracteres. Digite a senha novamente na confirmação. A inicialização fica disponível apenas para bases sem usuários. Se já houver um administrador na base, avance para o próximo passo.

3. Suba a aplicação completa.

   ```sh
   docker compose up -d --build --wait
   docker compose ps
   ```

4. Abra [http://127.0.0.1:5173](http://127.0.0.1:5173) ou use a porta definida em `APP_PORT`. Na tela **Entrar**, informe **E-mail** e **Senha** da conta criada. No primeiro acesso, preencha **Senha atual**, **Nova senha** e **Confirme a nova senha**. Clique em **Salvar nova senha** e entre novamente com a senha definitiva.
5. Na página inicial, abra **Consultar fornecedores** e clique em **Cadastrar fornecedor**. Em **Nome**, digite `Fornecedor de demonstração` e clique em **Cadastrar fornecedor**. Abra [Enviar PDF](http://127.0.0.1:5173/importacoes/nova), usando a mesma porta escolhida no passo 4. Em **Fornecedor**, selecione **Fornecedor de demonstração**. Em **Arquivo PDF**, selecione `fixtures/pdfs/sinteticos/tabela-v1.pdf`. Clique em **Enviar PDF**. A **Prévia dos itens** deve mostrar quatro linhas e o status **Aguardando conferência**.

O navegador usa a mesma origem para a interface e a API. O Compose publica a interface e a porta do PostgreSQL somente em `127.0.0.1`. O backend é acessível pela rede interna dos containers. A configuração padrão é HTTP local.

### Reutilizar uma base com PDFs

Ao migrar da execução direta para o Compose, preserve o banco e copie os PDFs para o novo volume antes de iniciar o backend containerizado. Pare o backend executado no host. No macOS, Linux ou WSL, a partir da raiz do projeto, execute o comando abaixo com a imagem do backend já construída. Ajuste `backend/storage/importacoes` se o backend no host usava outro caminho.

```sh
tar -C backend/storage/importacoes -cf - . |
  docker compose run --rm --no-deps -T --entrypoint tar backend --keep-old-files -xf - -C /app/storage/importacoes
```

O comando preserva os arquivos de origem e recusa sobrescrever arquivos existentes no volume.

### Logs e encerramento

```sh
docker compose logs --tail=100 backend
docker compose logs -f frontend backend postgres
docker compose down
```

Use **Ctrl+C** para sair do acompanhamento de logs. O comando `docker compose down` encerra o ambiente, preservando o banco no volume `postgres_data` e os PDFs no volume `pdfs_data`. Suba novamente com `docker compose up -d --wait`. A opção `down --volumes` apaga os dados desses volumes. O caminho `IMPORTACAO_PASTA` em `.env` é usado apenas na execução do backend diretamente no host.

## Executar para desenvolvimento

Use Temurin Java 25.0.5+8, Node 26.13.1 e o Maven Wrapper. Para executar somente o banco pelo Compose, rode `docker compose up -d --wait postgres`.

Prepare o banco e exporte as variáveis conforme [persistência](docs/persistencia.md). Crie o primeiro administrador conforme [acesso](docs/acesso.md). Para iniciar o backend, em `backend/`:

```sh
./mvnw verify
./mvnw spring-boot:run
```

Em outro terminal, em `frontend/`:

```sh
npm ci
npm run dev
```

Abrir `http://127.0.0.1:5173`. O backend inicia em `127.0.0.1:8080`; o login protege o acesso. Para validar o frontend: `npm run lint`, `npm run test:coverage`, `npm run build`, `npx playwright install chromium` e `npm run test:e2e`. Detalhes de ambiente, Windows e relatórios em [ambiente](docs/ambiente.md).

## Tecnologias

React, TypeScript e Vite; Spring Boot e Maven; PostgreSQL, Flyway e Spring Data JPA; Spring Security e Spring Session JDBC; Apache PDFBox; GitHub Actions, SonarQube Cloud e Docker Compose.

Testes: Vitest, React Testing Library e Playwright; Spring Boot Test, JUnit, Mockito, MockMvc, Testcontainers e JaCoCo.

## Entregas previstas

- Backlog: 15/11/2026.
- Projeto final: 23/11/2026.

Dados locais de execução, credenciais e uploads ficam fora do Git.
