# Pruebas manuales - core-accounting-service

## Obtener token

```powershell
$loginBody = @{ username = "admin.core"; password = "password" } | ConvertTo-Json
$loginResponse = Invoke-RestMethod -Method Post -Uri "http://localhost:8081/api/v1/auth/login" -ContentType "application/json" -Body $loginBody
$token = $loginResponse.accessToken
```

## Plan de cuentas

```powershell
Invoke-RestMethod -Method Get -Uri "http://localhost:8084/api/v1/accounting/chart-of-accounts" -Headers @{ Authorization = "Bearer $token" }
```

## Abrir fecha contable

```powershell
$body = @{ cutoffTime = "20:00:00"; observation = "Apertura laboratorio" } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri "http://localhost:8084/api/v1/accounting/accounting-dates/2026-06-03/open" -ContentType "application/json" -Headers @{ Authorization = "Bearer $token" } -Body $body
```

## Crear asiento cuadrado

```powershell
$body = @{
  correlationId = "11111111-1111-1111-1111-111111111111"
  transactionUuid = "22222222-2222-2222-2222-222222222222"
  originContext = "ACCOUNT"
  operationType = "DEMO_DEPOSITO"
  description = "Asiento demo deposito"
  accountingDate = "2026-06-03"
  externalReference = "DEMO-001"
  lines = @(
    @{ institutionalAccountCode = "BOVEDA_CENTRAL"; movementType = "DEBITO"; amount = 100.00; reference = "Caja"; lineOrder = 1 },
    @{ institutionalAccountCode = "CLIENTES_PASIVO"; movementType = "CREDITO"; amount = 100.00; reference = "Cliente"; lineOrder = 2 }
  )
} | ConvertTo-Json -Depth 5
Invoke-RestMethod -Method Post -Uri "http://localhost:8084/api/v1/accounting/journal-entries" -ContentType "application/json" -Headers @{ Authorization = "Bearer $token" } -Body $body
```

## Ejecutar EOD

```powershell
Invoke-RestMethod -Method Post -Uri "http://localhost:8084/api/v1/accounting/eod/run?accountingDate=2026-06-03" -Headers @{ Authorization = "Bearer $token" }
```
