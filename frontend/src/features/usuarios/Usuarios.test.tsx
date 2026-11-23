import { act, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { createMemoryRouter, RouterProvider } from 'react-router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { routes } from '../../app/routes';
import type { Identidade } from '../acesso/api';
import type { Usuario } from './api';

const admin: Usuario = { id: 1, nome: 'Marina Administradora', email: 'admin@example.com', perfil: 'ADMINISTRADOR', ativo: true, trocaSenhaObrigatoria: false };
const operador: Usuario = { id: 2, nome: 'João Compras', email: 'compras@example.com', perfil: 'COMPRAS', ativo: true, trocaSenhaObrigatoria: false };
let contas: Usuario[];
let sessao: Identidade | null;
let falha: { status: number; codigo: string } | null;
let tamanhoPagina: number;
let fetchMock: ReturnType<typeof vi.fn>;

beforeEach(() => {
  contas = [{ ...admin }, { ...operador }]; sessao = { ...admin }; falha = null; tamanhoPagina = 20;
  fetchMock = vi.fn(async (url: string, options?: RequestInit) => {
    if (url.endsWith('/auth/me')) return Response.json(sessao ?? {}, { status: sessao ? 200 : 401 });
    if (url.endsWith('/auth/csrf')) return Response.json({ token: 'csrf-teste', headerName: 'X-CSRF-TOKEN' });
    if (!sessao) return Response.json({}, { status: 401 });
    if (sessao.perfil !== 'ADMINISTRADOR') return Response.json({}, { status: 403 });
    if (falha) return Response.json({ codigo: falha.codigo }, { status: falha.status });
    const route = new URL(url, 'http://localhost');
    if (options?.method === 'POST' && route.pathname === '/api/usuarios') {
      const body = JSON.parse(options.body as string) as Usuario;
      const usuario = { ...body, id: 3, ativo: true, trocaSenhaObrigatoria: true };
      contas.push(usuario);
      return Response.json(usuario, { status: 201 });
    }
    const id = Number(route.pathname.split('/')[3]);
    const usuario = contas.find(item => item.id === id);
    if (options?.method === 'PATCH' && usuario) {
      const body = JSON.parse(options.body as string) as Pick<Usuario, 'perfil' | 'ativo'>;
      Object.assign(usuario, body);
      if (id === sessao.id) sessao = null;
      return Response.json(usuario);
    }
    if (options?.method === 'POST' && usuario) {
      usuario.trocaSenhaObrigatoria = true;
      if (id === sessao.id) sessao = null;
      return new Response(null, { status: 204 });
    }
    if (route.pathname === '/api/usuarios') {
      const pagina = Number(route.searchParams.get('pagina') ?? 0);
      return Response.json({ itens: contas.slice(pagina * tamanhoPagina, (pagina + 1) * tamanhoPagina), pagina,
        tamanho: tamanhoPagina, total: contas.length, paginas: Math.ceil(contas.length / tamanhoPagina) });
    }
    return usuario ? Response.json(usuario) : Response.json({ codigo: 'USUARIO_NAO_ENCONTRADO' }, { status: 404 });
  });
  vi.stubGlobal('fetch', fetchMock);
});
afterEach(() => vi.unstubAllGlobals());
function abrir(path = '/usuarios') {
  const router = createMemoryRouter(routes, { initialEntries: [path] });
  render(<RouterProvider router={router} />); return router;
}
function preencher(label: string, value: string) { fireEvent.change(screen.getByLabelText(label), { target: { value } }); }
function preencherCadastro() {
  preencher('Nome', 'Nova Conta'); preencher('E-mail', 'nova@example.com'); preencher('Perfil', 'ESTOQUE');
  preencher('Senha temporária', 'Temporaria-teste-123'); preencher('Confirme a senha temporária', 'Temporaria-teste-123');
}

describe('Administração de usuários', () => {
  it('oferece navegação administrativa somente para Administrador', async () => {
    abrir('/');
    const menu = await screen.findByRole('navigation', { name: 'Navegação principal' });
    const atalhos = screen.getByRole('navigation', { name: 'Acesso rápido' });
    expect(within(menu).getByRole('link', { name: 'Administrar usuários' })).toHaveAttribute('href', '/usuarios');
    fireEvent.click(within(atalhos).getByRole('link', { name: 'Administrar usuários' }));
    expect(await screen.findByRole('heading', { name: 'Usuários' })).toBeVisible();
  });
  it('lista contas e navega entre páginas', async () => {
    tamanhoPagina = 1; const router = abrir();
    await screen.findByRole('heading', { name: 'Usuários' });
    expect(screen.getByRole('table')).toHaveAccessibleName('Contas e níveis de acesso');
    expect(screen.getByText('Marina Administradora')).toBeVisible();
    fireEvent.click(screen.getByRole('link', { name: 'Próxima página' }));
    await screen.findByText('João Compras'); expect(router.state.location.search).toBe('?pagina=1');
    fireEvent.click(screen.getByRole('link', { name: 'Página anterior' }));
    await screen.findByText('Marina Administradora');
  });
  it('informa página vazia e permite voltar', async () => {
    abrir('/usuarios?pagina=9'); expect(await screen.findByText('Nenhum usuário nesta página.')).toBeVisible();
    expect(screen.getByRole('link', { name: 'Página anterior' })).toBeVisible();
  });
  it('cadastra conta após confirmar a senha e abre administração', async () => {
    const router = abrir('/usuarios/novo'); await screen.findByRole('heading', { name: 'Cadastrar usuário' });
    preencherCadastro(); preencher('Confirme a senha temporária', 'Diferente-teste-456');
    fireEvent.click(screen.getByRole('button', { name: 'Cadastrar usuário' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('As senhas não conferem.');
    expect(fetchMock.mock.calls.some(([, init]) => init?.method === 'POST')).toBe(false);
    preencher('Confirme a senha temporária', 'Temporaria-teste-123');
    fireEvent.click(screen.getByRole('button', { name: 'Cadastrar usuário' }));
    expect(await screen.findByRole('heading', { name: 'Administrar Nova Conta' })).toBeVisible();
    expect(router.state.location.pathname).toBe('/usuarios/3');
    expect(screen.getByRole('status')).toHaveTextContent('Usuário cadastrado com senha temporária.');
    const escrita = fetchMock.mock.calls.find(([, init]) => init?.method === 'POST');
    expect(new Headers(escrita?.[1]?.headers).get('X-CSRF-TOKEN')).toBe('csrf-teste');
    expect(JSON.parse(escrita?.[1]?.body as string)).toMatchObject({ perfil: 'ESTOQUE', email: 'nova@example.com' });
  });
  it('apresenta conflito de e-mail no cadastro', async () => {
    abrir('/usuarios/novo'); await screen.findByRole('heading', { name: 'Cadastrar usuário' }); preencherCadastro();
    falha = { status: 409, codigo: 'EMAIL_DUPLICADO' };
    fireEvent.click(screen.getByRole('button', { name: 'Cadastrar usuário' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Já existe um usuário com esse e-mail.');
    expect(contas).toHaveLength(2);
  });
  it('altera perfil e estado da conta', async () => {
    abrir('/usuarios/2'); await screen.findByRole('heading', { name: 'Administrar João Compras' });
    preencher('Perfil', 'ESTOQUE'); preencher('Estado', 'false');
    fireEvent.click(screen.getByRole('button', { name: 'Salvar acesso' }));
    expect(await screen.findByRole('status')).toHaveTextContent('Acesso atualizado.');
    const escrita = fetchMock.mock.calls.find(([, init]) => init?.method === 'PATCH');
    expect(JSON.parse(escrita?.[1]?.body as string)).toEqual({ perfil: 'ESTOQUE', ativo: false });
  });
  it('explica bloqueio do último Administrador', async () => {
    abrir('/usuarios/1'); await screen.findByRole('heading', { name: 'Administrar Marina Administradora' });
    falha = { status: 409, codigo: 'ULTIMO_ADMINISTRADOR' }; preencher('Estado', 'false');
    fireEvent.click(screen.getByRole('button', { name: 'Salvar acesso' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('É necessário manter um Administrador ativo.');
  });
  it('confirma redefinição e limpa campos de senha após sucesso', async () => {
    abrir('/usuarios/2'); await screen.findByRole('heading', { name: 'Administrar João Compras' });
    const form = within(screen.getByRole('form', { name: 'Redefinir senha' }));
    preencher('Senha temporária', 'Temporaria-teste-123'); preencher('Confirme a senha temporária', 'Outra-senha-teste-456');
    fireEvent.click(form.getByRole('button', { name: 'Redefinir senha' }));
    expect(await form.findByRole('alert')).toHaveTextContent('As senhas não conferem.');
    preencher('Confirme a senha temporária', 'Temporaria-teste-123');
    fireEvent.click(form.getByRole('button', { name: 'Redefinir senha' }));
    expect(await form.findByRole('status')).toHaveTextContent('Senha temporária redefinida.');
    await waitFor(() => expect(form.getByLabelText('Senha temporária')).toHaveValue(''));
    expect(form.getByLabelText('Confirme a senha temporária')).toHaveValue('');
    expect(localStorage.length).toBe(0);
  });
  it('volta ao login quando altera o próprio perfil', async () => {
    const router = abrir('/usuarios/1'); await screen.findByRole('heading', { name: 'Administrar Marina Administradora' });
    preencher('Perfil', 'CONSULTA'); fireEvent.click(screen.getByRole('button', { name: 'Salvar acesso' }));
    await screen.findByRole('heading', { name: 'Entrar' }); expect(router.state.location.pathname).toBe('/login');
  });
  it('volta ao login após redefinir a própria senha', async () => {
    abrir('/usuarios/1'); await screen.findByRole('heading', { name: 'Administrar Marina Administradora' });
    preencher('Senha temporária', 'Temporaria-teste-123'); preencher('Confirme a senha temporária', 'Temporaria-teste-123');
    fireEvent.click(screen.getByRole('button', { name: 'Redefinir senha' }));
    await screen.findByRole('heading', { name: 'Entrar' });
  });
  it.each(['COMPRAS', 'ESTOQUE', 'CONSULTA'] as const)('oculta navegação e bloqueia administração para %s', async perfil => {
    sessao = { ...operador, perfil }; const router = abrir('/');
    await screen.findByRole('heading', { name: 'Olá, João Compras' });
    expect(screen.queryByRole('link', { name: 'Administrar usuários' })).not.toBeInTheDocument();
    await act(() => router.navigate('/usuarios')); await waitFor(() => expect(screen.getByRole('alert')).toHaveTextContent('Acesso exclusivo'));
    await act(() => router.navigate('/usuarios/novo')); expect(await screen.findByRole('heading', { name: 'Acesso restrito' })).toBeVisible();
    await act(() => router.navigate('/usuarios/2')); expect(await screen.findByRole('heading', { name: 'Acesso restrito' })).toBeVisible();
  });
  it('redireciona visitante da listagem para login', async () => {
    sessao = null; abrir(); expect(await screen.findByRole('heading', { name: 'Entrar' })).toBeVisible();
  });
  it('apresenta conta ausente e falha do servidor sem expor detalhes', async () => {
    const router = abrir('/usuarios/999'); expect(await screen.findByRole('alert')).toHaveTextContent('Usuário não encontrado.');
    falha = { status: 500, codigo: 'ERRO_INTERNO' }; await act(() => router.navigate('/usuarios'));
    await waitFor(() => expect(screen.getByRole('alert')).toHaveTextContent('Não foi possível conectar ao sistema.'));
  });
  it('traduz validação de senha recusada e permite corrigir', async () => {
    abrir('/usuarios/2'); await screen.findByRole('heading', { name: 'Administrar João Compras' });
    preencher('Senha temporária', 'Temporaria-teste-123'); preencher('Confirme a senha temporária', 'Temporaria-teste-123');
    falha = { status: 400, codigo: 'DADOS_INVALIDOS' }; fireEvent.click(screen.getByRole('button', { name: 'Redefinir senha' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Confira os campos informados.');
  });
});
