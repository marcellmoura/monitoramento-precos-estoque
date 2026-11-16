import { redirect } from 'react-router';
import type { ActionFunctionArgs, LoaderFunctionArgs } from 'react-router';
import type { Identidade } from '../acesso/api';
import { mensagemErro } from '../acesso/api';
import { HttpError, mutateJson, requestJson } from '../../shared/http';

export interface Usuario extends Identidade { ativo: boolean }
export interface PaginaUsuarios { itens: Usuario[]; pagina: number; tamanho: number; total: number; paginas: number }
export interface Falha { erro: string }
export const perfis = { ADMINISTRADOR: 'Administrador', COMPRAS: 'Compras', ESTOQUE: 'Estoque', CONSULTA: 'Consulta' };

function erroUsuario(error: unknown) {
  if (error instanceof HttpError) {
    if (error.status === 401) return redirect('/login');
    const mensagens: Record<string, string> = {
      EMAIL_DUPLICADO: 'Já existe um usuário com esse e-mail.',
      ULTIMO_ADMINISTRADOR: 'É necessário manter um Administrador ativo.',
      USUARIO_NAO_ENCONTRADO: 'Usuário não encontrado.',
    };
    if (error.code && mensagens[error.code]) return { erro: mensagens[error.code] };
    if (error.status === 403) return { erro: 'Acesso exclusivo do Administrador.' };
  }
  return { erro: mensagemErro(error) };
}

export async function carregarUsuarios({ request }: LoaderFunctionArgs): Promise<PaginaUsuarios | Falha | Response> {
  const pagina = new URL(request.url).searchParams.get('pagina') ?? '0';
  try {
    const result = await requestJson<PaginaUsuarios>(`/usuarios?pagina=${encodeURIComponent(pagina)}`);
    if (!result) throw new Error('Resposta ausente.');
    return result;
  } catch (error) { return erroUsuario(error); }
}
export async function carregarUsuario({ params }: LoaderFunctionArgs): Promise<Usuario | Falha | Response> {
  try {
    const result = await requestJson<Usuario>(`/usuarios/${encodeURIComponent(params.id ?? '')}`);
    if (!result) throw new Error('Resposta ausente.');
    return result;
  } catch (error) { return erroUsuario(error); }
}
export async function cadastrarUsuario({ request }: ActionFunctionArgs) {
  const data = await request.formData();
  if (data.get('senhaTemporaria') !== data.get('confirmacao')) return { erro: 'As senhas não conferem.' };
  try {
    const result = await mutateJson<Usuario>('/usuarios', 'POST', {
      nome: data.get('nome'), email: data.get('email'), perfil: data.get('perfil'), senhaTemporaria: data.get('senhaTemporaria'),
    });
    if (!result) throw new Error('Resposta ausente.');
    return redirect(`/usuarios/${result.id}?criado=1`);
  } catch (error) { return erroUsuario(error); }
}
export async function administrarUsuario({ request, params }: ActionFunctionArgs) {
  const data = await request.formData();
  const url = `/usuarios/${encodeURIComponent(params.id ?? '')}`;
  try {
    if (data.get('operacao') === 'acesso') {
      await mutateJson<Usuario>(`${url}/acesso`, 'PATCH', { perfil: data.get('perfil'), ativo: data.get('ativo') === 'true' });
      return { sucesso: 'Acesso atualizado. As sessões afetadas foram encerradas.' };
    }
    if (data.get('operacao') !== 'senha') return { erro: 'Operação inválida.' };
    if (data.get('senhaTemporaria') !== data.get('confirmacao')) return { erro: 'As senhas não conferem.' };
    await mutateJson(`${url}/redefinir-senha`, 'POST', { senhaTemporaria: data.get('senhaTemporaria') });
    return { sucesso: 'Senha temporária redefinida. O usuário deverá trocá-la no próximo acesso.' };
  } catch (error) { return erroUsuario(error); }
}
