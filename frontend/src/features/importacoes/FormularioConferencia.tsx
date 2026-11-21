import { useFetcher } from 'react-router';
import { unidades } from '../produtos/api';
import type { ConferenciaCotacao, FalhaImportacao } from './api';
import styles from '../../shared/gestao.module.css';

export function FormularioConferencia({ conferencia }: { conferencia: ConferenciaCotacao }) {
  const fetcher = useFetcher<FalhaImportacao>();
  const { colunas, confirmada, sugestao, dataSugerida } = conferencia;
  const base = confirmada?.base ?? sugestao;
  return <section aria-labelledby="conferencia-comercial">
    <h3 id="conferencia-comercial">Conferência comercial</h3>
    {confirmada && <p>Conferência comercial salva.</p>}
    {!confirmada && sugestao && <p>Configuração sugerida a partir de uma cotação deste fornecedor. Confira os campos antes de salvar.</p>}
    <p>Escolha a base de preço de compra. Itens com divergências ou dados ausentes continuam pendentes.</p>
    <fetcher.Form method="post" className={styles.formulario}>
      <input type="hidden" name="operacao" value="conferir" />
      <input type="hidden" name="revisao" value={confirmada?.id ?? 0} />
      <div className={styles.campo}>
        <label htmlFor="coluna-compra">Coluna de preço de compra</label>
        <select id="coluna-compra" name="coluna" defaultValue={base?.coluna ?? ''} required>
          <option value="" disabled>Selecione</option>
          {colunas.map(coluna => <option key={coluna} value={coluna}>{coluna}</option>)}
        </select>
      </div>
      <div className={styles.campo}>
        <label htmlFor="unidade-compra">Unidade de compra</label>
        <select id="unidade-compra" name="unidade" defaultValue={base?.unidade ?? ''} required>
          <option value="" disabled>Selecione</option>
          {Object.entries(unidades).map(([codigo, nome]) => <option key={codigo} value={codigo}>{nome}</option>)}
        </select>
      </div>
      <label>Embalagem de compra<input name="embalagem" required maxLength={120} defaultValue={base?.embalagem ?? ''} /></label>
      <label>Data comercial<input type="date" name="dataComercial" required max="9999-12-31"
        defaultValue={confirmada?.dataComercial ?? dataSugerida} /></label>
      {fetcher.data?.erro && <p role="alert" className={styles.erro}>{fetcher.data.erro}</p>}
      <button type="submit" disabled={fetcher.state !== 'idle'}>
        {fetcher.state === 'idle' ? 'Salvar conferência' : 'Salvando…'}
      </button>
    </fetcher.Form>
  </section>;
}
