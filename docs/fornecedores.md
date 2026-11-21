# Cadastro e consulta de fornecedores

Administrador e Compras podem cadastrar, listar e consultar fornecedores. Após entrar com senha definitiva, abra **Consultar fornecedores** e selecione **Cadastrar fornecedor**. O nome é obrigatório. CNPJ, telefone e e-mail podem ficar em branco.

O telefone aceita DDD e número nacional, com 10 ou 11 dígitos no total. São aceitos formatos como `(83) 3333-4444`, `(83) 99999-9999` e `83999999999`. O campo rejeita prefixos internacionais, números sem DDD e caracteres fora da máscara aceita. O CNPJ é armazenado como texto fornecido no cadastro, sem verificação fiscal.

Fornecedores com o mesmo nome aparecem com códigos diferentes. O código, como `FOR-000001`, é derivado do ID interno com pelo menos seis dígitos. A seleção de fornecedores será ligada à tela de importação de PDFs na US-06.

| Método | Rota | Resultado |
|---|---|---|
| GET | `/api/fornecedores?pagina=0&tamanho=20` | Lista ordenada por ID e informações de paginação |
| GET | `/api/fornecedores/{id}` | Detalhe do fornecedor |
| POST | `/api/fornecedores` | 201 com o fornecedor cadastrado e o cabeçalho Location |

Cadastro recebe `nome`, `cnpj`, `telefone` e `email`. Os limites são 255, 32, 40 e 254 caracteres, respectivamente. O sistema remove espaços nas extremidades e grava opcionais vazios como nulos. A resposta inclui `id`, `codigo` e os campos do cadastro. O tamanho da página vai de 1 a 100 e o padrão é 20.

A API exige sessão e aplica a autorização em todas as rotas. O cadastro também exige CSRF. Entradas inválidas retornam 400, falta de sessão retorna 401, falta de permissão ou troca de senha pendente retorna 403 e fornecedor inexistente retorna 404. A migration V5 cria a tabela `fornecedores`.

A validação local incluiu 16 testes unitários e 75 de integração do backend, além de 70 testes do frontend, lint, build e smoke no Chromium. O cadastro e a consulta também foram exercitados no Chromium com a API real e um banco PostgreSQL temporário.
