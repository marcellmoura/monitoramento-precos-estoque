# Integração contínua

Workflow: [CI](../.github/workflows/ci.yml). Configuração inicial validada em PR na TT-06; SonarQube e proteção da main ainda não estão configurados.

## Eventos e ambiente

- Pull Requests destinados a `main`, incluindo atualizações da branch.
- Push na `main`, inclusive após integração de PR.
- Execução manual por `workflow_dispatch`, disponível depois que o workflow existir na branch padrão.

Um job `Build e testes` em Ubuntu 24.04, limite de 20 minutos. Passos sequenciais enquanto a suíte é pequena; medir os tempos antes de introduzir paralelismo. Commits novos na mesma referência cancelam a execução anterior. Nenhum segredo de serviço é necessário neste incremento.

Temurin Java 25.0.5 e Node definido em `.nvmrc`; Maven Wrapper e lockfile npm preservam as versões. Cache de dependências usa POM e lockfile. Actions fixadas pelo SHA de releases oficiais, com a versão indicada em comentário. Permissão `contents: read`; credencial do checkout não é persistida.

## Verificações

1. Backend: `./mvnw -B -ntp clean verify`, a partir de `backend/`.
2. Frontend: `npm ci`, lint, Vitest/cobertura e build, a partir de `frontend/`.
3. Chromium e bibliotecas Linux instalados pelo Playwright; executar o teste de navegação do build.
4. Guardar relatórios existentes, inclusive em caso de falha de testes.
5. Guardar JAR e `dist/` somente quando todas as verificações anteriores passarem, sem reconstruir os pacotes para o upload.

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

A configuração inicial não realiza análise SonarQube nem verifica Quality Gate; esses pontos continuam na TT-07/TT-08. A meta de qualidade AP19 permanece vigente e pendente de verificação. CI vermelha sozinha não bloqueia o botão de merge: checks obrigatórios dependem da configuração adicional da branch, a alinhar depois dos checks existirem.

Não encerrar TT-06 antes dos critérios ainda dependentes do Quality Gate. A execução da main após merge será conferida quando a integração ocorrer. Evidências da validação inicial estão abaixo; execuções futuras permanecem consultáveis no PR e na aba Actions.

## Evidências — 14/11/2026

| Verificação | Evidência | Resultado |
|---|---|---|
| Execução inicial | [run 43572672207](https://github.com/marcellmoura/monitoramento-precos-estoque/actions/runs/43572672207) — commit `d608a8f` | Build/testes de backend/frontend/Chromium aprovados; pacote e relatórios publicados |
| Falha controlada na branch | [run 43572899017](https://github.com/marcellmoura/monitoramento-precos-estoque/actions/runs/43572899017) — commit `03014a4` | Teste de integração reprovou por esperar 200 em rota ausente (404); workflow reprovado, diagnóstico publicado, pacote da aplicação não publicado |
| Restauração do teste | [run 43573032239](https://github.com/marcellmoura/monitoramento-precos-estoque/actions/runs/43573032239) — commit `28f678d` | Todas as verificações aprovadas novamente; pacote e relatórios publicados |

Pacote inicial baixado e inspecionado: JAR executável, `frontend/dist/index.html` e assets JS/CSS. Relatórios inspecionados: Failsafe com 1 teste de integração aprovado, JaCoCo XML e LCOV; relatório do ensaio registra 1 falha na asserção de status. O teste foi restaurado e o estado final não contém a falha deliberada. Nenhum estado reprovado foi integrado na main.

Incremento em revisão no [PR #57](https://github.com/marcellmoura/monitoramento-precos-estoque/pull/57). Commits posteriores de documentação também executam CI; conferir o check do último commit antes do merge. TT-06 permanece em andamento por depender da verificação de Quality Gate no incremento de qualidade.
