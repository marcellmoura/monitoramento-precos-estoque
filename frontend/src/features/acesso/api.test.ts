import { afterEach, describe, expect, it, vi } from 'vitest';
import { HttpError } from '../../shared/http';
import { alterarAcesso, carregarIdentidade, mensagemErro } from './api';

afterEach(() => vi.unstubAllGlobals());
describe('Contratos de acesso', () => {
  it('distingue sessão ausente de falha do servidor', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(Response.json({}, { status: 500 })));
    await expect(carregarIdentidade()).rejects.toMatchObject({ status: 500 });
  });
  it('não envia operação sem obter CSRF', async () => {
    const fetch = vi.fn().mockResolvedValue(new Response(null, { status: 204 }));
    vi.stubGlobal('fetch', fetch);
    await expect(alterarAcesso('logout', 'POST')).rejects.toThrow('Não foi possível iniciar');
    expect(fetch).toHaveBeenCalledTimes(1);
  });
  it.each([
    [400, undefined, 'Confira os campos informados.'],
    [401, undefined, 'Sua sessão expirou. Entre novamente.'],
    [403, undefined, 'Requisição não autorizada. Atualize a página e tente novamente.'],
    [500, undefined, 'Não foi possível conectar ao sistema. Tente novamente.'],
  ])('traduz erro %s', (status, code, expected) => {
    expect(mensagemErro(new HttpError(status, code))).toBe(expected);
  });
});
