export class HttpError extends Error {
  constructor(public readonly status: number, public readonly code?: string) {
    super(`Não foi possível concluir a requisição (${status}).`);
    this.name = 'HttpError';
  }
}

export async function requestJson<T>(
  path: string,
  options: RequestInit = {},
): Promise<T | undefined> {
  const headers = new Headers(options.headers);
  headers.set('Accept', 'application/json');
  const response = await fetch(`/api${path}`, {
    ...options,
    credentials: 'same-origin',
    headers,
  });

  if (!response.ok) {
    const error: unknown = await response.json().catch(() => null);
    const code = error && typeof error === 'object' && 'codigo' in error
      && typeof error.codigo === 'string' ? error.codigo : undefined;
    throw new HttpError(response.status, code);
  }

  if (response.status === 204) {
    return undefined;
  }

  return response.json() as Promise<T>;
}

export async function mutateJson<T>(path: string, method: string, body?: object) {
  const csrf = await requestJson<{ token: string; headerName: string }>('/auth/csrf');
  if (!csrf) throw new Error('Não foi possível iniciar a operação.');
  return requestJson<T>(path, {
    method,
    headers: { 'Content-Type': 'application/json', [csrf.headerName]: csrf.token },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
}
