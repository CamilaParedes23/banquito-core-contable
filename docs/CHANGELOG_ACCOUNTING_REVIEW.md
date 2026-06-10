# Changelog de revisión - core-accounting-service

## Versión cloud-ready

Cambios aplicados:

- Parametrización completa de `application.yml` y `application-docker.yml`.
- Dockerfile multi-stage con Java 21, usuario no root y healthcheck.
- `.env.example` actualizado para local/nube.
- `.gitignore` y `.dockerignore` agregados.
- SQL base incluido en `docs/database/04_core_accounting_db.sql`.
- README actualizado con alcance, endpoints, integración y pendientes.
- Documentación de despliegue Docker/cloud agregada.
- Se conserva manejo profesional de errores: `BusinessException`, `@RestControllerAdvice`, `AuthenticationEntryPoint` y `AccessDeniedHandler`.
- Se conserva JWT compatible con `identity-access-service`.

## Cobertura funcional revisada

El servicio cubre:

- Plan de cuentas.
- Cuentas institucionales.
- Jornada/fecha contable.
- Asientos contables.
- Validación de suma cero.
- Reversos contables.
- EOD.
- Balance de comprobación.
- Auditoría local y outbox.

## Pendientes posteriores

- gRPC real con `core-account-service`.
- Evidencias EOD/balance con `document-service`.
- Notificaciones EOD con `notification-service`.
- Eventos RabbitMQ para procesos no bloqueantes.
- Seeds robustos para pruebas E2E.
