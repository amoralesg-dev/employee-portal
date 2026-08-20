$ErrorActionPreference = "Stop"
$BaseUrl = "http://localhost:8083/employee-portal/api/v1/auth"

Write-Host "1. Estado de brenda.mm2026 antes del reset:"
docker exec rasinniDB mysql -u iam_admin -pIamAdmin#2026! iam -e "SELECT id, username, force_password_change FROM users WHERE username='brenda.mm2026';"
Write-Host "--------------------------------------------------"

Write-Host "2. Login como admin..."
$LoginAdminBody = @{
    username = "admin"
    password = "Admin123!"
} | ConvertTo-Json

$AdminLogin = Invoke-RestMethod -Uri "$BaseUrl/login" -Method Post -Body $LoginAdminBody -ContentType "application/json"
$AdminToken = $AdminLogin.accessToken
Write-Host "Admin token obtenido."
Write-Host "--------------------------------------------------"

Write-Host "3. Resetear contrasena de brenda.mm2026 (TempPass123!)..."
$ResetBody = @{
    username = "brenda.mm2026"
    newPassword = "TempPass123!"
} | ConvertTo-Json

$Headers = @{
    Authorization = "Bearer $AdminToken"
}

Invoke-RestMethod -Uri "$BaseUrl/reset-password" -Method Post -Headers $Headers -Body $ResetBody -ContentType "application/json"
Write-Host "Reseteo exitoso (204 No Content)."
Write-Host "--------------------------------------------------"

Write-Host "4. Estado de brenda.mm2026 despues del reset:"
docker exec rasinniDB mysql -u iam_admin -pIamAdmin#2026! iam -e "SELECT id, username, force_password_change FROM users WHERE username='brenda.mm2026';"
Write-Host "--------------------------------------------------"

Write-Host "5. Login como brenda.mm2026 con contrasena temporal..."
$LoginBrendaBody = @{
    username = "brenda.mm2026"
    password = "TempPass123!"
} | ConvertTo-Json

$BrendaLogin = Invoke-RestMethod -Uri "$BaseUrl/login" -Method Post -Body $LoginBrendaBody -ContentType "application/json"
Write-Host "Respuesta de Login de Brenda:"
$BrendaLogin | ConvertTo-Json -Depth 5
$BrendaToken = $BrendaLogin.accessToken
Write-Host "--------------------------------------------------"

Write-Host "6. Cambiar contrasena obligatoria (BrendaNewPass123!)..."
$ChangePassBody = @{
    currentPassword = "TempPass123!"
    newPassword = "BrendaNewPass123!"
    confirmPassword = "BrendaNewPass123!"
} | ConvertTo-Json

$BrendaHeaders = @{
    Authorization = "Bearer $BrendaToken"
}

Invoke-RestMethod -Uri "$BaseUrl/change-password" -Method Post -Headers $BrendaHeaders -Body $ChangePassBody -ContentType "application/json"
Write-Host "Cambio de contrasena exitoso (204 No Content)."
Write-Host "--------------------------------------------------"

Write-Host "7. Estado de brenda.mm2026 despues del cambio:"
docker exec rasinniDB mysql -u iam_admin -pIamAdmin#2026! iam -e "SELECT id, username, force_password_change FROM users WHERE username='brenda.mm2026';"
Write-Host "--------------------------------------------------"

Write-Host "8. Segundo Login como brenda con su nueva contrasena..."
$LoginBrendaBody2 = @{
    username = "brenda.mm2026"
    password = "BrendaNewPass123!"
} | ConvertTo-Json

$BrendaLogin2 = Invoke-RestMethod -Uri "$BaseUrl/login" -Method Post -Body $LoginBrendaBody2 -ContentType "application/json"
Write-Host "Respuesta de Login 2 de Brenda:"
$BrendaLogin2 | ConvertTo-Json -Depth 5
Write-Host "--------------------------------------------------"

Write-Host "PRUEBA FINALIZADA."
