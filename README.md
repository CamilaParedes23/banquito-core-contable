# core-accounting-service

Microservicio de contabilidad del Core Bancario BanQuito V2. Es dueño del plan de cuentas, cuentas institucionales, jornada/fecha contable, asientos contables, reversos, EOD y balance de comprobación.

## Responsabilidad

Este servicio cubre el bounded context contable del Core:

- Plan de cuentas jerárquico.
- Cuentas institucionales funcionales.
- Jornada y fecha contable.
- Asientos contables bajo partida doble.
- Validación de suma cero.
- Reversos contables.
- Proceso EOD / cierre diario.
- Balance de comprobación.
- Auditoría local y outbox contable.

No administra clientes, cuentas bancarias de clientes, saldos operativos, login, roles, notificaciones ni documentos.

## Endpoints principales

```text
GET  /api/v1/accounting/chart-of-accounts
GET  /api/v1/accounting/chart-of-accounts/{code}
GET  /api/v1/accounting/institutional-accounts/{functionalCode}
GET  /api/v1/accounting/accounting-dates/current
POST /api/v1/accounting/accounting-dates/{date}/open
POST /api/v1/accounting/accounting-dates/{date}/close
POST /api/v1/accounting/journal-entries
GET  /api/v1/accounting/journal-entries/{journalEntryUuid}
POST /api/v1/accounting/journal-entries/{journalEntryUuid}/reverse
POST /api/v1/accounting/eod/run?accountingDate=YYYY-MM-DD
GET  /api/v1/accounting/eod/{eodUuid}
GET  /api/v1/accounting/eod/{eodUuid}/status
GET  /api/v1/accounting/eod/by-date/{accountingDate}
GET  /api/v1/accounting/trial-balances/{accountingDate}
```

## OpenAPI

```text
http://localhost:8084/swagger-ui.html
http://localhost:8084/api-docs
```

## Ejecución local

```powershell
mvn clean package
mvn spring-boot:run
```

## Docker

```powershell
docker build -t banquito/core-accounting-service:local .
docker run --rm -p 8084:8084 --env-file .env banquito/core-accounting-service:local
```

## Variables principales

Revisar `.env.example`. Para nube no usar `localhost`; usar nombres internos de Docker, por ejemplo `mysql-accounting:3306`.

## Integraciones previstas

- `core-account-service -> core-accounting-service`: gRPC interno para creación de asientos, reversos y consulta de fecha contable.
- `core-accounting-service -> document-service`: evidencia de EOD y balance de comprobación, por gRPC o evento.
- `core-accounting-service -> notification-service`: notificaciones EOD, preferiblemente asíncronas.
- Kong expone REST/OpenAPI hacia consumidores externos o entre sistemas Core/Switch cuando corresponda.

## Pendientes posteriores

- Implementar/validar gRPC real con `core-account-service`.
- Publicar eventos RabbitMQ para EOD, balance y asientos si se requiere trazabilidad asíncrona.
- Integrar generación documental de balance/EOD con `document-service`.
- Integrar notificaciones EOD con `notification-service`.
- Seeds robustos de jornadas, asientos demo y balances para pruebas E2E.
