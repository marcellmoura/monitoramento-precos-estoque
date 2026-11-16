import { redirect } from 'react-router';
import type { ActionFunctionArgs, LoaderFunctionArgs } from 'react-router';
import { HttpError, mutateJson, requestJson } from '../../shared/http';
import { precoParaApi } from '../../shared/decimal';
import { mensagemErro } from '../acesso/api';

export type Unidade = 'KG' | 'G' | 'L' | 'ML' | 'UN';
export interface Produto {
  id: number; codigo: string; nome: string; categoria: string | null; unidade: Unidade;
  estoqueAtual: string; estoqueMinimo: string; ativo: boolean;
  precoVenda?: string; precoCompra?: string | null;
}
export interface PaginaProdutos { itens: Produto[]; pagina: number; tamanho: number; total: number; paginas: number }
export interface FalhaProduto { erro: string }
export const unidades: Record<Unidade, string> = { KG: 'Quilograma (KG)', G: 'Grama (G)', L: 'Litro (L)', ML: 'Mililitro (ML)', UN: 'Unidade (UN)' };

function tratarFalha(error: unknown) {
  if (error instanceof HttpError) {
    if (error.status === 401) return redirect('/login');
    if (error.status === 403) return { erro: 'Seu perfil não permite esta operação.' };
    if (error.status === 404) return { erro: 'Produto não encontrado.' };
  }
  return { erro: mensagemErro(error) };
}
export async function carregarProdutos({ request }: LoaderFunctionArgs): Promise<PaginaProdutos | FalhaProduto | Response> {
  const pagina = new URL(request.url).searchParams.get('pagina') ?? '0';
  try {
    const result = await requestJson<PaginaProdutos>(`/produtos?pagina=${encodeURIComponent(pagina)}`);
    if (!result) throw new Error('Resposta ausente.');
    return result;
  } catch (error) { return tratarFalha(error); }
}
export async function carregarProduto({ params }: LoaderFunctionArgs): Promise<Produto | FalhaProduto | Response> {
  try {
    const result = await requestJson<Produto>(`/produtos/${encodeURIComponent(params.id ?? '')}`);
    if (!result) throw new Error('Resposta ausente.');
    return result;
  } catch (error) { return tratarFalha(error); }
}
export async function cadastrarProduto({ request }: ActionFunctionArgs) {
  const data = await request.formData();
  const nome = String(data.get('nome') ?? '').trim();
  if (!nome) return { erro: 'Informe o nome do produto.' };
  let precoCompra: string | null;
  let precoVenda: string | null;
  try {
    precoCompra = precoParaApi(String(data.get('precoCompra') ?? ''), true);
    precoVenda = precoParaApi(String(data.get('precoVenda') ?? ''));
  } catch { return { erro: 'Confira os preços: use valores não negativos, com até duas casas decimais e sem separador de milhar.' }; }
  try {
    const result = await mutateJson<Produto>('/produtos', 'POST', {
      nome, categoria: String(data.get('categoria') ?? '').trim() || null,
      unidade: data.get('unidade'), precoCompra, precoVenda,
    });
    if (!result) throw new Error('Resposta ausente.');
    return redirect(`/produtos/${result.id}?criado=1`);
  } catch (error) { return tratarFalha(error); }
}
