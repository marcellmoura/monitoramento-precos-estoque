import type { RouteObject } from 'react-router';
import { App, FalhaDeConexao } from './App';
import { carregarIdentidade } from '../features/acesso/api';
import { entrar, sair, trocarSenha } from '../features/acesso/actions';
import { AlterarSenha, Inicio, Login } from '../features/acesso/TelasAcesso';

export const routes: RouteObject[] = [{
  path: '/', element: <App />, loader: carregarIdentidade, errorElement: <FalhaDeConexao />,
  children: [
    { index: true, element: <Inicio /> },
    { path: 'login', element: <Login />, action: entrar },
    { path: 'alterar-senha', element: <AlterarSenha />, action: trocarSenha },
    { path: 'sair', action: sair },
  ],
}];
