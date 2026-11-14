# Instalación en Windows 10/11

**Recomendación:** Docker Desktop con motor Linux/WSL 2 habilitado, conexión a internet para descargar imágenes y dependencias, al menos 4 GB de RAM asignados a Docker.

1. Descarga y extrae el ZIP en una carpeta sin restricciones, por ejemplo `C:\proyectos\SGRV_MundoVolvo_FullStack`.
2. Abre PowerShell dentro de esa carpeta.
3. Crea `.env` a partir de la plantilla:

```powershell
Copy-Item .env.example .env
notepad .env
```

4. Rellena `DB_PASSWORD`, `BOOTSTRAP_ADMIN_USER` y `BOOTSTRAP_ADMIN_PASSWORD` (12 caracteres o más). No uses contraseñas de ejemplo.
5. Arranca Docker Desktop.
6. Ejecuta:

```powershell
docker compose up --build -d
docker compose ps
docker compose logs -f backend
```

7. Al terminar de arrancar, abre **http://localhost:4200**.
8. Entra con la cuenta ADMIN creada con `.env`; registra usuarios de Vendedor y Almacén desde la interfaz.

## Solución de problemas

- `unauthorized` o `401`: revisa usuario/contraseña inicial; si la base ya tenía cuentas, la cuenta inicial no se reescribe al reiniciar.
- `connection refused`: verifica que el contenedor `db` esté sano (`docker compose ps`) y que los puertos 4200/8080 estén libres.
- Error descargando dependencias: se requiere red durante el primer `docker compose up --build`.
- Errores de SQL: revisa `docker compose logs backend` para ver migraciones Flyway.
- No aparecen productos: es normal; el proyecto **no incorpora registros de negocio ficticios**. Registra marcas, categorías y productos reales, o usa importación CSV autorizada.
- La pantalla muestra precios en S/: el sistema presupone PEN. Confirma la moneda real de Mundo Volvo antes de ingresar listas de precios.

Para detener sin perder los datos: `docker compose down`. Nunca uses `-v` salvo que desees borrar los datos definitivamente.
