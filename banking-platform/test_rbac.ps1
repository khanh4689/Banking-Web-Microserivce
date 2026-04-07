$ErrorActionPreference = "Stop"

Write-Output "1. Getting Regular User Token..."
$userLogin = Invoke-RestMethod -Uri "http://localhost:8082/auth/login" -Method Post -ContentType "application/json" -Body '{"username":"user", "password":"password"}'
$userToken = $userLogin.access_token

Write-Output "2. Getting Admin User Token..."
$adminLogin = Invoke-RestMethod -Uri "http://localhost:8082/auth/login" -Method Post -ContentType "application/json" -Body '{"username":"admin", "password":"password"}'
$adminToken = $adminLogin.access_token

Write-Output "3. Testing /api/users/health with NO token (Expect 401)"
try {
    $res = Invoke-RestMethod -Uri "http://localhost:8080/api/users/health" -Method Get
    Write-Output "Result: $($res)"
} catch {
    Write-Output "Result: $($_.Exception.Response.StatusCode.value__)"
}

Write-Output "4. Testing /api/users/health with INVALID token (Expect 401)"
try {
    $res = Invoke-RestMethod -Uri "http://localhost:8080/api/users/health" -Method Get -Headers @{Authorization="Bearer invalid"}
    Write-Output "Result: $($res)"
} catch {
    Write-Output "Result: $($_.Exception.Response.StatusCode.value__)"
}

Write-Output "5. Testing /api/users/health with REGULAR token (Expect 200)"
try {
    $res = Invoke-RestMethod -Uri "http://localhost:8080/api/users/health" -Method Get -Headers @{Authorization="Bearer $userToken"}
    Write-Output "Result: 200 OK ($res)"
} catch {
    Write-Output "Result: $($_.Exception.Response.StatusCode.value__)"
}

Write-Output "6. Testing /api/users/admin with REGULAR token (Expect 403)"
try {
    $res = Invoke-RestMethod -Uri "http://localhost:8080/api/users/admin" -Method Get -Headers @{Authorization="Bearer $userToken"}
    Write-Output "Result: $($res)"
} catch {
    Write-Output "Result: $($_.Exception.Response.StatusCode.value__)"
}

Write-Output "7. Testing /api/users/admin with ADMIN token (Expect 200)"
try {
    $res = Invoke-RestMethod -Uri "http://localhost:8080/api/users/admin" -Method Get -Headers @{Authorization="Bearer $adminToken"}
    Write-Output "Result: 200 OK ($res)"
} catch {
    Write-Output "Result: $($_.Exception.Response.StatusCode.value__)"
}

