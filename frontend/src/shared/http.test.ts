import { afterEach, describe, expect, it, vi } from 'vitest';
import { HttpError, requestJson } from './http';

afterEach(() => vi.unstubAllGlobals());

describe('Cliente HTTP', () => {
  it('envia cabeçalhos, sinal e sessão para a API e devolve o JSON', async () => {
    const fetchMock = vi.fn().mockResolvedValue(Response.json({ valor: 'ok' }));
    vi.stubGlobal('fetch', fetchMock);
    const signal = new AbortController().signal;

    await expect(requestJson('/exemplo', { signal, headers: { 'X-Teste': '1' } }))
      .resolves.toEqual({ valor: 'ok' });

    const [url, options] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toBe('/api/exemplo');
    expect(options.signal).toBe(signal);
    expect(options.credentials).toBe('same-origin');
    expect(new Headers(options.headers).get('Accept')).toBe('application/json');
    expect(new Headers(options.headers).get('X-Teste')).toBe('1');
  });

  it('aceita uma resposta sem conteúdo sem tentar interpretar JSON', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(null, { status: 204 })));
    await expect(requestJson('/exemplo')).resolves.toBeUndefined();
  });

  it('mantém o status do erro HTTP sem expor o corpo da falha', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('detalhes internos', {
      status: 500,
    })));
    const error = await requestJson('/exemplo').catch((cause: unknown) => cause);
    expect(error).toBeInstanceOf(HttpError);
    expect(error).toMatchObject({ status: 500 });
    expect((error as Error).message).not.toContain('detalhes internos');
  });
});
