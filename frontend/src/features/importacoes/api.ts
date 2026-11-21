import { redirect } from 'react-router';
import type { ActionFunctionArgs, LoaderFunctionArgs } from 'react-router';
import { enviarFormulario, HttpError, mutateJson, requestJson } from '../../shared/http';
import { mensagemErro } from '../acesso/api';
import type { Identidade } from '../acesso/api';
import type { Fornecedor, PaginaFornecedores } from '../fornecedores/api';

export const estados = {
  PENDENTE: 'Aguardando processamento',
  PROCESSANDO: 'Processando PDF',
  AGUARDANDO_CONFERENCIA: 'Aguardando conferência',
  FALHA_EXTRACAO: 'Falha na extração',
  REJEITADA: 'PDF rejeitado',
  FALHA_ENVIO: 'Falha no envio',
  DUPLICADA: 'Envio duplicado',
  AGUARDANDO_CONFIRMACAO: 'Aguardando confirmação',
};

export interface TentativaImportacao {
  id: number;
  fornecedorCodigo: string;
  fornecedorNome: string;
  atorId: number;
  arquivo: string;
  enviadaEm: string;
  status: keyof typeof estados;
  motivo: string | null;
  paginas: number | null;
  quantidadeItens: number | null;
  importacaoExistenteId: number | null;
}

interface ItemPrevia {
  dados: { referencia: string | null; descricao: string; unidade: string | null;
    apresentacao: string | null; quantidade: string | null } | null;
  precos: Record<string, string>;
  textoOriginal: string;
  pendencias: string[];
}

export interface PreviaImportacao {
  layout: string;
  dataReferencia: string | null;
  itens: ItemPrevia[];
}

export interface DetalheTentativa extends TentativaImportacao { previa?: PreviaImportacao }

export interface PaginaImportacoes { itens: TentativaImportacao[]; pagina: number; tamanho: number; total: number; paginas: number }
export interface FalhaImportacao { erro: string }

export function podeImportar(perfil: Identidade['perfil']) {
  return perfil === 'ADMINISTRADOR' || perfil === 'COMPRAS';
}

function tratarFalha(error: unknown) {
  if (error instanceof HttpError) {
    if (error.status === 401) return redirect('/login');
    if (error.status === 403) return { erro: 'Acesso disponível para Administrador e Compras.' };
    if (error.status === 404 || error.status === 409 || error.status === 413) {
      return { erro: error.detail ?? 'Não foi possível concluir a operação.' };
    }
  }
  return { erro: mensagemErro(error) };
}

async function consultar<T>(path: string, signal: AbortSignal) {
  const dados = await requestJson<T>(path, { signal });
  if (!dados) throw new Error('Resposta ausente.');
  return dados;
}

export async function carregarImportacoes({ request }: LoaderFunctionArgs) {
  const pagina = new URL(request.url).searchParams.get('pagina') ?? '0';
  try {
    return await consultar<PaginaImportacoes>(`/importacoes?${new URLSearchParams({ pagina })}`, request.signal);
  } catch (error) { return tratarFalha(error); }
}

export async function carregarTentativa({ params, request }: LoaderFunctionArgs) {
  try {
    const rota = `/importacoes/${encodeURIComponent(params.id ?? '')}`;
    const tentativa = await consultar<TentativaImportacao>(rota, request.signal);
    if (tentativa.status !== 'AGUARDANDO_CONFERENCIA') return tentativa;
    const previa = await consultar<PreviaImportacao>(rota + '/previa', request.signal);
    return { ...tentativa, previa };
  } catch (error) { return tratarFalha(error); }
}

export async function carregarFornecedoresDoEnvio({ request }: LoaderFunctionArgs) {
  const carregarPagina = (pagina: number) => consultar<PaginaFornecedores>(`/fornecedores?pagina=${pagina}&tamanho=100`, request.signal);
  try {
    const primeira = await carregarPagina(0);
    const fornecedores: Fornecedor[] = [...primeira.itens];
    for (let pagina = 1; pagina < primeira.paginas; pagina++) {
      fornecedores.push(...(await carregarPagina(pagina)).itens);
    }
    return fornecedores;
  } catch (error) { return tratarFalha(error); }
}

export async function enviarPdf({ request }: ActionFunctionArgs) {
  const form = await request.formData();
  if (!form.get('fornecedorId')) return { erro: 'Selecione o fornecedor do PDF.' };
  const arquivo = form.get('arquivo');
  if (!arquivo || typeof arquivo === 'string' || arquivo.size === 0) return { erro: 'Selecione um PDF que não esteja vazio.' };
  try {
    const tentativa = await enviarFormulario<TentativaImportacao>('/importacoes', form);
    if (!tentativa) return { erro: 'Consulte as tentativas para verificar o envio.' };
    return redirect(`/importacoes/${tentativa.id}`);
  } catch (error) { return tratarFalha(error); }
}

export async function atualizarTentativa({ params, request }: ActionFunctionArgs) {
  const id = encodeURIComponent(params.id ?? '');
  const form = await request.formData();
  const operacao = form.get('operacao') === 'reprocessar' ? 'reprocessamento' : 'confirmacao';
  try {
    await mutateJson<TentativaImportacao>(`/importacoes/${id}/${operacao}`, 'POST');
    return redirect(`/importacoes/${id}`);
  } catch (error) { return tratarFalha(error); }
}
