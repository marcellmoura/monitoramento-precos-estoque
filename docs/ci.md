# Integração contínua

Workflow: [CI](../.github/workflows/ci.yml). Build/testes integrados e validados em PR e na main (TT-06). Integração SonarQube em revisão no [PR #58](https://github.com/marcellmoura/monitoramento-precos-estoque/pull/58) (TT-07), com inicialização pela main antes da análise de PRs; proteção de merge pendente.

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
4. Na main, verificar a presença da credencial e dos relatórios de cobertura, analisar backend e frontend e aguardar cada Quality Gate.
5. Guardar relatórios existentes, inclusive em caso de falha de testes ou análise.
6. Guardar JAR e `dist/` somente quando todas as verificações anteriores passarem, sem reconstruir os pacotes para o upload. Neste primeiro incremento, PRs validam build/testes; seus gates ainda não são executados.

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

Os relatórios podem existir em uma execução reprovada; isso não libera o pacote da aplicação. Dependências, credenciais, uploads e contexto local não são incluídos. Artefato não equivale a deploy. Em PR, o SHA identifica o commit de integração que o runner verificou, não necessariamente o último commit isolado da branch de trabalho.

## Limites atuais

A integração SonarQube deste incremento executa somente na main (push ou execução manual na main). A autenticação, importação de cobertura e resultado remoto ainda dependem da primeira execução após integração. PRs exibem aviso explícito e ainda não executam Sonar; habilitar e comprovar seus gates após estabelecer a baseline. A meta de qualidade AP19 permanece vigente e pendente de verificação. CI vermelha sozinha não bloqueia o botão de merge: checks obrigatórios dependem da configuração adicional da branch, a alinhar depois dos checks existirem.

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

Identificadores são públicos; apenas `SONAR_TOKEN` é secreto. O secret está cadastrado no GitHub; sua validade será comprovada pela análise. O checkout obtém histórico completo (`fetch-depth: 0`). Cada scanner recebe o token apenas no ambiente do próprio passo. Não há `pull_request_target`, credenciais persistidas no checkout nem leitura local do token.

O backend importa `target/site/jacoco/jacoco.xml` e resultados Surefire/Failsafe; o frontend importa `coverage/lcov.info`. Testes `.test.ts`/`.test.tsx`, suporte `src/test/` e `e2e/` são classificados como testes, sem duplicá-los como fontes de produção. Nenhuma exclusão de cobertura de código funcional foi acrescentada. Configurações locais dos scanners ficam fora do Git.

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
