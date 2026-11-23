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
5. Na página inicial, abra **Consultar fornecedores** e clique em **Cadastrar fornecedor**. Em **Nome**, digite `Fornecedor de demonstração`. Deixe os campos opcionais em branco e clique em **Cadastrar fornecedor**. Anote o **Código** exibido. Abra [Enviar PDF](http://127.0.0.1:5173/importacoes/nova), usando a mesma porta escolhida no passo 4. Em **Fornecedor**, selecione a opção com o código anotado e o nome **Fornecedor de demonstração**. Em **Arquivo PDF**, selecione `fixtures/pdfs/sinteticos/tabela-v1.pdf`. Clique em **Enviar PDF**. A **Prévia dos itens** deve mostrar quatro linhas e o status **Aguardando conferência**.

O navegador usa a mesma origem para a interface e a API. O Compose publica a interface e a porta do PostgreSQL somente em `127.0.0.1`. O backend é acessível pela rede interna dos containers. A configuração padrão é HTTP local.

O [arquivo .env.example](.env.example) lista os parâmetros disponíveis. Consulte as opções de [conexão ao banco](docs/persistencia.md#execução-local) e os [limites de upload e da fila](docs/importacoes.md#configuração-local). Use `SESSION_COOKIE_SECURE=true` ao servir a aplicação por HTTPS.

## Demonstrar duas cotações do mesmo fornecedor

Use uma base de demonstração limpa e siga os passos de [execução com Docker Compose](#executar-com-docker-compose) até abrir a prévia de `tabela-v1.pdf`.

### Primeira cotação

1. Em **Conferência comercial**, preencha os campos abaixo e clique em **Salvar conferência**.

   | Campo | Valor |
   |---|---|
   | **Coluna de preço de compra** | **Preço** |
   | **Unidade de compra** | **Quilograma (KG)** |
   | **Embalagem de compra** | `Saco de 1 kg` |
   | **Data comercial** | `01/11/2026` |

2. Em **Prévia dos itens**, faça a sequência abaixo uma vez em cada item, na ordem **Hibisco**, **Alecrim**, **Camomila** e **Erva-doce**.
   - **Associar produto** → clique no botão dentro do item.
   - **Cadastrar produto** → clique no link do formulário de associação.
   - **Nome** → digite o nome do item da vez. **Unidade** → selecione **Quilograma (KG)**. **Preço de venda** → digite `40`. Deixe os campos opcionais em branco.
   - **Cadastrar produto** → clique no botão para voltar à cotação. **Confirmar associação** → clique para gravar o vínculo com o produto recém-cadastrado.
3. Em **Confirmação de preços**, clique em **Confirmar itens válidos**. Espere o status **Concluída** e quatro preços confirmados.

### Segunda cotação

1. Abra **Enviar PDF** novamente, no mesmo endereço usado na primeira cotação. Em **Fornecedor**, selecione a mesma opção de **Fornecedor de demonstração**. Em **Arquivo PDF**, selecione `fixtures/pdfs/sinteticos/tabela-v2.pdf`. Clique em **Enviar PDF** e espere a **Prévia dos itens** com seis linhas.
2. Em **Conferência comercial**, use **Coluna de preço de compra** → **Preço**, **Unidade de compra** → **Quilograma (KG)**, **Embalagem de compra** → `Saco de 1 kg` e **Data comercial** → `08/11/2026`. Clique em **Salvar conferência**.
3. Na **Prévia dos itens**, confira **Vínculo reaproveitado** em **Hibisco**, **Alecrim** e **Camomila**. Associe **Melissa** com a mesma sequência de cadastro e associação da primeira cotação, usando **Nome** → `Melissa`. Deixe **Boldo** e **Mistura sem identificação** pendentes.
4. Em **Confirmação de preços**, clique em **Confirmar itens válidos**. A tentativa fica **Concluída com pendências**. Confira os resultados na **Prévia dos itens**.

   | Item | Resultado da segunda cotação |
   |---|---|
   | Hibisco | **Aumentou** de R$ 33,00 para R$ 35,00, **+6,06%** |
   | Alecrim | **Diminuiu** de R$ 15,00 para R$ 14,00, **-6,67%** |
   | Camomila | **Sem alteração**, R$ 38,00 e **0,00%** |
   | Melissa | **Novo**, R$ 20,00 |
   | Boldo | **Pendente** por preço zero |
   | Mistura sem identificação | **Pendente** por dados de apresentação e associação |
   | Erva-doce | Ausente na segunda cotação, com R$ 22,00 preservados no histórico |

5. Na página inicial, abra **Histórico de preços**. Em **Código do fornecedor**, digite o código anotado e clique em **Filtrar histórico**. A tabela **Preços registrados** mostra oito registros. Abra **Tentativa #…** em uma linha do Hibisco para voltar à cotação de origem.

### PDFs aceitos

A extração lê PDFs com texto selecionável nos layouts **KG/Fardo**, **Atacado/Varejo**, **tabela** e **fichas**. Documentos digitalizados exigiriam OCR, que ainda não está disponível. Em uma falha de extração, o detalhe informa o motivo e **Tentar novamente** agenda uma nova leitura do PDF original armazenado. Consulte [formatos, pendências e reprocessamento](docs/importacoes.md) e os [arquivos de referência](fixtures/pdfs/README.md).

## Operar o ambiente local

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
./mvnw spring-boot:run
```

Em outro terminal, em `frontend/`:

```sh
npm ci
npm run dev
```

Abra `http://127.0.0.1:5173`. O backend inicia em `127.0.0.1:8080` e o login protege o acesso. Consulte [ambiente](docs/ambiente.md) para versões e instruções de desenvolvimento no Windows.

## Testes, CI e SonarQube Cloud

Com Docker disponível, execute `./mvnw -B -ntp clean verify` em `backend/` para rodar testes unitários e de integração com PostgreSQL via Testcontainers. O relatório de cobertura fica em `backend/target/site/jacoco/`. No Windows, use `.\mvnw.cmd` no lugar de `./mvnw`.

Em `frontend/`, rode os comandos abaixo. A cobertura fica em `frontend/coverage/` e o relatório de navegação em `frontend/playwright-report/`. O smoke Chromium verifica a navegação inicial. O roteiro de demonstração acima exercita a aplicação completa com API e banco.

```sh
npm ci
npm run lint
npm run test:coverage
npm run build
npx playwright install chromium
npm run test:e2e
```

O [workflow no GitHub Actions](https://github.com/marcellmoura/monitoramento-precos-estoque/actions/workflows/ci.yml) executa builds, testes e os Quality Gates em PRs para `main` e após o merge. Abra uma execução e selecione **Build e testes** para ver os logs e o resultado de cada etapa. Os relatórios de diagnóstico e os pacotes aprovados ficam no resumo da execução. Um exemplo aprovado está no [PR #81](https://github.com/marcellmoura/monitoramento-precos-estoque/pull/81) e na [execução de CI correspondente](https://github.com/marcellmoura/monitoramento-precos-estoque/actions/runs/45134191490).

Consulte o status e a cobertura nos painéis do [backend](https://sonarcloud.io/dashboard?id=marcellmoura_monitoramento-precos-estoque_backend) e do [frontend](https://sonarcloud.io/dashboard?id=marcellmoura_monitoramento-precos-estoque_frontend). A CI aguarda os dois Quality Gates antes de publicar o pacote da aplicação. Detalhes em [Integração contínua](docs/ci.md).

## Tecnologias e acesso

React, TypeScript e Vite; Spring Boot e Maven; PostgreSQL, Flyway e Spring Data JPA; Spring Security e Spring Session JDBC; Apache PDFBox; GitHub Actions, SonarQube Cloud e Docker Compose.

Testes: Vitest, React Testing Library e Playwright; Spring Boot Test, JUnit, Mockito, MockMvc, Testcontainers e JaCoCo.

O administrador inicial é criado pelo comando interativo do backend. Os perfis são **Administrador**, **Compras**, **Estoque** e **Consulta**. O roteiro de importação e histórico usa **Administrador** ou **Compras**. As sessões ficam no PostgreSQL e expiram após 30 minutos de inatividade. Consulte [login e sessões](docs/acesso.md) e [gestão de perfis](docs/administracao-usuarios.md).

## Entregas previstas

- Backlog: 15/11/2026.
- Projeto final: 23/11/2026.

Dados locais de execução, credenciais e uploads ficam fora do Git.
