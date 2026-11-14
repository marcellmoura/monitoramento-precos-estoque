export class HttpError extends Error {
  constructor(public readonly status: number) {
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
    throw new HttpError(response.status);
  }

  if (response.status === 204) {
    return undefined;
  }

  return response.json() as Promise<T>;
}
