# Massa de testes de importação

Criada em 2026-11-13. Dados e fornecedores inteiramente fictícios. São PDFs sintéticos identificados, permitidos na apresentação além dos exemplos do professor, conforme definição registrada em 2026-11-13.

## Arquivos e sequência

- Serra Verde: `tabela-v1.pdf` (01/11/2026) e `tabela-v2.pdf` (08/11/2026).
- Raiz Natural: `fichas-v1.pdf` (01/11/2026) e `fichas-v2.pdf` (08/11/2026).
- `dados-esperados.json`: valores de origem para futuros testes; não é relatório de execução do sistema.
- Originais do professor continuam um nível acima: `../Fornecedor_1.pdf` e `../Fornecedor_2.pdf`.

Todos têm texto selecionável. Layout tabela e layout em fichas usam os mesmos dados para permitir comparar a extração; fornecedores são distintos, portanto comparar v1 e v2 apenas dentro do mesmo fornecedor. Quantidade cotada não representa movimentação de estoque. Preços são BRL por apresentação; nestes exemplos cada apresentação válida é um saco de 1 kg.

## Preparação futura do cenário

Cadastrar produtos internos e fornecedores fictícios, vincular os itens identificados e validar a v1 antes de comparar a v2. Sem vínculo, mesmo um preço positivo pode permanecer pendente de associação. Não transformar as expectativas abaixo em regras baseadas no nome do produto.

| Item | V1 | V2 | Resultado esperado após validação/associação |
|---|---|---|---|
| H01 Hibisco | 33,00 | 35,00 | Aumentou; +6,06% com exibição de duas casas |
| A01 Alecrim | 15,00 | 14,00 | Diminuiu; -6,67% com exibição de duas casas |
| C01 Camomila | 38,00 | 38,00 | Sem alteração; 0% |
| E01 Erva-doce | 22,00 | ausente | Preservar último preço válido; ausência não significa descontinuação |
| M01 Melissa | ausente | 20,00 | Novo, após associação; sem percentual anterior |
| Z01 Boldo | ausente | 0,00 | Pendente/inválido; não inserir preço zero no histórico válido |
| X01 Mistura sem identificação | ausente | 18,00 | Pendente para revisão de apresentação e associação; não inventar embalagem |

V1 tem quatro linhas; V2 tem seis. Na primeira importação, os itens válidos e associados são novos. Reenviar exatamente o mesmo arquivo ao mesmo fornecedor deve detectar duplicidade. Confirmar itens válidos e associados, preservando os demais como pendentes, conforme AP06.

## Limites

Esta amostra não cobre toda a matriz do projeto: faltam, entre outros, terceiro período com reaparecimento, código repetido entre embalagens, preço ausente, arquivo corrompido, descrições quebradas, tabelas multipágina e documentos digitalizados/OCR. Planejar esses cenários antes das implementações correspondentes. A apresentação com dados sintéticos é permitida; os originais continuam obrigatórios nos testes.

Arquivos conferidos visualmente e por extração de texto. Nenhum leitor do sistema foi implementado ou validado. Conjunto de referência para validar suporte aos layouts tabela e fichas.
