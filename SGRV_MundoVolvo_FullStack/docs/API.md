# API REST — SGRV v1

Prefijo: `/api`. Todas las solicitudes salvo login requieren `Authorization: Bearer <token>`.

## Autenticación y usuarios
| Método | Ruta | Roles | Función |
|---|---|---|---|
| POST | `/auth/login` | Público | `{"username":"...","password":"..."}` |
| GET | `/auth/me` | Todos | Datos de sesión |
| POST | `/auth/logout` | Todos | Revoca token |
| GET | `/users` | ADMIN | Listar cuentas |
| POST | `/users` | ADMIN | Crear cuenta y asignar rol |
| PUT | `/users/{id}` | ADMIN | Actualizar rol, estado o contraseña; no se elimina historial |

## Maestros
| Método | Ruta | Roles | Función |
|---|---|---|---|
| GET | `/brands`, `/categories` | Todos | Catálogos |
| POST,PUT,DELETE | `/brands`, `/categories` | ADMIN/WAREHOUSE (DELETE ADMIN) | CRUD de catálogos |
| GET | `/products?q=&brandId=&categoryId=` | Todos | Búsqueda y filtros |
| GET | `/products/{id}` | Todos | Detalle |
| GET | `/products/duplicates?code=` | ADMIN/WAREHOUSE | Coincidencias comerciales |
| POST,PUT,DELETE | `/products[/{id}]` | ADMIN/WAREHOUSE | Crear, actualizar y eliminar sin transacciones asociadas |
| PUT | `/products/{id}/prices` | ADMIN | Actualizar precios y costos |
| GET,POST,PUT,DELETE | `/customers[/{id}]` | ADMIN/SELLER | CRUD clientes |
| GET,POST,PUT,DELETE | `/suppliers[/{id}]` | ADMIN; lectura WAREHOUSE | CRUD proveedores |

## Inventario / operaciones
| Método | Ruta | Roles | Función |
|---|---|---|---|
| GET | `/stock/movements?productId=` | ADMIN/WAREHOUSE | Histórico trazable |
| POST | `/stock/adjust` | ADMIN/WAREHOUSE | Ajuste con justificación obligatoria |
| GET | `/purchases`, `/purchases/{id}` | ADMIN/WAREHOUSE | Compras |
| POST | `/purchases` | ADMIN | Registrar compra sin tocar stock |
| PUT | `/purchases/{id}` | ADMIN | Editar compra sin recepciones |
| DELETE | `/purchases/{id}` | ADMIN | Eliminar compra sin recepciones |
| POST | `/purchases/{id}/receive` | ADMIN/WAREHOUSE | Registrar recepción parcial/completa y movimientos |
| GET,POST | `/quotes` | ADMIN/SELLER | Listar y crear cotizaciones |
| GET,PUT,DELETE | `/quotes/{id}` | ADMIN/SELLER | Consultar / modificar / eliminar únicamente borrador |
| PATCH | `/quotes/{id}/status` | ADMIN/SELLER | DRAFT→SENT→APPROVED/REJECTED |
| POST | `/quotes/{id}/convert` | ADMIN/SELLER | Convertir APPROVED en venta, una sola vez |
| GET,POST | `/sales` | ADMIN/SELLER | Listar y registrar ventas |
| GET | `/sales/{id}` | ADMIN/SELLER | Detalle |
| POST | `/sales/{id}/cancel` | ADMIN | Anulación compensatoria; restaura existencias |

## Reportes / configuración
| Método | Ruta | Roles | Función |
|---|---|---|---|
| GET | `/dashboard` | Todos | Resumen; almacén sin detalles comerciales |
| GET | `/reports/sales` | ADMIN/SELLER | Ventas mensuales |
| GET | `/reports/purchases`, `/reports/abc`, `/reports/rotation?days=90` | ADMIN | Compras / ABC |
| GET | `/reports/inventory` | ADMIN/WAREHOUSE | Inventario |
| GET | `/settings`, `/audit` | ADMIN | Parámetros de lectura y auditoría |

## Ejemplo: venta
```json
{"customerId":1,"discountPct":0,"items":[{"productId":3,"quantity":2}]}
```
Las líneas toman el precio de la BD al registrar. No se acepta un precio arbitrario enviado por el navegador.

## Ejemplo: recepción parcial
```json
{"items":[{"purchaseItemId":17,"quantity":3}]}
```

## Ejemplo: ajuste manual
```json
{"productId":3,"direction":"IN","quantity":10,"note":"Conteo físico de apertura autorizado"}
```

Los importes se calculan en el backend. Los códigos comerciales pueden repetirse entre marcas: la clave interna de la BD es `products.id`.
