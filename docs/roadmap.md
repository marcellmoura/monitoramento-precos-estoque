# Roadmap do projeto

Atualização: 2026-11-13. Estado: especificação preparada e 53 issues publicadas no Project; Git local e documentação versionados; primeiro incremento documental publicado pela TT-02; estrutura executável e implementação pendentes.

## Entregas

- Backlog E0: **15/11/2026**.
- Entrega final prevista: **23/11/2026**.

As etapas seguem dependências e critérios de saída, sem datas intermediárias. As datas não significam entregas já realizadas.

## Acompanhamento

Consultar [planejamento](planejamento.md), [decisões](decisoes.md) e [backlog](backlog.md). Estado Concluída exige evidências dos critérios; criação de documentos não conclui funcionalidades.

| ID / marco | Etapa | Estado | Depende de | Critério para finalizar | Evidência atual ou esperada |
|---|---|---|---|---|---|
| P0 / preparação | Inventariar requisitos e preparar documentação técnica | Concluída | Documento da disciplina | RF/RNF mapeados, fontes conferidas e regras documentadas | Planejamento, decisões técnicas e roadmap |
| P1 / alinhamento | Revisar e aprovar escopo, forma de trabalho e decisões iniciais | Concluída | P0 | Especificação funcional/técnica e controle de escopo registrados; versões verificadas antes da configuração | AP01–AP20; validações externas e detalhes executáveis por incremento |
| P2 / E0 | Detalhar histórias de usuário e priorizar Product Backlog | Concluída | P1 e definição do formato da entrega | RF01–RF10 cobertos por histórias com ator, objetivo, benefício e critérios testáveis; prioridade, épico e escopo identificados; tarefas DevOps rastreáveis; MVP separado das evoluções; estimativas definidas ou explicitamente a definir | [Project público](https://github.com/users/marcellmoura/projects/2) com 53 issues (37 US/16 TT); campos e vínculos verificados; links em `docs/backlog.md`. Submissão no Classroom pendente |
| P3 / E1 | Configurar Git/GitHub e estrutura técnica mínima | Em andamento | P1, P2 e aprovação da stack/versões | Raiz e visibilidade definidas; estrutura frontend/backend, wrappers/lockfile, README inicial e exemplo de ambiente; build local reproduzível; branch/PR com revisão; nenhuma regra funcional fora do incremento definido | Repositório, PR e instruções de build verificadas |
| P4 / E1–E3 | Estabelecer CI e análise SonarQube desde o início | Pendente | P3; contas/chaves; metas internas AP19 definidas | Actions executa build/testes em push/PR definidos; falhas reprovam; análise via CI com secret; configuração do scanner coerente; resultado e Quality Gate inspecionados; limitações iniciais registradas | Workflow, execução de CI e análise vinculada ao commit; eventual `Not computed` permanece pendência |
| P3a / MVP adicional | Usuários, autenticação e autorização | Pendente | P3, P4 | US-29–US-34; perfis/filtragem, primeiro acesso/reset, sessões 30min/revogação/CSRF, bootstrap sem senha pública; testes incrementais TT-14 e autoria TT-16 | Matriz AP15/AP16 testada na API e frontend; implementação pendente |
| P5 / E1 | Implementar cadastros e dados básicos de estoque | Pendente | P3a; AP01–AP03 definidos | Cadastrar/consultar/editar produto (RF01), cadastrar/consultar fornecedor (RF02), dados de estoque/preços/status; validações e testes HTTP/domínio/persistência; fluxo frontend/API definido | Histórias aceitas, testes e PR revisado |
| P6 / E2 | Receber PDFs e registrar tentativas de importação | Pendente | P5; AP04–AP11; trabalho assíncrono TT-15 | Fornecedor obrigatório; arquivo/data/contagens/status rastreáveis; entradas inválidas e falhas registradas; duplicidade detectada sem duplicação silenciosa | Testes de upload e duplicidade; logs com ID; PR |
| P7 / E2 | Extrair ambos os layouts e conferir/corrigir dados | Pendente | P6; significado das colunas e unidade ausente definidos | Dois itens detectados em cada PDF original; incluir os layouts sintéticos tabela/fichas já nesta etapa, conforme AP11 e fixtures de referência; valores brutos conferíveis; campos ausentes não inventados; erros e falhas parciais visíveis; correções validadas | Testes com PDFs de referência, prévia na interface e PR |
| P8 / E2 | Associar apresentações e concluir importação preservando histórico | Pendente | P7; AP05–AP08 definidos | Vínculos manuais reutilizáveis; associações inseguras pendentes; somente dados válidos promovidos; confirmação/repetição consistente, sem sobrescrever histórico nem perder tentativas | Testes integrados, importação conferida, histórico persistido e PR |
| P8a / MVP adicional | Correções, referência interna e cronologia comercial | Pendente | P8 | US-35–US-37, correção vinculada/ator, referência explícita, retroativos e desempate; confirmação parcial sem duplicação; nada altera venda/estoque automaticamente | T15, PR e histórico verificáveis |
| P9 / E3 | Comparar preços e consultar resultados/histórico | Pendente | P8a; AP08/AP09 | RF07–RF10 completos; último preço válido por identidade correta, mesmo se item ausente no PDF anterior; cinco classificações, diferenças e percentual correto; filtros e rótulos textuais acessíveis | Testes dos casos críticos e de histórico; interface demonstrável; CI aprovado |
| P10 / E4 | Containerizar e parametrizar ambiente completo | Pendente | P3; integração completa verificada após P9 | Frontend, backend e PostgreSQL executam via Compose em ambiente limpo; parâmetros externos e persistência funcionam; procedimentos de inicialização e consulta de logs documentados | Dockerfiles, Compose, README e registro de execução limpa |
| P11 / E3–E5 | Validar sistema completo e qualidade final | Pendente | P9, P10; massa de demonstração aceita e metas internas AP19 | Casos da matriz de testes executados; fluxos de ponta a ponta e erros verificados; regressões corrigidas; CI verde; classificação A no escopo definido e Quality Gate com resultado efetivo conforme critério definido | Relatórios, análise SonarQube, PRs e checklist de aceitação |
| P12 / E5 | Documentar, ensaiar e realizar entrega | Pendente | P11 | Execução somente pelo README; importar ao menos dois PDFs compatíveis do mesmo fornecedor; demonstrar aumento, redução, igualdade, novo e pendente; mostrar histórico, CI, revisão de PR e logs/Compose; confirmar formato e prazo no Classroom | README final, roteiro, links e registro real de apresentação/entrega |
| P13a / evolução | Sinalizar oportunidades de reposição | Pendente | Núcleo MVP validado; regras definidas | Comparar estoque atual/mínimo, exibir oportunidades e cobrir limites/estados com testes; não criar compras automaticamente | Histórias, testes, PR e demonstração |
| P13b / evolução | Sugerir revisão de preço de venda | Pendente | Política de margem e base de custo definidas | Sugestão calculada e explicável, com testes de cálculo/arredondamento; preço cadastrado nunca muda automaticamente | Política documentada, testes e PR |
| P13c / evolução | Gráficos e indicadores gerenciais | Pendente | Histórico validado; indicadores definidos | Visualizar evolução com filtros/unidades coerentes e dados conferíveis; indicadores definidos sem inventar escopo | Histórias, validação dos dados e testes |
| P13d / ampliação | Melhorar reconhecimento e aceitar layouts variados de PDF | Pendente | P7–P9; estratégia de extração assistida e limites definidos | Layout desconhecido pode ser conferido/mapeado; campos incertos pendentes; avaliar e testar OCR para digitalizados; testar multipágina/colunas diferentes, preservar origem e falhas; documentar limites | Matriz ampliada de fixtures, testes, PRs e guia de formatos/limitações |
| P14 / conclusão ampliada | Validar todas as funcionalidades planejadas | Pendente | P11–P13d | Regressão integrada das evoluções, CI/qualidade verificados e README/roteiro atualizados; diferenciar entrega obrigatória E5 de conclusão do escopo estendido | Checklist completo, evidências e registro real de entrega |

## Próxima atividade

Revisão inicial da TT-01 realizada: repositório, E0 e stack conferidos, sem impedimento para o versionamento documental. Primeiro incremento documental da TT-02 publicado na branch `docs/tt-02-documentacao-inicial`, com [Pull Request #54](https://github.com/marcellmoura/monitoramento-precos-estoque/pull/54) e validação documental registrada. TT-02 permanece aberta para os critérios restantes. Próximo alinhamento técnico: verificar versões estáveis compatíveis e o escopo da estrutura mínima da TT-03, antes de gerar arquivos executáveis. Conferir a submissão do E0 no Classroom. Versões compatíveis e contratos concretos serão alinhados antes da configuração executável e dos incrementos funcionais dependentes.

Testes, logs, documentação e qualidade acompanham os incrementos; as etapas finais consolidam a verificação. CI é introduzida cedo para detectar regressões continuamente.

## Definições de entrega e pendências atuais

- Revisão individual esclarecida: o responsável abre, confere e aceita o próprio PR.
- PDFs sintéticos permitidos na apresentação, além dos originais de referência.
- Classificação A é a orientação relatada; percentuais e demais metas de AP19 são internos ao projeto.
- E0 será submetido por link do Project na atividade do Classroom; envio ainda não realizado.
- Permanecem pendentes as versões numéricas compatíveis e os contratos concretos dos incrementos dependentes.
- TT-02 aguarda apenas a organização dos diretórios frontend/backend na TT-03; revisão individual deixa de ser pendência externa.

## Histórico de progresso

| Data | Etapa | Registro | Evidência / estado |
|---|---|---|---|
| 2026-11-13 | P0–P1 | Documentação de requisitos, regras de negócio, arquitetura e critérios de qualidade preparada | Planejamento e AP01–AP20; sem implementação |
| 2026-11-13 | P2 | Backlog especificado com 37 histórias e 16 tarefas; RF/RNF e dependências rastreados | 53 arquivos; publicação das issues pendente |
| 2026-11-13 | P3 / repositório | Criado repositório público vazio | [marcellmoura/monitoramento-precos-estoque](https://github.com/marcellmoura/monitoramento-precos-estoque); sem commits ou arquivos enviados. TT-02/P3 continuam pendentes nas demais atividades. |
| 2026-11-13 | P2 / Project | Criado Project público em tabela, com repositório padrão monitoramento-precos-estoque | [AV2 DevOps — Product Backlog](https://github.com/users/marcellmoura/projects/2); ainda sem itens. Publicação das issues pendente. |
| 2026-11-13 | P2 / publicação concluída | Publicadas 37 histórias e 16 tarefas técnicas, atribuídas a marcellmoura; campos de prioridade, épico, escopo, categoria, estimativa e fase preenchidos | 53 registros exportados e comparados com os arquivos locais, sem divergências; todos em Backlog. Tabela Backlog e Quadro por status; submissão no Classroom pendente. |
| 2026-11-13 | TT-01 / revisão inicial | Repositório, raiz local, organização do E0 e stack conferidos; primeiro critério atendido; pendências classificadas por incremento | [Registro na issue #38](https://github.com/marcellmoura/monitoramento-precos-estoque/issues/38#issuecomment-6859744791); TT-01 Em andamento e aberta. TT-02 sem bloqueio documental; ainda não iniciada. |
| 2026-11-13 | TT-02 / início do versionamento | Git inicializado exclusivamente em `Projeto/`, remoto confirmado, base `main` com `.gitignore` e branch documental criada | Seleção de 68 arquivos técnicos auditada; links locais válidos; PDFs originais preservados. Sem configuração executável da aplicação; submissão no Classroom e validação da revisão individual continuam pendentes. |

| 2026-11-13 | TT-02 / publicação documental | Enviados commits da base Git e documentação; Pull Request relacionado à issue #39 | [PR #54](https://github.com/marcellmoura/monitoramento-precos-estoque/pull/54); 69 arquivos selecionados; links, originais e diff conferidos. Estado de integração consultável no PR. Critérios de estrutura executável e revisão individual continuam pendentes. |

| 2026-11-13 | TT-01 / definições de entrega | Esclarecidos revisão individual, uso de PDFs sintéticos, orientação de classificação A e entrega do backlog por link | D01/D11/D12 atualizados com origem e data; removidas as quatro pendências externas. TT-01 segue com detalhamentos técnicos; TT-02 aguarda a estrutura frontend/backend. |

Atualizar estados e acrescentar histórico após cada incremento, com evidências e pendências reais. Não registrar apresentação ou submissão antes de sua realização.
