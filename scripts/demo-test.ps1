# IoT 设备数据上报平台 - 一键演示
$ErrorActionPreference = 'Stop'
$base = 'http://127.0.0.1:8090'

Write-Host '== 1) 注册演示设备 ==' -ForegroundColor Cyan
$reg = Invoke-RestMethod -Uri ($base + '/api/device/register') -Method Post -ContentType 'application/json' -Body (@{ deviceNo = 'DEMO-DEV-01'; deviceName = '演示传感器' } | ConvertTo-Json)
$dev = $reg.data.deviceNo; $key = $reg.data.apiKey
Write-Host ("    设备: {0} / {1}" -f $dev, $key)
$headers = @{ 'X-Device-No' = $dev; 'X-Api-Key' = $key }

Write-Host '== 2) 批量上报 30 条 ==' -ForegroundColor Cyan
$now = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
$reports = @()
for ($i = 1; $i -le 30; $i++) {
  $reports += @{ msgId = ('demo-' + $i.ToString('D4')); reportTime = ($now - ($i * 5000)); metrics = @{ temperature = [math]::Round(20 + ($i % 10) * 0.5, 1); humidity = [math]::Round(50 + ($i % 20), 1) } }
}
$body = @{ reports = $reports } | ConvertTo-Json -Depth 6
$r1 = Invoke-RestMethod -Uri ($base + '/api/device/report') -Method Post -Headers $headers -ContentType 'application/json' -Body $body
Write-Host ("    accepted={0} duplicated={1}" -f $r1.data.accepted, $r1.data.duplicated)

Start-Sleep -Seconds 3

Write-Host '== 3) 重复上报同一批(验证幂等) ==' -ForegroundColor Cyan
$r2 = Invoke-RestMethod -Uri ($base + '/api/device/report') -Method Post -Headers $headers -ContentType 'application/json' -Body $body
Write-Host ("    accepted={0} duplicated={1} (应 accepted=0)" -f $r2.data.accepted, $r2.data.duplicated)

Write-Host '== 4) 最新状态 ==' -ForegroundColor Cyan
$r3 = Invoke-RestMethod -Uri ($base + ('/api/device/{0}/latest' -f $dev))
Write-Host ("    reportTime={0} temp={1} source={2}" -f $r3.data.reportTime, $r3.data.metrics.temperature, $r3.data.source)

Write-Host '== 5) 历史分页 ==' -ForegroundColor Cyan
$r4 = Invoke-RestMethod -Uri ($base + ('/api/device/{0}/reports?page=1&size=3' -f $dev))
Write-Host ("    total={0}" -f $r4.data.total)

Write-Host '== 6) 按小时统计 ==' -ForegroundColor Cyan
$r5 = Invoke-RestMethod -Uri ($base + ('/api/device/{0}/stats?granularity=hour' -f $dev))
$r5.data | ForEach-Object { Write-Host ("    {0} => {1} 条" -f $_.bucket, $_.count) }
Write-Host ''
Write-Host '演示完成 ✅' -ForegroundColor Green