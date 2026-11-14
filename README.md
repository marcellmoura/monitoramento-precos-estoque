# Monitoramento de Preços, Estoque e Compras

Projeto da disciplina de DevOps para organizar produtos, fornecedores, estoque e preços a partir da importação de tabelas e orçamentos em PDF.

**Estado atual:** backlog publicado com 37 histórias e 16 tarefas técnicas; estrutura React/Spring Boot com builds e testes locais verificados. Funcionalidades de negócio, banco e containers pendentes; CI inicial em validação.

Repositório: [marcellmoura/monitoramento-precos-estoque](https://github.com/marcellmoura/monitoramento-precos-estoque).

Acompanhamento: [AV2 DevOps — Product Backlog](https://github.com/users/marcellmoura/projects/2).

## Documentação

- [Ambiente, versões, execução e testes](docs/ambiente.md)
- [Integração contínua](docs/ci.md)
- [Product Backlog](docs/backlog.md)
- [Planejamento funcional e técnico](docs/planejamento.md)
- [Decisões de arquitetura e negócio](docs/decisoes.md)
- [Roadmap e entregas](docs/roadmap.md)
- [Arquivos de referência para testes](fixtures/pdfs/README.md)

## Versionamento

A branch principal é `main`. Cada incremento utiliza uma branch por tarefa e um Pull Request relacionado à issue correspondente. A documentação inicial está registrada no [PR #54](https://github.com/marcellmoura/monitoramento-precos-estoque/pull/54); a estrutura executável foi integrada pelo [PR #56](https://github.com/marcellmoura/monitoramento-precos-estoque/pull/56).

## Execução inicial

Requisitos: Temurin Java 25.0.5+8 e Node 26.13.1. Maven está fixado pelo Wrapper.

Em `backend/`:

```sh
./mvnw verify
./mvnw spring-boot:run
```

Em outro terminal, em `frontend/`:

```sh
npm ci
npm run dev
```

Abrir `http://127.0.0.1:5173`. O backend inicia em `127.0.0.1:8080`; ainda não há endpoints de negócio. Para validar o frontend: `npm run lint`, `npm run test:coverage`, `npm run build`, `npx playwright install chromium` e `npm run test:e2e`. Detalhes de ambiente, Windows e relatórios em [ambiente](docs/ambiente.md).

## Tecnologias

React, TypeScript e Vite; Spring Boot e Maven; PostgreSQL, Flyway e Spring Data JPA; Spring Security e Spring Session JDBC; Apache PDFBox; GitHub Actions, SonarQube Cloud e Docker Compose.

Testes: Vitest, React Testing Library e Playwright; Spring Boot Test, JUnit, Mockito, MockMvc, Testcontainers e JaCoCo.

## Entregas previstas

- Backlog: 15/11/2026.
- Projeto final: 23/11/2026.

Banco, pipeline, segurança, PDFs e execução completa por containers serão documentados nos incrementos correspondentes. Dados locais de execução, credenciais e uploads não fazem parte do conteúdo versionado.
