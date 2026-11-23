# Validação do MVP para a entrega

Evidências conferidas em 23/11/2026 na base `32693e8`. A tarefa cobre a verificação do núcleo obrigatório e o preparo da demonstração. O roteiro de execução está no [README](../README.md#demonstrar-duas-cotações-do-mesmo-fornecedor).

## Evidências técnicas

| Verificação | Resultado observado | Evidência |
|---|---|---|
| T11 · CI e revisão | A CI do merge concluiu build, testes, Chromium e os dois Quality Gates. O PR #82 foi integrado após a revisão do roteiro. | [Commit 32693e8](https://github.com/marcellmoura/monitoramento-precos-estoque/commit/32693e8bd2864e0dc54de7e06373d8c68e6f3948), [PR #82](https://github.com/marcellmoura/monitoramento-precos-estoque/pull/82), [CI da main](https://github.com/marcellmoura/monitoramento-precos-estoque/actions/runs/45137076822), [ensaio anterior de falha da CI](ci.md#evidências--14112026) |
| T12 · Demonstração | Em Chromium com API e PostgreSQL, o mesmo fornecedor recebeu `tabela-v1.pdf` e `tabela-v2.pdf`. A primeira tentativa ficou **Concluída**. A segunda ficou **Concluída com pendências**. O filtro por fornecedor retornou oito preços e o link **Tentativa #…** abriu a cotação de origem. | [Passo a passo e resultados esperados](../README.md#demonstrar-duas-cotações-do-mesmo-fornecedor), [amostras e manifesto](../fixtures/pdfs/sinteticos/LEIA-ME.md) |
| T13 · Ambiente e logs | Compose iniciou frontend, backend e PostgreSQL em ambiente limpo. Os logs do backend mostraram o registro e a conclusão das extrações das duas tentativas. A persistência do banco e dos PDFs após recriar os containers foi validada na TT-10. | [PR #81](https://github.com/marcellmoura/monitoramento-precos-estoque/pull/81), [execução e logs](../README.md#operar-o-ambiente-local) |

### Resultados da comparação

| Item | Resultado na segunda cotação |
|---|---|
| Hibisco | **Aumentou** de R$ 33,00 para R$ 35,00, **+6,06%** |
| Alecrim | **Diminuiu** de R$ 15,00 para R$ 14,00, **-6,67%** |
| Camomila | **Sem alteração**, R$ 38,00 e **0,00%** |
| Melissa | **Novo**, R$ 20,00 |
| Boldo e Mistura sem identificação | **Pendentes** por preço zero e por dados de apresentação e associação, respectivamente |
| Erva-doce | Ausente na segunda cotação, com R$ 22,00 preservados no histórico |

A [suíte de importações](../backend/src/test/java/br/com/marcellmoura/monitoramento/ImportacaoIT.java) também verifica os dois PDFs originais do professor, duplicidade, falhas de extração, isolamento por fornecedor e apresentação, confirmação parcial, reprocessamento e permissões. A [matriz de requisitos](backlog.md#cobertura-dos-requisitos-e-critérios-finais) relaciona os testes e módulos às histórias do backlog.

### Qualidade na main

Os dois painéis registraram a revisão `32693e8` e Quality Gate **OK**. No SonarQube Cloud, segurança, confiabilidade e manutenibilidade ficaram em **A** no resultado geral e no código novo dos dois componentes.

| Componente | Cobertura geral | Cobertura no código novo | Duplicação no código novo | Hotspots abertos |
|---|---:|---:|---:|---:|
| [Backend](https://sonarcloud.io/dashboard?id=marcellmoura_monitoramento-precos-estoque_backend) | 93,6% | 93,6% | 0,0% | 0 |
| [Frontend](https://sonarcloud.io/dashboard?id=marcellmoura_monitoramento-precos-estoque_frontend) | 90,8% | 91,1% | 0,0% | 0 |

Os percentuais são evidências das [metas internas AP19](decisoes.md#ap19--qualidade). A classificação A é a orientação da disciplina registrada no projeto.

## Situação da TT-12

| Critério | Situação em 23/11/2026 |
|---|---|
| Executar pelo README e importar dois PDFs do mesmo fornecedor | Comprovado na validação automatizada em Compose limpo, com comparação e histórico. |
| Demonstrar as cinco classificações com os PDFs de referência | As cinco classificações foram observadas com os sintéticos. A suíte de integração verificou a extração dos dois originais. |
| Mostrar Actions, PR integrado, Compose e logs | Evidências reunidas acima. |
| Reunir links e confirmar prazo e formato no Classroom | Links reunidos. A turma exibe a atividade da primeira entrega do backlog. A data prevista de 23/11/2026 consta no planejamento. Prazo, formato e canal da entrega final ainda precisam de confirmação. |
| Validar as evoluções P13a–P13d separadamente | A validação ampliada segue no P14. O fluxo obrigatório usa os PDFs textuais de referência. |
| Registrar submissão e apresentação | O registro fica pendente até a realização da submissão e da apresentação. |

Antes da entrega, execute a rodada manual de [demonstração](../README.md#demonstrar-duas-cotações-do-mesmo-fornecedor) para conferir navegação, clareza das mensagens e leitura dos resultados. Use uma base de demonstração e dados fictícios. Confirme as orientações da entrega final no canal da disciplina antes de submeter.
