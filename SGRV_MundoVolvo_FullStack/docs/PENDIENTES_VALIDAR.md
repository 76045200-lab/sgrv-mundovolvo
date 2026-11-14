# Políticas de Mundo Volvo que requieren confirmación

Este repositorio implementa **reglas iniciales explícitas**, no políticas formalmente aprobadas por Mundo Volvo. Antes de operarlo con transacciones oficiales, validar:

1. **Moneda base:** interfaz en PEN. El Excel de referencia tiene precios US$; no existe conversión automática ni tipo de cambio.
2. **Inventario:** stock negativo prohibido; validar excepciones, conteos físicos, ajustes, devoluciones, ubicaciones y almacenes.
3. **Precios:** solo ADMIN modifica precios y costos; definir listas, descuentos máximos, márgenes, impuestos, promociones y autorizaciones.
4. **Cotizaciones:** no reservan existencias; flujo DRAFT→SENT→APPROVED/REJECTED; vigencia y aprobaciones reales por confirmar.
5. **Compras:** admiten recepciones parciales, con cantidades pendientes; no se permiten sobrantes no autorizados.
6. **Usuarios:** funciones por ADMIN/SELLER/WAREHOUSE propuestas; revisar puestos y segregación real de funciones.
7. **SUNAT:** sin facturación electrónica ni emisión de comprobantes fiscales. Integración requiere definición tributaria y credenciales oficiales.
8. **Históricos:** Excel etiquetado 2025, encabezado Inka Automotriz Diesel y precios US$. Requiere autorización de Mundo Volvo antes de cualquier migración.
9. **Operación:** backups, HTTPS, concurrencia esperada, monitoreo, auditoría, privacidad y recuperación necesitan configuración de infraestructura.
10. **ABC:** umbrales 80/95 % y valor de ventas confirmadas propuestos; rotación 90 días usa media entre saldos de inicio/fin inferidos desde movimientos, no replica necesariamente el Excel 2025.

**Estado:** pendiente de validación empresarial. No presentar estos supuestos como políticas vigentes.
