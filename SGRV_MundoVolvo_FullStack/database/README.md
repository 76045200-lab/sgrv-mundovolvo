# Base de datos del SGRV

**Motor:** PostgreSQL 17+.

- `01_esquema_postgresql.sql`: copia de referencia íntegra del DDL.
- La versión canónica es `backend/src/main/resources/db/migration/V1__schema.sql` y Spring Boot + Flyway la aplica automáticamente en el primer arranque.
- No ejecutes manualmente ambos archivos contra la misma base; generarías conflictos por tablas existentes.
- No se incluyen clientes, inventarios o ventas inventados. La primera cuenta administrador se crea al iniciar con tus credenciales definidas en `.env`.
- Los backups se pueden realizar con `docker compose exec -T db pg_dump -U sgrv sgrv > backup.sql`.
