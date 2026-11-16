import { HttpError, mutateJson, requestJson } from '../../shared/http';

export interface Identidade {
  id: number;
  nome: string;
  email: string;
  perfil: 'ADMINISTRADOR' | 'COMPRAS' | 'ESTOQUE' | 'CONSULTA';
  trocaSenhaObrigatoria: boolean;
}

export async function carregarIdentidade() {
  try {
    return await requestJson<Identidade>('/auth/me') ?? null;
  } catch (error) {
    if (error instanceof HttpError && error.status === 401) return null;
    throw error;
  }
}

export async function alterarAcesso(path: string, method: string, body?: object) {
  return mutateJson<Identidade>(`/auth/${path}`, method, body);
}

export function mensagemErro(error: unknown): string {
  if (error instanceof HttpError) {
    if (error.code === 'CREDENCIAL_INVALIDA') return 'E-mail ou senha inválidos.';
    if (error.code === 'SENHA_INVALIDA') return 'Confira a senha atual e escolha uma nova senha diferente.';
    if (error.status === 400) return 'Confira os campos informados.';
    if (error.status === 401) return 'Sua sessão expirou. Entre novamente.';
    if (error.status === 403) return 'Requisição não autorizada. Atualize a página e tente novamente.';
  }
  return 'Não foi possível conectar ao sistema. Tente novamente.';
}
