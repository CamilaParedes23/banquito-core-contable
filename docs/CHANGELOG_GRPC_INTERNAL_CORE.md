# Cambio: gRPC interno Core V2

Este ajuste incorpora comunicación gRPC interna del Core, manteniendo REST/OpenAPI para Kong y consumidores externos.

## Decisiones

- No se elimina ningún endpoint REST existente.
- No se incorpora RabbitMQ en el Core en esta fase.
- `core-account-service` usa gRPC para validar datos maestros y registrar asientos contables.
- `core-customer-service`, `core-admin-service` y `core-accounting-service` exponen servidores gRPC internos.

## Comunicaciones cubiertas

- `core-account-service -> core-customer-service` para validar cliente activo y pagos masivos.
- `core-account-service -> core-admin-service` para validar sucursal, subtipos y obtener IVA.
- `core-account-service -> core-accounting-service` para obtener fecha contable y registrar asientos.

## Pruebas sugeridas

1. Compilar los cuatro microservicios.
2. Levantar todo con Docker Compose.
3. Probar por Kong depósito, retiro, P2P y consulta de movimientos.
4. Verificar que se generen asientos en `core-accounting-service`.
5. Apagar temporalmente `core-accounting-service` y confirmar que Account responde error controlado sin dejar saldos inconsistentes.
