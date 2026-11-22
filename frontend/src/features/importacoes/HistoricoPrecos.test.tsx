import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { createMemoryRouter, RouterProvider } from 'react-router';
import { afterEach, beforeEach, expect, it, vi } from 'vitest';
import { routes } from '../../app/routes';
import type { Identidade } from '../acesso/api';
import type { PrecoHistorico } from './api';

const substituto: PrecoHistorico = {
  id: 3, produto: { codigo: 'PRD-000002', nome: 'Açúcar de coco' },
  fornecedor: { codigo: 'FOR-000001', nome: 'Distribuidora Teste' },
  unidade: 'KG', embalagem: 'A Granel', modalidade: 'Preço KG',
  preco: 36, ordem: 1, dataComercial: '2026-11-21',
  origem: { tentativa: 5, linha: 1, arquivo: 'substituicao.pdf' },
  substituiu: { tentativa: 1, linha: 1, preco: 32, motivo: 'Cotação corrigida' }, substituidoPor: null,
};

const original: PrecoHistorico = {
  ...substituto, id: 1, preco: 32, ordem: null, origem: { tentativa: 1, linha: 1, arquivo: 'original.pdf' },
  substituiu: null, substituidoPor: { tentativa: 5, linha: 1, preco: 36, motivo: 'Cotação corrigida' },
};

let perfil: Identidade['perfil'];
let consultas: URLSearchParams[];

beforeEach(() => {
  perfil = 'COMPRAS';
  consultas = [];
  vi.stubGlobal('fetch', vi.fn(async (input: string) => {
    const url = new URL(input, 'http://localhost');
    if (url.pathname === '/api/auth/me') return Response.json({ id: 1, nome: 'Usuário', perfil, trocaSenhaObrigatoria: false });
    if (url.pathname !== '/api/importacoes/historico-precos') throw new Error('Rota inesperada.');
    if (perfil === 'ESTOQUE') return Response.json({}, { status: 403 });
    consultas.push(url.searchParams);
    if (url.searchParams.get('inicio') === '2026-11-22') {
      return Response.json({ codigo: 'PERIODO_INVALIDO', mensagem: 'Informe um período comercial válido.' }, { status: 400 });
    }
    const pagina = Number(url.searchParams.get('pagina') ?? '0');
    const itens = url.searchParams.get('tentativa') === '9' ? [] : [pagina === 0 ? substituto : original];
    return Response.json({ itens, pagina, tamanho: 1, total: itens.length ? 2 : 0, paginas: itens.length ? 2 : 0 });
  }));
});

afterEach(() => vi.unstubAllGlobals());

function abrir(caminho = '/historico-precos') {
  const router = createMemoryRouter(routes, { initialEntries: [caminho] });
  render(<RouterProvider router={router} />);
}

it('combina filtros, preserva-os ao paginar e mostra a auditoria nos dois sentidos', async () => {
  abrir('/');
  fireEvent.click(await screen.findByRole('link', { name: 'Histórico de preços' }));
  const submit = await screen.findByRole('button', { name: 'Filtrar histórico' });
  fireEvent.change(screen.getByLabelText('Código do produto'), { target: { value: 'PRD-000002' } });
  fireEvent.change(screen.getByLabelText('Código do fornecedor'), { target: { value: 'FOR-000001' } });
  fireEvent.change(screen.getByLabelText('Data comercial inicial'), { target: { value: '2026-11-21' } });
  fireEvent.change(screen.getByLabelText('Data comercial final'), { target: { value: '2026-11-21' } });
  fireEvent.click(submit);

  await waitFor(() => expect(screen.getByRole('link', { name: 'Próxima página' }))
    .toHaveAttribute('href', expect.stringContaining('produto=PRD-000002')));
  const proxima = screen.getByRole('link', { name: 'Próxima página' });
  expect(Object.fromEntries(consultas.at(-1)!)).toEqual({
    produtoId: '000002', fornecedorId: '000001', inicio: '2026-11-21', fim: '2026-11-21',
  });
  const vigente = within(screen.getByRole('row', { name: /Posição 1 no dia/ }));
  expect(vigente.getByText('Vigente')).toBeVisible();
  expect(vigente.getByRole('link', { name: 'Tentativa #5' })).toHaveAttribute('href', '/importacoes/5?linha=1');
  expect(vigente.getByText(/Cotação corrigida/)).toBeVisible();

  fireEvent.click(proxima);
  expect(await screen.findByText('Substituído')).toBeVisible();
  expect(screen.getByRole('link', { name: 'Tentativa #5' })).toHaveAttribute('href', '/importacoes/5?linha=1');
  expect(screen.getByLabelText('Código do produto')).toHaveValue('PRD-000002');
  expect(consultas.at(-1)?.get('pagina')).toBe('1');
  fireEvent.click(screen.getByRole('link', { name: 'Limpar filtros' }));
  await waitFor(() => expect(screen.getByLabelText('Código do produto')).toHaveValue(''));
});

it('abre o histórico filtrado pela tentativa, com acesso às importações quando estiver vazio', async () => {
  abrir('/historico-precos?tentativa=9');
  expect(await screen.findByText('Nenhum preço encontrado para os filtros informados.')).toBeVisible();
  expect(screen.getByLabelText('Número da tentativa')).toHaveValue('9');
  expect(screen.getByRole('link', { name: 'Consultar importações e pendências' })).toHaveAttribute('href', '/importacoes');
});

it('mostra o erro de período e mantém os campos para correção', async () => {
  abrir('/historico-precos?inicio=2026-11-22&fim=2026-11-21');
  expect(await screen.findByRole('alert')).toHaveTextContent('Informe um período comercial válido.');
  expect(screen.getByLabelText('Data comercial inicial')).toHaveValue('2026-11-22');
});

it.each([
  ['produto=coco', 'Informe o código do produto no formato PRD-000001.'],
  ['fornecedor=coco', 'Informe o código do fornecedor no formato FOR-000001.'],
])('informa o formato de código inválido (%s) sem consultar a API', async (query, mensagem) => {
  abrir(`/historico-precos?${query}`);
  expect(await screen.findByRole('alert')).toHaveTextContent(mensagem);
  expect(consultas).toHaveLength(0);
});

it('bloqueia a consulta pelo perfil Estoque', async () => {
  perfil = 'ESTOQUE';
  abrir();
  expect(await screen.findByRole('alert')).toHaveTextContent('Acesso disponível para Administrador e Compras.');
  expect(screen.queryByRole('table')).not.toBeInTheDocument();
});
