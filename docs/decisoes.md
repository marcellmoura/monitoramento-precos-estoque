# Decisões de arquitetura e negócio

Versão: 2026-11-13. Especificação das regras funcionais, escolhas técnicas e limites do projeto. AP01–AP20 identificam as decisões referenciadas no backlog.

## Identificação, entregas e forma de trabalho

- Desenvolvimento individual, conta GitHub `marcellmoura`, repositório público `monitoramento-precos-estoque`. [Repositório criado](https://github.com/marcellmoura/monitoramento-precos-estoque), público e inicialmente vazio.
- Nome do sistema: Monitoramento de Preços, Estoque e Compras. Project público: [AV2 DevOps — Product Backlog](https://github.com/users/marcellmoura/projects/2), com repositório padrão `monitoramento-precos-estoque`.
- Um repositório com frontend, backend e documentação; main, branches por tarefa, commits pequenos, issues e PRs revisáveis. No trabalho individual, o próprio responsável abre, confere e aceita o PR; não se exige outro integrante como revisor.
- Backlog E0: **15/11/2026**, prazo da primeira entrega. Final: **23/11/2026**, data prevista para entrega final. Somente duas datas; sem cronograma diário/intermediário.
- Revisar especificação, dependências e critérios de aceitação antes de cada incremento. Planejamento não equivale a implementação concluída.

## AP01 — Cadastro de produtos

- Nome, unidade e preço de venda obrigatórios. Categoria opcional. Preço de compra pode ficar ausente enquanto desconhecido; ausência não equivale a zero.
- Estoque atual e mínimo inicialmente zero, editáveis. Status inicialmente ativo. Código interno gerado pelo sistema e separado da referência do fornecedor.
- Preços e estoques não negativos. Venda zero permitida. Na importação, preço zero, negativo ou ausente é inválido/pendente, não histórico válido.
- Administrador e Compras cadastram produtos. Compras pode informar preço inicial de venda, mas alterações posteriores são exclusivas do Administrador. Margem também é exclusiva do Administrador.

## AP02 — Fornecedores

- Código interno gerado, nome obrigatório; CNPJ, telefone e e-mail opcionais. Nome pode se repetir; identidade interna não depende dele.
- Validar formato de telefone/e-mail se preenchidos; não obrigar CNPJ. Validação fiscal especial não foi estabelecida.

## AP03 — Unidade e estoque

- Unidades iniciais: KG, G, L, ML e UN. Quantidades de KG/G/L/ML com até 3 casas decimais; UN apenas inteiro. Atual e mínimo na unidade do produto; sem negativos.
- Embalagem do fornecedor é separada da unidade interna. Conversão exige fator informado e confirmado; não adivinhar.
- Produto com vínculos/histórico não pode trocar unidade por edição simples: exigir alteração controlada para preservar coerência. A interação desse procedimento deverá ser apresentada antes de implementar.
- Cotação não altera estoque: ajustes manuais no MVP, com registro do responsável.

## AP04 — Colunas e modalidades de preço

- Extrair/mostrar todas as colunas de preços. Na primeira importação, usuário confirma coluna de compra, unidade e embalagem; salvar configuração por fornecedor/layout.
- Reusar configuração somente quando compatível; mudança/incerteza exige reconfirmação.
- Comparar mesma modalidade/apresentação. Não misturar KG/Fardo nem outras bases sem conversão explícita. Varejo do fornecedor não é automaticamente venda interna.

## AP05 — Associação

- Na primeira importação, usuário confirma vínculo com produto interno. Persistir por fornecedor, referência quando existir, unidade e embalagem; código isolado não é identidade universal.
- Reaproveitar vínculo consistente. Nome parecido apenas sugere. Falta/mudança de apresentação que torne a identificação incerta mantém pendência.
- Produto inexistente pode ser cadastrado por perfil autorizado e depois associado; perfil sem permissão não ganha acesso por esse atalho.

## AP06 — Confirmação parcial

- Confirmar itens válidos e associados sem bloquear pelos restantes. Eles entram no histórico/comparação; os demais ficam visíveis com motivo.
- Status "concluída com pendências" enquanto restarem problemas, sem alegar validação integral. Corrigir/confirmar restantes depois sem duplicar os já salvos.
- Falha de leitura integral: status de falha, motivo e tentativa rastreáveis.

## AP07 — Correções posteriores

- Não sobrescrever preço confirmado. Criar correção vinculada ao original, com valor correto, motivo, data e responsável.
- Original permanece consultável, mas preço incorreto deixa de ser base válida. Recalcular comparações afetadas.
- Correção não altera automaticamente preço de venda. Administrador/Compras podem corrigir importações; manter histórico auditável.

## AP08 — Cronologia comercial

- Data de referência comercial extraída quando disponível e confirmada pelo usuário. Sem data no documento, sugerir data do envio, ajustável antes da confirmação.
- Guardar também envio e confirmação. Ordenar pela data comercial; comparar com último preço válido anterior da mesma identidade/modalidade.
- Retroativos entram no período correto e recalculam comparações afetadas.
- Conflito de preços da mesma apresentação/data exige usuário informar ordem ou substituição, sem escolha silenciosa; substituição preserva rastreabilidade.

## AP09 — Preços, cálculos e referência interna

- Cálculo decimal, preços em BRL com 2 casas; percentual exibido com 2 casas, arredondamento convencional (terceira casa >= 5 arredonda a magnitude para cima).
- Diferença absoluta sempre positiva, acompanhada da direção textual. Percentual com sinal: ((atual - anterior) / anterior) × 100, somente com base positiva. Novo: sem anterior/percentual.
- Somente valores válidos compõem bases; comparações usam precisão decimal, sem tolerância arbitrária.
- Preços confirmados alimentam histórico por fornecedor/apresentação. Preço de compra interno não muda sozinho: Administrador/Compras escolhem um preço válido como referência e sua origem é guardada.
- Venda e estoque nunca são alterados automaticamente pela importação.

## AP10 — Duplicidade e reprocessamento

- Identificar conteúdo idêntico mesmo com nome diferente. Mesmo arquivo/fornecedor: registrar nova tentativa, apontar importação existente, sem duplicar itens/histórico.
- Pendências são retomadas na importação existente. Falha técnica permite repetir processamento com histórico de tentativas preservado.
- Arquivo igual em outro fornecedor pede confirmação antes de prosseguir; se confirmado, históricos separados. Correção posterior segue AP07, não exige reenviar.

## AP11 — Upload, processamento e armazenamento

- PDF até 10 MB e 50 páginas, limites configuráveis. Validar conteúdo além da extensão. Arquivo corrompido, protegido por senha ou incompatível: erro explícito e tentativa registrada.
- MVP: dois layouts originais + sintéticos tabela/fichas. Digitalizados sem texto não suportados até etapa OCR; não prometer extração universal.
- Salvar original e tentativa, devolver identificador; extrair em segundo plano e consultar status/prévia. Trabalho pendente registrado no banco, sem depender só da memória.
- Interrupção não vira conclusão: identificar e permitir nova tentativa sem duplicação. Critério temporal operacional de detecção deverá ser verificado na implementação e documentado.
- Originais em pasta configurável e volume persistente; PostgreSQL guarda dados, histórico, erros e referência ao arquivo. Nome interno gerado; nome original preservado.
- Preservar documentos enquanto projeto estiver em uso, sem exclusão automática. Somente Administrador/Compras acessam PDFs por fluxo autorizado.
- Uploads e dados de execução fora do Git. Exemplos/sintéticos são candidatos a versionamento. Logs sem credenciais ou despejo integral do PDF.

## AP12 — Reposição

- Alertar produtos ativos quando estoque atual <= mínimo, inclusive ambos zero. Mostrar unidade, atual, mínimo e motivo.
- Não gerar compras, não alterar estoque, não sugerir quantidade sem estoque-alvo definido.

## AP13 — Margem e sugestão de venda

- Margem desejada configurável por produto, sem percentual padrão; somente Administrador altera política.
- Base: preço de compra de referência escolhido internamente. Fórmula: custo / (1 - margem), margem de 0% até menos de 100%.
- Exigir custo positivo e margem definida; caso contrário explicar impossibilidade. Mostrar custo, margem, resultado e preço atual. Nunca aplicar automaticamente.
- Só custo cadastrado entra na fórmula; frete/tributos/despesas não incluídos sem decisão posterior. Ex.: 20 / (1 - 0,20) = 25.

## AP14 — Gráficos e indicadores

- Série por produto, fornecedor, apresentação e período, baseada em preços válidos/data comercial; não misturar bases.
- Resumo da importação: quantidades de aumento/redução/igualdade/novo/pendente. Resumo de estoque: quantidade de produtos ativos em reposição.
- Dados-base conferíveis; sem tendência inventada quando faltarem dados. Visibilidade limitada ao perfil: custos e importações apenas Administrador/Compras; alertas de reposição Administrador/Estoque.

## AP15 — Acesso por função

Um perfil por usuário. Perfis:

| Perfil | Permissões |
|---|---|
| Administrador | Tudo, incluindo usuários, perfis, margens e edição posterior de venda |
| Compras | Cadastrar produtos/fornecedores, informar venda inicial, importar/conferir/associar/confirmar/corrigir, selecionar referência de compra e consultar históricos/comparações/sugestões. Sem gerir usuários, alterar margens ou editar venda posterior |
| Estoque | Consultar identificação/quantidades e ajustar estoque atual/mínimo; acompanhar reposição. Sem custos de compra ou importação |
| Consulta | Catálogo, preços de venda e disponibilidade de estoque; sem alterações nem custos |

Validar permissão na API e nos campos devolvidos; UI adapta navegação/ações, sem ser única barreira. Cadastros iniciais por Compras podem conter os campos do cadastro; ajustes posteriores de estoque são Administrador/Estoque. Acesso não previsto permanece negado até alinhamento, sem presumir novas permissões.

## AP16 — Login, usuários e sessões

- E-mail único e senha; administrador cadastra nome/e-mail/perfil/senha temporária. Sem cadastro público.
- Troca obrigatória no primeiro acesso; senha com mínimo de 12 caracteres e hash seguro, nunca texto puro. Esquecimento: administrador redefine temporária; sem e-mail nesta versão.
- Sessão expira após 30 minutos sem requisições autenticadas; sem chamadas só para prolongá-la. Logout disponível.
- Inativação, mudança de perfil ou redefinição de senha invalida sessões correspondentes. Proteger último Administrador ativo contra desativação/rebaixamento.
- Primeiro Administrador por procedimento local documentado, sem senha padrão publicada.
- Spring Security + Spring Session JDBC em PostgreSQL; cookie de sessão HttpOnly, SameSite configurado e Secure com HTTPS. CSRF integrado ao React; sessão no servidor, sem JWT como mecanismo de autenticação escolhido.
- Senhas: Argon2id por Argon2PasswordEncoder com Bouncy Castle; parâmetros medidos/configurados no ambiente.
- Registrar ator de importações, confirmações, correções e ajustes de estoque.

## AP17 — Ferramentas

- Frontend: React + TypeScript, Vite, React Router, Fetch centralizado em serviços, CSS Modules. Testes Vitest + React Testing Library; Playwright para fluxos completos.
- Backend: Spring Boot/Maven, Spring Data JPA/Hibernate, PostgreSQL, Flyway, Jakarta Validation/Hibernate Validator, Apache PDFBox.
- Testes backend: Spring Boot Test/JUnit/Mockito/MockMvc, Testcontainers com PostgreSQL, JaCoCo. Testar também autorização, sessões e exposição indevida de campos.
- Flyway controla schema; Hibernate valida compatibilidade. Módulos com HTTP, serviços/regras e persistência separados; regras testáveis sem PDF/endpoint.
- Fixar versões atuais estáveis e mutuamente compatíveis na configuração; não escolher prereleases nem versões isoladas incompatíveis. Base numérica aprovada e verificada na TT-03 em 13/11/2026: Temurin 25.0.5+8, Boot 4.1.2, Maven 3.10.0, Node 26.13.1, React 19.3.1, Vite 8.3.8 e TypeScript 6.0.3. Lint com ESLint/typescript-eslint aprovado; TS 6 atende à faixa desse analisador. Versões completas, fontes e validações em [ambiente](ambiente.md). Dependências futuras serão fixadas antes do incremento correspondente.

## AP18 — API

- REST/JSON; mensagens em português, código de erro estável, campos inválidos quando aplicável.
- 400 entrada inválida; 401 sem sessão válida; 403 sem permissão; 404 não encontrado; 409 conflito; 500 falha inesperada com mensagem genérica e ID rastreável.
- Não retornar senha/hash/detalhes de exceção. Pendência de item dentro de PDF não necessariamente falha toda a requisição.
- Listagens paginadas: padrão 20, máximo 100. Filtrar campos conforme perfil também no backend.

## AP19 — Qualidade

Orientação da disciplina informada em 2026-11-13: obter classificação A no SonarQube. Não foram informados percentuais de cobertura, duplicação ou limites adicionais em aula.

Metas internas de qualidade, mantidas como escolhas do projeto e não exigências adicionais atribuídas ao professor:

- A em segurança, confiabilidade e manutenibilidade, tanto código novo quanto resultado geral da entrega.
- Cobertura >= 80% no código novo; duplicação <= 3% no código novo.
- Todos os Security Hotspots revisados com correção quando necessária.
- Quality Gate Passed; gate reprovado reprova a verificação de qualidade na CI. Not computed não é aprovação.
- Backend e frontend analisados. Não excluir código funcional só para melhorar métrica. Testar comportamentos críticos além da porcentagem.

## AP20 — Ambiente e limites de escopo

- Desenvolvimento/demonstração locais com Compose. Repositório/backlog públicos não significam aplicação ou uploads publicados na internet.
- PDFs sintéticos e dados fictícios são permitidos na demonstração, além dos exemplos fornecidos. Preservar a identificação dos dados sintéticos e testar também os dois originais.
- Autenticação e perfis compõem o MVP estendido; o núcleo obrigatório permanece rastreado pelos requisitos RF/RNF.
- Novos layouts e OCR permanecem evoluções. A tecnologia de OCR será avaliada antes dessa etapa; implantação pública da aplicação não faz parte do ambiente local previsto.

## Definições de entrega — 2026-11-13

- Revisão individual: o responsável abre, confere e aceita o próprio PR. Não há pendência de obter aprovação de outro integrante.
- Massa de demonstração: PDFs criados para os cenários do projeto são permitidos, além dos originais.
- Qualidade: classificação A foi a orientação relatada; os limites numéricos de AP19 são metas internas.
- Backlog E0: enviar o link do GitHub Project na atividade aberta no Classroom. Publicação do link não equivale a submissão realizada.

Origem: informações da disciplina fornecidas pelo responsável pelo projeto em 2026-11-13. Essas definições substituem as quatro dúvidas de validação anteriormente registradas.

## Pendências técnicas por incremento

Base da TT-03 fixada e validada; consultar [ambiente](ambiente.md). Verificar versões das dependências restantes antes de configurá-las. Antes de cada incremento, detalhar modelos, contratos e interações. Não há pendência externa registrada sobre revisão individual, massa sintética, metas numéricas do SonarQube ou formato de entrega do backlog.

## Referências técnicas

- https://docs.spring.io/spring-boot/reference/web/spring-session.html
- https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html
- https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html
- https://docs.sonarsource.com/sonarqube-cloud/standards/managing-quality-gates/introduction-to-quality-gates
- https://vite.dev/guide/
- https://reactrouter.com/start/declarative/installation
- https://vitest.dev/guide/
- https://testing-library.com/docs/react-testing-library/intro/
- https://playwright.dev/docs/intro
- https://docs.spring.io/spring-boot/reference/data/sql.html
- https://docs.spring.io/spring-boot/how-to/data-initialization.html
- https://docs.spring.io/spring-boot/reference/testing/testcontainers.html
- https://docs.spring.io/spring-boot/reference/io/validation.html
- https://pdfbox.apache.org/3.0/faq.html
- https://www.jacoco.org/jacoco/trunk/doc/maven.html
