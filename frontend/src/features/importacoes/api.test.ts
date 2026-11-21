// @vitest-environment node
import { afterEach, expect, it, vi } from 'vitest';
import { enviarPdf } from './api';

afterEach(() => vi.unstubAllGlobals());

function enviar(origem: string | null, arquivo?: string) {
  const form = new FormData();
  if (origem !== null) form.set('fornecedorId', origem);
  if (arquivo !== undefined) form.set('arquivo', new File([arquivo], 'orcamento.pdf', { type: 'application/pdf' }));
  const request = new Request('http://localhost/importacoes/nova', { method: 'POST', body: form });
  return enviarPdf({ request, params: {}, context: {} } as Parameters<typeof enviarPdf>[0]);
}

it('envia multipart com CSRF, mantém os bytes e abre a tentativa por ID', async () => {
  const fetchMock = vi.fn(async (_url: string, init?: RequestInit) => init?.method === 'POST'
    ? Response.json({ id: 1 }, { status: 201 })
    : Response.json({ token: 'csrf-teste', headerName: 'X-CSRF-TOKEN' }));
  vi.stubGlobal('fetch', fetchMock);

  const resposta = await enviar('2', 'PDF de teste');
  expect(resposta).toBeInstanceOf(Response);
  expect((resposta as Response).headers.get('Location')).toBe('/importacoes/1');
  const [url, init] = fetchMock.mock.calls[1];
  expect(url).toBe('/api/importacoes');
  expect(new Headers(init?.headers).get('X-CSRF-TOKEN')).toBe('csrf-teste');
  expect(new Headers(init?.headers).has('Content-Type')).toBe(false);
  const form = init?.body as FormData;
  expect(form.get('fornecedorId')).toBe('2');
  const arquivo = form.get('arquivo') as File;
  expect(arquivo.name).toBe('orcamento.pdf');
  expect(await arquivo.text()).toBe('PDF de teste');
});

it('valida o formulário e apresenta limite de transporte', async () => {
  const fetchMock = vi.fn(async (_url: string, init?: RequestInit) => init?.method === 'POST'
    ? Response.json({ mensagem: 'O arquivo ultrapassa o limite de envio.' }, { status: 413 })
    : Response.json({ token: 'csrf-teste', headerName: 'X-CSRF-TOKEN' }));
  vi.stubGlobal('fetch', fetchMock);

  expect(await enviar(null, 'pdf')).toEqual({ erro: 'Selecione o fornecedor do PDF.' });
  expect(await enviar('1', '')).toEqual({ erro: 'Selecione um PDF que não esteja vazio.' });
  expect(fetchMock).not.toHaveBeenCalled();
  expect(await enviar('1', 'pdf')).toEqual({ erro: 'O arquivo ultrapassa o limite de envio.' });
});
