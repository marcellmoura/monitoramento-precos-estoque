import { useFetcher } from 'react-router';
import { useState } from 'react';
import { unidades } from '../produtos/api';
import type { Unidade } from '../produtos/api';
import type { ConferenciaCotacao, ItemPrevia, RevisaoItem } from './api';
import styles from '../../shared/gestao.module.css';
import importacoes from './Importacoes.module.css';

type BaseConfirmada = NonNullable<ConferenciaCotacao['confirmada']>;

interface Props {
  item: ItemPrevia;
  linha: number;
  base: BaseConfirmada;
  colunas: string[];
  revisao?: RevisaoItem;
  associado?: boolean;
}

const moeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });

export function CorrecaoItem(props: Props) {
  const { linha, revisao } = props;
  const [editando, setEditando] = useState(false);
  const rotulo = props.item.dados?.descricao ?? `linha ${linha}`;
  return <>
    {revisao && <DadosConferidos revisao={revisao} associado={props.associado} />}
    {editando
      ? <FormularioCorrecao {...props} fechar={() => setEditando(false)} />
      : <button className={importacoes.botaoCorrecao} type="button" aria-label={`Corrigir item ${rotulo}`}
          onClick={() => setEditando(true)}>Corrigir item</button>}
  </>;
}

function DadosConferidos({ revisao, associado }: { revisao: RevisaoItem; associado?: boolean }) {
  const { dados, pendencias } = revisao;
  return <div>
    <h4>Dados conferidos</h4>
    <dl className={`${styles.dados} ${importacoes.dadosItem}`}>
      <dt>Descrição</dt><dd>{dados.descricao}</dd>
      <dt>Referência</dt><dd>{dados.referencia ?? 'Não informada'}</dd>
      <dt>Unidade</dt><dd>{dados.unidade ?? 'Não informada'}</dd>
      <dt>Embalagem</dt><dd>{dados.embalagem ?? 'Não informada'}</dd>
      <dt>Quantidade</dt><dd>{dados.quantidade ?? 'Não informada'}</dd>
      <dt>Origem do preço</dt><dd>{dados.coluna ?? 'Verificação manual'}</dd>
      <dt>Preço conferido</dt><dd>{dados.preco == null ? 'Não informado' : moeda.format(dados.preco)}</dd>
      <dt>Motivo</dt><dd>{dados.motivo}</dd>
    </dl>
    {pendencias.map(aviso => <p key={aviso}>{aviso}</p>)}
    {pendencias.length > 0 && <p>Correção salva com pendências.</p>}
    {pendencias.length === 0 && <p>{associado ? 'Dados conferidos.' : 'Dados conferidos. Pendente de associação.'}</p>}
  </div>;
}

function FormularioCorrecao({ item, linha, base, colunas, revisao, fechar }: Props & { fechar: () => void }) {
  const fetcher = useFetcher<{ erro?: string; salvo?: boolean }>();
  const [versoes] = useState({ revisao: revisao?.id ?? 0, conferencia: base.id });
  const dados = revisao?.dados;
  const original = item.dados;
  const unidadeExtraida = original?.unidade?.toUpperCase() as Unidade | undefined;
  const unidade = unidadeExtraida && Object.hasOwn(unidades, unidadeExtraida) ? unidadeExtraida : '';
  const embalagemExtraida = original?.apresentacao?.toLowerCase() === 'não informada' ? '' : original?.apresentacao;
  const salvando = fetcher.state !== 'idle';
  if (!salvando && fetcher.data?.salvo) {
    return <button className={importacoes.botaoCorrecao} type="button" onClick={fechar}>Fechar correção salva</button>;
  }
  return <fetcher.Form className={styles.formulario} method="post" aria-label={`Correção da linha ${linha}`}>
    <input type="hidden" name="operacao" value="corrigir-item" />
    <input type="hidden" name="linha" value={linha} />
    <input type="hidden" name="revisao" value={versoes.revisao} />
    <input type="hidden" name="conferencia" value={versoes.conferencia} />
    <label>Descrição<input name="descricao" required maxLength={255} defaultValue={dados?.descricao ?? original?.descricao ?? ''} /></label>
    <label>Referência<input name="referencia" maxLength={120} defaultValue={(dados ? dados.referencia : original?.referencia) ?? ''} /></label>
    <label>Unidade conferida
      <select name="unidade" defaultValue={(dados ? dados.unidade : unidade) ?? ''}>
        <option value="">Pendente</option>
        {Object.entries(unidades).map(([codigo, nome]) => <option key={codigo} value={codigo}>{nome}</option>)}
      </select>
    </label>
    <label>Embalagem conferida<input name="embalagem" maxLength={120}
      defaultValue={(dados ? dados.embalagem : embalagemExtraida) ?? ''} /></label>
    <label>Quantidade conferida<input name="quantidade" type="number" min="0.001" max="999999999999.999" step="0.001"
      defaultValue={dados ? dados.quantidade ?? '' : original?.quantidade?.replace(',', '.') ?? ''} /></label>
    <p>Informe o preço confirmado para a unidade e a embalagem deste item. Se ainda houver dúvida, deixe o preço em branco.</p>
    <label>Origem do preço conferido
      <select name="coluna" required defaultValue={dados ? dados.coluna ?? 'manual' : ''}>
        <option value="" disabled>Selecione</option>
        {colunas.map(coluna => <option key={coluna} value={coluna}>{coluna}</option>)}
        <option value="manual">Verificação manual</option>
      </select>
    </label>
    <label>Preço conferido (R$)<input name="preco" type="number" min="0.01" max="999999999999.99" step="0.01"
      defaultValue={dados?.preco ?? ''} /></label>
    <label>Motivo da correção<input name="motivo" required maxLength={300} /></label>
    {fetcher.data?.erro && <p role="alert" className={styles.erro}>{fetcher.data.erro}</p>}
    <button type="submit" disabled={salvando}>Salvar correção</button>
    <button className={importacoes.botaoCorrecao} type="button" disabled={salvando} onClick={fechar}>Cancelar</button>
  </fetcher.Form>;
}
