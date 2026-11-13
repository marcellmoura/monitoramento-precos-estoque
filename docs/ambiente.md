# Ambiente de desenvolvimento

Base configurada em 13/11/2026 para a TT-03. A aplicação inicia sem banco e ainda não oferece funcionalidades de negócio.

## Versões fixadas

| Componente | Versão | Controle |
|---|---|---|
| Eclipse Temurin JDK | 25.0.5+8 LTS | `.java-version`; `JAVA_HOME` apontando para o JDK |
| Spring Boot | 4.1.2 | Parent do `backend/pom.xml` |
| Maven / Wrapper | 3.10.0 / 3.3.4 | `backend/.mvn/wrapper/maven-wrapper.properties` |
| Node.js | 26.13.1 LTS | `.nvmrc` e `engines` do frontend |
| React / React DOM | 19.3.1 | `frontend/package.json` e lockfile |
| TypeScript | 6.0.3 | Compatível com typescript-eslint 8.72.2; TS 7 fora da faixa declarada |
| Vite / plugin React | 8.3.8 / 6.3.0 | `frontend/package.json` e lockfile |
| React Router | 8.5.1 | `frontend/package.json` e lockfile |
| Vitest / cobertura V8 | 5.1.0 | Versões pareadas no lockfile |
| React Testing Library / jsdom | 16.3.3 / 31.0.1 | `frontend/package.json` e lockfile |
| Playwright | 1.65.0 | Chromium instalado pelo próprio Playwright |
| JaCoCo | 0.8.16 | `backend/pom.xml` |
| JUnit Jupiter / Mockito / Testcontainers | 6.0.3 / 5.23.0 / 2.0.5 | Gerenciamento de dependências do Spring Boot |

ESLint 10.14.0 com typescript-eslint 8.72.2, regras de Hooks e React Refresh. Dependências auxiliares e transitivas estão no lockfile. Usar npm fornecido com Node (validação local com npm 11.20.0); `npm ci` preserva a resolução. Não executar atualização automática de dependências sem verificar compatibilidade e testes.

PostgreSQL, JPA/Flyway, segurança e PDFBox serão configurados nos incrementos correspondentes. Testcontainers/PostgreSQL já consta nas dependências de testes, mas não há teste de banco nesta etapa. Docker ativo será necessário quando essa integração existir.

## Execução e validação

Selecionar Java e Node nas versões acima; conferir `java -version`, `node --version` e `npm --version`. Arquivos de versão auxiliam gerenciadores, mas não instalam runtimes automaticamente. Maven global não é necessário.

Backend, a partir de `backend/`:

```sh
./mvnw verify
./mvnw spring-boot:run
```

No Windows, usar `mvnw.cmd`. Após `verify`, também é possível executar `java -jar target/monitoramento-0.1.0.jar`. Backend disponível em `127.0.0.1:8080`; rotas não implementadas retornam 404. `SERVER_PORT` e `SERVER_ADDRESS` são variáveis opcionais, exemplificadas em `.env.example`. Spring Boot não carrega esse arquivo automaticamente: exportar as variáveis no terminal ou configurá-las no ambiente de execução. Não há credenciais neste incremento.

Frontend, em outro terminal, a partir de `frontend/`:

```sh
npm ci
npm run dev
```

Abrir `http://127.0.0.1:5173`. O proxy de desenvolvimento encaminha `/api` para `127.0.0.1:8080`. Se mudar a porta do backend, ajustar esse destino em `vite.config.ts`. A política de mesma origem na execução completa será estabelecida nos containers.

Validação do frontend:

```sh
npm run lint
npm run test:coverage
npm run build
npx playwright install chromium
npm run test:e2e
```

Playwright serve o build em `127.0.0.1:4173`. Em Linux, suas bibliotecas de sistema podem ser instaladas com `npx playwright install --with-deps chromium`. A primeira execução do Wrapper, instalação npm e navegador exige internet. Builds, dependências, relatórios e caches não são versionados.

## Evidências iniciais

Executados em macOS ARM64 com os runtimes fixados:

- Instalação pelo lockfile com `npm ci`, lint sem avisos e build de produção aprovados.
- Vitest: 4 testes em 2 arquivos, aprovados; tela inicial e cliente HTTP (JSON, cabeçalhos/cancelamento, resposta 204 e erro 500).
- Playwright/Chromium: 1 teste aprovado, navegação inicial sem erros JavaScript.
- Maven `verify`: 1 teste HTTP/MockMvc aprovado; inicialização real do contexto e 404 para rota ausente; JAR empacotado.
- JaCoCo: 100% das 3 linhas da única classe inicial. Esse valor não representa cobertura de funcionalidades ainda inexistentes.
- V8: 70,58% de linhas/instruções; entrada `main.tsx` e composição do Router ainda sem cobertura unitária. Abertura real é verificada por Playwright, em relatório separado. A meta interna de 80% não foi atingida por essa medição; não foram excluídos arquivos de aplicação para elevá-la.

Relatórios locais: `backend/target/surefire-reports/`, `backend/target/site/jacoco/`, `frontend/coverage/`, `frontend/playwright-report/`. CI, análise SonarQube e Quality Gate ainda não executados. Nenhum teste de persistência, acesso, PDF ou regra comercial é declarado aprovado.

## Fontes oficiais consultadas

- [Spring Initializr](https://start.spring.io/metadata/client), [requisitos do Spring Boot](https://docs.spring.io/spring-boot/system-requirements.html) e [BOM 4.1.2](https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-dependencies/4.1.2/spring-boot-dependencies-4.1.2.pom).
- [Temurin 25.0.5](https://adoptium.net/news/2026/10/eclipse-temurin-17021-21013-2505-2701-available/), [Node 26.13.1](https://nodejs.org/en/blog/release/v26.13.1/) e [histórico do Maven](https://maven.apache.org/docs/history.html).
- Metadados do registro npm: [React](https://registry.npmjs.org/react/latest), [Vite](https://registry.npmjs.org/vite/latest), [typescript-eslint](https://registry.npmjs.org/typescript-eslint/latest), [React Router](https://registry.npmjs.org/react-router/latest), [Vitest](https://registry.npmjs.org/vitest/latest) e [Playwright](https://registry.npmjs.org/@playwright/test/latest).
- [JaCoCo](https://www.jacoco.org/jacoco/trunk/doc/changes.html); suporte SonarQube Cloud a [Java](https://docs.sonarsource.com/sonarqube-cloud/analyzing-source-code/languages/java) e [JavaScript/TypeScript](https://docs.sonarsource.com/sonarqube-cloud/analyzing-source-code/languages/javascript-typescript-css). O scanner deverá provisionar seu próprio runtime Node; não presumir compatibilidade do seu runtime interno com Node 26 de build.
