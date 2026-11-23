import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { createMemoryRouter, RouterProvider } from 'react-router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { routes } from '../../app/routes';
import type { Identidade } from '../acesso/api';
import type { Produto } from './api';

const comprador: Identidade = { id: 1, nome: 'Marina Teste', email: 'marina@example.com', perfil: 'COMPRAS', trocaSenhaObrigatoria: false };
const produto: Produto = { id: 1, codigo: 'PRD-000001', nome: 'Arroz', categoria: 'Alimentos', unidade: 'KG', precoCompra: '8.25', precoVenda: '12.34', estoqueAtual: '0.000', estoqueMinimo: '0.000', ativo: true, versao: 0, unidadeEditavel: true };
let sessao: Identidade | null;
let catalogo: Produto[];
let tamanho: number;
let falha: number | null;
let fetchMock: ReturnType<typeof vi.fn>;

beforeEach(() => {
  sessao = { ...comprador }; catalogo = [{ ...produto }]; tamanho = 20; falha = null;
  fetchMock = vi.fn(async (url: string, init?: RequestInit) => {
    if (url.endsWith('/auth/me')) return Response.json(sessao ?? {}, { status: sessao ? 200 : 401 });
    if (url.endsWith('/auth/csrf')) return Response.json({ token: 'csrf-teste', headerName: 'X-CSRF-TOKEN' });
    if (!sessao) return Response.json({}, { status: 401 });
    if (falha) return Response.json({}, { status: falha });
    const path = new URL(url, 'http://localhost');
    if (init?.method === 'POST') {
      if (!['ADMINISTRADOR', 'COMPRAS'].includes(sessao.perfil)) return Response.json({}, { status: 403 });
      const body = JSON.parse(init.body as string) as Pick<Produto, 'nome' | 'categoria' | 'unidade' | 'precoCompra' | 'precoVenda'>;
      const decimal = (v: string) => { const [i, f = ''] = v.split('.'); return `${i}.${f.padEnd(2, '0')}`; };
      const novo = { ...produto, ...body, id: 2, codigo: 'PRD-000002', precoVenda: decimal(body.precoVenda!), precoCompra: body.precoCompra == null ? null : decimal(body.precoCompra) };
      catalogo.push(novo); return Response.json(novo, { status: 201 });
    }
    const filtrar = (p: Produto) => {
      const r = { ...p };
      if (sessao?.perfil === 'CONSULTA' || sessao?.perfil === 'ESTOQUE') delete r.precoCompra;
      if (sessao?.perfil === 'ESTOQUE') delete r.precoVenda;
      return r;
    };
    if (path.pathname === '/api/produtos') {
      const pagina = Number(path.searchParams.get('pagina') ?? 0);
      return Response.json({ itens: catalogo.slice(pagina * tamanho, (pagina + 1) * tamanho).map(filtrar), pagina,
        tamanho, total: catalogo.length, paginas: Math.ceil(catalogo.length / tamanho) });
    }
    const item = catalogo.find(p => p.id === Number(path.pathname.split('/')[3]));
    if (item && init?.method === 'PATCH') {
      const body = JSON.parse(init.body as string) as Partial<Produto>;
      if (body.versao !== item.versao) return Response.json({ codigo: 'PRODUTO_DESATUALIZADO' }, { status: 409 });
      Object.assign(item, body, { versao: item.versao + 1 });
    }
    return item ? Response.json(filtrar(item)) : Response.json({}, { status: 404 });
  });
  vi.stubGlobal('fetch', fetchMock);
});
afterEach(() => vi.unstubAllGlobals());
function abrir(path = '/produtos') {
  const router = createMemoryRouter(routes, { initialEntries: [path] }); render(<RouterProvider router={router} />); return router;
}
function preencher(label: string, value: string) { fireEvent.change(screen.getByLabelText(label), { target: { value } }); }
async function formulario() {
  abrir('/produtos/novo'); await screen.findByRole('heading', { name: 'Cadastrar produto' });
  preencher('Nome', 'Produto novo'); preencher('Unidade', 'UN'); preencher('Preço de venda', '0');
}

describe('Produtos', () => {
  it('oferece acesso ao catálogo no início', async () => {
    abrir('/'); expect(await screen.findByRole('link', { name: 'Consultar produtos' })).toBeVisible();
  });
  it('mostra estado vazio sem dados fictícios', async () => {
    catalogo = []; abrir(); expect(await screen.findByText('Nenhum produto cadastrado.')).toBeVisible();
    expect(screen.queryByRole('table')).not.toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Cadastrar produto' })).toBeVisible();
  });
  it('lista código, preços, estoques e navega ao detalhe', async () => {
    const router = abrir(); await screen.findByRole('table');
    expect(screen.getByText('PRD-000001')).toBeVisible(); expect(screen.getByText('R$ 12,34')).toBeVisible();
    expect(screen.getByText('R$ 8,25')).toBeVisible();
    fireEvent.click(screen.getByRole('link', { name: 'Consultar Arroz' }));
    await screen.findByRole('heading', { name: 'Arroz' }); expect(router.state.location.pathname).toBe('/produtos/1');
    expect(screen.getByText('Quilograma (KG)')).toBeVisible();
  });
  it('cadastra venda zero e compra ausente sem converter ausência em zero', async () => {
    await formulario(); fireEvent.click(screen.getByRole('button', { name: 'Cadastrar produto' }));
    expect(await screen.findByRole('status')).toHaveTextContent('Produto cadastrado.');
    expect(screen.getByText('R$ 0,00')).toBeVisible(); expect(screen.getByText('Não informado')).toBeVisible();
    expect(screen.getByText('Sem categoria')).toBeVisible();
    const escrita = fetchMock.mock.calls.find(([, options]) => options?.method === 'POST');
    expect(JSON.parse(escrita?.[1]?.body as string)).toMatchObject({ precoVenda: '0', precoCompra: null, unidade: 'UN' });
    expect(new Headers(escrita?.[1]?.headers).get('X-CSRF-TOKEN')).toBe('csrf-teste');
  });
  it('normaliza centavos com vírgula e mantém compra zero distinta de ausência', async () => {
    await formulario(); preencher('Preço de venda', '99999999999999999,99'); preencher('Preço de compra de referência (opcional)', '0');
    preencher('Categoria (opcional)', 'Alimentos');
    fireEvent.click(screen.getByRole('button', { name: 'Cadastrar produto' }));
    await screen.findByRole('status');
    const escrita = fetchMock.mock.calls.find(([, options]) => options?.method === 'POST');
    expect(JSON.parse(escrita?.[1]?.body as string)).toMatchObject({ precoVenda: '99999999999999999.99', precoCompra: '0' });
    expect(screen.getByText('R$ 99.999.999.999.999.999,99')).toBeVisible();
  });
  it('recusa nome em branco antes do envio', async () => {
    await formulario(); preencher('Nome', ' '); fireEvent.click(screen.getByRole('button', { name: 'Cadastrar produto' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Informe o nome do produto.');
    expect(fetchMock.mock.calls.some(([, init]) => init?.method === 'POST')).toBe(false);
  });
  it('explica preço inválido e mantém validação nativa dos campos', async () => {
    await formulario();
    const venda = screen.getByLabelText('Preço de venda');
    expect(venda).toHaveAttribute('inputmode', 'decimal'); expect(venda).toBeRequired();
    expect(screen.getByLabelText('Nome')).toHaveAttribute('maxlength', '255');
    // Submissão direta comprova a validação da action além da validação HTML.
    preencher('Preço de venda', '-1'); fireEvent.submit(venda.closest('form')!);
    expect(await screen.findByRole('alert')).toHaveTextContent('Confira os preços:');
    expect(fetchMock.mock.calls.some(([, init]) => init?.method === 'POST')).toBe(false);
  });
  it.each(['ADMINISTRADOR', 'COMPRAS', 'ESTOQUE', 'CONSULTA'] as const)('aplica campos e navegação do perfil %s', async perfil => {
    sessao = { ...comprador, perfil }; const router = abrir(); await screen.findByRole('table');
    const cria = perfil === 'ADMINISTRADOR' || perfil === 'COMPRAS';
    expect(screen.queryByRole('link', { name: 'Cadastrar produto' }) !== null).toBe(cria);
    expect(screen.queryByText('R$ 8,25') !== null).toBe(cria);
    expect(screen.queryByText('R$ 12,34') !== null).toBe(perfil !== 'ESTOQUE');
    await act(() => router.navigate('/produtos/1')); await screen.findByRole('heading', { name: 'Arroz' });
    expect(screen.queryByText('Preço de compra') !== null).toBe(cria);
    expect(screen.queryByText('Preço de venda') !== null).toBe(perfil !== 'ESTOQUE');
    if (!cria) {
      await act(() => router.navigate('/produtos/novo'));
      expect(await screen.findByRole('alert')).toHaveTextContent('Cadastro disponível para Administrador e Compras.');
      expect(screen.queryByRole('button', { name: 'Cadastrar produto' })).not.toBeInTheDocument();
    }
  });
  it('navega entre páginas e informa página sem itens', async () => {
    tamanho = 1; catalogo.push({ ...produto, id: 2, codigo: 'PRD-000002', nome: 'Feijão' });
    const router = abrir(); await screen.findByRole('table');
    fireEvent.click(screen.getByRole('link', { name: 'Próxima página' })); await screen.findByText('Feijão');
    expect(router.state.location.search).toBe('?pagina=1');
    fireEvent.click(screen.getByRole('link', { name: 'Página anterior' })); await screen.findByText('Arroz');
    await act(() => router.navigate('/produtos?pagina=8'));
    expect(await screen.findByText('Nenhum produto nesta página.')).toBeVisible();
  });
  it('apresenta 404 e erro de rede sem alterar catálogo', async () => {
    const router = abrir('/produtos/999'); expect(await screen.findByRole('alert')).toHaveTextContent('Produto não encontrado.');
    falha = 500; await act(() => router.navigate('/produtos'));
    expect(screen.getByRole('alert')).toHaveTextContent('Não foi possível conectar ao sistema.'); expect(catalogo).toHaveLength(1);
  });
  it('redireciona sessão ausente ao login', async () => {
    sessao = null; abrir(); expect(await screen.findByRole('heading', { name: 'Entrar' })).toBeVisible();
  });
  it('informa recusa e falha de validação do cadastro', async () => {
    await formulario(); falha = 400; fireEvent.click(screen.getByRole('button', { name: 'Cadastrar produto' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Confira os campos informados.');
    falha = 403; fireEvent.click(screen.getByRole('button', { name: 'Cadastrar produto' }));
    await waitFor(() => expect(screen.getByRole('alert')).toHaveTextContent('Seu perfil não permite esta operação.'));
  });

  it.each([
    ['ADMINISTRADOR', ['Nome', 'Categoria (opcional)', 'Unidade', 'Preço de venda', 'Estado', 'Estoque atual', 'Estoque mínimo']],
    ['COMPRAS', ['Nome', 'Categoria (opcional)']],
    ['ESTOQUE', ['Estoque atual', 'Estoque mínimo']],
    ['CONSULTA', []],
  ] as const)('oferece somente os campos editáveis do perfil %s', async (perfil, campos) => {
    sessao = { ...comprador, perfil };
    const router = abrir('/produtos/1');
    await screen.findByRole('heading', { name: 'Arroz' });
    const atalho = screen.queryByRole('link', { name: 'Editar produto' });
    if (perfil === 'CONSULTA') {
      expect(atalho).not.toBeInTheDocument();
      await act(() => router.navigate('/produtos/1/editar'));
      expect(await screen.findByRole('alert')).toHaveTextContent('Seu perfil não permite editar produtos.');
      return;
    }
    fireEvent.click(atalho!);
    await screen.findByRole('heading', { name: 'Editar produto' });
    const nomes: readonly string[] = campos;
    for (const rotulo of ['Nome', 'Categoria (opcional)', 'Unidade', 'Preço de venda', 'Estado', 'Estoque atual', 'Estoque mínimo', 'Preço de compra']) {
      expect(screen.queryByLabelText(rotulo) !== null).toBe(nomes.includes(rotulo));
    }
  });

  it('salva e volta ao detalhe com versão, CSRF e decimais exatos', async () => {
    sessao = { ...comprador, perfil: 'ADMINISTRADOR' };
    abrir('/produtos/1/editar');
    await screen.findByRole('heading', { name: 'Editar produto' });
    preencher('Nome', 'Arroz especial');
    preencher('Preço de venda', '13,90');
    preencher('Estoque atual', '9,125');
    fireEvent.click(screen.getByRole('button', { name: 'Salvar alterações' }));
    expect(await screen.findByRole('status')).toHaveTextContent('Produto atualizado.');
    expect(screen.getByRole('heading', { name: 'Arroz especial' })).toBeVisible();
    const escrita = fetchMock.mock.calls.find(([, options]) => options?.method === 'PATCH');
    expect(JSON.parse(escrita?.[1]?.body as string)).toEqual({ versao: 0, nome: 'Arroz especial', categoria: 'Alimentos',
      estoqueAtual: '9.125', estoqueMinimo: '0.000', unidade: 'KG', ativo: true, precoVenda: '13.90' });
    expect(new Headers(escrita?.[1]?.headers).get('X-CSRF-TOKEN')).toBe('csrf-teste');
  });

  it('explica a unidade bloqueada e preserva a versão original em caso de conflito', async () => {
    sessao = { ...comprador, perfil: 'ADMINISTRADOR' };
    catalogo[0].unidadeEditavel = false;
    abrir('/produtos/1/editar');
    expect(await screen.findByText(/A troca direta exige/)).toBeVisible();
    expect(screen.queryByLabelText('Unidade')).not.toBeInTheDocument();
    catalogo[0].versao = 1;
    preencher('Nome', 'Rascunho');
    fireEvent.click(screen.getByRole('button', { name: 'Salvar alterações' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('O produto mudou.');
    expect(screen.getByLabelText('Nome')).toHaveValue('Rascunho');
    expect(catalogo[0].nome).toBe('Arroz');
  });

  it('rejeita estoque negativo antes de enviar a API', async () => {
    sessao = { ...comprador, perfil: 'ESTOQUE' };
    abrir('/produtos/1/editar');
    const estoque = await screen.findByLabelText('Estoque atual');
    preencher('Estoque atual', '-1');
    fireEvent.submit(estoque.closest('form')!);
    expect(await screen.findByRole('alert')).toHaveTextContent('Informe estoques não negativos');
    expect(fetchMock.mock.calls.some(([, options]) => options?.method === 'PATCH')).toBe(false);
  });
});
