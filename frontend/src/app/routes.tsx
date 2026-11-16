import type { RouteObject } from 'react-router';
import { App, FalhaDeConexao } from './App';
import { carregarIdentidade } from '../features/acesso/api';
import { entrar, sair, trocarSenha } from '../features/acesso/actions';
import { AlterarSenha, Inicio, Login } from '../features/acesso/TelasAcesso';

import { Usuarios, NovoUsuario, AdministrarUsuario } from '../features/usuarios/Usuarios';
import { carregarUsuarios, carregarUsuario, cadastrarUsuario, administrarUsuario } from '../features/usuarios/api';

import { Produtos, NovoProduto, DetalheProduto } from '../features/produtos/Produtos';
import { carregarProdutos, carregarProduto, cadastrarProduto } from '../features/produtos/api';

export const routes: RouteObject[] = [{
  path: '/', element: <App />, loader: carregarIdentidade, errorElement: <FalhaDeConexao />,
  children: [
    { index: true, element: <Inicio /> },
    { path: 'produtos', element: <Produtos />, loader: carregarProdutos },
    { path: 'produtos/novo', element: <NovoProduto />, action: cadastrarProduto },
    { path: 'produtos/:id', element: <DetalheProduto />, loader: carregarProduto },
    { path: 'login', element: <Login />, action: entrar },
    { path: 'alterar-senha', element: <AlterarSenha />, action: trocarSenha },
    { path: 'sair', action: sair },
    { path: 'usuarios', element: <Usuarios />, loader: carregarUsuarios },
    { path: 'usuarios/novo', element: <NovoUsuario />, action: cadastrarUsuario },
    { path: 'usuarios/:id', element: <AdministrarUsuario />, loader: carregarUsuario, action: administrarUsuario },
  ],
}];
