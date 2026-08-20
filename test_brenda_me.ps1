$ErrorActionPreference = "Stop"
$BaseUrl = "http://localhost:8083/employee-portal/api/v1/auth"

Write-Host "1. Login como brenda.mm2026..."
$LoginBrendaBody = @{
    username = "brenda.mm2026"
    password = "BrendaNewPass123!"
} | ConvertTo-Json

$BrendaLogin = Invoke-RestMethod -Uri "$BaseUrl/login" -Method Post -Body $LoginBrendaBody -ContentType "application/json"
$BrendaToken = $BrendaLogin.accessToken
Write-Host "Login exitoso. Token obtenido."
Write-Host "--------------------------------------------------"

Write-Host "2. Ejecutando /auth/me..."
$Headers = @{
    Authorization = "Bearer $BrendaToken"
}

$MeResponse = Invoke-RestMethod -Uri "$BaseUrl/me" -Method Get -Headers $Headers
Write-Host "Respuesta de /auth/me:"
$MeResponse | ConvertTo-Json -Depth 5
Write-Host "--------------------------------------------------"
