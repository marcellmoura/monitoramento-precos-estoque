import { fireEvent, render, screen } from '@testing-library/react';
import { createMemoryRouter, RouterProvider } from 'react-router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { routes } from '../../app/routes';
import type { Identidade } from '../acesso/api';
import type { TentativaImportacao } from './api';

const base = {
  id: 1, fornecedorCodigo: 'FOR-000001', fornecedorNome: 'Distribuidora Teste', atorId: 1,
  arquivo: 'orcamento.pdf', enviadaEm: '2026-11-21T08:00:00Z',
  status: 'PENDENTE', paginas: 2, quantidadeItens: null, motivo: null, importacaoExistenteId: null,
} satisfies TentativaImportacao;

let perfil: Identidade['perfil'];
let fornecedores: boolean;
let tentativas: TentativaImportacao[];
let falha: number | undefined;
let fetchMock: ReturnType<typeof vi.fn>;

beforeEach(() => {
  perfil = 'COMPRAS';
  fornecedores = true;
  tentativas = [];
  falha = undefined;
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
    const tentativa = tentativas.find(item => item.id === Number(url.pathname.split('/')[3]));
    if (tentativa && url.pathname.endsWith('/confirmacao')) {
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
});
