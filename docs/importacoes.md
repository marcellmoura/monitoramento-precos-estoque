# Envio, prévia e conferência de PDFs de fornecedores

Administrador e Compras podem enviar PDFs textuais e consultar as tentativas. Abra **Consultar importações → Enviar PDF**, escolha o fornecedor pelo código e nome, selecione o arquivo e envie. A aplicação abre o detalhe com o identificador, fornecedor, nome original, data e status. A lista é paginada, da tentativa mais recente para a mais antiga.

| Status | Significado |
|---|---|
| Aguardando processamento | PDF recebido, validado e guardado. A tentativa é trabalho pendente no banco. |
| Processando PDF | Uma execução está lendo o original. |
| Aguardando conferência | A prévia está disponível. Confira itens, colunas de preço e pendências no detalhe. |
| Falha na extração | A leitura falhou ou foi interrompida. O detalhe mostra o motivo e oferece **Tentar novamente**. |
| PDF rejeitado | O conteúdo, extensão ou limite funcional não passou na validação. Veja o motivo no detalhe. |
| Falha no envio | Houve erro ao guardar o arquivo. A tentativa fica registrada e pode ser feito um novo envio. |
| Envio duplicado | O mesmo conteúdo já foi enviado para esse fornecedor. Abra a tentativa original pelo link. |
| Aguardando confirmação | O mesmo conteúdo foi encontrado para outro fornecedor. Confira o fornecedor indicado e escolha **Confirmar para este fornecedor** se quiser prosseguir. |

O número de páginas resulta da validação. Após a extração, a contagem inclui as linhas reconhecidas e as linhas preservadas para revisão. Os formatos disponíveis são KG/Fardo, Atacado/Varejo, tabela e fichas. A detecção usa cabeçalhos e rótulos do conteúdo. A prévia mostra todas as colunas de preço, valores originais, campos ausentes e motivos de pendência, incluindo preço zero e leitura parcial. A prévia mostra a data extraída do PDF quando encontrada.

O detalhe atualiza o status enquanto o processamento estiver pendente ou em curso. A prévia é gravada no registro de extração.

## Conferência comercial

Abra a tentativa em **Aguardando conferência**. Compare as colunas em **Prévia dos itens**. Em **Conferência comercial**, escolha a coluna que representa o preço de compra, selecione a unidade, informe a embalagem e confira a data comercial antes de clicar em **Salvar conferência**. A data sugerida vem do PDF quando válida ou do dia do envio no fuso comercial. A data extraída continua visível na prévia.

A aplicação sugere a última configuração do mesmo fornecedor e layout quando o conjunto de colunas e todos os itens são compatíveis com a base salva. A sugestão exige confirmação explícita e usa a data sugerida para a nova tentativa. Colunas de modalidades diferentes continuam distintas. No layout KG/Fardo, a coluna **Preço KG** exige unidade KG.

A V9 preserva as revisões por tentativa, com ator e momento de gravação. Repetir a mesma confirmação devolve a revisão existente. Ao salvar uma alteração, a revisão esperada deve corresponder à atual. Uma alteração concorrente pede que a tela seja reaberta antes de salvar outra escolha.

Itens com unidade ou embalagem ausentes ou divergentes, preço escolhido inválido ou leitura incompleta ficam pendentes para conferência individual. A tentativa permanece em **Aguardando conferência**. A escolha da base comercial não converte unidades, não associa produtos e não atualiza preços do catálogo ou estoque.

## Correção por item

Salve a conferência comercial e clique em **Corrigir item** na linha desejada. Confira descrição, referência, unidade, embalagem e quantidade ao lado da prévia. Escolha a coluna de origem ou **Verificação manual** para um valor confirmado fora das colunas do PDF. Digite o preço para a unidade e a embalagem escolhidas e registre o motivo. Um campo comercial sem confirmação pode ficar em branco, e a revisão fica pendente. Preços e quantidades informados devem ser positivos.

A coluna **Preço KG** exige KG também na correção individual. Um item cotado em UN pode receber um preço por unidade verificado manualmente. Cada revisão da V10 guarda ator, data, linha e versão da conferência usada. A aplicação mantém os dados extraídos e as revisões anteriores. Dados comerciais completos ficam pendentes de associação ao produto interno.

## Associação ao catálogo

Salve a conferência comercial. Na linha com unidade e embalagem conferidas, clique em **Associar produto**. Busque pelo nome ou deixe o campo em branco para listar os produtos ativos da mesma unidade. Use o código interno para distinguir nomes repetidos. Selecione o produto e clique em **Confirmar associação**.

**Cadastrar produto** abre o cadastro existente. Ao salvar, a aplicação volta à mesma linha e oferece o produto compatível para confirmação. A V11 grava fornecedor, referência, descrição, unidade, embalagem, produto, versão conferida, ator e data. A confirmação bloqueia mudanças diretas de unidade no produto. Se a identidade comercial da linha mudar, a tela passa a pedir nova confirmação. Uma linha sem referência pode receber vínculo manual quando a correspondência tiver sido verificada.

## Reaproveitamento em novas cotações

Na nova tentativa, salve a conferência comercial e confirme os dados que faltarem no item. Quando houver um vínculo manual vigente em uma tentativa anterior do mesmo fornecedor, a prévia reaproveita o produto se referência, unidade, embalagem, modalidade e descrição forem compatíveis. Diferenças de maiúsculas, acentos e espaços na descrição são normalizadas. A origem do vínculo aparece ao lado do produto reaproveitado.

A prévia recalcula essa associação a partir das revisões atuais. A V12 indexa os vínculos manuais usados na consulta. A associação reaproveitada aponta para o vínculo manual de origem. A aplicação mantém a linha pendente quando há conflito entre produtos, uma apresentação incerta, referência ausente ou produto inativo. **Alterar associação** continua disponível para confirmar um vínculo manual na nova tentativa.

## Confirmação de preços

Em **Confirmação de preços**, consulte o total de itens prontos, confirmados e pendentes. **Confirmar itens válidos** grava apenas linhas com associação vigente, unidade e embalagem conferidas e preço de compra positivo. Uma associação reaproveitada aponta para a confirmação manual que a originou. A data comercial pode ser anterior à do envio.

Após a confirmação parcial, a tentativa fica **Concluída com pendências**. Corrija ou associe as linhas ainda pendentes e confirme novamente. O botão grava apenas as novas linhas válidas. Quando todas estiverem confirmadas, a tentativa fica **Concluída**. A primeira gravação bloqueia a conferência comercial e cada linha confirmada. O catálogo e o estoque não são alterados.

A V13 guarda os dados do preço e sua origem, o ator e as datas comercial, de envio e de confirmação. Uma transação grava os preços e o status. Quando mais de uma linha da mesma cotação está pronta para a mesma apresentação e modalidade, as linhas ficam pendentes para revisão.

### Conflitos na mesma data comercial

Quando já existe preço para o mesmo fornecedor, item, apresentação e modalidade naquela data, a linha mostra **Resolver conflito de preço**, com o novo valor e os valores confirmados em ordem, cada um com sua tentativa de origem.

- **Resolução do conflito** · **Manter os dois preços**. Em **Preço confirmado de referência**, selecione o registro. Em **Posição do novo preço**, escolha **Antes do preço selecionado** ou **Depois do preço selecionado**.
- **Resolução do conflito** · **Substituir preço confirmado**. Em **Preço confirmado de referência**, selecione o registro. Em **Motivo da substituição**, informe a razão. O novo preço ocupa a posição do registro substituído, que permanece disponível na tentativa de origem.

Clique em **Confirmar resolução** para gravar a linha. As demais pendências continuam visíveis. Se outro usuário alterar os valores ou a ordem durante a revisão, confira os dados atualizados e escolha novamente.

A V14 preserva preços existentes na primeira posição do dia. A ordem dos preços vigentes é definida por data comercial e posição dentro da mesma identidade e modalidade. A resolução é registrada com a operação, o preço de referência, o motivo da substituição quando aplicável e o ator. A validação da revisão, a atualização da ordem, a gravação do preço e o estado da tentativa são transacionais.

### Último preço válido anterior

Nas linhas com preço vigente, a prévia mostra **Último preço válido anterior** com valor, data comercial e link para a tentativa de origem. A primeira ocorrência mostra **Sem preço válido anterior**. Preços substituídos continuam visíveis na tentativa original.

A consulta usa a mesma identidade comercial, fornecedor, unidade, embalagem e modalidade. A data comercial e a posição no dia determinam a precedência. A busca percorre os preços confirmados vigentes mesmo quando o item está ausente no PDF imediatamente anterior. Uma confirmação retroativa ou substituição passa a refletir nas consultas posteriores. A V15 adiciona o índice para essa busca.

### Classificação dos preços

Em **Prévia dos itens**, **Classificação** indica **Aumentou**, **Diminuiu** ou **Sem alteração** conforme o preço vigente e o último preço válido anterior. A primeira ocorrência válida mostra **Novo**. A comparação usa valores decimais.

Linhas à espera de correção, associação, resolução de conflito ou confirmação mostram **Pendente** e o motivo. Após um retroativo ou uma substituição, a classificação acompanha a base vigente na próxima consulta. A tentativa de origem mantém o rótulo **Preço substituído no histórico** para auditoria.

### Diferença em reais e variação percentual

Quando há preço anterior válido, a prévia mostra **Diferença em reais** e **Variação percentual** abaixo dos preços. A diferença em reais é absoluta. A classificação indica a direção e o percentual usa sinal em aumentos e reduções. Preços iguais mostram R$ 0,00 e 0,00%.

O backend calcula (atual − anterior) × 100 ÷ anterior somente para uma base positiva, arredonda para duas casas com `HALF_UP` e transmite o percentual em texto decimal para preservar a precisão na exibição. Itens novos mostram **Sem preço válido anterior**. A consulta dos preços exige perfil Administrador ou Compras.

## Histórico de preços

Na tela inicial, abra **Histórico de preços**. Use **Código do produto**, **Código do fornecedor**, **Número da tentativa**, **Data comercial inicial** ou **Data comercial final**. Os filtros podem ser combinados e as datas-limite são inclusivas. A consulta é paginada e ordenada pela data comercial mais recente. Dentro da mesma apresentação e modalidade no dia, a posição comercial mais recente aparece primeiro.

A tabela exibe produto, fornecedor, unidade, embalagem, modalidade, preço e tentativa de origem. Um preço **Substituído** permanece visível, com link para o substituto e motivo. O substituto também indica o preço original. Abra **Tentativa #…** para ver a linha do PDF e **Consultar importações e pendências** para consultar tentativas e itens que ainda aguardam confirmação. O histórico exige perfil Administrador ou Compras.

`GET /api/importacoes/historico-precos` aceita `produtoId`, `fornecedorId`, `tentativa`, `inicio`, `fim`, `pagina` e `tamanho`. As datas usam ISO, a página inicial é 0 e o tamanho padrão é 20, com máximo de 100.

## Configuração local

| Variável | Padrão | Uso |
|---|---|---|
| `IMPORTACAO_PASTA` | `storage/importacoes` | Pasta de originais, relativa ao diretório de execução do backend quando não for absoluta |
| `IMPORTACAO_LIMITE_BYTES` | `10485760` | Limite funcional do PDF, equivalente a 10 MiB |
| `IMPORTACAO_LIMITE_PAGINAS` | `50` | Limite funcional de páginas |
| `IMPORTACAO_LIMITE_TRANSPORTE_ARQUIVO` | `12MB` | Limite de cada arquivo multipart antes da aplicação |
| `IMPORTACAO_LIMITE_TRANSPORTE_REQUISICAO` | `13MB` | Limite do multipart completo antes da aplicação |
| `IMPORTACAO_PROCESSAMENTO_ATIVO` | `true` | Executa a fila nesta instância do backend |
| `IMPORTACAO_INTERVALO_EXTRACAO_MS` | `2000` | Intervalo entre consultas da fila nesta instância |
| `IMPORTACAO_PRAZO_EXTRACAO_SEGUNDOS` | `120` | Tempo concedido à execução para publicar o resultado |
| `IMPORTACAO_FUSO_COMERCIAL` | `America/Fortaleza` | Fuso usado para sugerir o dia do envio como data comercial |

Mantenha o limite de transporte do arquivo acima do funcional e reserve espaço para os campos multipart no limite da requisição. Se o transporte rejeitar o envio, o servidor devolve `413` antes de identificar fornecedor e registrar tentativa. A pasta de originais precisa ser persistente e gravável pelo backend. Para uma pasta externa, mantenha as permissões locais restritas ao serviço. O diretório padrão está excluído do Git.

## API

| Método | Rota | Contrato |
|---|---|---|
| POST | `/api/importacoes` | Multipart com `fornecedorId` e `arquivo`; `201` com `Location` e detalhe quando uma tentativa foi registrada, inclusive se ficou rejeitada ou duplicada |
| GET | `/api/importacoes?pagina=0&tamanho=20` | Página de tentativas, com `itens`, `pagina`, `tamanho`, `total` e `paginas`; tamanho máximo 100 |
| GET | `/api/importacoes/{id}` | Detalhe da tentativa, com `motivo`, `paginas`, `quantidadeItens` e `importacaoExistenteId` quando aplicáveis |
| POST | `/api/importacoes/{id}/confirmacao` | Confirma a associação ao fornecedor que já consta na tentativa; `200` se estava aguardando confirmação, `409` se o estado já mudou |
| GET | `/api/importacoes/{id}/previa` | Prévia do último processamento concluído, com layout, data, dados, preços e pendências por linha |
| GET | `/api/importacoes/{id}/conferencia` | Colunas disponíveis, data sugerida, revisão atual, sugestão compatível e pendências comerciais por linha |
| PUT | `/api/importacoes/{id}/conferencia` | Confirma `revisao` (0 na primeira gravação), `coluna`, `unidade`, `embalagem` e `dataComercial` (AAAA-MM-DD); `409` para revisão desatualizada |
| GET | `/api/importacoes/{id}/itens` | Revisões atuais por linha com dados conferidos, ator, data e pendências |
| PUT | `/api/importacoes/{id}/itens/{linha}` | Registra uma revisão para a linha numerada a partir de 1. Envia `revisao` (0 se inédita), `conferencia` (ID atual), campos corrigidos e `motivo`. `coluna: null` representa verificação manual. Alterações concorrentes retornam `409`. |
| GET | `/api/importacoes/catalogo?unidade=KG&nome=&pagina=0` | Lista paginada de candidatos ativos na unidade, com código, nome e unidade |
| GET | `/api/importacoes/{id}/vinculos` | Identidade comercial, vínculo manual e, quando aplicável, vínculo reaproveitado com a tentativa e o vínculo manual de origem |
| PUT | `/api/importacoes/{id}/itens/{linha}/vinculo` | Confirma `produto`, `conferencia`, `revisaoItem` e `vinculo`. Use 0 para revisão individual ou vínculo ainda ausente. Retorna `409` se a escolha ficou desatualizada. |
| GET | `/api/importacoes/{id}/precos` | Validação por linha com estado, preço, motivos e registro confirmado quando disponível |
| POST | `/api/importacoes/{id}/confirmar-itens` | Revalida e grava os itens elegíveis em uma transação. Repetir a solicitação devolve os mesmos registros quando não houver novos itens prontos. |
| POST | `/api/importacoes/{id}/reprocessamento` | Reagenda a mesma tentativa com falha de extração e mantém o histórico de execuções |

As alterações exigem sessão, perfil autorizado e CSRF. Sem fornecedor identificado, o envio retorna `400` ou `404`. A validação verifica o formato e a presença de texto com Apache PDFBox, sem guardar o texto. Arquivos corrompidos, protegidos ou sem texto recebem motivo explícito. Os logs registram identificador e status, sem nomes de arquivos ou conteúdo. [Apache PDFBox 3.0.8](https://pdfbox.apache.org/3.0/getting-started.html)

## Persistência e duplicidade

A V7 cria `arquivos_importacao` e `tentativas_importacao`. O arquivo vai primeiro para uma cópia temporária na pasta de armazenamento. Depois da validação, o original é publicado por renomeação atômica com UUID como nome interno. Um rollback confirmado limpa o original recém-criado. Uma interrupção abrupta entre o sistema de arquivos e a confirmação do banco pode deixar arquivo sem referência, para conciliação operacional. As tentativas confirmadas sempre são relacionadas por chave estrangeira ao arquivo registrado.

A deduplicação usa o SHA-256 do conteúdo, mesmo com nomes diferentes. Um bloqueio transacional por conteúdo e um índice único para o par fornecedor/conteúdo preservam uma tentativa canônica por fornecedor. Reenvios mantêm seus próprios identificadores; um mesmo original físico pode servir a tentativas de fornecedores distintos após confirmação explícita. Este recorte preserva os dados do catálogo e do estoque.

## Processamento durável

A V8 mantém PDFs pendentes existentes na fila e registra uma execução por processamento. O PostgreSQL impede que duas instâncias reivindiquem o mesmo envio. O resultado e a troca de status são gravados na mesma transação e só são publicados pela execução atual dentro de seu prazo. Uma execução expirada é marcada como falha em uma consulta posterior da fila, inclusive após reinício. **Tentar novamente** agenda outra execução no mesmo envio e preserva o motivo anterior. O prazo controla a publicação do resultado. A leitura do PDF ocorre fora da transação e tem limite de texto extraído.

## Verificação

Validação local em 21/11/2026. Maven Verify aprovou 16 testes unitários e 93 de integração, incluindo 12 de recepção. Os dois PDFs de referência e as quatro amostras sintéticas foram aceitos como PDFs textuais e armazenados com os bytes originais. Também foram verificados perfil e CSRF, limites, PDFs inválidos/protegidos/sem texto, duplicidade concorrente, confirmação entre fornecedores, falha de armazenamento e limpeza em rollback.

No frontend passaram 88 testes, lint, build e smoke Chromium. O fluxo real com Chromium, API e PostgreSQL validou seleção de homônimos, envio e consulta, duplicidade com nome diferente, confirmação para outro fornecedor e rejeição visível na lista.

No recorte de prévia passaram 19 testes unitários e 102 de integração no backend, além de 90 testes frontend, lint, build e smoke Chromium. Os testes conferiram referência, preços, unidade, apresentação e quantidade dos seis arquivos, bem como layout desconhecido, leitura parcial, expiração, retentativa e descarte de execução atrasada. O fluxo Chromium com API e PostgreSQL verificou a atualização automática da prévia em três layouts.

A conferência comercial foi validada com 19 testes unitários, 104 de integração, 91 testes de frontend, lint, build e Chromium. O teste real em Chromium, API e PostgreSQL verificou a rejeição de unidade incompatível com **Preço KG**, o salvamento da base e da data e a persistência da prévia e das pendências após recarregar a página.

A correção individual passou em 19 testes unitários e 105 de integração no backend, além de 92 testes de frontend, lint, build e smoke Chromium. O fluxo real em Chromium, API e PostgreSQL verificou o salvamento, a persistência após recarregar, a preservação dos valores originais e a pendência de um preço cuja unidade ainda precisava de verificação.

Na associação manual passaram 19 testes unitários e 107 de integração no backend, 94 testes de frontend, lint, build e smoke Chromium. A validação integrada cobriu perfis, homônimos, referências repetidas, unidades incompatíveis e revalidação após mudança de embalagem. O fluxo real em Chromium, API e PostgreSQL cobriu o cadastro, o retorno à mesma linha e a persistência da associação.

No reaproveitamento de vínculos passaram 19 testes unitários, 110 de integração e 95 de frontend, além de lint, build e Chromium. A validação integrada cobriu item que reaparece, fornecedores distintos, conflito de produtos, cadastro inativo e mudanças de unidade, apresentação ou identificação. O fluxo com navegador, API e PostgreSQL verificou a origem reaproveitada após recarregar e a volta à associação manual após mudar a modalidade.

A confirmação parcial passou em 19 testes unitários, 113 de integração e 96 de frontend, além de lint, build e Chromium. Os testes cobriram rollback após falha na segunda linha, confirmação concorrente sem duplicação, vínculo de origem reaproveitado, datas retroativas e conflito na mesma data. O fluxo real em Chromium, API e PostgreSQL confirmou o açúcar, corrigiu e associou o adoçante pendente e concluiu a tentativa com dois registros no histórico.
