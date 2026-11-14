# SGRV — Mundo Volvo

**Sistema de Gestión Integral de Repuestos Volvo** — entrega de código fuente **Angular + Java (Spring Boot) + PostgreSQL**.

> El nombre comercial de la empresa es Mundo Volvo. La aplicación permite administrar repuestos de **todas las marcas comercializadas**, no solamente Volvo.

## 1. Contenido del proyecto

```text
SGRV_MundoVolvo_FullStack/
├── backend/
│   ├── pom.xml                        Java 21 + Spring Boot + JDBC + Security
│   ├── Dockerfile
│   └── src/
│       ├── main/java/com/mundovolvo/sgrv/
│       │   ├── AuthController.java     Autenticación y CRUD de usuarios
│       │   ├── AuthService.java        Sesiones y BCrypt
│       │   ├── SecurityConfig.java     Protección de API por roles
│       │   ├── CatalogController.java  CRUD productos, marcas, categorías, clientes y proveedores
│       │   ├── StockController.java    Ajustes y movimientos
│       │   ├── BusinessRules.java      Precios, descuentos, IGV y stock
│       │   ├── QuoteController.java    CRUD y estados de cotizaciones
│       │   ├── PurchaseController.java Compras y recepciones
│       │   ├── SaleService.java        Venta/convertir cotización/anulación transaccional
│       │   ├── SalesController.java
│       │   ├── ReportsController.java  Dashboard, reportes y ABC
│       │   ├── Db.java                 Consultas parametrizadas a PostgreSQL
│       │   └── ApiErrors.java          Manejo de errores
│       ├── main/resources/db/migration/V1__schema.sql
│       └── test/java/.../BusinessRulesTest.java
├── frontend/
│   ├── src/app/services/             Servicios Angular conectados a REST (sin mocks)
│   ├── src/app/pages/                Dashboard, inventario, cotizaciones, ventas,
│   │                                 compras, contactos, usuarios, reportes, configuración
│   └── src/styles.css                Tailwind CSS, diseño responsive
├── docs/                             Modelo de datos, API, plan de pruebas y plantilla CSV
├── scripts/import_products_csv.py    Carga de datos reales autorizados
├── tests/smoke.py                    Prueba integral contra BD aislada
├── docker-compose.yml
└── .env.example
```

## 2. ¿Cómo instalar y ejecutar? (ruta recomendada)

**Requisitos:** Docker Engine + Docker Compose (o Docker Desktop), puerto 4200 libre, conexión a internet inicial para descargar imágenes y dependencias.

```bash
# Desde la carpeta extraída:
cp .env.example .env
```

Edita `.env` y define **tu propia contraseña segura** de la base de datos y del administrador inicial:

```dotenv
DB_PASSWORD=ESCOGE_UNA_CLAVE_LARGA_Y_UNICA
BOOTSTRAP_ADMIN_USER=admin
BOOTSTRAP_ADMIN_PASSWORD=ESCOGE_OTRA_CLAVE_SEGURA_DE_12_O_MAS_CARACTERES
```

No uses esas cadenas de ejemplo como claves reales.

```bash
docker compose up --build -d
docker compose ps
docker compose logs -f backend
```

Accede a **http://localhost:4200**. Ingresa con el usuario y contraseña administrativos definidos en `.env`. El backend arranca en `http://localhost:8080`, PostgreSQL queda accesible **solo localmente** en el puerto 5432.

En el **primer inicio**, el sistema crea **solo una cuenta de administrador**, usando las credenciales suministradas por ti. No agrega clientes, proveedores, productos, cotizaciones ni ventas inventadas.

### Orden sugerido de uso
1. Administrador ingresa; en *Usuarios y permisos* crea cuentas de Vendedor y Almacén con contraseñas propias.
2. En *Inventario* crea marcas y categorías; después registra productos con precios y stock mínimo.
3. Registra **clientes** y **proveedores** verdaderos.
4. Registra **compras** y luego **recepciones** para incrementar existencias; o usa un **ajuste manual justificado** para el conteo físico inicial.
5. Crea **cotizaciones**, envíalas, apruébalas y conviértelas en venta. También puedes registrar ventas directas.
6. Revisa reportes, stock bajo, movimientos y auditoría.

### Apagar y conservar datos

```bash
docker compose down
```

**No utilices `docker compose down -v`** salvo que quieras destruir explícitamente el volumen persistente de PostgreSQL.

## 3. Desarrollo local sin Docker

**Requisitos:** JDK 21, Maven 3.9+, Node.js 22.12+, npm, PostgreSQL 17+. Angular 21 requiere Node.js y TypeScript compatibles.

Crea una base de datos `sgrv` y un usuario PostgreSQL con permisos para el esquema (Flyway crea todas las tablas automáticamente al iniciar).

**Terminal 1 — API**

```bash
cd backend
export DB_URL=jdbc:postgresql://localhost:5432/sgrv
export DB_USER=sgrv
export DB_PASSWORD='TU_CLAVE_POSTGRES'
export BOOTSTRAP_ADMIN_USER=admin
export BOOTSTRAP_ADMIN_PASSWORD='CLAVE_ADMIN_DE_12_O_MAS_CARACTERES'
mvn spring-boot:run
```

**Terminal 2 — Angular**

```bash
cd frontend
npm install
npm start
```

Visita `http://localhost:4200`; el proxy Angular enviará `/api` al backend local.

## 4. Módulos y permisos

| Función | Administrador | Vendedor | Almacén |
|---|---|---|---|
| Dashboard | Completo | Completo | Solo inventario (sin cotizaciones) |
| Productos / inventario | CRUD, precios y ajustes | Lectura | CRUD y ajustes, sin alterar precios |
| Marcas / categorías | CRUD | Lectura | Crear y modificar |
| Clientes | CRUD | CRUD | Sin acceso |
| Proveedores | CRUD | Sin acceso | Lectura para recepciones |
| Cotizaciones | CRUD y conversión | CRUD y conversión | Sin acceso |
| Ventas | Crear, leer, anular | Crear y leer | Sin acceso |
| Compras | CRUD mientras estén pendientes, recibir | Sin acceso | Leer y recepcionar |
| Usuarios y parámetros | Administrar | Sin acceso | Sin acceso |
| Reportes | Todos | Ventas | Inventario mediante movimientos |

**Reglas:** el backend impide las peticiones prohibidas (HTTP 403), aunque alguien intente saltarse el menú. La contraseña se cifra con BCrypt. Los tokens de sesión expiran tras 12 horas y pueden revocarse.

## 5. Persistencia y funcionamiento

- **No hay datos mock** en el código de producción. Todas las pantallas consultan la API, y la API opera sobre PostgreSQL.
- El esquema SQL está en `backend/src/main/resources/db/migration/V1__schema.sql`. Flyway lo aplica automáticamente.
- Las reglas de precio, descuento e IGV (18 %), bloqueo de stock negativo y transacciones están en `BusinessRules.java` y `SaleService.java`.
- Las transacciones de venta y recepción tienen integridad: si hay error, la operación completa se revierte.
- No se eliminan ventas confirmadas: se **anulan**, dejando registro compensatorio y auditoría. Las cotizaciones se eliminan solo en borrador.
- La impresión genera **cotizaciones** o **resúmenes internos**, no facturas/boletas electrónicas SUNAT.
- Reportes de ABC y rotación se calculan desde ventas y movimientos reales del sistema. Sin ventas, aparecerá SIN_DATOS.
- La moneda comercial predeterminada en la interfaz es **PEN**, pendiente de confirmación con Mundo Volvo.

## 6. Sobre el Excel histórico 2025

Se revisó el documento aportado. Su encabezado dice **«INKA AUTOMOTRIZ DIESEL»**, las columnas de precios/costos indican **US$**, y los movimientos corresponden al año **2025**. Este origen **no prueba** que esos precios, existencias o productos estén vigentes para Mundo Volvo.

Por ello **no se importó automáticamente** el Excel a PostgreSQL. Para cargar **datos verificados y autorizados** existe `docs/PLANTILLA_CARGA_PRODUCTOS.csv` y el importador `scripts/import_products_csv.py`:

```bash
export SGRV_IMPORT_AUTHORIZED=YES
export SGRV_ADMIN_USER=admin
export SGRV_ADMIN_PASSWORD='TU_CLAVE_ADMIN'
python scripts/import_products_csv.py datos_actuales_confirmados.csv
```

Los valores `sale_price_pen` y `purchase_cost_pen` deben estar **en soles**, y `initial_stock` debe proceder de un conteo físico actual. El script **no es atómico a nivel de archivo**; revisa las filas fallidas y no reintentes a ciegas. Los códigos comerciales repetidos se detectan para revisión, no se fusionan automáticamente.

## 7. Pruebas y validación

```bash
# Ejecutar pruebas unitarias Java (necesita Maven):
cd backend && mvn test

# Compilar Angular (necesita npm install):
cd frontend && npm run build
```

Prueba integral con base **aislada de QA**, no de producción:

```bash
export SGRV_TEST_ALLOW_WRITE=YES
export SGRV_ADMIN_USER=admin
export SGRV_ADMIN_PASSWORD='CLAVE_DEL_ADMIN_EN_QA'
python tests/smoke.py
```

Consulta `docs/PRUEBAS.md` para casos manuales de permisos, CRUD y operaciones. El script crea **registros de prueba solo en la base de QA**; no se ejecuta en producción.

## 8. Límites conocidos antes de un despliegue empresarial

Se entrega código fuente para un **núcleo funcional ejecutable**. Antes de afirmar que está listo para producción es necesario **compilar y ejecutar la batería integral en un entorno real**, confirmar políticas comerciales de Mundo Volvo, activar HTTPS, revisar gestión de secretos/rotación de credenciales, establecer copias de seguridad y alertas, y completar integraciones fiscales si se solicitan.

**No incluidos como funcionalidades operativas:** facturación electrónica SUNAT, conexión a bancos, múltiples almacenes/sucursales, POS/hardware fiscal, envío de correo, integración contable y migración automática del Excel histórico. Tampoco hay pagos integrados ni factura electrónica simulada.

## 9. API y documentación complementaria

- `docs/API.md` — contratos de endpoints y ejemplos de payloads.
- `docs/MODELO_DATOS.md` — tablas y relaciones.
- `docs/PRUEBAS.md` — protocolo de aceptación y QA.
- `backend/src/main/resources/db/migration/V1__schema.sql` — esquema fuente íntegro.

Para publicar en internet, poner el proyecto detrás de un proxy HTTPS, no exponer PostgreSQL, y gestionar tokens y cabeceras con política de seguridad revisada.
