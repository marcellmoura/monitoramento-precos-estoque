# Integração contínua

Workflow: [CI](../.github/workflows/ci.yml). Build/testes integrados e validados em PR e na main (TT-06). Inicialização SonarQube integrada pelo [PR #58](https://github.com/marcellmoura/monitoramento-precos-estoque/pull/58) (TT-07), com os dois gates aprovados na main. Ajuste do escopo e análise de PRs validados no [PR #59](https://github.com/marcellmoura/monitoramento-precos-estoque/pull/59), aguardando integração; proteção de merge pendente.

## Eventos e ambiente

- Pull Requests destinados a `main`, incluindo atualizações da branch.
- Push na `main`, inclusive após integração de PR.
- Execução manual por `workflow_dispatch`, disponível depois que o workflow existir na branch padrão.

Um job `Build e testes` em Ubuntu 24.04, limite de 30 minutos. Passos sequenciais enquanto a suíte é pequena; medir os tempos antes de introduzir paralelismo. Commits novos na mesma referência cancelam a execução anterior. As análises na main exigem o repository secret `SONAR_TOKEN`. Seu valor não deve aparecer em logs, arquivos ou argumentos de comando.

Temurin Java 25.0.5 e Node definido em `.nvmrc`; Maven Wrapper e lockfile npm preservam as versões. Cache de dependências usa POM e lockfile. Actions fixadas pelo SHA de releases oficiais, com a versão indicada em comentário. Permissão `contents: read`; credencial do checkout não é persistida.

## Verificações

1. Backend: `./mvnw -B -ntp clean verify`, a partir de `backend/`.
2. Frontend: `npm ci`, lint, Vitest/cobertura e build, a partir de `frontend/`.
3. Chromium e bibliotecas Linux instalados pelo Playwright; executar o teste de navegação do build.
4. Em PRs e na main, verificar a presença da credencial e dos relatórios de cobertura, analisar backend e frontend e aguardar cada Quality Gate.
5. Guardar relatórios existentes, inclusive em caso de falha de testes ou análise.
6. Guardar JAR e `dist/` somente quando todas as verificações anteriores passarem, sem reconstruir os pacotes para o upload. O pacote depende também dos dois gates em PRs.

Não há `skipTests` nem `continue-on-error`. Um JAR pode ser criado antes da fase Maven `verify`; isso não significa que foi aprovado. Seu upload depende do resultado de todas as verificações.

## Testes Java

Surefire e Failsafe usam a versão 3.5.6 gerenciada pelo Spring Boot:

- `./mvnw test`: testes rápidos `Test*`, `*Test`, `*Tests` e `*TestCase`.
- `./mvnw verify`: inclui também integração `*IT` via Failsafe e gera cobertura JaCoCo.
- O teste de inicialização/contexto com MockMvc é `MonitoramentoApplicationIT`; não abre um servidor HTTP real. Testes de rede e PostgreSQL serão acrescentados com as funcionalidades correspondentes.
- Ainda não existem testes unitários Java de negócio. Uma rodada `test` vazia não comprova o teste de integração; usar `verify` na validação completa. Failsafe reprova se a suíte de integração esperada não for encontrada.
- JaCoCo acumula a execução das suítes no mesmo arquivo durante o ciclo; `clean` remove dados de ciclos anteriores. Não excluir código de aplicação para melhorar a métrica.

## Relatórios e artefatos

Na aba Actions, abrir a execução e o job; conferir o primeiro passo com falha e seus logs. Artefatos ficam disponíveis no resumo por 7 dias:

- `relatorios-<sha>-<run-id>-<tentativa>`: Surefire/Failsafe, JaCoCo, cobertura frontend e relatórios/diagnóstico Playwright, quando produzidos.
- `aplicacao-<sha>-<run-id>-<tentativa>`: JAR executável do backend e build estático do frontend, após sucesso.

Os relatórios podem existir em uma execução reprovada; isso não libera o pacote da aplicação. Dependências, credenciais, uploads e contexto local não são incluídos. Artefato não equivale a deploy. Em PR, checkout e nomes dos artefatos usam o SHA da branch de origem (`pull_request.head.sha`); na main, usam `github.sha`. Assim, pacote e relatórios identificam o código efetivamente analisado.

## Limites atuais

A configuração em revisão habilita Sonar na main e nos PRs destinados a ela. Autenticação, importação de cobertura e gates da main já foram verificados; ajuste de escopo e gates de PR comprovados na CI do PR #59. Execuções manuais devem selecionar main. A meta de qualidade AP19 permanece vigente e pendente de verificação. CI vermelha sozinha não bloqueia o botão de merge: checks obrigatórios dependem da configuração adicional da branch, a alinhar depois dos checks existirem.

Não encerrar TT-06 antes dos critérios ainda dependentes do Quality Gate. Execução após merge na main conferida e aprovada em 14/11/2026. Evidências da validação inicial estão abaixo; execuções futuras permanecem consultáveis no PR e na aba Actions.

## Evidências — 14/11/2026

| Verificação | Evidência | Resultado |
|---|---|---|
| Execução inicial | [run 43572672207](https://github.com/marcellmoura/monitoramento-precos-estoque/actions/runs/43572672207) — commit `d608a8f` | Build/testes de backend/frontend/Chromium aprovados; pacote e relatórios publicados |
| Falha controlada na branch | [run 43572899017](https://github.com/marcellmoura/monitoramento-precos-estoque/actions/runs/43572899017) — commit `03014a4` | Teste de integração reprovou por esperar 200 em rota ausente (404); workflow reprovado, diagnóstico publicado, pacote da aplicação não publicado |
| Restauração do teste | [run 43573032239](https://github.com/marcellmoura/monitoramento-precos-estoque/actions/runs/43573032239) — commit `28f678d` | Todas as verificações aprovadas novamente; pacote e relatórios publicados |

Pacote inicial baixado e inspecionado: JAR executável, `frontend/dist/index.html` e assets JS/CSS. Relatórios inspecionados: Failsafe com 1 teste de integração aprovado, JaCoCo XML e LCOV; relatório do ensaio registra 1 falha na asserção de status. O teste foi restaurado e o estado final não contém a falha deliberada. Nenhum estado reprovado foi integrado na main.

Incremento integrado pelo [PR #57](https://github.com/marcellmoura/monitoramento-precos-estoque/pull/57), merge `8787b1b` em 14/11/2026. Commits posteriores de documentação também executam CI; conferir o check do último commit antes do merge. TT-06 permanece em andamento por depender da verificação de Quality Gate no incremento de qualidade.

### Validação após integração

[Run 43573638537](https://github.com/marcellmoura/monitoramento-precos-estoque/actions/runs/43573638537), disparado por push na main para o merge `8787b1b`: concluído com sucesso. Backend, frontend, navegador e uploads dos relatórios/pacote aprovados. A issue #43 permanece aberta, Em andamento, com o critério de Quality Gate pendente.

## SonarQube Cloud — inicialização

Dois projetos públicos vinculados ao mesmo repositório, organização `marcellmoura`:

| Componente | Chave | Configuração |
|---|---|---|
| [Backend](https://sonarcloud.io/dashboard?id=marcellmoura_monitoramento-precos-estoque_backend) | `marcellmoura_monitoramento-precos-estoque_backend` | Propriedades e SonarScanner Maven 5.8.0.7211 fixados em `backend/pom.xml`; `./mvnw -B -ntp sonar:sonar` depois de `clean verify` |
| [Frontend](https://sonarcloud.io/dashboard?id=marcellmoura_monitoramento-precos-estoque_frontend) | `marcellmoura_monitoramento-precos-estoque_frontend` | `frontend/sonar-project.properties`; Action oficial SonarSource 8.4.1 fixada por SHA, base `frontend/` |

Identificadores são públicos; apenas `SONAR_TOKEN` é secreto. O secret está cadastrado no GitHub; sua autenticação foi comprovada pelas análises da main. O checkout obtém histórico completo (`fetch-depth: 0`). Cada scanner recebe o token apenas no ambiente do próprio passo. Não há `pull_request_target`, credenciais persistidas no checkout nem leitura local do token.

O backend importa `target/site/jacoco/jacoco.xml` e resultados Surefire/Failsafe; o frontend importa `coverage/lcov.info`. O escopo explícito separa testes `.test.ts`/`.test.tsx`, suporte `src/test/` e `e2e/` das fontes de produção. A configuração automática do scanner está fixada como desativada nos arquivos versionados; resultado remoto deste ajuste conferido no PR #59, conforme o registro abaixo. Nenhuma exclusão de cobertura de código funcional foi acrescentada. Configurações locais dos scanners ficam fora do Git.

Ambos usam `sonar.qualitygate.wait=true` e timeout de 300 segundos. Erro de scanner, credencial ausente, relatório ausente, timeout ou gate reprovado interrompem o job e impedem o upload do pacote na main. Se o backend falhar, a análise seguinte não é executada; isso não representa aprovação do frontend. Relatórios já existentes continuam disponíveis para diagnóstico.

### Sequência e critérios de validação

1. Integrar a configuração inicial por PR, após build/testes aprovados. O check desse PR **não comprova qualidade Sonar**, pois a main ainda não possui baseline.
2. Conferir a execução automática pós-merge na main e os dois painéis: commit analisado, fontes, testes, cobertura importada, gate e ratings. Confirmar análise automática desativada; monorepos usam análise por CI.
3. Se houver `Not computed`, registrar a pendência e estabelecer a referência de código novo antes de repetir a análise. Não alterar limites nem excluir código para obter aprovação.
4. Habilitar análises de PR em incremento seguinte e comprovar resultado dos dois gates, inclusive a reprovação na CI; só então configurar checks obrigatórios da main.

Usar o gate gratuito Sonar way; não depender de gate/perfil personalizado do trial. O comportamento de mudanças pequenas e a definição efetiva de código novo serão registrados na primeira análise. A aprovação do gate de código novo não garante as classificações gerais de AP19; acompanhar também segurança, confiabilidade, manutenibilidade e hotspots no resultado geral. TT-06/TT-07 permanecem abertas durante essa inicialização.

Referências: [monorepos e análise por CI](https://docs.sonarsource.com/sonarqube-cloud/analyzing-source-code/monorepo-support), [SonarScanner Maven](https://docs.sonarsource.com/sonarqube-cloud/analyzing-source-code/scanners/sonarscanner-for-maven), [Action oficial 8.4.1](https://github.com/SonarSource/sonarqube-scan-action/releases/tag/v8.4.1).

### Validação local — 16/11/2026

Maven `clean verify` aprovado com 1 teste de integração; lint, 4 testes Vitest, build e 1 teste Chromium aprovados. POM/XML, sintaxe YAML e caminhos JaCoCo/LCOV conferidos; o plugin Maven 5.8.0.7211 foi resolvido pelo Maven Central e seu goal `sonar` confirmado. O teste de navegador exigiu execução com permissão para abrir a porta local 4173. Nenhuma análise autenticada foi executada localmente. A primeira análise remota e os gates permanecem pendentes da integração.

### Validação remota após o PR #58 — 16/11/2026

Merge `af0ea2c`; main local sincronizada. [run 43825308754, tentativa 4](https://github.com/marcellmoura/monitoramento-precos-estoque/actions/runs/43825308754/attempts/4) concluído com sucesso: testes, builds, análises backend/frontend, espera pelos dois gates e uploads aprovados. Pacote publicado somente na tentativa aprovada, identificado pelo mesmo SHA e tentativa. Nenhuma credencial acessada localmente.

| Tentativa | Resultado observado |
|---|---|
| 1 | Maven Central respondeu HTTP 429 ao resolver o parent; testes e Sonar não iniciaram |
| 2 | Build/testes aprovados; primeira análise backend criou referência, gate `NONE`/Not computed; scanner falhou e frontend/pacote foram bloqueados |
| 3 | Backend com gate `OK`; primeira análise frontend criou referência, gate `NONE`; CI reprovou e não publicou pacote |
| 4 | Gates `OK` em ambos; CI aprovada, relatórios e pacote publicados |

Os dois componentes registram A em segurança, confiabilidade e manutenibilidade gerais, zero problemas abertos e duplicação geral 0%. Backend: JaCoCo 100% e 1 teste importado. Frontend: cobertura geral Sonar 54,5%; essa métrica e seu escopo não são diretamente equivalentes às linhas do relatório Vitest. Não representa comprovação da meta de cobertura de código novo de AP19.

A referência de código novo é `previous_version`, versão 0.1.0: backend em 16/11/2026 07:43:48 UTC e frontend em 07:50:02 UTC. Os resultados de gate recebidos têm condições de ratings/hotspots; ainda não há valor de cobertura/duplicação de código novo para demonstrar os limites numéricos. A configuração completa do gate e o tratamento de mudanças pequenas devem ser conferidos antes de encerrar TT-07.

#### Ajustes identificados na conferência

- O log do frontend informa que `sonar.sources`, `sonar.tests` e `sonar.test.inclusions` não têm efeito no novo mecanismo de escopo deste ambiente. A configuração foi carregada, mas os arquivos indexados incluem configurações na raiz e `e2e/inicio.spec.ts`. A intenção de analisar fontes/testes separadamente ainda não foi comprovada; adaptar o escopo suportado antes de tratar as métricas como definitivas. A documentação pública consultada ainda descreve os parâmetros antigos; prevalece o comportamento observado na execução para este diagnóstico.
- O backend importou o resultado Failsafe, mas o scanner registra ausência de `target/surefire-reports`, pois ainda não há testes unitários Java. Ajustar a seleção dos relatórios existentes, sem inventar testes ou relatórios vazios.
- O scanner adotou nomes derivados do POM/package.json; as chaves dos dois projetos continuam distintas. Fixar os rótulos Backend/Frontend para facilitar identificação.

TT-06/TT-07 continuam abertas: falta validar o escopo correto e o comportamento dos gates em PR. Nenhum limite foi reduzido, regra desabilitada ou fonte funcional excluída durante a inicialização. Ajustes, análise de PR e proteção devem seguir o próximo incremento.

### Ajuste de escopo e análise de PRs — 16/11/2026

Configuração na branch `ci/tt-07-sonar-prs`, em revisão:

- `sonar.scanner.autoconfig.enabled=false` nos dois componentes preserva o escopo explícito do repositório. A interface do serviço informa que a configuração automática está em evolução; não depender dela para substituir fontes/testes. Nenhuma regra, limite de gate ou fonte funcional foi excluída.
- Nomes Backend/Frontend definidos nos scanners, mantendo as mesmas chaves públicas. Os nomes da main serão atualizados na análise após merge.
- Backend seleciona somente diretórios de relatórios existentes após `verify`, incluindo Surefire quando houver testes unitários e Failsafe para os testes de integração.
- Sonar executa em cada PR para main, além de push/manual na main. Checkout da origem do PR e histórico completo; autodetecção do contexto do PR pelo scanner. O job espera ambos os gates antes de publicar o pacote.
- Sem secret, a verificação reprova explicitamente. PRs de forks não recebem `SONAR_TOKEN` e não comprovam qualidade por pular análise; não usar `pull_request_target` para contornar essa restrição.

Sintaxe YAML e blocos shell conferidos; a CI do PR validará builds/testes, escopo, cobertura e os dois gates. O fluxo normal passa a ser desenvolver com testes, abrir PR e conferir as verificações automáticas. Banco e funcionalidades seguem nos incrementos próprios; novas configurações somente por necessidade concreta.

#### Resultado do ajuste no PR #59

[CI 43828814157](https://github.com/marcellmoura/monitoramento-precos-estoque/actions/runs/43828814157) aprovada para o commit `40d4dd8`: builds, testes, análises de ambos os componentes, espera pelos dois gates e publicação de relatórios/pacote concluídos. API pública confirma `OK` nos dois gates de `pullRequest=59`.

O log do frontend confirma o arquivo de configuração carregado e `Included tests: src/**/*.test.ts, src/**/*.test.tsx, src/test/**, e2e/**`, com os testes excluídos das fontes de produção. Foram indexados 10 arquivos, correspondentes aos 6 arquivos de aplicação/estilo e 4 arquivos de testes/suporte existentes; configurações da raiz ficaram fora do escopo explícito. Perfis de linguagem Sonar way core. A análise de PR não lista arquivos sem alterações na árvore pública; a conferência do escopo usou o log do scanner. A main receberá o mesmo ajuste após o merge.

A configuração pode acompanhar o desenvolvimento automaticamente; não é necessário reconfigurá-la por funcionalidade. Testes continuam sendo acrescentados com o comportamento implementado. Após a integração, seguir para o incremento de banco e funcionalidades, mantendo AP19 acompanhado durante o desenvolvimento. Checks obrigatórios de merge ainda não foram ativados.
