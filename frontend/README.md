# Frontend

React/TypeScript/Vite com uma rota inicial, CSS Modules e cliente Fetch compartilhado. Não há fluxos de cadastro, acesso ou importação nesta etapa.

Com Node 26.13.1 selecionado, executar nesta pasta:

```sh
npm ci
npm run dev
```

Abrir `http://127.0.0.1:5173`. Durante o desenvolvimento, `/api` é encaminhado ao backend local na porta 8080. O backend ainda não possui endpoints de negócio. Em produção, o encaminhamento da mesma origem será configurado na etapa de containers.

Validação:

```sh
npm run lint
npm run test:coverage
npm run build
npx playwright install chromium
npm run test:e2e
```

O teste de navegador usa o build servido em `http://127.0.0.1:4173`; executar o build antes dele. Relatórios ficam em `coverage/`, `playwright-report/` e `test-results/`, fora do Git.

Organização: `src/app/` contém composição, rota e tela inicial; `src/shared/`, a infraestrutura HTTP reutilizável. Funcionalidades futuras serão agrupadas por domínio em `src/features/`, separando telas, componentes e serviços conforme o incremento.
