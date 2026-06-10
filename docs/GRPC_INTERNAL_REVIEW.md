# Revisión gRPC interna - Banco BanQuito Core V2

Esta versión corrige la integración gRPC interna del Core y mantiene REST/OpenAPI para Kong.

## Decisiones aplicadas

- Core no usa RabbitMQ en esta fase.
- REST/OpenAPI se conserva para Kong, frontend, Postman/Newman e integración Core/Switch.
- gRPC se usa únicamente para comunicación interna Core → Core.
- Se evita `com.fasterxml.jackson.databind.ObjectMapper` de Jackson 2 en los componentes gRPC.
- Se usa `tools.jackson.databind.ObjectMapper`, consistente con Spring Boot 4.

## Integraciones esperadas

- `core-account-service` consume `core-customer-service` por gRPC.
- `core-account-service` consume `core-admin-service` por gRPC.
- `core-account-service` consume `core-accounting-service` por gRPC.

## Validación sugerida

1. Construir todos los servicios con Docker Compose.
2. Validar que Account y Accounting arranquen sin errores de `ObjectMapper`.
3. Probar saldo y movimientos por Kong.
4. Probar depósito por ventanilla y verificar asiento contable.
5. Apagar Accounting y validar falla controlada.
