import { fireEvent, render, screen } from '@testing-library/react';
import { createMemoryRouter, RouterProvider } from 'react-router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { routes } from '../../app/routes';
import type { Identidade } from '../acesso/api';

const fornecedor = { id: 1, codigo: 'FOR-000001', nome: 'Distribuidora Teste', cnpj: null, telefone: null, email: null };
let perfil: Identidade['perfil'];
const salvar = vi.fn<(corpo: unknown, csrf: string | null) => void>();
let status: number;
let telefoneSalvo: string | null;

beforeEach(() => {
  perfil = 'COMPRAS';
  status = 200;
  telefoneSalvo = null;
  salvar.mockClear();
  vi.stubGlobal('fetch', async (url: string, options?: RequestInit) => {
    if (url.endsWith('/auth/me')) return Response.json({ id: 1, nome: 'Marina', perfil, trocaSenhaObrigatoria: false });
    if (url.endsWith('/auth/csrf')) return Response.json({ token: 'teste', headerName: 'X-CSRF-TOKEN' });
    if (status !== 200) return Response.json({}, { status });
    if (options?.method === 'POST') {
      salvar(JSON.parse(String(options.body)), new Headers(options.headers).get('X-CSRF-TOKEN'));
      return Response.json(fornecedor, { status: 201 });
    }
    if (url.includes('?pagina=')) {
      const pagina = Number(new URL(url, 'http://localhost').searchParams.get('pagina'));
      const itens = pagina === 0 ? [fornecedor] : [];
      return Response.json({ itens, pagina, tamanho: 20, total: 1, paginas: 2 });
    }
    return Response.json({ ...fornecedor, telefone: telefoneSalvo });
  });
});
afterEach(() => vi.unstubAllGlobals());

function abrir(path: string) {
  render(<RouterProvider router={createMemoryRouter(routes, { initialEntries: [path] })} />);
}

describe('Fornecedores', () => {
  it('identifica o fornecedor por código e permite abrir o detalhe', async () => {
    abrir('/fornecedores');
    fireEvent.click(await screen.findByRole('link', { name: 'FOR-000001 · Distribuidora Teste' }));
    expect(await screen.findByRole('heading', { name: 'Distribuidora Teste' })).toBeVisible();
    expect(screen.getAllByText('Não informado')).toHaveLength(3);
  });

  it('navega entre páginas, incluindo página vazia', async () => {
    abrir('/fornecedores');
    fireEvent.click(await screen.findByRole('link', { name: 'Próxima página' }));
    expect(await screen.findByText('Nenhum fornecedor nesta página.')).toBeVisible();
    expect(screen.getByRole('link', { name: 'Página anterior' })).toBeVisible();
  });

  it('cadastra pelo formulário e envia CSRF com contatos opcionais nulos', async () => {
    abrir('/fornecedores/novo');
    fireEvent.change(await screen.findByLabelText('Nome'), { target: { value: ' Distribuidora Teste ' } });
    fireEvent.click(screen.getByRole('button', { name: 'Cadastrar fornecedor' }));
    expect(await screen.findByRole('status')).toHaveTextContent('Fornecedor cadastrado.');
    expect(salvar).toHaveBeenCalledWith({ nome: 'Distribuidora Teste', cnpj: null, telefone: null, email: null }, 'teste');
  });

  it.each([
    ['8333334444', '(83) 3333-4444'],
    ['83999999999', '(83) 99999-9999'],
  ])('formata %s ao sair do campo e no detalhe de um cadastro existente', async (digitos, formatado) => {
    telefoneSalvo = digitos;
    abrir('/fornecedores/novo');
    fireEvent.change(await screen.findByLabelText('Nome'), { target: { value: fornecedor.nome } });
    const telefone = screen.getByLabelText('Telefone (opcional)');
    fireEvent.change(telefone, { target: { value: digitos } });
    fireEvent.blur(telefone);
    expect(telefone).toHaveValue(formatado);
    fireEvent.click(screen.getByRole('button', { name: 'Cadastrar fornecedor' }));
    expect(await screen.findByText(formatado)).toBeVisible();
    expect(salvar).toHaveBeenCalledWith({ nome: fornecedor.nome, cnpj: null, telefone: formatado, email: null }, 'teste');
  });

  it('valida o formato nacional do telefone no formulário', async () => {
    abrir('/fornecedores/novo');
    const telefone = await screen.findByLabelText<HTMLInputElement>('Telefone (opcional)');
    fireEvent.change(telefone, { target: { value: '+55 (83) 99999-9999' } });
    fireEvent.blur(telefone);
    expect(telefone).toHaveValue('+55 (83) 99999-9999');
    expect(telefone.validity.patternMismatch).toBe(true);
    fireEvent.change(telefone, { target: { value: '(83) 3333-4444' } });
    expect(telefone.validity.patternMismatch).toBe(false);
  });

  it('rejeita nome composto só de espaços', async () => {
    abrir('/fornecedores/novo');
    fireEvent.change(await screen.findByLabelText('Nome'), { target: { value: ' ' } });
    fireEvent.click(screen.getByRole('button', { name: 'Cadastrar fornecedor' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Informe o nome do fornecedor.');
    expect(salvar).not.toHaveBeenCalled();
  });

  it('bloqueia a rota de cadastro para Estoque', async () => {
    perfil = 'ESTOQUE';
    abrir('/fornecedores/novo');
    expect(await screen.findByRole('alert')).toHaveTextContent('Acesso disponível para Administrador e Compras.');
    expect(screen.queryByRole('button', { name: 'Cadastrar fornecedor' })).not.toBeInTheDocument();
  });

  it('omite o atalho para Consulta', async () => {
    perfil = 'CONSULTA';
    abrir('/');
    await screen.findByRole('link', { name: 'Consultar produtos' });
    expect(screen.queryByRole('link', { name: 'Consultar fornecedores' })).not.toBeInTheDocument();
  });

  it.each([
    [403, '/fornecedores', 'Acesso disponível para Administrador e Compras.'],
    [404, '/fornecedores/999', 'Fornecedor não encontrado.'],
    [500, '/fornecedores', 'Não foi possível conectar ao sistema.'],
  ])('exibe mensagem para HTTP %s', async (codigo, caminho, mensagem) => {
    status = codigo;
    abrir(caminho);
    expect(await screen.findByRole('alert')).toHaveTextContent(mensagem);
  });
});
