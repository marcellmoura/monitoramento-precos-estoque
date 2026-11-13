# Planejamento funcional e técnico

Versão: 2026-11-13. Estado: especificação documentada; implementação pendente.

## 1. Objetivo e referências

Sistema web para transformar tabelas e orçamentos de fornecedores em PDF em dados estruturados e rastreáveis. Preservar importações e preços para apoiar decisões de compra, estoque e preço de venda de uma loja de produtos naturais.

Referência de requisitos: documento do projeto integrador da disciplina de DevOps (AV2). O núcleo funcional corresponde a RF01–RF10 e as entregas técnicas a RNF01–RNF08. As extensões são identificadas no backlog sem alterar a definição do núcleo obrigatório.

- [Backlog e cobertura](backlog.md)
- [Regras e decisões de arquitetura](decisoes.md)
- [Roadmap](roadmap.md)
- [PDF de referência 1](../fixtures/pdfs/Fornecedor_1.pdf)
- [PDF de referência 2](../fixtures/pdfs/Fornecedor_2.pdf)
- [Massa sintética e expectativas](../fixtures/pdfs/sinteticos/LEIA-ME.md)

## 2. Escopo

### Núcleo obrigatório

Cadastro/consulta/edição de produtos, cadastro/consulta de fornecedores, dados básicos de estoque/preços, registro de tentativas, extração dos layouts de referência, conferência/correção, associação manual e reutilização de vínculos, histórico preservado, comparação e cinco classificações com consulta acessível.

### MVP estendido

Autenticação/perfis e sessões; layouts sintéticos tabela/fichas; configuração de leitura por fornecedor/layout; processamento durável em segundo plano; confirmação parcial; correções vinculadas e escolha explícita de referência de compra interna.

### Evoluções

Reposição por estoque mínimo, sugestão de revisão de venda por margem, gráficos/indicadores e leitura assistida de novos layouts. OCR é evolução em avaliação, sem biblioteca selecionada. Novos formatos não dispensam validação dos arquivos de referência nem garantem interpretação automática universal.

### Fora do escopo inicial

Pedidos de compra, pagamentos, registro de vendas, emissão fiscal e integrações externas. Ambiente previsto: execução local por Docker Compose. Repositório público não publica a aplicação nem documentos enviados.

## 3. Matriz funcional e de aceitação

Os RF abaixo preservam os IDs do guia. Os critérios aqui detalhados combinam a fonte e suas regras; complementações de projeto permanecem identificadas como propostas/pendências.

| ID | Comportamento exigido e dados | Aceitação a demonstrar | Testes / fase |
|---|---|---|---|
| RF01 | Cadastrar, consultar e editar produto: código, nome, categoria, unidade, preço de compra, preço de venda, estoque atual, estoque mínimo e status | Dados persistem e podem ser consultados/editados. US-01 exige nome/preço de venda e impede preço de venda negativo. Regras complementares em AP01–AP03 | T01; P5 |
| RF02 | Cadastrar e consultar fornecedores | Fornecedor cadastrado é consultável e selecionável na importação; campos mínimos em AP02 | T02; P5 |
| RF03 | Vincular item/apresentação de fornecedor a produto interno | Confirmação manual disponível; vínculo seguro reutilizado em próximas importações; código isolado não define identidade | T05/T08; P8 |
| RF04 | Enviar PDF associado a fornecedor; registrar data, arquivo, quantidade de itens e status | Cada tentativa é rastreável, inclusive rejeições/falhas; detectar duplicidade sem duplicar histórico silenciosamente | T03/T07; P6 |
| RF05 | Extrair referência, descrição, unidade/embalagem, quantidade e preço quando disponíveis; prévia para conferência | Ambos os layouts interpretados segundo mapeamento aprovado; ausências explícitas, sem inventar valores; dados conferíveis/corrigíveis | T04; P7 |
| RF06 | Sinalizar inválidos/não reconhecidos e manter pendentes de correção ou associação | Item incompleto/incerto não vira preço válido automaticamente; falha parcial não aparece como validação integral | T05/T06; P7–P8 |
| RF07 | Comparar com último preço válido do mesmo item/fornecedor/apresentação | Buscar a base correta mesmo se ausente na importação imediatamente anterior; nunca misturar fornecedores/embalagens | T08/T09; P9 |
| RF08 | Classificar aumentou, diminuiu, sem alteração, novo e pendente | Cinco estados demonstráveis; novo não tem percentual sem base; pendência permanece visível | T06/T09; P9 |
| RF09 | Consultar histórico por produto, fornecedor e importação | Filtros permitem rastrear registros anteriores preservados e sua origem; pendências/tentativas distinguíveis de preços válidos | T07/T10; P8–P9 |
| RF10 | Mostrar preço anterior/atual, diferença absoluta, variação percentual e status acessível | Cálculo correto com base positiva e rótulo textual; interface não depende apenas de cores | T09/T10/T12; P9 |

O exemplo US-01 é parcial em relação ao RF01. As histórias US-01–US-03 detalham cadastro, consulta e edição.

## 4. Módulos e interface

Agrupamentos de responsabilidades e funcionalidades previstas.

| Módulo | Responsabilidade | Telas/ações previstas para atender ao guia |
|---|---|---|
| Produto | Cadastro interno, situação e preços cadastrados | Lista, cadastro, detalhe e edição; validações de campos |
| Fornecedor | Cadastro e identificação da origem das importações | Lista/cadastro e seleção no envio do PDF |
| Estoque | Dados básicos atual/mínimo vinculados ao produto | Consulta e edição dos dados básicos, podendo compor a tela de produto; movimentação automática não definida |
| Importação | Recepção, extração por layout, conferência, erros, associação e confirmação | Envio, prévia, correção, associação, acompanhamento de status e consulta de tentativas |
| Precificação/comparação | Comparar preços válidos sem alterar preço de venda | Resultados com filtros e cinco classificações; detalhamento dos cálculos |
| Histórico | Consultar importações, preços e origem | Filtros por produto/fornecedor/importação; pode integrar módulos anteriores, sem serviço separado |

Estados de carregamento, vazio, erro e pendência devem ser compreensíveis; erros de validação indicam campo/motivo. Navegação, contratos e design visual serão detalhados nos incrementos correspondentes.


Usuários e Acesso: autenticação, administração de usuários, sessões e permissões por função (AP15–AP16, US-29–US-34).

## 5. Fluxo operacional

1. Entrar com usuário ativo e perfil autorizado; trocar senha temporária no primeiro acesso.
2. Cadastrar/consultar fornecedor e produtos com as permissões previstas.
3. Selecionar fornecedor e enviar PDF: salvar original, registrar tentativa e retornar ID.
4. Processar em segundo plano com trabalho durável e apresentar status/prévia.
5. Confirmar data comercial e configuração de coluna/unidade/embalagem; conferir/corrigir dados e associar produtos.
6. Confirmar itens válidos; manter os demais pendentes. Repetições não duplicam histórico.
7. Comparar com último preço válido anterior da mesma identidade comercial; consultar resultados e histórico.
8. Corrigir preços confirmados por registros vinculados, preservando original e recalculando comparações afetadas.
9. Escolher explicitamente preço válido como referência de compra interna. Estoque e venda não mudam automaticamente.

Estados de importação devem distinguir recepção/processamento, conferência, conclusão integral, conclusão com pendências, falha e duplicidade. Estados dos itens e classificação de preço são conceitos diferentes. Registrar ator e datas das ações críticas.

## 6. Regras de negócio

AP01–AP20 em [decisoes.md](decisoes.md) detalham regras e arquitetura. RF/RNF preservam os identificadores do documento da disciplina; RBxx são referências internas de rastreabilidade.

| Regra | Comportamento |
|---|---|
| RB01 | Último preço válido anterior por identidade comercial/fornecedor, mesmo se ausente no PDF imediatamente anterior |
| RB02 | Primeiro preço válido é novo, sem percentual anterior |
| RB03 | Importado zero, negativo ou ausente permanece inválido/pendente |
| RB04 | Código isolado não distingue apresentações; considerar unidade, embalagem e modalidade |
| RB05 | Primeira associação confirmada manualmente; reutilizar vínculo consistente; semelhança de nome só sugere |
| RB06 | Ausência de item não zera preço nem descontinua produto automaticamente |
| RB07 | Arquivo igual no mesmo fornecedor não duplica histórico; registrar tentativa e permitir retomada segura |
| RB08 | Falha parcial permanece explícita; confirmação parcial promove apenas válidos/associados |
| RB09 | Sugestão e importação não alteram venda automaticamente |
| RB10 | Percentual somente com base positiva; diferença absoluta positiva, direção textual e percentual com sinal |
| RB11 | Classificação comunicada por texto/valores, além de cor |
| RB12 | Nome, unidade e venda obrigatórios; venda não negativa; AP01 distingue venda zero de compra importada inválida |

Preços e percentuais com duas casas decimais e arredondamento convencional; estoques até três casas conforme unidade. Identidade e conversão explícitas, cronologia comercial confirmada, correções rastreáveis e confirmação idempotente são requisitos de integridade.

## 7. Estrutura técnica

Frontend React/TypeScript/Vite → API REST Spring Boot → PostgreSQL. Organização por domínios e separação de entrada HTTP, serviços/regras e persistência. Leitura de PDF separada da validação comercial e dos cálculos. Aplicação modular, sem microsserviços.

Arquivos originais em volume persistente; metadados, itens, trabalho pendente, histórico e sessões no banco. Acesso a PDFs e custos restrito a Administrador/Compras. Configuração por ambiente e nomes internos gerados para arquivos.

### Modelo conceitual

| Conceito | Dados/relações necessários ou propostos |
|---|---|
| Produto interno | Campos RF01; estoque pode ficar no produto no MVP, sem presumir livro de movimentações |
| Fornecedor | Identificação mínima a definir; possui itens/apresentações e importações |
| Item/apresentação do fornecedor | Referência opcional, descrição, unidade, embalagem e possível modalidade de preço; identidade não pode depender só do código |
| Vínculo | Item/apresentação → produto interno; vínculo manual persistente e origem da associação para rastreio |
| Importação/tentativa | Fornecedor, arquivo/identificador, data, contagens, status, hash candidato e erros; vínculo com linhas extraídas |
| Item importado | Dados originais, valores conferidos, inconsistências, associação e estado; referência à importação e à apresentação |
| Registro de preço válido | Valor, base comercial e origem; histórico preservado com vínculo ao item/importação; correção vinculada em AP07 |
| Resultado de comparação | Atual, anterior, diferença, percentual e classificação; calcular na consulta ou persistir resultado é decisão pendente |

O schema SQL será detalhado nas migrations. AP09 definiu que preço de compra interno só muda por escolha explícita de referência válida; importações não alteram automaticamente compra interna, venda ou estoque.

### Ferramentas e propósito

Ver AP16/AP17 em [decisoes.md](decisoes.md). Bibliotecas: versões numéricas compatíveis serão fixadas antes da configuração, sem prereleases.

| Área | Escolha aprovada |
|---|---|
| HTTP/validação | Spring Web e Jakarta Validation/Hibernate Validator |
| Persistência/schema | Spring Data JPA/Hibernate, driver PostgreSQL e Flyway; Flyway cria/evolui schema, Hibernate valida |
| Extração | Apache PDFBox com interpretação dos layouts; OCR fora do MVP acordado |
| Segurança | Spring Security, Spring Session JDBC/PostgreSQL e Argon2id/Bouncy Castle |
| Testes backend | Spring Boot Test/JUnit/Mockito/MockMvc e Testcontainers/PostgreSQL |
| Cobertura | JaCoCo backend e cobertura frontend com Vitest; metas internas AP19 |
| Frontend | React/TypeScript/Vite, React Router, Fetch, CSS Modules; Vitest/Testing Library/Playwright |
| CI/execução | GitHub Actions/SonarQube Cloud e Dockerfiles/Compose; logs, banco e PDFs persistentes |

Status: dependências e configuração executável ainda não instaladas. Registrar mudanças de dependências no controle de arquitetura.

## 8. Requisitos DevOps, evidências e qualidade

| ID | Obrigação do guia | Plano de evidência |
|---|---|---|
| RNF01 | GitHub, commits compreensíveis, branches e PRs revisados | Histórico real, PR com revisão e ligação a histórias/tarefas; alinhar revisão em equipe individual |
| RNF02 | Build backend Maven e frontend npm | Comandos locais documentados e build de ambos na CI; wrapper/lockfile propostos |
| RNF03 | Testes críticos: aumento, redução, igualdade, novo, zero e associação pendente | Matriz T05/T06/T09 e relatórios executados no pipeline |
| RNF04 | Actions em push e/ou PR com build/testes; falhas reprovam pipeline | Jobs sem mascarar erros; logs e execução vinculados ao commit |
| RNF05 | Banco, portas e parâmetros por ambiente; sem segredos publicados | Configuração externa, exemplo sem valores sensíveis e verificação de arquivos versionados |
| RNF06 | Dockerfiles e Compose com instruções | Subida limpa de frontend/backend/banco, configuração e persistência verificadas |
| RNF07 | Logs de início/conclusão/falha com ID e motivo | Importação de sucesso e com erro; consulta dos logs pelo roteiro |
| RNF08 | README com requisitos, execução, testes, pipeline e implantação/demonstração | Outra execução seguindo exclusivamente o README, sem passos implícitos |

### Metas internas de qualidade

A em segurança, confiabilidade e manutenibilidade no código novo e resultado geral; cobertura >=80% e duplicação <=3% no código novo; Security Hotspots revisados e Quality Gate Passed. Gate reprovado falha a verificação de qualidade. Analisar frontend/backend com relatórios reais, sem excluir código funcional para melhorar métricas.

## 9. Estratégia e matriz de testes

Testes unitários das regras; testes HTTP de sucesso/validação/erro; integração de banco/importação e testes de frontend; poucos fluxos completos de ponta a ponta com alto valor. Ferramentas definidas em AP17. Testes devem acompanhar cada incremento e rodar na CI; cobertura é indicador, não substituto para cenários críticos.

| ID | Cenários e resultado verificável | Tipo |
|---|---|---|
| T01 | Produto válido persiste/consulta/edita; nome/venda ausentes falham; venda negativa falha; estoque/status/unidade conforme regras aprovadas | Domínio, HTTP, persistência e frontend |
| T02 | Fornecedor cadastra/consulta e fica selecionável; dados inválidos tratados conforme contrato definido | HTTP/integração/frontend |
| T03 | Upload associado a fornecedor; arquivo inválido/vazio/incompatível/corrompido gera resposta e tentativa rastreável; contagens/status corretos | HTTP e integração |
| T04 | Cada PDF original extrai exatamente 2 linhas com textos/valores esperados; RF05 ausentes não são fabricados; mapeamento das duas colunas segue D05 | Parser com fixtures e integração |
| T05 | Associação manual persistida/reutilizada; descrição incerta e unidade ausente ficam pendentes; não vincular apenas por código/nome entre fornecedores | Unitário/integração/frontend |
| T06 | Preço zero/ausente não entra no histórico válido; dado malformado e falha parcial não viram sucesso integral; política de negativo a definir | Unitário, parser e HTTP |
| T07 | Reimportar mesmo arquivo, repetir confirmação e falhar durante gravação não duplica histórico nem perde rastreio; confirmar comportamento para mesmo arquivo em outro fornecedor | Integração/transação |
| T08 | Código repetido com embalagem distinta; mesmo nome em fornecedores distintos; item ausente e depois reaparece; somente a base correta é escolhida | Unitário/integração |
| T09 | 33→35 (+6,06%), 15→14 (−6,67%), 38→38 (0%); primeiro preço NOVO sem percentual; pendência separada; base inválida não causa divisão por zero | Unitário das regras críticas |
| T10 | Filtros produto/fornecedor/importação; registros antigos preservados; importação anterior sem item não elimina última base; data retroativa/desempate segundo D09 | Integração/consulta/frontend |
| T11 | Build/teste falho reprova CI; relatórios correspondem ao código; análise e Quality Gate identificáveis; ambiente sem credenciais não inventa aprovação | Verificação do pipeline |
| T12 | Cadastro→upload→conferência→associação→confirmação→segunda importação→comparação/histórico; cinco classificações e estados de erro compreensíveis, status textual além da cor | Fluxo integrado/ponta a ponta |
| T13 | Ambiente limpo por Compose, banco persistente, parâmetros externos, logs sucesso/falha com ID, execução exclusivamente por README | Verificação operacional |

Dados sintéticos devem ficar separados dos originais e documentados. Não alterar PDFs do professor para fazer testes passarem. Campos monetários devem ser comparados sem erro de ponto flutuante; arredondamento conforme AP09, sem tolerância arbitrária. Regressões descobertas devem receber teste quando ele previne repetição relevante.

### Testes complementares

- T14: autenticação, primeiro acesso/reset, timeout/logout/revogação, último administrador, CSRF, permissões e campos sensíveis.
- T15: confirmação parcial, correção preservando original, retroativos/desempate, referência interna e processamento interrompido/retentativa.
- T16: limites de upload, arquivos protegidos/corrompidos, volume persistente, acesso ao PDF e autoria de ações.
- T17: margem, reposição, gráficos/indicadores e visibilidade por perfil.

## 11. Limitações dos documentos de referência

O primeiro layout contém referência, produto, unidade, Preço KG e Preço Fardo. O segundo contém descrição, ATACADO e VAREJO, sem unidade/referência explícitas. Ambos têm dois itens com valores 32 e 78; não representam uma sequência temporal de variações do mesmo fornecedor.

A configuração de leitura apresenta todas as colunas e solicita confirmação de modalidade/unidade/embalagem, salvando-a por fornecedor/layout. Campos ausentes não são inventados e conversões exigem fator explícito.

A massa sintética contém fornecedores fictícios com duas versões, cenários de aumento, redução, igualdade, novo e pendências. Os originais permanecem inalterados. A matriz ainda exige cenários adicionais como reaparecimento, embalagem diferente, preço ausente e falha parcial.

## 12. Controle de definições e execução

| ID | Situação |
|---|---|
| D01 | Entregas: backlog 15/11/2026 e final prevista 23/11/2026; detalhes de submissão e revisão individual a confirmar |
| D02 | Repositório público marcellmoura/monitoramento-precos-estoque; Project AV2 DevOps — Product Backlog |
| D03 | Frontend e testes definidos em AP17 |
| D04 | Verificar/fixar versões estáveis e compatíveis na configuração |
| D05 | Configuração de colunas/modalidade em AP04; escolha comercial confirmada na importação |
| D06 | Associação e reutilização em AP05 |
| D07 | Parcial/correção/duplicidade em AP06/AP07/AP10 |
| D08 | Cadastro, fornecedor e unidades em AP01–AP03 |
| D09 | Cronologia/cálculos em AP08/AP09; detalhar interações de desempate e alteração controlada de unidade |
| D10 | Referência interna/estoque manual em AP09 |
| D11 | Metas internas AP19; detalhe oficial da avaliação a confirmar |
| D12 | Confirmar aceitação da massa sintética na apresentação ou obter versões adicionais |
| D13 | Bibliotecas em AP16/AP17; OCR ainda em avaliação |
| D14 | Reposição, margem e indicadores em AP12–AP14 |
| D15 | Upload/armazenamento, perfis/sessões e API em AP11/AP15–AP18; detalhar contratos e parâmetros operacionais por incremento |

### Revisão inicial da TT-01 — 2026-11-13

Identificação do repositório, organização do E0 e stack verificadas; primeiro critério de TT-01 atendido. Pendências D01/D04/D11/D12 e detalhamentos executáveis de D05–D10/D15 permanecem acompanhados por incremento; não impedem o versionamento documental da TT-02. Nenhuma regra, tecnologia ou funcionalidade nova foi definida nesta revisão. Evidência e classificação das pendências em [TT-01](backlog/TT-01.md).

## 13. Aceitação final

1. Inicializar aplicação seguindo exclusivamente o README.
2. Importar ao menos dois PDFs compatíveis do mesmo fornecedor, comprovando histórico e comparação.
3. Demonstrar aumento, redução, igualdade, novo e pendente.
4. Executar testes dos casos críticos com sucesso no pipeline.
5. Evidenciar revisão e integração por Pull Request.
6. Executar serviços por Docker Compose e consultar logs de importação.

Validar também autenticação/perfis, extensões do MVP e metas internas de qualidade. Evoluções têm validação integrada em P14. Evidências devem corresponder a commits, execuções e entregas reais.
