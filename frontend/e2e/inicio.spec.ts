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

test('permite conferir a senha pelo teclado em uma tela estreita', async ({ page }) => {
  await page.setViewportSize({ width: 320, height: 568 });
  await page.route('**/api/auth/me', (route) => route.fulfill({ status: 401, json: {} }));
  await page.goto('/login');

  const email = page.getByLabel('E-mail');
  const senha = page.getByLabel('Senha', { exact: true });
  const entrar = page.getByRole('button', { name: 'Entrar' });

  await expect(email).toBeVisible();
  await page.keyboard.press('Tab');
  await expect(email).toBeFocused();
  await page.keyboard.press('Tab');
  await expect(senha).toBeFocused();
  await page.keyboard.type('senha-de-teste');
  await page.keyboard.press('Tab');
  await expect(page.getByRole('button', { name: 'Mostrar senha' })).toBeFocused();
  await page.keyboard.press('Space');
  await expect(senha).toHaveAttribute('type', 'text');
  await expect(senha).toHaveValue('senha-de-teste');
  await expect(page.getByRole('button', { name: 'Ocultar senha' })).toBeFocused();
  await page.keyboard.press('Space');
  await expect(senha).toHaveAttribute('type', 'password');
  await page.keyboard.press('Tab');
  await expect(entrar).toBeFocused();
  await expect(entrar).toBeInViewport();
  expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBe(320);
});
