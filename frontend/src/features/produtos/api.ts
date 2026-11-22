import { redirect } from 'react-router';
import type { ActionFunctionArgs, LoaderFunctionArgs } from 'react-router';
import { HttpError, mutateJson, requestJson } from '../../shared/http';
import { precoParaApi } from '../../shared/decimal';
import { mensagemErro } from '../acesso/api';

export type Unidade = 'KG' | 'G' | 'L' | 'ML' | 'UN';
export interface Produto {
  id: number; codigo: string; nome: string; categoria: string | null; unidade: Unidade;
  estoqueAtual: string; estoqueMinimo: string; ativo: boolean; versao: number; unidadeEditavel: boolean;
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
    if (error.status === 409) return { erro: 'O produto mudou. Volte aos detalhes e reabra a edição antes de salvar.' };
    if (error.code === 'PRODUTO_EDICAO_INVALIDA' && error.detail) return { erro: error.detail };
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
export function destinoImportacao(params: URLSearchParams) {
  const tentativa = params.get('tentativa');
  const linha = params.get('linha');
  return tentativa && linha && /^[1-9][0-9]{0,15}$/.test(tentativa) && /^[1-9][0-9]{0,8}$/.test(linha)
    ? `/importacoes/${tentativa}?linha=${linha}` : null;
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
    const destino = destinoImportacao(new URL(request.url).searchParams);
    return redirect(destino ? `${destino}&produto=${result.id}` : `/produtos/${result.id}?criado=1`);
  } catch (error) { return tratarFalha(error); }
}

export async function editarProduto({ request, params }: ActionFunctionArgs) {
  const form = await request.formData();
  const versao = Number(form.get('versao'));
  if (!form.has('versao') || !Number.isSafeInteger(versao) || versao < 0) {
    return { erro: 'Volte aos detalhes e reabra a edição antes de salvar.' };
  }
  const alteracao: Record<string, unknown> = { versao };
  for (const campo of ['nome', 'categoria', 'unidade']) {
    if (form.has(campo)) alteracao[campo] = String(form.get(campo)).trim();
  }
  if (form.has('ativo')) alteracao.ativo = form.get('ativo') === 'true';
  if (alteracao.nome === '') return { erro: 'Informe o nome do produto.' };
  if (form.has('precoVenda')) {
    try {
      alteracao.precoVenda = precoParaApi(String(form.get('precoVenda')));
    } catch {
      return { erro: 'Informe um preço de venda não negativo com até duas casas decimais.' };
    }
  }
  for (const campo of ['estoqueAtual', 'estoqueMinimo']) {
    if (!form.has(campo)) continue;
    const valor = String(form.get(campo)).trim();
    if (!/^[0-9]{1,16}([.,][0-9]{1,3})?$/.test(valor)) {
      return { erro: 'Informe estoques não negativos, com até três casas decimais e sem separador de milhar.' };
    }
    alteracao[campo] = valor.replace(',', '.');
  }
  try {
    const produto = await mutateJson<Produto>(`/produtos/${encodeURIComponent(params.id ?? '')}`, 'PATCH', alteracao);
    if (!produto) throw new Error('Resposta ausente.');
    return redirect(`/produtos/${produto.id}?editado=1`);
  } catch (error) {
    return tratarFalha(error);
  }
}
