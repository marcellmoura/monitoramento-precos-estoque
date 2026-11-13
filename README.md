# Monitoramento de Preços, Estoque e Compras

Projeto da disciplina de DevOps para organizar produtos, fornecedores, estoque e preços a partir da importação de tabelas e orçamentos em PDF.

**Estado atual:** especificação preparada e backlog publicado com 37 histórias de usuário e 16 tarefas técnicas; implementação pendente. Ainda não há comandos de execução da aplicação.

Repositório: [marcellmoura/monitoramento-precos-estoque](https://github.com/marcellmoura/monitoramento-precos-estoque).

Acompanhamento: [AV2 DevOps — Product Backlog](https://github.com/users/marcellmoura/projects/2).

## Documentação

- [Product Backlog](docs/backlog.md)
- [Planejamento funcional e técnico](docs/planejamento.md)
- [Decisões de arquitetura e negócio](docs/decisoes.md)
- [Roadmap e entregas](docs/roadmap.md)
- [Arquivos de referência para testes](fixtures/pdfs/README.md)

## Versionamento

A branch principal é `main`. Cada incremento utiliza uma branch por tarefa e um Pull Request relacionado à issue correspondente. A documentação inicial está registrada no [PR #54](https://github.com/marcellmoura/monitoramento-precos-estoque/pull/54).

## Tecnologias previstas

React, TypeScript e Vite; Spring Boot e Maven; PostgreSQL, Flyway e Spring Data JPA; Spring Security e Spring Session JDBC; Apache PDFBox; GitHub Actions, SonarQube Cloud e Docker Compose.

Testes: Vitest, React Testing Library e Playwright; Spring Boot Test, JUnit, Mockito, MockMvc, Testcontainers e JaCoCo.

## Entregas previstas

- Backlog: 15/11/2026.
- Projeto final: 23/11/2026.

Os comandos de instalação, execução, testes e demonstração serão documentados conforme a implementação de cada incremento. Dados locais de execução, credenciais e uploads não fazem parte do conteúdo versionado.
