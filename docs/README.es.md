# Documentacion de JPRewards

Guias, referencias y el registro de verificacion del plugin. La pagina principal es
[README.es.md](../README.es.md); esta carpeta entra en detalle.

| Pagina | Idioma | Que cubre |
| --- | --- | --- |
| [Comandos y permisos](COMMANDS.md) | Ingles | Todos los subcomandos, alias, argumentos, permisos y ejemplos. |
| [Placeholders](PLACEHOLDERS.md) | Ingles | Placeholders de menus, mensajes y recompensas, y la expansion de PlaceholderAPI. |
| [API para plugins](API.md) | Ingles | API de Java, eventos y premios con nombre para jefes, votos o misiones. |
| [Preguntas frecuentes](FAQ.md) | Ingles | Idiomas, MySQL, actualizar desde la 1.x, reclamar dos veces, inventario lleno. |
| [Guia de configuracion](CONFIGURACION.es.md) | Espanol | Guia paso a paso de `config.yml`, `menus.yml` y `recompensas/*.yml`. |
| [Registro de verificacion](VERIFICATION.md) | Ingles | Que se probo, en que servidores, y el hash del artefacto publicado. |
| [Checklist de QA](QA_CHECKLIST.md) | Ingles | La lista que se repasa antes de publicar una version. |
| [Pack de SpigotMC](SPIGOTMC_PAGE.md) | Ingles | La descripcion del recurso, el orden de las imagenes y las notas de actualizacion. |
| [Arte generado por codigo](assets/README.md) | Ingles | Como se genera cada imagen. |
| [Plantilla de pagina](TEMPLATE.md) | Ingles | Como escribir una pagina nueva en esta carpeta. |

## Para desarrolladores

| Pagina | Que cubre |
| --- | --- |
| [CONTRIBUTING.md](../CONTRIBUTING.md) | Compilar, comprobaciones, reglas y que es un buen pull request. |
| [SECURITY.md](../SECURITY.md) | Como reportar una vulnerabilidad y que hace el plugin para proteger los datos. |
| [testing/README.md](../testing/README.md) | El harness de integracion y los servidores de prueba. |
| [CHANGELOG.md](../CHANGELOG.md) | Lo que cambio en cada version. |

## Lo minimo para empezar

```bash
# Instalar: deja el .jar en plugins/ y arranca el servidor; luego en el juego:
/rewards validate      # revisa todos los archivos y dice que esta mal
/rewards               # abre el menu
/rewards help          # los comandos que permiten tus permisos
```
