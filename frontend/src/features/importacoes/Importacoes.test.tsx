import { fireEvent, render, screen, within } from '@testing-library/react';
import { createMemoryRouter, RouterProvider } from 'react-router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { routes } from '../../app/routes';
import type { Identidade } from '../acesso/api';
import type { ConferenciaCotacao, TentativaImportacao } from './api';

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
let fetchMock: ReturnType<typeof vi.fn>;

beforeEach(() => {
  perfil = 'COMPRAS';
  fornecedores = true;
  tentativas = [];
  falha = undefined;
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
