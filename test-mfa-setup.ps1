$body = @{
    username = 'admin'
    password = 'Admin123!'
}
$json = $body | ConvertTo-Json
$response = Invoke-RestMethod -Uri 'http://localhost:8080/api/v1/auth/login' -Method Post -Body $json -ContentType 'application/json'

$token = $response.accessToken
Write-Host "Token: $token"

$headers = @{
    Authorization = "Bearer $token"
}
$setupResponse = Invoke-RestMethod -Uri 'http://localhost:8080/api/v1/auth/mfa/setup' -Method Get -Headers $headers
$setupResponse | ConvertTo-Json
