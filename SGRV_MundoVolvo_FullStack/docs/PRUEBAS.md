# Plan de validación — SGRV

## Pruebas automatizadas
1. `mvn test` en `backend/`: pruebas unitarias del cálculo monetario.
2. Prueba integral de API: iniciar una instancia y BD separadas de las de producción, exportar `SGRV_TEST_ALLOW_WRITE=YES`, `SGRV_ADMIN_USER`, `SGRV_ADMIN_PASSWORD`, ejecutar `python tests/smoke.py` desde la raíz. La prueba crea datos **solo de QA** y no los borra.
3. `npm run build` en `frontend/`: valida compilación Angular con plantillas.

## Casos de aceptación manual a recorrer
| Caso | Procedimiento | Esperado |
|---|---|---|
| AC-01 | Login inválido | Respuesta 401, sin sesión |
| AC-02 | Cuenta SELLER intenta POST `/products` | 403, sin registro |
| AC-03 | Crear producto con precio negativo | 400, sin registro |
| AC-04 | Registrar compra sin recepción | Stock no cambia |
| AC-05 | Recepcionar parcialmente | Incremento por cantidad recibida, saldo pendiente |
| AC-06 | Registrar venta con stock suficiente | Venta + movimiento OUT en una transacción |
| AC-07 | Registrar venta con stock insuficiente | 409, sin venta ni salida |
| AC-08 | Anular venta | Estado CANCELLED y entrada compensatoria |
| AC-09 | Crear cotización con descuento | Base imponible e IGV del 18 % calculados servidor |
| AC-10 | Aprobar y convertir cotización | Genera solo una venta y resta stock |
| AC-11 | Repetir conversión | 409, sin descuento adicional |
| AC-12 | Eliminar cliente asociado a venta | 409 por restricción relacional |
| AC-13 | Actualizar precio como SELLER | 403 |
| AC-14 | Consultar dashboard como WAREHOUSE | No contiene detalles de cotizaciones |
| AC-15 | Imprimir cotización | Vista imprimible; no pretende ser comprobante fiscal |
| AC-16 | Navegar desde móvil | Menú colapsable y tablas desplazables |

## Estado real de pruebas al entregar
Los escenarios están **implementados como código de prueba y/o definidos manualmente**, pero ejecutar el entorno con Maven, npm y PostgreSQL requiere instalar dependencias e iniciar los contenedores. No se debe declarar que se aprobaron pruebas end-to-end hasta ejecutarlas en infraestructura disponible.
