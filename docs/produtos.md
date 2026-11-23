# Cadastro, consulta e edição de produtos

P5 cobre cadastro e consulta de produtos (US-01/US-02), edição de produto/estoque (recorte da US-03) e as permissões correspondentes da US-34.

## Uso

Após entrar com senha definitiva, selecione **Consultar produtos**. A listagem oferece paginação e acesso ao detalhe. Administrador e Compras também encontram **Cadastrar produto**.

Informe nome, unidade e preço de venda. Categoria e **Preço de compra de referência (opcional)** são opcionais. Ao abrir o cadastro pela importação, o preço cotado aparece como contexto. A confirmação desse preço é feita na importação e o valor entra no histórico do fornecedor. Aceitam-se KG/G/L/ML/UN. Nos preços, use vírgula ou ponto para os centavos, sem separador de milhar; valores não negativos, com até duas casas. Venda zero é válida. Compra em branco fica desconhecida e aparece como **Não informado**; compra zero aparece como **R$ 0,00**.

O servidor gera o código interno e inicia o produto ativo, com estoques atual/mínimo zero. Esses estados não são campos configuráveis neste cadastro. Nomes podem repetir; a identificação interna distingue os produtos. O detalhe mostra código, categoria, unidade, valores autorizados e estado.

| Perfil | Cadastro | Campos da consulta |
|---|---|---|
| Administrador / Compras | Permitido, incluindo venda inicial | Identificação, categoria, unidade, compra/venda, estoques e estado |
| Estoque | Não permitido | Identificação, categoria, unidade, estoques e estado; sem preços |
| Consulta | Não permitido | Identificação, categoria, unidade, venda, estoques e estado; sem compra |

A API omite os campos restritos. Esconder colunas no frontend complementa a autorização do servidor. A edição posterior da venda é exclusiva do Administrador.

## API

| Método | Rota | Contrato |
|---|---|---|
| GET | `/api/produtos?pagina=0&tamanho=20` | `itens`, `pagina`, `tamanho`, `total`, `paginas`. Aceita filtros opcionais `busca` (trecho do nome ou código), `ativo` e `unidade`. Filtra antes de paginar e ordena por ID. |
| GET | `/api/produtos/{id}` | Consulta de um produto, com campos filtrados pelo perfil |
| POST | `/api/produtos` | `nome`, `categoria` opcional, `unidade`, `precoCompra` opcional, `precoVenda`; 201 com Location |

Campos comuns da resposta: `id`, `codigo`, `nome`, `categoria`, `unidade`, `estoqueAtual`, `estoqueMinimo`, `ativo`. `precoVenda` é omitido para Estoque; `precoCompra` é omitido para Estoque/Consulta. Para Administrador/Compras, compra desconhecida é `null`, não zero.

A interface envia os preços como texto decimal normalizado com ponto. A API devolve preços como texto com duas casas e quantidades como texto com três, preservando precisão no transporte. O frontend formata a parte inteira da moeda com BigInt e mantém os centavos como texto; não usa ponto flutuante para converter preços. UN é apresentado sem casas na interface.

Limites técnicos: nome obrigatório após remover espaços nas extremidades, até 255 caracteres; categoria até 100, vazia normalizada para `null`; preços com até 17 dígitos inteiros e 2 fracionários. Entrada com mais casas é recusada, sem arredondamento silencioso. Paginação padrão 20, máximo 100, índice inicial zero.

Autenticação exigida em todas as consultas; cadastro exige Administrador/Compras, senha definitiva e CSRF. Erros AP18: 400 dados/parâmetros inválidos, com campos quando aplicável; 401 sem sessão; 403 perfil sem permissão ou troca obrigatória pendente; 404 produto ausente; 405 método sem implementação; 409 conflito de edição. Respostas de falha não expõem detalhes internos.

## Persistência e organização

Migration V4 cria `produtos`, mantendo V1–V3 intactas. ID BIGINT identity; código de apresentação `PRD-000001`, derivado do ID com mínimo de seis dígitos. Não há promessa de numeração sem lacunas ou relação com referência do fornecedor. Valores de compra/venda são BigDecimal/NUMERIC(19,2); estoques NUMERIC(19,3), não negativos, com restrição de inteiro para UN. Código, preços, unidade e estados são persistidos/derivados no servidor, sem confiar em valores extras de estado enviados pelo cliente.

Organização por `produto/domain`, `application`, `persistence` e `http`; controllers delegam à aplicação e retornam projeções específicas por perfil. O catálogo não serializa a entidade JPA diretamente. Nenhuma biblioteca nova foi adicionada. Estilos de gestão são compartilhados entre produtos e usuários em CSS Modules.

A compra manual deste cadastro não cria histórico de cotação. Importações, seleção de referência, mudanças posteriores de preço/unidade e ajustes de estoque mantêm suas regras próprias em AP01/AP03/AP09 e nas histórias correspondentes. A permissão de zero neste cadastro não altera a regra de preço válido de uma importação.

## Edição de produto e estoque

Abra **Consultar produtos**, selecione **Consultar** no produto e clique em **Editar produto**. Após salvar, o sistema retorna aos detalhes. O código permanece o mesmo.

| Perfil | Campos editáveis |
|---|---|
| Administrador | Nome, categoria, unidade elegível, preço de venda, estado e estoques atual/mínimo |
| Compras | Nome e categoria |
| Estoque | Estoques atual/mínimo |
| Consulta | Consulta do catálogo |

Para UN, informe quantidades inteiras. Para KG, G, L e ML, informe até três casas decimais. São admitidos valores não negativos. A troca direta de unidade exige que os estoques já estejam zerados e que não haja histórico ou vínculos. Um ajuste que muda as quantidades grava responsável, instante, unidade e valores anteriores e novos. Salvar valores iguais não cria um ajuste. O histórico fica no banco.

`PATCH /api/produtos/{id}` recebe `versao` (inteiro da última consulta) e os campos a alterar. A resposta `200` segue a projeção da consulta e inclui `versao` e `unidadeEditavel`. Omitir um campo preserva seu valor; `categoria: null` ou em branco remove a categoria. Envie decimais como texto com ponto, sem separador de milhar. O preço de venda aceita até 17 dígitos inteiros e duas casas; estoques, até 16 inteiros e três casas. O servidor valida o perfil e rejeita campos fora do contrato. A resposta `409` indica que outra edição mudou o produto. Nesse caso, volte aos detalhes e reabra o formulário.

V6 acrescenta versão e bloqueio de unidade ao produto e cria o histórico de ajustes. A escrita do ajuste e a alteração de estoque são atômicas. O bloqueio permanece após voltar a zero se houver ajustes anteriores. A criação de vínculo e histórico de cotação deve bloquear a unidade e participar do controle de versão na mesma transação. A compra de referência com origem, os efeitos das importações e a margem serão validados nos fluxos correspondentes.

## Verificação

Validação local em 16/11/2026: 16 testes unitários e 68 de integração backend; 60 testes frontend; lint, build e smoke Chromium aprovados. Testes cobrem persistência, código, unidades, preços zero/ausente/máximo, negativos, precisão, limites, paginação, estado vazio, campos por perfil, sessão/CSRF/primeiro acesso e ausência de edição neste recorte. Restrições de estoque no banco também são verificadas.

Fluxo React/API/PostgreSQL real exercitado em banco temporário, com credenciais sintéticas: cadastro por Administrador/Compras, consulta nos quatro perfis, compra ausente/zero, venda zero e valores máximos exatos. Telas desktop/móvel conferidas. A [CI 43930563488](https://github.com/marcellmoura/monitoramento-precos-estoque/actions/runs/43930563488), commit `f56c3c8`, aprovou todas as verificações e os dois Quality Gates. Implementação em revisão no [PR #63](https://github.com/marcellmoura/monitoramento-precos-estoque/pull/63).

Referência técnica: [tipos numéricos exatos do PostgreSQL](https://www.postgresql.org/docs/18/datatype-numeric.html).
