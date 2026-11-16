import { expect, test } from '@playwright/test';

test('visitante é direcionado ao login sem erros no navegador', async ({ page }) => {
  const errors: string[] = [];
  page.on('pageerror', (error) => errors.push(error.message));
  // Smoke da interface; integração HTTP real com PostgreSQL é verificada em AcessoIT.
  await page.route('**/api/auth/me', (route) => route.fulfill({ status: 401, json: {} }));
  await page.goto('/');
  await expect(page).toHaveURL(/\/login$/);
  await expect(page).toHaveTitle('Monitoramento de Preços, Estoque e Compras');
  await expect(page.getByRole('heading', { name: 'Entrar', exact: true })).toBeVisible();
  await expect(page.getByLabel('E-mail')).toHaveAttribute('type', 'email');
  await expect(page.getByLabel('Senha', { exact: true })).toHaveAttribute('type', 'password');
  expect(errors).toEqual([]);
});
