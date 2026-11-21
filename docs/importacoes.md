# Envio e prévia de PDFs de fornecedores

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

O número de páginas resulta da validação. Após a extração, a contagem inclui as linhas reconhecidas e as linhas preservadas para revisão. Os formatos disponíveis são KG/Fardo, Atacado/Varejo, tabela e fichas. A detecção usa cabeçalhos e rótulos do conteúdo. A prévia mostra todas as colunas de preço, valores originais, campos ausentes e motivos de pendência, incluindo preço zero e leitura parcial. A data comercial aparece quando presente no PDF.

O detalhe atualiza o status enquanto o processamento estiver pendente ou em curso. A edição dos itens, a escolha da coluna de compra, a associação e a confirmação comercial ficam para os fluxos correspondentes. A prévia é gravada somente no registro de extração.

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

Mantenha o limite de transporte do arquivo acima do funcional e reserve espaço para os campos multipart no limite da requisição. Se o transporte rejeitar o envio, o servidor devolve `413` antes de identificar fornecedor e registrar tentativa. A pasta de originais precisa ser persistente e gravável pelo backend. Para uma pasta externa, mantenha as permissões locais restritas ao serviço. O diretório padrão está excluído do Git.

## API

| Método | Rota | Contrato |
|---|---|---|
| POST | `/api/importacoes` | Multipart com `fornecedorId` e `arquivo`; `201` com `Location` e detalhe quando uma tentativa foi registrada, inclusive se ficou rejeitada ou duplicada |
| GET | `/api/importacoes?pagina=0&tamanho=20` | Página de tentativas, com `itens`, `pagina`, `tamanho`, `total` e `paginas`; tamanho máximo 100 |
| GET | `/api/importacoes/{id}` | Detalhe da tentativa, com `motivo`, `paginas`, `quantidadeItens` e `importacaoExistenteId` quando aplicáveis |
| POST | `/api/importacoes/{id}/confirmacao` | Confirma a associação ao fornecedor que já consta na tentativa; `200` se estava aguardando confirmação, `409` se o estado já mudou |
| GET | `/api/importacoes/{id}/previa` | Prévia do último processamento concluído, com layout, data, dados, preços e pendências por linha |
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

No recorte de prévia passaram 18 testes unitários e 102 de integração no backend, além de 90 testes frontend, lint, build e smoke Chromium. Os testes conferiram referência, preços, unidade, apresentação e quantidade dos seis arquivos, bem como layout desconhecido, leitura parcial, expiração, retentativa e descarte de execução atrasada. O fluxo Chromium com API e PostgreSQL verificou a atualização automática da prévia em três layouts.
