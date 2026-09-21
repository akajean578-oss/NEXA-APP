import { test, expect } from '@playwright/test';

/**
 * Ce test suppose que le backend Spring Boot tourne sur http://localhost:8080
 * (`mvn spring-boot:run`) en plus du frontend (`npm run dev`), comme en local.
 * Il simule un vrai utilisateur dans un navigateur : inscription, publication,
 * like, commentaire — la chaîne complète.
 */
test('parcours complet : inscription, publication, like, commentaire', async ({ page }) => {
  const email = `e2e_${Date.now()}@nexa.dev`;

  await page.goto('/register');
  await page.getByPlaceholder('Nom complet').fill('Utilisateur E2E');
  await page.getByPlaceholder('Adresse e-mail').fill(email);
  await page.getByPlaceholder(/mot de passe/i).fill('motdepasse123');
  await page.getByRole('button', { name: "S'inscrire" }).click();

  // Redirigé vers le fil d'actualité après inscription
  await expect(page).toHaveURL('/');
  await expect(page.getByPlaceholder(/exprimez-vous/i)).toBeVisible();

  // Publier un post
  await page.getByText(/exprimez-vous/i).first().click();
  await page.getByPlaceholder(/exprimez-vous/i).fill('Mon post de test end-to-end');
  await page.getByRole('button', { name: 'Publier' }).click();

  await expect(page.getByText('Mon post de test end-to-end')).toBeVisible();

  // Aimer le post
  const postCard = page.locator('.post', { hasText: 'Mon post de test end-to-end' });
  await postCard.getByText("J'aime").click();
  await expect(postCard.getByText("J'aime")).toHaveClass(/liked/);

  // Commenter le post
  await postCard.getByText('Commenter').click();
  await postCard.getByPlaceholder(/écrire un commentaire/i).fill('Super post e2e !');
  await postCard.getByPlaceholder(/écrire un commentaire/i).press('Enter');

  await expect(postCard.getByText('Super post e2e !')).toBeVisible();
});

test('un utilisateur non connecté est redirigé vers /login', async ({ page }) => {
  await page.goto('/');
  await expect(page).toHaveURL('/login');
});
