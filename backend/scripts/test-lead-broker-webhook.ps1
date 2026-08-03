# Teste do webhook Lead Broker (licenca demo)
# Execute com o backend rodando em localhost:8080

$body = @'
{
  "event": "lead.purchased",
  "broker_lead_id": "brk_manual_test_002",
  "license_id": "lic_demo",
  "purchased_at": "2026-07-28T14:32:00Z",
  "price_paid": 45.00,
  "currency": "USD",
  "source": {
    "campaign": "P2 | Broad | US & CAN | RE #1",
    "platform": "meta"
  },
  "lead": {
    "first_name": "Lucas",
    "last_name": "Burckle",
    "email": "lucas.burc@example.com",
    "phone": "+14075550012",
    "revenue_monthly": "$10k - $25k/mo",
    "consent": { "tcpa_opt_in": true, "collected_at": "2026-07-28T14:30:00Z" },
    "custom_fields": {}
  }
}
'@

$secret = "whsec_demo_license_secret_change_me"
$hmac = New-Object System.Security.Cryptography.HMACSHA256
$hmac.Key = [Text.Encoding]::UTF8.GetBytes($secret)
$hash = -join ($hmac.ComputeHash([Text.Encoding]::UTF8.GetBytes($body)) | ForEach-Object { $_.ToString("x2") })
$signature = "sha256=$hash"

Write-Host "Assinatura: $signature"
Write-Host "Enviando para http://localhost:8080/api/webhooks/lead-broker ..."

try {
    $response = Invoke-WebRequest `
        -Uri "http://localhost:8080/api/webhooks/lead-broker" `
        -Method POST `
        -ContentType "application/json; charset=utf-8" `
        -Headers @{ "X-Broker-Signature" = $signature } `
        -Body $body `
        -UseBasicParsing

    Write-Host "Status:" $response.StatusCode
    Write-Host "Resposta:" $response.Content
}
catch {
    Write-Host "ERRO:" $_.Exception.Message
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        Write-Host "Corpo do erro:" $reader.ReadToEnd()
    }
}
