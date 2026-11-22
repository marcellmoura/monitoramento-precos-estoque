import { redirect } from 'react-router';
import type { ActionFunctionArgs, LoaderFunctionArgs } from 'react-router';
import { enviarFormulario, HttpError, mutateJson, requestJson } from '../../shared/http';
import { mensagemErro } from '../acesso/api';
import type { Produto, Unidade } from '../produtos/api';
import type { Identidade } from '../acesso/api';
import type { Fornecedor, PaginaFornecedores } from '../fornecedores/api';

export const estados = {
  PENDENTE: 'Aguardando processamento',
  PROCESSANDO: 'Processando PDF',
  AGUARDANDO_CONFERENCIA: 'Aguardando conferência',
  CONCLUIDA_COM_PENDENCIAS: 'Concluída com pendências',
  CONCLUIDA: 'Concluída',
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

export interface ItemPrevia {
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

export interface BaseComercial {
  coluna: string;
  unidade: Unidade;
  embalagem: string;
}

export interface ConferenciaCotacao {
  colunas: string[];
  dataSugerida: string;
  confirmada: { id: number; base: BaseComercial; dataComercial: string } | null;
  sugestao: BaseComercial | null;
  pendenciasComerciais: string[][];
}

export interface RevisaoItem {
  id: number;
  linha: number;
  atorId: number;
  revisadaEm: string;
  dados: {
    referencia: string | null;
    descricao: string;
    unidade: Unidade | null;
    embalagem: string | null;
    quantidade: number | null;
    coluna: string | null;
    preco: number | null;
    motivo: string;
  };
  pendencias: string[];
}

export type ProdutoCatalogo = Pick<Produto, 'id' | 'codigo' | 'nome' | 'unidade'>;
export interface PaginaCatalogo { itens: ProdutoCatalogo[]; pagina: number; paginas: number }

export interface SituacaoVinculo {
  linha: number;
  conferencia: number | null;
  revisaoItem: number;
  identidade: { referencia: string | null; descricao: string; unidade: Unidade; embalagem: string } | null;
  vinculo: { id: number; produto: ProdutoCatalogo } | null;
  revalidar: boolean;
  reaproveitado?: { tentativaOrigem: number; vinculoOrigem: number; produto: ProdutoCatalogo } | null;
}

export interface ConflitoPreco {
  preco: number;
  dataComercial: string;
  anteriores: { id: number; tentativa: number; preco: number; ordem: number }[];
  revisao: string;
}

export type ClassificacaoPreco = 'AUMENTOU' | 'DIMINUIU' | 'SEM_ALTERACAO' | 'NOVO' | 'PENDENTE';

export interface LinhaConfirmacao {
  linha: number;
  estado: 'PENDENTE' | 'PRONTO' | 'CONFIRMADO';
  classificacao: ClassificacaoPreco | null;
  variacao: { diferencaAbsoluta: number; percentual: `${number}` } | null;
  motivos: string[];
  preco: number | null;
  confirmado: {
    id: number;
    produtoId: number;
    dataComercial: string;
    confirmadaEm: string;
    substituido?: boolean;
    anterior?: { id: number; tentativa: number; preco: number; dataComercial: string } | null;
  } | null;
  conflito?: ConflitoPreco | null;
}

export interface ConfirmacaoImportacao { itens: LinhaConfirmacao[] }

export interface DetalheTentativa extends TentativaImportacao {
  previa?: PreviaImportacao;
  conferencia?: ConferenciaCotacao;
  revisoes?: RevisaoItem[];
  vinculos?: SituacaoVinculo[];
  produtoCriado?: ProdutoCatalogo;
  precos?: ConfirmacaoImportacao;
}

export interface PaginaImportacoes { itens: TentativaImportacao[]; pagina: number; tamanho: number; total: number; paginas: number }
export interface FalhaImportacao { erro: string }

export function podeImportar(perfil: Identidade['perfil']) {
  return perfil === 'ADMINISTRADOR' || perfil === 'COMPRAS';
}

function tratarFalha(error: unknown) {
  if (error instanceof HttpError) {
    if (error.status === 401) return redirect('/login');
    if (error.status === 403) return { erro: 'Acesso disponível para Administrador e Compras.' };
    if (error.code && ['COLUNA_INDISPONIVEL', 'UNIDADE_INCOMPATIVEL', 'DATA_COMERCIAL_INVALIDA', 'EMBALAGEM_INVALIDA'].includes(error.code)) {
      return { erro: error.detail ?? 'Confira os dados comerciais.' };
    }
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
    if (!['AGUARDANDO_CONFERENCIA', 'CONCLUIDA_COM_PENDENCIAS', 'CONCLUIDA'].includes(tentativa.status)) return tentativa;
    const produtoId = new URL(request.url).searchParams.get('produto');
    const produto = produtoId && /^[1-9][0-9]{0,15}$/.test(produtoId)
      ? requestJson<ProdutoCatalogo>(`/produtos/${produtoId}`, { signal: request.signal }).catch(() => undefined)
      : undefined;
    const [previa, conferencia, revisoes, vinculos, produtoCriado, precos] = await Promise.all([
      consultar<PreviaImportacao>(rota + '/previa', request.signal),
      consultar<ConferenciaCotacao>(rota + '/conferencia', request.signal),
      consultar<RevisaoItem[]>(rota + '/itens', request.signal),
      consultar<SituacaoVinculo[]>(rota + '/vinculos', request.signal),
      produto,
      consultar<ConfirmacaoImportacao>(rota + '/precos', request.signal),
    ]);
    return { ...tentativa, previa, conferencia, revisoes, vinculos, produtoCriado, precos };
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
  const operacao = form.get('operacao');
  try {
    if (operacao === 'resolver-conflito-preco') return resolverConflitoPreco(id, form);
    if (operacao === 'confirmar-itens') return gravarPrecos(id);
    if (operacao === 'associar-item') return salvarVinculo(id, form);
    if (operacao === 'corrigir-item') return salvarCorrecao(id, form);
    if (operacao === 'conferir') {
      await mutateJson<ConferenciaCotacao>(`/importacoes/${id}/conferencia`, 'PUT', {
        revisao: Number(form.get('revisao')),
        coluna: form.get('coluna'),
        unidade: form.get('unidade'),
        embalagem: form.get('embalagem'),
        dataComercial: form.get('dataComercial'),
      });
    } else {
      const destino = operacao === 'reprocessar' ? 'reprocessamento' : 'confirmacao';
      await mutateJson<TentativaImportacao>(`/importacoes/${id}/${destino}`, 'POST');
    }
    return redirect(`/importacoes/${id}`);
  } catch (error) { return tratarFalha(error); }
}

async function salvarCorrecao(id: string, form: FormData) {
  const campo = (nome: string) => String(form.get(nome) ?? '').trim() || null;
  try {
    const revisao = await mutateJson<RevisaoItem>(`/importacoes/${id}/itens/${Number(campo('linha'))}`, 'PUT', {
      revisao: Number(campo('revisao')),
      conferencia: Number(campo('conferencia')),
      referencia: campo('referencia'),
      descricao: campo('descricao'),
      unidade: campo('unidade'),
      embalagem: campo('embalagem'),
      quantidade: campo('quantidade'),
      coluna: campo('coluna') === 'manual' ? null : campo('coluna'),
      preco: campo('preco'),
      motivo: campo('motivo'),
    });
    return revisao ? { salvo: true } : { erro: 'Consulte a prévia para verificar a correção.' };
  } catch (error) { return tratarFalha(error); }
}

export async function carregarCatalogo({ request }: LoaderFunctionArgs) {
  const params = new URL(request.url).searchParams;
  try {
    return await consultar<PaginaCatalogo>('/importacoes/catalogo?' + params, request.signal);
  } catch (error) { return tratarFalha(error); }
}

async function salvarVinculo(id: string, form: FormData) {
  try {
    const linha = Number(form.get('linha'));
    await mutateJson(`/importacoes/${id}/itens/${linha}/vinculo`, 'PUT', {
      produto: Number(form.get('produto')),
      conferencia: Number(form.get('conferencia')),
      revisaoItem: Number(form.get('revisaoItem')),
      vinculo: Number(form.get('vinculo')),
    });
    return { salvo: true };
  } catch (error) { return tratarFalha(error); }
}

async function gravarPrecos(id: string) {
  try {
    await mutateJson<ConfirmacaoImportacao>(`/importacoes/${id}/confirmar-itens`, 'POST');
    return redirect(`/importacoes/${id}`);
  } catch (error) {
    if (error instanceof HttpError && error.status >= 500) {
      return { erro: 'A confirmação falhou. Consulte a tentativa antes de tentar novamente.' };
    }
    return tratarFalha(error);
  }
}

async function resolverConflitoPreco(id: string, form: FormData) {
  const linha = encodeURIComponent(String(form.get('linha') ?? ''));
  const substituir = form.get('decisao') === 'SUBSTITUIR';
  try {
    await mutateJson<void>(`/importacoes/${id}/itens/${linha}/resolver-conflito-preco`, 'POST', {
      operacao: substituir ? 'SUBSTITUIR' : form.get('ordem'),
      precoReferencia: Number(form.get('precoReferencia')),
      motivo: substituir ? form.get('motivo') : null,
      revisao: form.get('revisao'),
    });
    return redirect(`/importacoes/${id}#item-${linha}`);
  } catch (error) { return tratarFalha(error); }
}
