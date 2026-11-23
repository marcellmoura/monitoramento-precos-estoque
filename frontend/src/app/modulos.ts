import type { ItemMenu } from './MenuPrincipal';
import type { Identidade } from '../features/acesso/api';
import { podeAcessarFornecedores } from '../features/fornecedores/api';
import { podeImportar } from '../features/importacoes/api';

export const perfis = { ADMINISTRADOR: 'Administrador', COMPRAS: 'Compras', ESTOQUE: 'Estoque', CONSULTA: 'Consulta' };

type Modulo = ItemMenu & { atalho?: string };

export function modulosVisiveis(perfil: Identidade['perfil']) {
  const modulos: Modulo[] = [{ caminho: '/produtos', nome: 'Produtos', atalho: 'Consultar produtos', icone: 'caixa' }];
  if (podeAcessarFornecedores(perfil)) {
    modulos.push({ caminho: '/fornecedores', nome: 'Fornecedores', atalho: 'Consultar fornecedores', icone: 'pessoas' });
  }
  if (podeImportar(perfil)) {
    modulos.push(
      { caminho: '/importacoes', nome: 'Importações', atalho: 'Consultar importações', icone: 'importar' },
      { caminho: '/historico-precos', nome: 'Preços', atalho: 'Histórico de preços', icone: 'grafico' },
    );
  }
  if (perfil === 'ADMINISTRADOR') {
    modulos.push({ caminho: '/usuarios', nome: 'Administrar usuários', atalho: 'Administrar usuários', icone: 'pessoa' });
  }
  return modulos;
}
