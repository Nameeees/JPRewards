const { chromium } = require('playwright-core');
(async () => {
  const navegador = await chromium.launch({ channel: 'msedge', headless: true });
  const pagina = await navegador.newPage();
  await pagina.goto('https://www.spigotmc.org/', { waitUntil: 'domcontentloaded' });
  console.log('titulo de la pagina: ' + (await pagina.title()));
  console.log('enlaces de login: ' + (await pagina.locator('a[href*="/login"]').count()));
  await navegador.close();
})().catch((error) => { console.error('ERROR: ' + error.message); process.exit(1); });
