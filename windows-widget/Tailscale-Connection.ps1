$ErrorActionPreference = 'Stop'
try {
    $taskIdentity = [Security.Principal.WindowsIdentity]::GetCurrent()
    $taskPrincipal = New-Object Security.Principal.WindowsPrincipal($taskIdentity)
    if (!$taskPrincipal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
        Start-Process powershell.exe -Verb RunAs -ArgumentList @('-NoProfile','-ExecutionPolicy','Bypass','-File',('"{0}"' -f $PSCommandPath))
        exit
    }
    Write-Host 'GPT HUD - Tailscale 연결 설정' -ForegroundColor Cyan
    Write-Host 'PC와 폰에서 Tailscale을 켜고 같은 계정으로 연결하세요.'
    Write-Host '폰의 Tailscale 앱에서 이 휴대폰의 100.x IPv4 주소를 복사하세요.'
    $taskPhoneIp = (Read-Host '휴대폰 Tailscale 주소').Trim()
    $taskParsedIp = $null
    if ($taskPhoneIp -notmatch '^100\.(?:6[4-9]|[7-9][0-9]|1[01][0-9]|12[0-7])\.[0-9]{1,3}\.[0-9]{1,3}$' -or
        ![Net.IPAddress]::TryParse($taskPhoneIp,[ref]$taskParsedIp) -or $taskParsedIp.ToString() -ne $taskPhoneIp) {
        throw '휴대폰의 Tailscale IPv4 주소를 확인하세요. 공개 IP는 허용하지 않습니다.'
    }
    $taskLocal = @(Get-NetIPAddress -AddressFamily IPv4 | Where-Object {
        $_.InterfaceAlias -like '*Tailscale*' -and $_.IPAddress -match '^100\.(?:6[4-9]|[7-9][0-9]|1[01][0-9]|12[0-7])\.'
    })
    if ($taskLocal.Count -ne 1) { throw 'PC Tailscale 연결을 켜 주세요. 기본 Tailscale 인터페이스의 IPv4 주소 1개가 필요합니다.' }
    $taskExe = (Get-Item -LiteralPath (Join-Path $PSScriptRoot 'CodexMeterWidget.exe')).FullName
    $taskBlocked = @(Get-NetFirewallApplicationFilter -Program $taskExe -ErrorAction SilentlyContinue |
        Get-NetFirewallRule | Where-Object { $_.Action -eq 'Block' -and $_.Enabled -eq 'True' })
    if ($taskBlocked.Count -gt 0) { throw '이 실행 파일에 기존 방화벽 차단 규칙이 있습니다. 규칙은 삭제하지 않았습니다. 차단 규칙을 확인한 뒤 다시 실행하세요.' }
    $taskHash = [Security.Cryptography.SHA256]::Create()
    try { $taskPathId = ([BitConverter]::ToString($taskHash.ComputeHash([Text.Encoding]::UTF8.GetBytes($taskExe)))).Replace('-','').Substring(0,12) }
    finally { $taskHash.Dispose() }
    $taskRule = 'GPT-HUD-Tailscale-' + $taskPathId + '-' + $taskPhoneIp
    $taskParams = @{
        Name=$taskRule; Direction='Inbound'; Action='Allow'; Enabled='True'; Profile='Any'
        Program=$taskExe; Protocol='TCP'; LocalPort=47653; LocalAddress=$taskLocal[0].IPAddress
        RemoteAddress=$taskPhoneIp; InterfaceAlias=$taskLocal[0].InterfaceAlias
    }
    if (Get-NetFirewallRule -Name $taskRule -ErrorAction SilentlyContinue) { Set-NetFirewallRule @taskParams }
    else { New-NetFirewallRule @taskParams -DisplayName 'GPT HUD - 내 휴대폰 Tailscale 연결' | Out-Null }
    Write-Host '설정 완료: 이 PC 위젯과 입력한 휴대폰 사이의 Tailscale 연결만 허용했습니다.' -ForegroundColor Green
    Write-Host ('PC 위젯 → 휴대폰 연결 코드에서 ' + $taskLocal[0].IPAddress + ' 주소를 고르세요.')
    Write-Host '연결 코드를 폰 → Codex 작업 상태 시험 → PC 연결 설정에 붙여넣으세요.'
    Write-Host 'PC EXE를 다른 폴더로 옮기거나 폰 주소가 바뀌면 이 설정을 다시 실행하세요.'
} catch { Write-Host $_.Exception.Message -ForegroundColor Red }
Read-Host 'Enter를 누르면 닫힙니다' | Out-Null
