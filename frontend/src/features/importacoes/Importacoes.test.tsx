import { fireEvent, render, screen, within } from '@testing-library/react';
import { createMemoryRouter, RouterProvider } from 'react-router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { routes } from '../../app/routes';
import type { Identidade } from '../acesso/api';
import type { ConferenciaCotacao, ConfirmacaoImportacao, ProdutoCatalogo, RevisaoItem, SituacaoVinculo, TentativaImportacao } from './api';

const base = {
  id: 1, fornecedorCodigo: 'FOR-000001', fornecedorNome: 'Distribuidora Teste', atorId: 1,
  arquivo: 'orcamento.pdf', enviadaEm: '2026-11-21T08:00:00Z',
  status: 'PENDENTE', paginas: 2, quantidadeItens: null, motivo: null, importacaoExistenteId: null,
} satisfies TentativaImportacao;

let perfil: Identidade['perfil'];
let fornecedores: boolean;
let tentativas: TentativaImportacao[];
let falha: number | undefined;
let conferencia: ConferenciaCotacao;
let revisoes: RevisaoItem[];
let precos: ConfirmacaoImportacao;
let falharConfirmacao: boolean;
let vinculos: SituacaoVinculo[];
let produtos: ProdutoCatalogo[];
const homonimos = [1, 2].map(id => ({ id, codigo: `PRD-00000${id}`, nome: 'Açúcar', unidade: 'KG' as const }));
let fetchMock: ReturnType<typeof vi.fn>;

beforeEach(() => {
  perfil = 'COMPRAS';
  fornecedores = true;
  tentativas = [];
  falha = undefined;
  revisoes = [];
  precos = { itens: [] };
  falharConfirmacao = false;
  vinculos = [];
  produtos = [...homonimos];
  conferencia = { colunas: ['ATACADO', 'VAREJO'], dataSugerida: '2026-11-21', confirmada: null, sugestao: null, pendenciasComerciais: [] };
  fetchMock = vi.fn(async (input: string, options?: RequestInit) => {
    const url = new URL(input, 'http://localhost');
    if (url.pathname === '/api/auth/me') return Response.json({ id: 1, nome: 'Usuário', perfil, trocaSenhaObrigatoria: false });
    if (url.pathname === '/api/auth/csrf') return Response.json({ token: 'csrf-teste', headerName: 'X-CSRF-TOKEN' });
    if (perfil === 'ESTOQUE' || perfil === 'CONSULTA') return Response.json({}, { status: 403 });
    if (falha) return Response.json({ mensagem: 'Falha simulada.' }, { status: falha });
    const pagina = Number(url.searchParams.get('pagina') ?? 0);
    if (url.pathname === '/api/fornecedores') {
      const ids = fornecedores ? [pagina + 1] : [];
      const itens = ids.map(id => ({ id, codigo: 'FOR-00000' + id, nome: 'Distribuidora Teste' }));
      return Response.json({ itens, pagina, paginas: fornecedores ? 2 : 0 });
    }
    if (url.pathname === '/api/importacoes') {
      if (options?.method === 'POST') return Response.json(base, { status: 201 });
      return Response.json({ itens: tentativas.slice(pagina, pagina + 1), pagina, total: tentativas.length, paginas: tentativas.length });
    }
    if (url.pathname.endsWith('/conferencia')) {
      if (options?.method === 'PUT') {
        const { coluna, unidade, embalagem, dataComercial } = JSON.parse(String(options.body));
        conferencia = { ...conferencia, confirmada: { id: 1, base: { coluna, unidade, embalagem }, dataComercial },
          pendenciasComerciais: [['Confirme a unidade deste item.'], []] };
      }
      return Response.json(conferencia);
    }
    if (url.pathname.endsWith('/precos')) return Response.json(precos);
    if (url.pathname.endsWith('/resolver-conflito-preco') && options?.method === 'POST') {
      if (falharConfirmacao) return Response.json({ mensagem: 'O conflito mudou. Confira novamente os preços.' }, { status: 409 });
      const item = precos.itens[0];
      precos = { itens: [{ ...item, estado: 'CONFIRMADO', classificacao: 'NOVO', motivos: [], conflito: null, preco: item.conflito!.preco,
        confirmado: { id: 7, produtoId: 2, dataComercial: '2026-11-21', confirmadaEm: base.enviadaEm } }] };
      return new Response(null, { status: 204 });
    }
    if (url.pathname.endsWith('/confirmar-itens') && options?.method === 'POST') {
      if (falharConfirmacao) return Response.json({}, { status: 500 });
      precos = { itens: precos.itens.map(item => item.estado === 'PRONTO'
        ? { ...item, estado: 'CONFIRMADO', classificacao: 'NOVO', motivos: [], confirmado: {
          id: 7, produtoId: 2, dataComercial: '2026-11-21', confirmadaEm: base.enviadaEm,
        } } : item) };
      tentativas[0].status = 'CONCLUIDA_COM_PENDENCIAS';
      return Response.json(precos);
    }
    if (url.pathname.endsWith('/vinculos')) return Response.json(vinculos);
    if (url.pathname.endsWith('/catalogo')) return Response.json({ itens: produtos, pagina, paginas: 1 });
    if (url.pathname.endsWith('/vinculo') && options?.method === 'PUT') {
      const dados = JSON.parse(String(options.body));
      const atual = vinculos[0];
      const produto = produtos.find(item => item.id === dados.produto);
      if (!produto) return Response.json({}, { status: 404 });
      atual.vinculo = { id: 3, produto };
      return Response.json(atual.vinculo);
    }
    if (url.pathname === '/api/produtos' && options?.method === 'POST') {
      const dados = JSON.parse(String(options.body));
      const produto = { id: 3, codigo: 'PRD-000003', nome: dados.nome, unidade: dados.unidade };
      produtos.push(produto);
      return Response.json(produto, { status: 201 });
    }
    if (url.pathname === '/api/produtos/3') return Response.json(produtos.at(-1));
    if (url.pathname.endsWith('/itens')) return Response.json(revisoes);
    if (url.pathname.endsWith('/itens/1') && options?.method === 'PUT') {
      const dados = JSON.parse(String(options.body));
      if (dados.preco === '1.00') {
        return Response.json({ mensagem: 'Os dados mudaram. Reabra o formulário antes de salvar.' }, { status: 409 });
      }
      const revisao = { id: 2, linha: 1, atorId: 1, revisadaEm: base.enviadaEm,
        dados: { ...dados, preco: Number(dados.preco) }, pendencias: [] };
      revisoes = [revisao];
      return Response.json(revisao);
    }
    if (url.pathname.endsWith('/previa')) return Response.json({
      layout: 'ATACADO_VAREJO', dataReferencia: null,
      itens: [
        { dados: { descricao: 'Açúcar', referencia: null, unidade: null, apresentacao: null, quantidade: null },
          precos: { ATACADO: '32', VAREJO: '35' }, textoOriginal: 'Açúcar 32 35',
          pendencias: ['Unidade não informada no PDF.'] },
        { dados: null, precos: {}, textoOriginal: 'Linha incompleta', pendencias: ['Confira o PDF.'] },
      ],
    });
    const tentativa = tentativas.find(item => item.id === Number(url.pathname.split('/')[3]));
    if (tentativa && (url.pathname.endsWith('/confirmacao') || url.pathname.endsWith('/reprocessamento'))) {
      tentativa.status = 'PENDENTE';
      tentativa.motivo = null;
    }
    return tentativa ? Response.json(tentativa) : Response.json({ mensagem: 'Tentativa não encontrada.' }, { status: 404 });
  });
  vi.stubGlobal('fetch', fetchMock);
});
afterEach(() => vi.unstubAllGlobals());

function abrir(path: string) {
  render(<RouterProvider router={createMemoryRouter(routes, { initialEntries: [path] })} />);
}

describe('Importações', () => {
  it('mostra o acesso e a lista inicialmente vazia', async () => {
    abrir('/');
    fireEvent.click(await screen.findByRole('link', { name: 'Consultar importações' }));
    expect(await screen.findByText('Nenhum envio registrado.')).toBeVisible();
  });

  it('carrega fornecedores de várias páginas e distingue homônimos pelo código', async () => {
    abrir('/importacoes/nova');
    await screen.findByRole('option', { name: 'FOR-000002 · Distribuidora Teste' });
    expect(screen.getByRole('option', { name: 'FOR-000001 · Distribuidora Teste' })).toHaveValue('1');
    expect(screen.getByLabelText('Fornecedor')).toBeRequired();
    expect(screen.getByLabelText('Arquivo PDF')).toBeRequired();
  });

  it('orienta o cadastro de fornecedor quando não há opção de envio', async () => {
    fornecedores = false;
    abrir('/importacoes/nova');
    expect(await screen.findByRole('link', { name: 'Cadastrar fornecedor' })).toBeVisible();
    expect(screen.queryByRole('button', { name: 'Enviar PDF' })).not.toBeInTheDocument();
  });

  it('mantém duplicata e rejeição na lista e acessa a tentativa original', async () => {
    tentativas = [
      { ...base, id: 3, arquivo: 'rejeitado.pdf', status: 'REJEITADA', motivo: 'PDF corrompido.', paginas: null },
      { ...base, id: 2, arquivo: 'repetido.pdf', status: 'DUPLICADA', importacaoExistenteId: 1 },
      { ...base },
    ];
    abrir('/importacoes');
    fireEvent.click(await screen.findByRole('link', { name: '#3 · rejeitado.pdf' }));
    expect(await screen.findByRole('status')).toHaveTextContent('PDF rejeitado');
    expect(screen.getByText('PDF corrompido.')).toBeVisible();
    expect(screen.queryByText('Itens', { exact: true })).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole('link', { name: 'Voltar às importações' }));
    fireEvent.click(await screen.findByRole('link', { name: 'Próxima página' }));
    expect(await screen.findByRole('link', { name: 'Página anterior' })).toBeVisible();
    fireEvent.click(screen.getByRole('link', { name: '#2 · repetido.pdf' }));
    expect(await screen.findByRole('status')).toHaveTextContent('Envio duplicado');
    fireEvent.click(screen.getByRole('link', { name: 'Abrir tentativa original' }));
    expect(await screen.findByText('Aguardando processamento')).toHaveAttribute('role', 'status');
  });

  it('exige confirmação explícita para outro fornecedor e atualiza o estado', async () => {
    tentativas = [{ ...base, status: 'AGUARDANDO_CONFIRMACAO', motivo: 'Confira o fornecedor.' }];
    abrir('/importacoes/1');
    const confirmar = await screen.findByRole('button', { name: 'Confirmar para este fornecedor' });
    expect(screen.getByRole('status')).toHaveTextContent('Aguardando confirmação');
    expect(fetchMock.mock.calls.some(([, init]) => init?.method === 'POST')).toBe(false);
    fireEvent.click(confirmar);
    await screen.findByText('Aguardando processamento');
    expect(screen.queryByRole('button', { name: 'Confirmar para este fornecedor' })).not.toBeInTheDocument();
  });

  it('nega o fluxo quando o perfil não pode importar', async () => {
    perfil = 'ESTOQUE';
    abrir('/importacoes/nova');
    expect(await screen.findByRole('alert')).toHaveTextContent('Acesso disponível para Administrador e Compras.');
    expect(screen.queryByRole('button', { name: 'Enviar PDF' })).not.toBeInTheDocument();
  });

  it('informa quando a tentativa consultada não existe', async () => {
    abrir('/importacoes/999');
    expect(await screen.findByRole('alert')).toHaveTextContent('Tentativa não encontrada.');
  });
  it('mostra as colunas de preço, campos ausentes e aviso de leitura parcial', async () => {
    tentativas = [{ ...base, status: 'AGUARDANDO_CONFERENCIA', quantidadeItens: 2 }];
    abrir('/importacoes/1');
    expect(await screen.findByRole('heading', { name: 'Prévia dos itens' })).toBeVisible();
    const previa = within(screen.getByRole('region', { name: 'Prévia dos itens' }));
    expect(previa.getByText('ATACADO', { exact: true })).toBeVisible();
    expect(screen.getByText('32', { exact: true })).toBeVisible();
    expect(previa.getByText('VAREJO', { exact: true })).toBeVisible();
    expect(screen.getByText('35', { exact: true })).toBeVisible();
    expect(screen.getByText('Data no PDF: Não informada')).toBeVisible();
    expect(screen.getByText('Linha incompleta')).toBeVisible();
    expect(screen.getByRole('alert')).toHaveTextContent('Leitura parcial');
  });

  it('envia a configuração sugerida e os preços em uma única ação explícita', async () => {
    tentativas = [{ ...base, status: 'AGUARDANDO_CONFERENCIA', quantidadeItens: 2 }];
    conferencia.sugestao = { coluna: 'ATACADO', unidade: 'KG', embalagem: 'Saco de 1 kg' };
    abrir('/importacoes/1');

    expect(await screen.findByRole('region', { name: 'Prévia dos itens' })).toBeVisible();
    expect(screen.getByText(/ATACADO · KG · Saco de 1 kg · 21\/11\/2026/)).toBeVisible();
    expect(screen.getByRole('link', { name: 'Revisar dados de compra' })).toHaveAttribute('href', '#conferencia-comercial');
    fireEvent.click(screen.getByRole('button', { name: 'Salvar preços válidos' }));

    await screen.findByText('Concluída com pendências');
    const gravacoes = fetchMock.mock.calls.filter(([, init]) => ['POST', 'PUT'].includes(init?.method));
    expect(gravacoes).toHaveLength(1);
    expect(gravacoes[0][0]).toMatch(/\/confirmar-itens$/);
    expect(JSON.parse(gravacoes[0][1].body)).toEqual({
      revisao: 0, coluna: 'ATACADO', unidade: 'KG', embalagem: 'Saco de 1 kg', dataComercial: '2026-11-21',
    });
  });

  it('apresenta a configuração sugerida e salva apenas após ação explícita', async () => {
    tentativas = [{ ...base, status: 'AGUARDANDO_CONFERENCIA', quantidadeItens: 2 }];
    conferencia.sugestao = { coluna: 'ATACADO', unidade: 'KG', embalagem: 'Saco de 1 kg' };
    abrir('/importacoes/1');
    const salvar = await screen.findByRole('button', { name: 'Salvar conferência' });
    expect(screen.getByLabelText('Coluna de preço de compra')).toHaveValue('ATACADO');
    expect(screen.getByLabelText('Unidade de compra')).toHaveValue('KG');
    expect(screen.getByLabelText('Embalagem de compra')).toBeRequired();
    expect(screen.getByLabelText('Data comercial')).toHaveValue('2026-11-21');
    expect(fetchMock.mock.calls.some(([, init]) => init?.method === 'PUT')).toBe(false);
    fireEvent.change(screen.getByLabelText('Data comercial'), { target: { value: '2026-11-20' } });
    fireEvent.click(salvar);
    expect(await screen.findByText('Conferência comercial salva.')).toBeVisible();
    expect(screen.getByText('Confirme a unidade deste item.')).toBeVisible();
    const requisicao = fetchMock.mock.calls.find(([rota, init]) => rota.endsWith('/conferencia') && init?.method === 'PUT');
    expect(JSON.parse(requisicao?.[1].body)).toEqual({
      revisao: 0, coluna: 'ATACADO', unidade: 'KG', embalagem: 'Saco de 1 kg', dataComercial: '2026-11-20',
    });
    expect(screen.getByLabelText('Data comercial')).toHaveValue('2026-11-20');
  });

  it('corrige uma linha e preserva os valores originais na prévia', async () => {
    tentativas = [{ ...base, status: 'AGUARDANDO_CONFERENCIA', quantidadeItens: 2 }];
    conferencia.confirmada = { id: 5, base: { coluna: 'ATACADO', unidade: 'KG', embalagem: 'Saco' },
      dataComercial: '2026-11-21' };
    abrir('/importacoes/1');
    fireEvent.click(await screen.findByRole('button', { name: 'Corrigir item Açúcar' }));
    const form = within(screen.getByRole('form', { name: 'Correção da linha 1' }));
    expect(form.getByLabelText('Preço conferido (R$)')).toHaveValue(null);
    fireEvent.change(form.getByLabelText('Unidade conferida'), { target: { value: 'KG' } });
    fireEvent.change(form.getByLabelText('Embalagem conferida'), { target: { value: 'Saco' } });
    fireEvent.change(form.getByLabelText('Origem do preço conferido'), { target: { value: 'manual' } });
    fireEvent.change(form.getByLabelText('Motivo da correção'), { target: { value: 'Cotação conferida' } });
    fireEvent.change(form.getByLabelText('Preço conferido (R$)'), { target: { value: '1.00' } });
    fireEvent.click(form.getByRole('button', { name: 'Salvar correção' }));
    expect(await form.findByRole('alert')).toHaveTextContent('Os dados mudaram.');
    fireEvent.change(form.getByLabelText('Preço conferido (R$)'), { target: { value: '33.25' } });
    fireEvent.click(form.getByRole('button', { name: 'Salvar correção' }));
    expect(await screen.findByText('Dados conferidos. Pendente de associação.')).toBeVisible();
    expect(screen.getByText('32', { exact: true })).toBeVisible();
    expect(screen.getByText(/33,25/)).toBeVisible();
    const chamada = fetchMock.mock.calls.filter(([url]) => url.endsWith('/itens/1')).at(-1);
    expect(JSON.parse(chamada?.[1].body)).toMatchObject({
      conferencia: 5, revisao: 0, unidade: 'KG', coluna: null, preco: '33.25', embalagem: 'Saco',
    });
  });

  function prepararVinculo() {
    tentativas = [{ ...base, status: 'AGUARDANDO_CONFERENCIA' }];
    vinculos = [{ linha: 1, conferencia: 5, revisaoItem: 2, vinculo: null, revalidar: false,
      identidade: { referencia: null, descricao: 'Açúcar', unidade: 'KG', embalagem: 'Saco' } }];
  }

  it('busca homônimos e só vincula o código escolhido após confirmar', async () => {
    prepararVinculo();
    abrir('/importacoes/1');
    fireEvent.click(await screen.findByRole('button', { name: 'Associar produto da linha 1' }));
    expect(screen.queryByRole('button', { name: 'Associar produto da linha 2' })).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Buscar produtos em KG' }));
    expect(await screen.findByRole('button', { name: /Selecionar PRD-000001/ })).toBeVisible();
    fireEvent.click(await screen.findByRole('button', { name: /Selecionar PRD-000002/ }));
    expect(fetchMock.mock.calls.some(([url]) => url.endsWith('/vinculo'))).toBe(false);
    fireEvent.click(screen.getByRole('button', { name: 'Confirmar associação' }));
    expect(await screen.findByText('Produto associado · PRD-000002 · Açúcar')).toBeVisible();
    const chamada = fetchMock.mock.calls.find(([url]) => url.endsWith('/vinculo'));
    expect(JSON.parse(chamada?.[1].body)).toEqual({ produto: 2, conferencia: 5, revisaoItem: 2, vinculo: 0 });
  });

  it('mostra de onde veio o vínculo aproveitado e permite escolher manualmente', async () => {
    prepararVinculo();
    vinculos[0].reaproveitado = { tentativaOrigem: 7, vinculoOrigem: 12, produto: homonimos[0] };
    abrir('/importacoes/1');
    expect(await screen.findByText(/Vínculo reaproveitado · PRD-000001/)).toBeVisible();
    expect(screen.getByRole('link', { name: 'Tentativa de origem #7' })).toHaveAttribute('href', '/importacoes/7');
    expect(screen.getByRole('button', { name: 'Associar produto da linha 1' })).toHaveTextContent('Alterar associação');
    expect(fetchMock.mock.calls.some(([url]) => url.endsWith('/vinculo'))).toBe(false);
  });

  it('retorna a uma única prévia com o novo produto selecionado mesmo quando o ID coincide com a conferência', async () => {
    prepararVinculo();
    conferencia.confirmada = { id: 3, base: { coluna: 'ATACADO', unidade: 'KG', embalagem: 'Saco' },
      dataComercial: '2026-11-21' };
    vinculos[0].conferencia = 3;
    abrir('/importacoes/1');
    fireEvent.click(await screen.findByRole('button', { name: 'Associar produto da linha 1' }));
    fireEvent.click(screen.getByRole('link', { name: 'Cadastrar produto' }));
    fireEvent.change(await screen.findByLabelText('Nome'), { target: { value: 'Açúcar recém-cadastrado' } });
    fireEvent.change(screen.getByLabelText('Unidade'), { target: { value: 'KG' } });
    fireEvent.change(screen.getByLabelText('Preço de venda'), { target: { value: '40' } });
    fireEvent.click(screen.getByRole('button', { name: 'Cadastrar produto' }));
    expect(await screen.findByText(/Selecionado · PRD-000003/)).toBeVisible();
    expect(screen.getByRole('region', { name: 'Prévia dos itens' })).toBeVisible();
    expect(screen.getByRole('button', { name: 'Confirmar associação' })).toBeEnabled();
    expect(fetchMock.mock.calls.some(([url]) => url.endsWith('/vinculo'))).toBe(false);
  });

  it('confirma os preços prontos, bloqueia a base e mantém o motivo da linha pendente', async () => {
    tentativas = [{ ...base, status: 'AGUARDANDO_CONFERENCIA', quantidadeItens: 2 }];
    conferencia.confirmada = { id: 5, base: { coluna: 'ATACADO', unidade: 'KG', embalagem: 'Saco' },
      dataComercial: '2026-11-21' };
    precos.itens = [
      { linha: 1, estado: 'PRONTO', classificacao: 'PENDENTE', variacao: null, preco: 32,
        motivos: ['Aguardando confirmação do preço.'], confirmado: null },
      { linha: 2, estado: 'PENDENTE', classificacao: 'PENDENTE', variacao: null, preco: null, motivos: ['Informe um preço positivo.'], confirmado: null },
    ];
    abrir('/importacoes/1');
    const confirmar = await screen.findByRole('button', { name: 'Salvar preços válidos' });
    expect(screen.getByText(/Preço pronto para confirmar/)).toBeVisible();
    expect(screen.getByText('Aguardando confirmação do preço.')).toBeVisible();
    expect(screen.getAllByText('Classificação').map(rotulo => rotulo.parentElement?.textContent))
      .toEqual(['Classificação · Pendente', 'Classificação · Pendente']);
    expect(screen.getByText(/Pendência para confirmar · Informe um preço positivo/)).toBeVisible();

    falharConfirmacao = true;
    fireEvent.click(confirmar);
    expect(await screen.findByText('A confirmação falhou. Consulte a tentativa antes de tentar novamente.')).toBeVisible();
    expect(screen.getByRole('status')).toHaveTextContent('Aguardando conferência');

    falharConfirmacao = false;
    fireEvent.click(confirmar);
    expect(await screen.findByText(/Preço confirmado no histórico/)).toBeVisible();
    expect(screen.getByText('Sem preço válido anterior.')).toBeVisible();
    expect(screen.getByText('· Novo')).toBeVisible();
    expect(screen.queryByText('Variação percentual')).not.toBeInTheDocument();
    expect(screen.getByRole('status')).toHaveTextContent('Concluída com pendências');
    expect(screen.getByText('Conferência bloqueada após a confirmação de preços.')).toBeVisible();
    expect(screen.queryByRole('button', { name: 'Corrigir item Açúcar' })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Corrigir item linha 2' })).toBeVisible();
    expect(screen.queryByRole('button', { name: 'Salvar preços válidos' })).not.toBeInTheDocument();
  });

  it.each([
    [33, 35, 'AUMENTOU', 'Aumentou', 2, '6.06', '+6,06%'],
    [15, 14, 'DIMINUIU', 'Diminuiu', 1, '-6.67', '-6,67%'],
    [38, 38, 'SEM_ALTERACAO', 'Sem alteração', 0, '0.00', '0,00%'],
  ] as const)('mostra valores e variação de %s para %s', async (
    anterior, preco, classificacao, rotulo, diferencaAbsoluta, percentual, esperado,
  ) => {
    tentativas = [{ ...base, status: 'CONCLUIDA', quantidadeItens: 2 }];
    const confirmado = { id: 7, produtoId: 2, dataComercial: '2026-11-21', confirmadaEm: base.enviadaEm };
    precos.itens = [
      {
        linha: 1, estado: 'CONFIRMADO', classificacao, preco, motivos: [],
        variacao: { diferencaAbsoluta, percentual },
        confirmado: { ...confirmado, anterior: { id: 5, tentativa: 3, preco: anterior, dataComercial: '2026-11-20' } },
      },
      {
        linha: 2, estado: 'CONFIRMADO', classificacao: null, variacao: null, preco: 32, motivos: [],
        confirmado: { ...confirmado, id: 8, substituido: true },
      },
    ];
    abrir('/importacoes/1');

    const origem = await screen.findByText(/Último preço válido anterior/);
    expect(origem).toHaveTextContent(`R$ ${anterior},00 · 20/11/2026`);
    expect(screen.getByText('Diferença em reais').nextElementSibling).toHaveTextContent(`R$ ${diferencaAbsoluta},00`);
    expect(screen.getByText('Variação percentual').nextElementSibling).toHaveTextContent(esperado);
    expect(screen.getByRole('link', { name: 'Tentativa #3' })).toHaveAttribute('href', '/importacoes/3');
    expect(screen.getByText(/Preço substituído no histórico/)).toBeVisible();
    expect(screen.getByText('Classificação').parentElement).toHaveTextContent(`Classificação · ${rotulo}`);
    expect(screen.getAllByText(/Último preço válido anterior/)).toHaveLength(1);
  });

  it.each([
    ['MANTER', 'ANTES', null],
    ['MANTER', 'DEPOIS', null],
    ['SUBSTITUIR', 'SUBSTITUIR', 'Cotação corrigida pelo fornecedor'],
  ])('envia a escolha explícita de conflito (%s %s)', async (decisao, operacao, motivo) => {
    tentativas = [{ ...base, status: 'AGUARDANDO_CONFERENCIA', quantidadeItens: 2 }];
    precos.itens = [{ linha: 1, estado: 'PENDENTE', classificacao: 'PENDENTE', variacao: null, preco: null, motivos: ['Já há outro preço.'], confirmado: null,
      conflito: { preco: 35, dataComercial: '2026-11-21', revisao: 'revisao-teste',
        anteriores: [{ id: 42, tentativa: 7, preco: 32, ordem: 1 }] } }];
    abrir('/importacoes/1');
    const secao = await screen.findByRole('region', { name: 'Resolver conflito de preço' });
    const formulario = within(secao);
    expect(formulario.getByText('Data comercial · 21/11/2026')).toBeVisible();
    expect(formulario.getByRole('link', { name: 'Tentativa #7' })).toHaveAttribute('href', '/importacoes/7');
    expect(formulario.getByRole('button', { name: 'Confirmar resolução' })).toBeDisabled();

    fireEvent.change(formulario.getByLabelText('Resolução do conflito'), { target: { value: decisao } });
    fireEvent.change(formulario.getByLabelText('Preço confirmado de referência'), { target: { value: '42' } });
    if (motivo) {
      fireEvent.change(formulario.getByLabelText('Motivo da substituição'), { target: { value: motivo } });
    } else {
      fireEvent.change(formulario.getByLabelText('Posição do novo preço'), { target: { value: operacao } });
    }
    fireEvent.click(formulario.getByRole('button', { name: 'Confirmar resolução' }));

    expect(await screen.findByText(/Preço confirmado no histórico/)).toBeVisible();
    const envio = fetchMock.mock.calls.find(([url]) => url.endsWith('/resolver-conflito-preco'));
    expect(envio?.[0]).toBe('/api/importacoes/1/itens/1/resolver-conflito-preco');
    expect(JSON.parse(envio?.[1].body)).toEqual({ operacao, precoReferencia: 42, motivo, revisao: 'revisao-teste' });
  });

  it('reprocessa a mesma tentativa em caso de falha', async () => {
    tentativas = [{ ...base, status: 'FALHA_EXTRACAO', motivo: 'Processamento interrompido.' }];
    abrir('/importacoes/1');
    expect(await screen.findByText('Processamento interrompido.')).toBeVisible();
    fireEvent.click(screen.getByRole('button', { name: 'Tentar novamente' }));
    expect(await screen.findByText('Aguardando processamento')).toBeVisible();
    expect(fetchMock.mock.calls.some(([rota, init]) =>
      rota === '/api/importacoes/1/reprocessamento' && init?.method === 'POST')).toBe(true);
  });

});
