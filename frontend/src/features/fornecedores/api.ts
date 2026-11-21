import { redirect } from 'react-router';
import type { ActionFunctionArgs, LoaderFunctionArgs } from 'react-router';
import { HttpError, mutateJson, requestJson } from '../../shared/http';
import { mensagemErro } from '../acesso/api';
import type { Identidade } from '../acesso/api';

export interface Fornecedor {
  id: number;
  codigo: string;
  nome: string;
  cnpj: string | null;
  telefone: string | null;
  email: string | null;
}

export interface PaginaFornecedores {
  itens: Fornecedor[];
  pagina: number;
  tamanho: number;
  total: number;
  paginas: number;
}

export interface FalhaFornecedor { erro: string }

export function podeAcessarFornecedores(perfil: Identidade['perfil']) {
  return perfil === 'ADMINISTRADOR' || perfil === 'COMPRAS';
}

function tratarFalha(error: unknown) {
  if (error instanceof HttpError) {
    if (error.status === 401) return redirect('/login');
    if (error.status === 403) return { erro: 'Acesso disponível para Administrador e Compras.' };
    if (error.status === 404) return { erro: 'Fornecedor não encontrado.' };
  }
  return { erro: mensagemErro(error) };
}

async function consultar<T>(path: string, signal: AbortSignal): Promise<T | FalhaFornecedor | Response> {
  try {
    const dados = await requestJson<T>(path, { signal });
    if (!dados) return { erro: 'A consulta não retornou dados. Tente novamente.' };
    return dados;
  } catch (error) {
    return tratarFalha(error);
  }
}

export function carregarFornecedores({ request }: LoaderFunctionArgs) {
  const pagina = new URL(request.url).searchParams.get('pagina') ?? '0';
  const query = new URLSearchParams({ pagina });
  return consultar<PaginaFornecedores>('/fornecedores?' + query, request.signal);
}

export function carregarFornecedor({ params, request }: LoaderFunctionArgs) {
  return consultar<Fornecedor>('/fornecedores/' + encodeURIComponent(params.id ?? ''), request.signal);
}

export async function cadastrarFornecedor({ request }: ActionFunctionArgs) {
  const form = await request.formData();
  const campo = (nome: string) => String(form.get(nome) ?? '').trim();
  const nome = campo('nome');
  if (!nome) return { erro: 'Informe o nome do fornecedor.' };

  try {
    const fornecedor = await mutateJson<Fornecedor>('/fornecedores', 'POST', {
      nome,
      cnpj: campo('cnpj') || null,
      telefone: campo('telefone') || null,
      email: campo('email') || null,
    });
    if (!fornecedor) return { erro: 'Consulte a lista para verificar se o fornecedor foi cadastrado.' };
    return redirect('/fornecedores/' + fornecedor.id + '?criado=1');
  } catch (error) {
    return tratarFalha(error);
  }
}
