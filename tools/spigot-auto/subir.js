// Sube una actualizacion de JPRewards a SpigotMC.
//
// SpigotMC NO tiene API para publicar recursos: este script maneja el navegador
// con Playwright usando el Microsoft Edge que ya tienes instalado (no descarga
// navegadores). La sesion se guarda en .sesion.json, asi que la primera vez
// inicias sesion a mano y las siguientes ya entran solas.
//
//   npm install
//   node subir.js --iniciar-sesion     abre el navegador para entrar (una vez)
//   node subir.js --dry-run            llega hasta el formulario y NO envia
//   node subir.js                      sube el update de verdad
//
// Variables (opcional, en un .env al lado de este archivo):
//   SPIGOT_RESOURCE_ID=123456
//   SPIGOT_USER=tu-usuario          (solo si quieres que entre solo)
//   SPIGOT_PASS=tu-contrasena
//
// Requisitos: dist/TITULO.txt y dist/DESCRIPCION.bbcode (los genera
// tools/publicar-spigot.ps1) y dist/JPRewards-<version>.jar.

const fs = require('fs');
const path = require('path');
const readline = require('readline');
const { chromium } = require('playwright-core');

const AQUI = __dirname;
const RAIZ = path.resolve(AQUI, '..', '..');
const DIST = path.join(RAIZ, 'dist');
const SESION = path.join(AQUI, '.sesion.json');
const CAPTURAS = path.join(AQUI, 'capturas');

// --- Selectores de SpigotMC (si algun dia cambian, se tocan aqui) ----------
const SEL = {
  loginLink: 'a[href*="/login"]',
  loginUser: 'input[name="login"]',
  loginPass: 'input[name="password"]',
  loginSubmit: 'button[type="submit"], input[type="submit"]',
  updateLink: 'a[href*="/update"]',
  titulo: 'input[name="title"]',
  mensajeHtml: 'textarea[name="message_html"], #ctrl_message_html',
  editor: '.fr-element[contenteditable="true"], .redactor-in, iframe.redactor',
  archivo: 'input[type="file"]',
  enviar: 'button[type="submit"], input[type="submit"]',
};

function leerEnv() {
  const archivo = path.join(AQUI, '.env');
  if (!fs.existsSync(archivo)) return;
  for (const linea of fs.readFileSync(archivo, 'utf8').split(/\r?\n/)) {
    const limpia = linea.trim();
    if (!limpia || limpia.startsWith('#')) continue;
    const separador = limpia.indexOf('=');
    if (separador < 0) continue;
    process.env[limpia.slice(0, separador).trim()] = limpia.slice(separador + 1).trim();
  }
}
leerEnv();

function version() {
  const pom = fs.readFileSync(path.join(RAIZ, 'pom.xml'), 'utf8');
  const encontrado = pom.match(/<version>([^<]+)<\/version>/);
  return encontrado ? encontrado[1] : null;
}

function esperar(mensaje) {
  const lector = readline.createInterface({ input: process.stdin, output: process.stdout });
  return new Promise((listo) => lector.question(mensaje, () => { lector.close(); listo(); }));
}

async function iniciarSesion(contexto, pagina) {
  const usuario = process.env.SPIGOT_USER;
  const clave = process.env.SPIGOT_PASS;
  if (usuario && clave) {
    await pagina.goto('https://www.spigotmc.org/login/', { waitUntil: 'domcontentloaded' });
    await pagina.fill(SEL.loginUser, usuario);
    await pagina.fill(SEL.loginPass, clave);
    await Promise.all([pagina.waitForLoadState('networkidle'), pagina.click(SEL.loginSubmit)]);
    return;
  }
  await pagina.goto('https://www.spigotmc.org/login/', { waitUntil: 'domcontentloaded' });
  console.log('Inicia sesion en la ventana del navegador y vuelve aqui cuando estes dentro.');
  await esperar('Pulsa Enter cuando ya hayas iniciado sesion... ');
  await contexto.storageState({ path: SESION });
  console.log('Sesion guardada en .sesion.json (no se sube al repositorio).');
}

async function principal() {
  const argumentos = process.argv.slice(2);
  const soloSesion = argumentos.includes('--iniciar-sesion');
  const seco = argumentos.includes('--dry-run');
  const oculto = argumentos.includes('--oculto');
  const id = (process.env.SPIGOT_RESOURCE_ID || '').trim();
  const v = version();

  if (!v) { console.error('No se pudo leer la version del pom.xml'); process.exit(1); }
  if (!id && !soloSesion) {
    console.error('Falta SPIGOT_RESOURCE_ID (el numero de la URL del recurso).');
    console.error('Ejemplo: set SPIGOT_RESOURCE_ID=123456');
    process.exit(1);
  }
  if (!soloSesion) {
    for (const necesario of [path.join(DIST, 'TITULO.txt'), path.join(DIST, 'DESCRIPCION.bbcode'),
                             path.join(DIST, 'JPRewards-' + v + '.jar')]) {
      if (!fs.existsSync(necesario)) {
        console.error('Falta ' + necesario + '. Ejecuta antes tools/publicar-spigot.ps1');
        process.exit(1);
      }
    }
  }

  if (!fs.existsSync(CAPTURAS)) fs.mkdirSync(CAPTURAS, { recursive: true });

  const navegador = await chromium.launch({
    channel: 'msedge',        // usa el Edge instalado
    headless: !soloSesion && oculto,
  });
  const contexto = await navegador.newContext({
    viewport: { width: 1440, height: 980 },
    storageState: fs.existsSync(SESION) ? SESION : undefined,
    locale: 'es-ES',
  });
  const pagina = await contexto.newPage();

  try {
    console.log('Abriendo spigotmc.org...');
    await pagina.goto('https://www.spigotmc.org/', { waitUntil: 'domcontentloaded' });

    if (soloSesion) {
      await iniciarSesion(contexto, pagina);
      console.log('Listo. Ya puedes usar node subir.js sin volver a entrar.');
      return;
    }

    // Si no hay sesion guardada (o caduco), se inicia.
    const enlaceLogin = await pagina.locator(SEL.loginLink).first().isVisible().catch(() => false);
    if (enlaceLogin) {
      await iniciarSesion(contexto, pagina);
      await contexto.storageState({ path: SESION });
      await pagina.goto('https://www.spigotmc.org/', { waitUntil: 'domcontentloaded' });
    }

    const urlRecurso = 'https://www.spigotmc.org/resources/' + id + '/';
    console.log('Recurso: ' + urlRecurso);
    await pagina.goto(urlRecurso, { waitUntil: 'domcontentloaded' });
    await pagina.screenshot({ path: path.join(CAPTURAS, 'recurso.png'), fullPage: false });

    // Ir al formulario de "post a new update".
    let actualizado = false;
    const enlaces = pagina.locator(SEL.updateLink);
    const total = await enlaces.count();
    for (let i = 0; i < total; i++) {
      const href = await enlaces.nth(i).getAttribute('href');
      if (href && href.toLowerCase().includes('update')) {
        await pagina.goto(new URL(href, urlRecurso).toString(), { waitUntil: 'domcontentloaded' });
        actualizado = true;
        break;
      }
    }
    if (!actualizado) {
      // Plan B: el boton de texto.
      const boton = pagina.getByText(/post a new update|publicar una nueva actualizaci/i).first();
      if (await boton.count()) {
        await boton.click();
        await pagina.waitForLoadState('domcontentloaded');
        actualizado = true;
      }
    }
    if (!actualizado) {
      throw new Error('No se encontro el formulario de actualizacion. Revisa capturas/recurso.png');
    }

    const titulo = fs.readFileSync(path.join(DIST, 'TITULO.txt'), 'utf8').trim();
    const descripcion = fs.readFileSync(path.join(DIST, 'DESCRIPCION.bbcode'), 'utf8').trim();

    console.log('Rellenando titulo...');
    await pagina.fill(SEL.titulo, titulo);

    console.log('Rellenando descripcion...');
    const editorVisible = pagina.locator(SEL.editor).first();
    if (await editorVisible.count()) {
      await editorVisible.click();
      // Ctrl+A + pegar desde el portapapeles puede no funcionar en headless:
      // se escribe el BBCode, que Spigot interpreta igual.
      await editorVisible.type(descripcion, { delay: 0 });
    } else {
      await pagina.evaluate(([selector, texto]) => {
        const area = document.querySelector(selector);
        if (area) {
          area.value = texto;
          area.dispatchEvent(new Event('input', { bubbles: true }));
        }
      }, [SEL.mensajeHtml, descripcion]);
    }

    console.log('Adjuntando el jar...');
    await pagina.setInputFiles(SEL.archivo, path.join(DIST, 'JPRewards-' + v + '.jar'));

    await pagina.screenshot({ path: path.join(CAPTURAS, 'formulario.png'), fullPage: true });

    if (seco) {
      console.log('Modo de prueba: formulario relleno y capturado en capturas/formulario.png. NO se envia.');
      await esperar('Pulsa Enter para cerrar el navegador... ');
      return;
    }

    console.log('Enviando la actualizacion...');
    await Promise.all([
      pagina.waitForLoadState('networkidle'),
      pagina.locator(SEL.enviar).last().click(),
    ]);
    await pagina.screenshot({ path: path.join(CAPTURAS, 'enviado.png'), fullPage: true });
    console.log('Hecho. Comprueba el resultado en capturas/enviado.png.');
  } finally {
    await navegador.close();
  }
}

principal().catch((error) => {
  console.error('Error: ' + error.message);
  process.exit(1);
});
