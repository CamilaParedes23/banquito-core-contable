# Despliegue Docker / Cloud - core-accounting-service

## Objetivo

Dejar el microservicio preparado para ejecutarse igual en local, Docker Compose y nube mediante variables de entorno.

## Principios

- No hardcodear credenciales ni hosts.
- Usar `SPRING_PROFILES_ACTIVE=docker` en contenedores.
- Usar `ACCOUNTING_DB_URL`, `ACCOUNTING_DB_USER` y `ACCOUNTING_DB_PASSWORD`.
- Exponer `/actuator/health` para health checks.
- Mantener logs en consola para Docker/Kubernetes/cloud logging.

## Ejecución local

```powershell
mvn clean package
mvn spring-boot:run
```

## Build Docker

```powershell
docker build -t banquito/core-accounting-service:local .
```

## Ejecución Docker local

```powershell
docker run --rm `
  --name core-accounting-service `
  -p 8084:8084 `
  --env-file .env `
  banquito/core-accounting-service:local
```

## Variables cloud recomendadas

```env
SPRING_PROFILES_ACTIVE=docker
SERVER_PORT=8084
ACCOUNTING_DB_URL=jdbc:mysql://mysql-accounting:3306/banquito_core_accounting_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Guayaquil
ACCOUNTING_DB_USER=root
ACCOUNTING_DB_PASSWORD=<secret>
JWT_ISSUER=banquito-identity-access
JWT_SECRET=<secret compartido con identity>
```

## Kong

Kong no llama al servicio por `localhost` en nube. Debe enrutar hacia:

```text
http://core-accounting-service:8084
```

Rutas REST esperadas:

```text
/api/v1/accounting/**
```

## gRPC

La regla mandatoria del proyecto indica que comunicación interna Core-Core debe ser gRPC. Este servicio conserva contrato proto en `src/main/proto/accounting_service.proto`. La integración real con `core-account-service` se cerrará en la fase de gRPC.
