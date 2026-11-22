import { useState } from 'react';
import { Link, useFetcher } from 'react-router';
import type { FalhaImportacao, PaginaCatalogo, ProdutoCatalogo, SituacaoVinculo } from './api';
import styles from '../../shared/gestao.module.css';
import importacoes from './Importacoes.module.css';

interface Props {
  tentativa: number;
  situacao: SituacaoVinculo;
  abrirAoVoltar: boolean;
  produtoCriado?: ProdutoCatalogo;
}

export function AssociacaoItem({ tentativa, situacao, abrirAoVoltar, produtoCriado }: Props) {
  const [aberto, setAberto] = useState(abrirAoVoltar);
  const { identidade, vinculo, revalidar, conferencia, reaproveitado } = situacao;
  return <>
    {vinculo && <p>{revalidar ? 'Vínculo anterior precisa de nova confirmação' : 'Produto associado'}{' · '}
      {vinculo.produto.codigo} · {vinculo.produto.nome}</p>}
    {reaproveitado && <p>
      Vínculo reaproveitado · {reaproveitado.produto.codigo} · {reaproveitado.produto.nome}{' · '}
      <Link to={`/importacoes/${reaproveitado.tentativaOrigem}`}>Tentativa de origem #{reaproveitado.tentativaOrigem}</Link>
    </p>}
    {identidade && conferencia && (aberto
      ? <FormularioVinculo tentativa={tentativa} situacao={situacao} unidade={identidade.unidade}
          produtoCriado={produtoCriado} fechar={() => setAberto(false)} />
      : <button className={importacoes.botaoCorrecao} type="button"
          aria-label={`Associar produto da linha ${situacao.linha}`} onClick={() => setAberto(true)}>
          {(vinculo && !revalidar) || reaproveitado ? 'Alterar associação' : 'Associar produto'}
        </button>)}
  </>;
}

function FormularioVinculo({ tentativa, situacao, unidade, produtoCriado, fechar }: {
  tentativa: number; situacao: SituacaoVinculo; unidade: ProdutoCatalogo['unidade'];
  produtoCriado?: ProdutoCatalogo; fechar: () => void;
}) {
  const busca = useFetcher<PaginaCatalogo | FalhaImportacao>();
  const gravacao = useFetcher<{ erro?: string; salvo?: boolean }>();
  const [nome, setNome] = useState('');
  const [consulta, setConsulta] = useState('');
  const [selecionado, selecionar] = useState(produtoCriado?.unidade === unidade ? produtoCriado : undefined);
  const [versoes] = useState({
    conferencia: situacao.conferencia,
    revisaoItem: situacao.revisaoItem,
    vinculo: situacao.vinculo?.id ?? 0,
  });
  const resultado = busca.data && !('erro' in busca.data) ? busca.data : undefined;
  const enviando = gravacao.state !== 'idle';

  function pesquisar(pagina: number, termo: string) {
    setConsulta(termo);
    void busca.load('/importacoes/catalogo?' + new URLSearchParams({ nome: termo, unidade, pagina: String(pagina) }));
  }

  if (!enviando && gravacao.data?.salvo) {
    return <button type="button" className={importacoes.botaoCorrecao} onClick={fechar}>Fechar associação salva</button>;
  }

  return <section aria-label={`Associação da linha ${situacao.linha}`}>
    <form className={styles.formulario} onSubmit={evento => { evento.preventDefault(); pesquisar(0, nome); }}>
      <label>Nome do produto<input value={nome} maxLength={255} onChange={evento => setNome(evento.target.value)} /></label>
      <button type="submit">Buscar produtos em {unidade}</button>
    </form>
    {busca.state !== 'idle' && <p>Buscando produtos…</p>}
    {busca.data && 'erro' in busca.data && <p role="alert" className={styles.erro}>{busca.data.erro}</p>}
    {resultado && <div>
      {resultado.itens.length === 0 && <p>Nenhum produto compatível encontrado.</p>}
      <ul className={styles.lista}>{resultado.itens.map(produto => <li key={produto.id}>
        <button type="button" className={importacoes.botaoCorrecao} onClick={() => selecionar(produto)}>
          Selecionar {produto.codigo} · {produto.nome} · {produto.unidade}
        </button>
      </li>)}</ul>
      {resultado.pagina > 0 && <button type="button" onClick={() => pesquisar(resultado.pagina - 1, consulta)}>Página anterior</button>}
      {resultado.pagina + 1 < resultado.paginas && <button type="button"
        onClick={() => pesquisar(resultado.pagina + 1, consulta)}>Próxima página</button>}
    </div>}
    <p><Link to={`/produtos/novo?tentativa=${tentativa}&linha=${situacao.linha}`}>Cadastrar produto</Link></p>
    {produtoCriado && produtoCriado.unidade !== unidade && <p>O produto cadastrado usa outra unidade. Selecione um produto em {unidade}.</p>}
    <gravacao.Form method="post" className={styles.formulario}>
      <input type="hidden" name="operacao" value="associar-item" />
      <input type="hidden" name="linha" value={situacao.linha} />
      <input type="hidden" name="produto" value={selecionado?.id ?? ''} />
      <input type="hidden" name="conferencia" value={versoes.conferencia ?? ''} />
      <input type="hidden" name="revisaoItem" value={versoes.revisaoItem} />
      <input type="hidden" name="vinculo" value={versoes.vinculo} />
      {selecionado && <p>Selecionado · {selecionado.codigo} · {selecionado.nome} · {selecionado.unidade}</p>}
      {gravacao.data?.erro && <p role="alert" className={styles.erro}>{gravacao.data.erro}</p>}
      <button type="submit" disabled={!selecionado || enviando}>Confirmar associação</button>
      <button type="button" disabled={enviando} onClick={fechar}>Cancelar</button>
    </gravacao.Form>
  </section>;
}
