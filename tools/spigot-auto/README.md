# Subir actualizaciones a SpigotMC automáticamente

SpigotMC **no tiene API** para publicar recursos: la única forma de automatizarlo del todo es manejar
el navegador como lo haría una persona. Eso es lo que hace este script, con Playwright y el
**Microsoft Edge que ya tienes instalado** (no descarga navegadores).

## Dos niveles: elige el que te encaje

### 1. Semiautomático y fiable (recomendado)

```powershell
powershell -ExecutionPolicy Bypass -File tools\publicar-spigot.ps1 -ResourceId 123456
```

Compila, valida, deja el jar y el sha256 en `dist/`, genera `TITULO.txt` y `DESCRIPCION.bbcode`
desde `docs/SPIGOTMC_UPDATE_<version>.md`, copia la descripción al portapapeles y abre el recurso.
Solo quedan dos clics: pegar y subir el archivo. **Es lo que no se rompe.**

### 2. Automático del todo (navegador)

```bash
cd tools/spigot-auto
npm install
node subir.js --iniciar-sesion      # una vez: abre el navegador, entras y guarda la sesión
node subir.js --dry-run             # rellena el formulario y NO envía (captura en capturas/)
node subir.js                       # sube el update
```

Configura antes:

```bash
copy .env.example .env
notepad .env        # SPIGOT_RESOURCE_ID=123456
```

- La sesión se guarda en `.sesion.json` (ignorado por git): las siguientes veces entra solo.
- Si rellenas `SPIGOT_USER`/`SPIGOT_PASS` inicia sesión sin intervención.
- `--oculto` lo ejecuta sin ventana (más frágil si Spigot pide captcha).
- Cada paso deja una captura en `capturas/` para que veas qué pasó.

## Qué hace, paso a paso

1. Abre spigotmc.org y, si no hay sesión, entra.
2. Va a la página del recurso.
3. Busca el enlace del formulario de actualización (`.../update`).
4. Rellena el título con `dist/TITULO.txt`.
5. Escribe la descripción de `dist/DESCRIPCION.bbcode` en el editor.
6. Adjunta `dist/JPRewards-<version>.jar`.
7. En `--dry-run` se para ahí; si no, pulsa enviar y captura el resultado.

## Advertencias honestas

- **Puede romperse** cuando SpigotMC cambie el HTML del formulario: los selectores están al principio
  de `subir.js` (`SEL`) para arreglarlos en un minuto.
- **Cloudflare** puede pedir un captcha en modo oculto: usa el modo con ventana.
- **Guarda tus credenciales con cuidado**: `.env` y `.sesion.json` no se suben al repositorio, pero
  están en tu equipo. Si usas usuario y contraseña, valora mejor iniciar sesión a mano una vez y
  reutilizar la sesión.
- SpigotMC puede considerar abusivo automatizar en exceso: úsalo para **tus propias actualizaciones**,
  una por versión, no en bucle.

## Alternativa 100 % oficial

Publicar el jar en **GitHub Releases** (ya está automatizado con el workflow: `git tag v2.1.0`) y
enlazarlo desde la página del recurso. SpigotMC permite marcar la descarga como externa; así el
código sí se puede publicar solo con CI.
