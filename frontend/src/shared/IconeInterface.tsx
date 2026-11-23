const desenhos = {
  casa: <path d="m3 11 9-8 9 8m-2-2v11h-5v-6h-4v6H5V9" />,
  pessoa: <><circle cx="12" cy="7" r="3.5" /><path d="M5 21v-2a5 5 0 0 1 5-5h4a5 5 0 0 1 5 5v2" /></>,
  pessoas: <><circle cx="9" cy="7" r="3" /><path d="M3 20v-2a5 5 0 0 1 5-5h2a5 5 0 0 1 5 5v2m2-16a3 3 0 0 1 0 6m1 3a5 5 0 0 1 3 5v2" /></>,
  importar: <path d="M12 3v12m-4-4 4 4 4-4M4 16v4h16v-4" />,
  enviar: <path d="M12 15V3m-4 4 4-4 4 4M4 16v4h16v-4" />,
  buscar: <><circle cx="10.5" cy="10.5" r="6.5" /><path d="m16 16 4 4" /></>,
  adicionar: <path d="M12 5v14M5 12h14" />,
  editar: <path d="m15 5 4 4M4 20l4.5-1 11-11a2.8 2.8 0 0 0-4-4l-11 11L4 20Z" />,
  filtro: <path d="M3 4h18l-7 8v6l-4 2v-8z" />,
  camadas: <path d="m12 3 9 5-9 5-9-5 9-5ZM3 12l9 5 9-5M3 16l9 5 9-5" />,
  relogio: <><circle cx="12" cy="12" r="9" /><path d="M12 7v5l3 2" /></>,
  atencao: <path d="M12 3 2.5 20h19L12 3Zm0 6v5m0 3h.01" />,
  confirmado: <path d="m5 12 4 4L19 6" />,
  bloqueado: <><circle cx="12" cy="12" r="9" /><path d="m6 18 12-12" /></>,
  seta: <path d="m9 6 6 6-6 6" />,
  voltar: <path d="M20 12H4m7-7-7 7 7 7" />,
  informacao: <><circle cx="12" cy="12" r="9" /><path d="M12 11v5m0-8h.01" /></>,
  caixa: <path d="m12 3 8 4.5v9L12 21l-8-4.5v-9L12 3Zm-8 4.5 8 4.5 8-4.5M12 12v9" />,
  documento: <path d="M6 3h8l4 4v14H6zM14 3v5h4M9 12h6m-6 4h6" />,
  grafico: <path d="M4 20h16M6 16v-3m4 3v-5m4 5v-7m4 7V5M5 9l5-4 3 2 5-4" />,
  email: <><rect x="3" y="5" width="18" height="14" rx="2" /><path d="m4 7 8 6 8-6" /></>,
  cadeado: <><rect x="5" y="10" width="14" height="11" rx="2" /><path d="M8 10V7a4 4 0 0 1 8 0v3m-4 4v3" /></>,
  entrar: <path d="M14 4h5v16h-5M3 12h12m-4-4 4 4-4 4" />,
  sair: <path d="M10 4H5v16h5m-1-8h12m-4-4 4 4-4 4" />,
  escudo: <path d="M12 22s7-3.5 7-10V5l-7-3-7 3v7c0 6.5 7 10 7 10Zm-3-10 2 2 4-4" />,
  olho: <><path d="M2 12s3.5-6 10-6 10 6 10 6-3.5 6-10 6-10-6-10-6Z" /><circle cx="12" cy="12" r="2.5" /></>,
};

export type NomeIcone = keyof typeof desenhos;

export function IconeInterface({ nome, riscado = false }: { nome: NomeIcone; riscado?: boolean }) {
  return <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    {desenhos[nome]}
    {riscado && <path d="m3 3 18 18" />}
  </svg>;
}
