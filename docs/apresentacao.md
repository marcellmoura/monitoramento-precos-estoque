# Preparação da apresentação

O roteiro demonstra duas cotações do mesmo fornecedor, da leitura do PDF até a comparação no histórico. O [README](../README.md#demonstrar-duas-cotações-do-mesmo-fornecedor) descreve o cenário completo. As [evidências de validação](validacao-entrega.md) reúnem os resultados técnicos e o que falta confirmar para a entrega.

## Revisão das telas

A revisão começa pelo detalhe da cotação. Em cada tela, executar a ação com dados de demonstração, conferir o resultado e registrar a melhoria junto da issue relacionada. Revisar novamente a tela após cada ajuste.

| Ordem | Tela | O que observar |
|---|---|---|
| 1 | Detalhe da cotação | Identificação do fornecedor e do arquivo, preços prontos acima de **Salvar preços válidos**, comparação com o preço anterior e motivo das pendências. Conferir o caminho de cadastro e associação no primeiro PDF e o reaproveitamento no segundo. |
| 2 | Histórico de preços | Encontrar os dois preços do Hibisco, identificar fornecedor, data e embalagem e voltar à cotação de origem. |
| 3 | Enviar PDF e lista de importações | Selecionar fornecedor, enviar o arquivo, acompanhar o processamento e reconhecer falhas, duplicidades e resultados. |
| 4 | Entrada e página inicial | Entrar e encontrar a ação de importação. Conferir os caminhos para produtos, fornecedores e histórico. |
| 5 | Cadastros usados na demonstração | Encontrar o próximo passo e entender a diferença entre preço cotado, preço de compra de referência e preço de venda. |

Registrar cada ajuste com a tela, o que aconteceu, o resultado esperado e como verificá-lo. Durante a revisão, priorizar bloqueios de funcionamento, depois clareza e navegação, e então os ajustes visuais da mesma tela.

## Proposta inicial de identidade visual

A identidade parte de uma interface de compras com fundo claro e leitura rápida de valores. Aplicar primeiro no detalhe da cotação e usar essa tela para decidir o padrão antes de estender às demais.

| Elemento | Proposta |
|---|---|
| Paleta | Fundo `#F4F7F5`, superfícies `#FFFFFF`, texto `#182820` e verde escuro `#234B39` para a ação principal. |
| Tipografia | **Source Sans 3** como candidata, com fonte do sistema como alternativa. Usar números tabulares onde preços ficam lado a lado. |
| Hierarquia | Fornecedor, arquivo e data no cabeçalho. Preços prontos e ação principal juntos. Dados extraídos e pendências nas linhas correspondentes. |
| Estados | Rótulos textuais para aumento, redução, preço novo e pendência. Usar cor como apoio e verificar contraste e foco pelo teclado. |
| Leitura em telas menores | Manter identificação, preço e ação visíveis na sequência de leitura. Conferir a tela em janela estreita e no tamanho usado na apresentação. |

A proposta será avaliada na primeira tela. Nome curto, marca e tipografia final ficam para decisão durante essa revisão.

## Pendências de entrega

Conferir o formato no canal da disciplina, ensaiar o cenário demonstrado e reunir os links de repositório, CI, SonarQube e evidências. O registro de submissão e apresentação é feito após sua realização. As evoluções ficam no [backlog pós-apresentação](backlog.md#prioridade-para-a-apresentação).
