import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { createMemoryRouter, RouterProvider } from 'react-router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { routes } from './routes';
import type { Identidade } from '../features/acesso/api';

const usuario: Identidade = { id: 1, nome: 'Marina Teste', email: 'marina@example.com', perfil: 'ADMINISTRADOR', trocaSenhaObrigatoria: true };
let sessao: Identidade | null;
let falha: string | null;
let fetchMock: ReturnType<typeof vi.fn>;

beforeEach(() => {
  sessao = null; falha = null;
  fetchMock = vi.fn(async (url: string, options?: RequestInit) => {
    if (url.endsWith('/csrf')) return Response.json({ token: 'csrf-teste', headerName: 'X-CSRF-TOKEN' });
    if (url.endsWith('/me')) return sessao ? Response.json(sessao) : Response.json({}, { status: 401 });
    if (falha) return Response.json({ codigo: falha }, { status: falha === 'CREDENCIAL_INVALIDA' ? 401 : 400 });
    if (url.endsWith('/login')) {
      const body = JSON.parse(options?.body as string) as { senha: string };
      sessao = { ...usuario, trocaSenhaObrigatoria: body.senha !== 'Definitiva-teste-456' };
      return Response.json(sessao);
    }
    sessao = null;
    return new Response(null, { status: 204 });
  });
  vi.stubGlobal('fetch', fetchMock);
});
afterEach(() => vi.unstubAllGlobals());

function abrir(path = '/') {
  const router = createMemoryRouter(routes, { initialEntries: [path] });
  render(<RouterProvider router={router} />);
  return router;
}
function preencher(label: string, value: string) { fireEvent.change(screen.getByLabelText(label), { target: { value } }); }
async function entrar(senha = 'Temporaria-teste-123') {
  await screen.findByRole('heading', { name: 'Entrar' });
  preencher('E-mail', usuario.email); preencher('Senha', senha);
  fireEvent.click(screen.getByRole('button', { name: 'Entrar' }));
}

describe('Fluxo de acesso', () => {
  it('redireciona visitante para o login e valida campos no navegador', async () => {
    const router = abrir();
    await screen.findByRole('heading', { name: 'Entrar' });
    expect(router.state.location.pathname).toBe('/login');
    expect(screen.getByLabelText('E-mail')).toHaveAttribute('type', 'email');
    expect(screen.getByLabelText('Senha')).toHaveAttribute('type', 'password');
    expect(screen.queryByText('Sistema em preparação.')).not.toBeInTheDocument();
  });
  it('realiza primeiro acesso, exige confirmação e pede novo login após a troca', async () => {
    const router = abrir('/login'); await entrar();
    await screen.findByRole('heading', { name: 'Defina sua senha' });
    expect(router.state.location.pathname).toBe('/alterar-senha');
    preencher('Senha atual', 'Temporaria-teste-123'); preencher('Nova senha', 'Definitiva-teste-456');
    preencher('Confirme a nova senha', 'Diferente-teste-789');
    fireEvent.click(screen.getByRole('button', { name: 'Salvar nova senha' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('As senhas não conferem.');
    expect(fetchMock.mock.calls.some(([url]) => (url as string).endsWith('/senha'))).toBe(false);
    preencher('Confirme a nova senha', 'Definitiva-teste-456');
    fireEvent.click(screen.getByRole('button', { name: 'Salvar nova senha' }));
    expect(await screen.findByRole('status')).toHaveTextContent('Senha alterada. Entre com a nova senha.');
    await entrar('Definitiva-teste-456');
    expect(await screen.findByRole('heading', { name: 'Olá, Marina Teste' })).toBeVisible();
    expect(within(screen.getByRole('region', { name: 'Olá, Marina Teste' })).getByText('Administrador')).toBeVisible();
    fireEvent.click(screen.getByRole('button', { name: 'Sair' }));
    await screen.findByRole('heading', { name: 'Entrar' });
    const escrita = fetchMock.mock.calls.find(([url]) => (url as string).endsWith('/senha'));
    expect(new Headers(escrita?.[1]?.headers).get('X-CSRF-TOKEN')).toBe('csrf-teste');
    expect(localStorage.length).toBe(0);
  });
  it('exibe erro de credenciais sem liberar acesso', async () => {
    falha = 'CREDENCIAL_INVALIDA'; abrir('/login'); await entrar();
    expect(await screen.findByRole('alert')).toHaveTextContent('E-mail ou senha inválidos.');
    expect(screen.getByRole('heading', { name: 'Entrar' })).toBeVisible();
  });
  it('bloqueia navegação direta do primeiro acesso e permite sair', async () => {
    sessao = usuario; const router = abrir('/');
    await screen.findByRole('heading', { name: 'Defina sua senha' });
    expect(router.state.location.pathname).toBe('/alterar-senha');
    fireEvent.click(screen.getByRole('button', { name: 'Sair' }));
    await screen.findByRole('heading', { name: 'Entrar' });
  });
  it('redireciona usuário definitivo do login para o início', async () => {
    sessao = { ...usuario, trocaSenhaObrigatoria: false }; const router = abrir('/login');
    await screen.findByRole('heading', { name: 'Olá, Marina Teste' });
    expect(router.state.location.pathname).toBe('/');
  });
  it('respeita as permissões no menu e nos atalhos do início', async () => {
    sessao = { ...usuario, perfil: 'CONSULTA', trocaSenhaObrigatoria: false };
    abrir();
    const menu = within(await screen.findByRole('navigation', { name: 'Navegação principal' }));
    const atalhos = within(screen.getByRole('navigation', { name: 'Acesso rápido' }));
    expect(menu.getAllByRole('link').map(link => link.getAttribute('href'))).toEqual(['/', '/produtos']);
    expect(atalhos.getAllByRole('link').map(link => link.getAttribute('href'))).toEqual(['/produtos']);
  });
  it('mantém formulário bloqueado durante envio', async () => {
    abrir('/login'); await screen.findByRole('heading', { name: 'Entrar' });
    preencher('E-mail', usuario.email); preencher('Senha', 'Temporaria-teste-123');
    fetchMock.mockImplementation(() => new Promise(() => {}));
    fireEvent.click(screen.getByRole('button', { name: 'Entrar' }));
    expect(await screen.findByRole('button', { name: 'Entrando…' })).toBeDisabled();
  });
  it('apresenta falha de conexão sem mostrar dados internos', async () => {
    fetchMock.mockRejectedValue(new Error('detalhe interno')); abrir();
    expect(await screen.findByRole('alert')).toHaveTextContent('Verifique sua conexão');
    expect(screen.queryByText('detalhe interno')).not.toBeInTheDocument();
  });
  it('permite corrigir senha atual rejeitada pelo backend', async () => {
    sessao = usuario; falha = 'SENHA_INVALIDA'; abrir('/alterar-senha');
    await screen.findByRole('heading', { name: 'Defina sua senha' });
    preencher('Senha atual', 'incorreta'); preencher('Nova senha', 'Definitiva-teste-456');
    preencher('Confirme a nova senha', 'Definitiva-teste-456');
    fireEvent.click(screen.getByRole('button', { name: 'Salvar nova senha' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Confira a senha atual');
    await waitFor(() => expect(screen.getByRole('button', { name: 'Salvar nova senha' })).toBeEnabled());
  });
});
