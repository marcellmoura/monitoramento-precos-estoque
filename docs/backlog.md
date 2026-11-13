# Product Backlog — Monitoramento de Preços, Estoque e Compras

Versão: 2026-11-13. **Status: especificação concluída e 53 issues publicadas no GitHub Project; implementação pendente.**

**Prazo do backlog E0: 15/11/2026. Entrega final do projeto no planejamento: 23/11/2026.** Sem datas intermediárias definidas.

## Como ler

US significa User Story (história de usuário); TT significa tarefa técnica. RF/RNF são os requisitos do professor; RB/T/D são regras, testes e decisões do [planejamento](planejamento.md). As decisões técnicas estão em [decisoes.md](decisoes.md). As fases Pxx estão no [roadmap](roadmap.md). A numeração US/TT é estável e não equivale ao número que o GitHub atribuirá a cada issue.

Cada link abre a especificação de uma história ou tarefa. A coluna Issue GitHub contém os links verificados das 53 issues publicadas. Manter status e conteúdo consistentes entre issues e documentos.

Prioridades e sequência representam o planejamento de execução. Alta não significa que a tarefa pode começar sem dependências. Estimativas ficam **A definir**, sem pontos/prazos inventados. Todos os itens começam em **Backlog**, sem funcionalidades concluídas. Situação atual: **TT-01 e TT-02 em Em andamento**; os outros 51 itens continuam em Backlog.

## Escopos

- **MVP obrigatório:** requisitos funcionais RF01–RF10 do documento da disciplina.
- **MVP estendido:** autenticação/perfis/sessões, layouts de referência adicionais, correções posteriores, configuração de leitura e referência de compra interna.
- **DevOps obrigatório:** entregas técnicas relacionadas aos RNF e aos critérios de aceitação.
- **Qualidade de código:** análise contínua e metas internas AP19.
- **Evolução:** reposição, sugestões, gráficos, indicadores e novos layouts previstos na evolução do sistema.
- **Evolução em avaliação:** OCR, dependente de avaliação técnica específica.

## Épicos

Usuários e Acesso; Produtos e Estoque; Fornecedores; Importação e Conferência; Associação; Histórico e Comparação; Evoluções. Para tarefas técnicas: Gestão e DevOps; Qualidade; Operação e Entrega. São agrupamentos do quadro; E0–E5 continuam sendo os marcos da disciplina.

## Histórias de usuário (37)

| ID / história | Épico | Escopo | Prioridade | Fase | Issue GitHub |
|---|---|---|---|---|---|
| [US-01 — Cadastrar produto interno](backlog/US-01.md) | Produtos e Estoque | MVP obrigatório | Alta | P5 / E1 | [#1](https://github.com/marcellmoura/monitoramento-precos-estoque/issues/1) |
| [US-02 — Consultar produtos e seus dados](backlog/US-02.md) | Produtos e Estoque | MVP obrigatório | Alta | P5 / E1 | [#2](https://github.com/marcellmoura/monitoramento-precos-estoque/issues/2) |
| [US-03 — Editar produto e dados básicos de estoque](backlog/US-03.md) | Produtos e Estoque | MVP obrigatório | Alta | P5 / E1 | [#3](https://github.com/marcellmoura/monitoramento-precos-estoque/issues/3) |
| [US-04 — Cadastrar fornecedor](backlog/US-04.md) | Fornecedores | MVP obrigatório | Alta | P5 / E1 | [#4](https://github.com/marcellmoura/monitoramento-precos-estoque/issues/4) |
| [US-05 — Consultar fornecedores](backlog/US-05.md) | Fornecedores | MVP obrigatório | Alta | P5 / E1 | [#5](https://github.com/marcellmoura/monitoramento-precos-estoque/issues/5) |
| [US-06 — Enviar PDF associado a fornecedor](backlog/US-06.md) | Importação e Conferência | MVP obrigatório | Alta | P6 / E2 | [#6](https://github.com/marcellmoura/monitoramento-precos-estoque/issues/6) |
| [US-07 — Consultar tentativas e status de importação](backlog/US-07.md) | Importação e Conferência | MVP obrigatório | Alta | P6 / E2 | [#7](https://github.com/marcellmoura/monitoramento-precos-estoque/issues/7) |
| [US-08 — Extrair itens dos PDFs de referência do professor](backlog/US-08.md) | Importação e Conferência | MVP obrigatório | Alta | P7 / E2 | [#8](https://github.com/marcellmoura/monitoramento-precos-estoque/issues/8) |
| [US-09 — Conferir prévia da extração](backlog/US-09.md) | Importação e Conferência | MVP obrigatório | Alta | P7 / E2 | [#9](https://github.com/marcellmoura/monitoramento-precos-estoque/issues/9) |
| [US-10 — Corrigir dados extraídos antes da confirmação](backlog/US-10.md) | Importação e Conferência | MVP obrigatório | Alta | P7 / E2 | [#10](https://github.com/marcellmoura/monitoramento-precos-estoque/issues/10) |
| [US-11 — Associar manualmente item ao produto interno](backlog/US-11.md) | Associação | MVP obrigatório | Alta | P8 / E2 | [#11](https://github.com/marcellmoura/monitoramento-precos-estoque/issues/11) |
| [US-12 — Reaproveitar vínculos seguros em novas importações](backlog/US-12.md) | Associação | MVP obrigatório | Alta | P8 / E2 | [#12](https://github.com/marcellmoura/monitoramento-precos-estoque/issues/12) |
| [US-13 — Confirmar importação e preservar preços válidos](backlog/US-13.md) | Importação e Conferência | MVP obrigatório | Alta | P8 / E2 | [#13](https://github.com/marcellmoura/monitoramento-precos-estoque/issues/13) |
| [US-14 — Identificar reimportação do mesmo arquivo](backlog/US-14.md) | Importação e Conferência | MVP obrigatório | Alta | P6–P8 / E2 | [#14](https://github.com/marcellmoura/monitoramento-precos-estoque/issues/14) |
| [US-15 — Identificar falha parcial de extração](backlog/US-15.md) | Importação e Conferência | MVP obrigatório | Alta | P7–P8 / E2 | [#15](https://github.com/marcellmoura/monitoramento-precos-estoque/issues/15) |
| [US-16 — Comparar com o último preço válido correto](backlog/US-16.md) | Histórico e Comparação | MVP obrigatório | Alta | P9 / E3 | [#16](https://github.com/marcellmoura/monitoramento-precos-estoque/issues/16) |
| [US-17 — Classificar aumentos, reduções, igualdade, novos e pendências](backlog/US-17.md) | Histórico e Comparação | MVP obrigatório | Alta | P9 / E3 | [#17](https://github.com/marcellmoura/monitoramento-precos-estoque/issues/17) |
| [US-18 — Visualizar valores e variações com status acessível](backlog/US-18.md) | Histórico e Comparação | MVP obrigatório | Alta | P9 / E3 | [#18](https://github.com/marcellmoura/monitoramento-precos-estoque/issues/18) |
| [US-19 — Consultar histórico por produto, fornecedor e importação](backlog/US-19.md) | Histórico e Comparação | MVP obrigatório | Alta | P8–P9 / E3 | [#19](https://github.com/marcellmoura/monitoramento-precos-estoque/issues/19) |
| [US-20 — Preservar preço de item ausente em PDF posterior](backlog/US-20.md) | Histórico e Comparação | MVP obrigatório | Alta | P9 / E3 | [#20](https://github.com/marcellmoura/monitoramento-precos-estoque/issues/20) |
| [US-21 — Tratar preços inválidos e itens não identificados](backlog/US-21.md) | Importação e Conferência | MVP obrigatório | Alta | P7–P8 / E2 | [#21](https://github.com/marcellmoura/monitoramento-precos-estoque/issues/21) |
| [US-22 — Importar os layouts sintéticos tabela e fichas](backlog/US-22.md) | Importação e Conferência | MVP estendido | Alta | P7 / E2 | [#22](https://github.com/marcellmoura/monitoramento-precos-estoque/issues/22) |
| [US-23 — Sinalizar oportunidades de reposição](backlog/US-23.md) | Evoluções | Evolução | Média | P13a | [#23](https://github.com/marcellmoura/monitoramento-precos-estoque/issues/23) |
| [US-24 — Sugerir revisão do preço de venda por margem](backlog/US-24.md) | Evoluções | Evolução | Média | P13b | [#24](https://github.com/marcellmoura/monitoramento-precos-estoque/issues/24) |
| [US-25 — Visualizar gráficos de evolução de preços](backlog/US-25.md) | Evoluções | Evolução | Média | P13c | [#25](https://github.com/marcellmoura/monitoramento-precos-estoque/issues/25) |
| [US-26 — Conferir e mapear dados de layouts desconhecidos](backlog/US-26.md) | Evoluções | Evolução | Média | P13d | [#26](https://github.com/marcellmoura/monitoramento-precos-estoque/issues/26) |
| [US-27 — Avaliar leitura de PDFs digitalizados com OCR](backlog/US-27.md) | Evoluções | Evolução em avaliação | Baixa | P13d | [#27](https://github.com/marcellmoura/monitoramento-precos-estoque/issues/27) |
| [US-28 — Consultar indicadores gerenciais especificados](backlog/US-28.md) | Evoluções | Evolução | Média | P13c | [#28](https://github.com/marcellmoura/monitoramento-precos-estoque/issues/28) |
| [US-29 — Entrar com e-mail e senha](backlog/US-29.md) | Usuários e Acesso | MVP estendido | Alta | P3a | [#29](https://github.com/marcellmoura/monitoramento-precos-estoque/issues/29) |
| [US-30 — Sair e expirar sessões](backlog/US-30.md) | Usuários e Acesso | MVP estendido | Alta | P3a | [#30](https://github.com/marcellmoura/monitoramento-precos-estoque/issues/30) |
| [US-31 — Administrar usuários e perfis](backlog/US-31.md) | Usuários e Acesso | MVP estendido | Alta | P3a | [#31](https://github.com/marcellmoura/monitoramento-precos-estoque/issues/31) |
| [US-32 — Trocar senha temporária no primeiro acesso](backlog/US-32.md) | Usuários e Acesso | MVP estendido | Alta | P3a | [#32](https://github.com/marcellmoura/monitoramento-precos-estoque/issues/32) |
| [US-33 — Redefinir senha por intervenção administrativa](backlog/US-33.md) | Usuários e Acesso | MVP estendido | Alta | P3a | [#33](https://github.com/marcellmoura/monitoramento-precos-estoque/issues/33) |
| [US-34 — Restringir funções e dados por perfil](backlog/US-34.md) | Usuários e Acesso | MVP estendido | Alta | P3a e transversal | [#34](https://github.com/marcellmoura/monitoramento-precos-estoque/issues/34) |
| [US-35 — Corrigir preço já confirmado preservando origem](backlog/US-35.md) | Histórico e Comparação | MVP estendido | Alta | P8a | [#35](https://github.com/marcellmoura/monitoramento-precos-estoque/issues/35) |
| [US-36 — Escolher preço de compra de referência interno](backlog/US-36.md) | Produtos e Estoque | MVP estendido | Alta | P8a | [#36](https://github.com/marcellmoura/monitoramento-precos-estoque/issues/36) |
| [US-37 — Configurar leitura de preços por fornecedor e layout](backlog/US-37.md) | Importação e Conferência | MVP estendido | Alta | P7 | [#37](https://github.com/marcellmoura/monitoramento-precos-estoque/issues/37) |

## Tarefas técnicas (16)

| ID / tarefa | Épico | Escopo | Prioridade | Fase | Issue GitHub |
|---|---|---|---|---|---|
| [TT-01 — Resolver decisões antes dos incrementos dependentes](backlog/TT-01.md) | Gestão e DevOps | DevOps obrigatório | Alta | P1 e transversal | [#38](https://github.com/marcellmoura/monitoramento-precos-estoque/issues/38) |
| [TT-02 — Versionar e organizar repositório público no GitHub](backlog/TT-02.md) | Gestão e DevOps | DevOps obrigatório | Alta | P3 / E1 | [#39](https://github.com/marcellmoura/monitoramento-precos-estoque/issues/39) |
| [TT-03 — Preparar estrutura React e Spring Boot com builds locais](backlog/TT-03.md) | Gestão e DevOps | DevOps obrigatório | Alta | P3 / E1 | [#40](https://github.com/marcellmoura/monitoramento-precos-estoque/issues/40) |
| [TT-04 — Preparar persistência PostgreSQL e evolução do modelo](backlog/TT-04.md) | Gestão e DevOps | DevOps obrigatório | Alta | P3–P8 | [#41](https://github.com/marcellmoura/monitoramento-precos-estoque/issues/41) |
| [TT-05 — Automatizar testes das regras, endpoints e fluxos](backlog/TT-05.md) | Qualidade | DevOps obrigatório | Alta | P5–P11; transversal | [#42](https://github.com/marcellmoura/monitoramento-precos-estoque/issues/42) |
| [TT-06 — Executar build e testes no GitHub Actions](backlog/TT-06.md) | Gestão e DevOps | DevOps obrigatório | Alta | P4 e evolução contínua / E3 | [#43](https://github.com/marcellmoura/monitoramento-precos-estoque/issues/43) |
| [TT-07 — Integrar SonarQube Cloud e acompanhar qualidade](backlog/TT-07.md) | Qualidade | Qualidade de código | Alta | P4–P11 | [#44](https://github.com/marcellmoura/monitoramento-precos-estoque/issues/44) |
| [TT-08 — Parametrizar ambiente e proteger segredos](backlog/TT-08.md) | Gestão e DevOps | DevOps obrigatório | Alta | P3–P10 | [#45](https://github.com/marcellmoura/monitoramento-precos-estoque/issues/45) |
| [TT-09 — Registrar logs rastreáveis de importação](backlog/TT-09.md) | Operação e Entrega | DevOps obrigatório | Alta | P6–P12 | [#46](https://github.com/marcellmoura/monitoramento-precos-estoque/issues/46) |
| [TT-10 — Executar frontend, backend e banco com Docker Compose](backlog/TT-10.md) | Operação e Entrega | DevOps obrigatório | Alta | P10 / E4 | [#47](https://github.com/marcellmoura/monitoramento-precos-estoque/issues/47) |
| [TT-11 — Documentar execução, testes, CI e demonstração](backlog/TT-11.md) | Operação e Entrega | DevOps obrigatório | Alta | P3–P12; transversal | [#48](https://github.com/marcellmoura/monitoramento-precos-estoque/issues/48) |
| [TT-12 — Validar critérios finais e organizar evidências da entrega](backlog/TT-12.md) | Operação e Entrega | DevOps obrigatório | Alta | P11–P12; P14 para evoluções | [#49](https://github.com/marcellmoura/monitoramento-precos-estoque/issues/49) |
| [TT-13 — Manter massa de testes e resultados esperados](backlog/TT-13.md) | Qualidade | DevOps obrigatório | Alta | P6–P11 | [#50](https://github.com/marcellmoura/monitoramento-precos-estoque/issues/50) |
| [TT-14 — Verificar segurança de autenticação e autorização](backlog/TT-14.md) | Qualidade | MVP estendido | Alta | P3a e P11 | [#51](https://github.com/marcellmoura/monitoramento-precos-estoque/issues/51) |
| [TT-15 — Processar importações em segundo plano com estado durável](backlog/TT-15.md) | Gestão e DevOps | MVP estendido | Alta | P6–P8 | [#52](https://github.com/marcellmoura/monitoramento-precos-estoque/issues/52) |
| [TT-16 — Registrar ações com responsável e proteger os PDFs](backlog/TT-16.md) | Operação e Entrega | MVP estendido | Alta | P3a–P12 | [#53](https://github.com/marcellmoura/monitoramento-precos-estoque/issues/53) |

## Sequência de trabalho

1. Publicação do E0 concluída: 53 issues revisadas e vinculadas ao Project. Conferir o formato exigido e realizar a submissão no Classroom. Nenhuma funcionalidade precisa estar implementada para isso.
2. Resolver decisões do próximo incremento (TT-01), criar repositório e estrutura/builds/configuração (TT-02–TT-04 e TT-08). Configurar CI e qualidade cedo (TT-06–TT-07). Testes e documentação acompanham os incrementos desde o início.
3. Autenticação e autorização: US-29–US-34 e TT-14/TT-16 (testes/auditoria incrementais); depois cadastros: US-01–US-05, com testes de TT-05.
4. Trabalho durável e processamento em segundo plano TT-15; recepção/tentativas/duplicidade: US-06–US-07 e US-14; logs TT-09 e massa TT-13.
5. Configuração de colunas/modalidades US-37 com a conferência; extração dos originais e sintéticos: US-08 e US-22; prévia/correção/erros US-09–US-10, US-15 e US-21.
6. Associação e confirmação parcial: US-11–US-13; correções e escolha de referência interna US-35–US-36, preservando histórico e cronologia comercial.
7. Comparação e consultas: US-16–US-20.
8. Ambiente completo/README/validação: TT-10–TT-12, verificando todos os critérios finais. Docker e documentação podem avançar incrementalmente antes da consolidação.
9. Evoluções US-23–US-26/US-28 e proposta OCR US-27 conforme decisões específicas; repetir validação integrada em P14.

As dependências de cada item são técnicas, não obrigação de publicar as issues nessa ordem. TT-05/TT-09/TT-11 são transversais. TT-01 é acompanhamento das decisões; não bloqueia todo o backlog até resolver todos os Dxx. US-22 não depende da finalização do parser original: ambos podem ser implementados incrementalmente após definição do contrato de extração.

## Definição de pronto para iniciar um incremento

- Escopo e abordagem técnica documentados e revisados.
- Dependências necessárias disponíveis e decisões aplicáveis resolvidas/registradas.
- Critérios de aceitação compreendidos e verificação prevista.
- Tarefa movida para A fazer/Em andamento somente quando isso refletir trabalho real.

A existência de decisões abertas não impede publicar uma história no backlog. Não remover a pendência para fazer o cartão parecer pronto.

## Definição de pronto para concluir

- Critérios atendidos e testes pertinentes executados, com resultados registrados.
- Contratos HTTP, frontend e persistência integrados quando a história envolver esses componentes.
- Sem regressão conhecida nos requisitos afetados; regras/validações não ficam apenas no frontend.
- Build/CI aplicáveis aprovados; análise/gate de qualidade conforme política vigente e honestamente reportados.
- PR revisável com evidência real de revisão/integração no fluxo de revisão do projeto.
- Documentação e roadmap atualizados; pendências residuais declaradas.

Revisão e aprovação da história não equivale a implementar. Publicação da issue não equivale a concluir o trabalho.

## Cobertura dos requisitos e critérios finais

| Fonte | Itens que a atendem |
|---|---|
| RF01 | US-01, US-02, US-03 |
| RF02 | US-04, US-05 |
| RF03 | US-11, US-12 |
| RF04 | US-06, US-07, US-13, US-14 |
| RF05 | US-08, US-09, US-10; US-22 como extensão de escopo |
| RF06 | US-09, US-10, US-11, US-15, US-21 |
| RF07 | US-16, US-20 |
| RF08 | US-17 |
| RF09 | US-07, US-13, US-19, US-20 |
| RF10 | US-18 |
| RNF01 | TT-02; revisão real também em TT-12 |
| RNF02 | TT-03, TT-06 |
| RNF03 | TT-05, TT-13; testes de cada história |
| RNF04 | TT-06 |
| RNF05 | TT-08 |
| RNF06 | TT-10 |
| RNF07 | TT-09 |
| RNF08 | TT-11 |
| Evoluções do guia | US-23–US-26, US-28; OCR US-27 é proposta adicional |
| Casos extremos | US-11–US-21, TT-05 e TT-13 |
| Execução só pelo README | TT-10, TT-11, TT-12 |
| Dois PDFs do mesmo fornecedor, histórico e cinco resultados | US-13, US-16–US-22; TT-12, TT-13 |
| Testes críticos no pipeline | TT-05, TT-06, TT-12 |
| PR com revisão e integração | TT-02, TT-12 |
| Compose e consulta de logs | TT-09–TT-12 |
| Metas de qualidade de código | TT-07; AP19; detalhe oficial ainda externo |
| Ampliação usuários/perfis/sessões | US-29–US-34, TT-14 |
| Ampliação correção e referência interna | US-35–US-36 |
| Configuração de preço por fornecedor/layout | US-37; US-08–US-10 |
| Processamento durável, arquivos e atores | TT-15–TT-16; US-06/US-07 |

## Especificação e pendências

As regras de negócio, arquitetura, acesso e qualidade estão em [decisoes.md](decisoes.md). A matriz D01–D15 em [planejamento.md](planejamento.md) distingue regras estabelecidas de detalhes de execução e validações externas.

O backlog contém 37 histórias e 16 tarefas técnicas. Todos os itens de implementação permanecem em Backlog. A avaliação de OCR não faz parte do suporte textual inicial.

Pendências externas: aceitação da massa sintética na apresentação, forma de evidenciar revisão em trabalho individual e detalhes oficiais da avaliação e submissão. Versões compatíveis e contratos executáveis serão fixados na configuração dos incrementos.

O escopo não inclui processamento de pagamentos, pedidos de compra ou registro de vendas. A aplicação apoia decisões de estoque, compras e precificação a partir dos dados importados.

## Publicação e acompanhamento

Consultar [documentação de Projects](https://docs.github.com/en/issues/planning-and-tracking-with-projects/learning-about-projects/quickstart-for-projects). Registrar links e números reais das issues após sua criação. Publicar uma issue não representa conclusão de seus critérios.

## Publicação verificada

- [Tabela Backlog](https://github.com/users/marcellmoura/projects/2/views/1) e [Quadro por status](https://github.com/users/marcellmoura/projects/2/views/2).
- 37 histórias de usuário e 16 tarefas técnicas; na publicação inicial, todos em Backlog e atribuídos a `marcellmoura`. TT-01 passou para Em andamento durante a revisão inicial; TT-02, ao iniciar o versionamento. Ambas permanecem abertas.
- Prioridade, épico, escopo, categoria, estimativa e fase conferidos contra as especificações locais em 2026-11-13. Estimativas permanecem A definir.
- Publicação do backlog não equivale a funcionalidades implementadas nem a submissão no Classroom.
