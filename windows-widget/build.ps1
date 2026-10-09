param([switch]$Test)
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$output = Join-Path $root 'build'
New-Item -ItemType Directory -Force -Path $output | Out-Null
$manifest = Get-Content -LiteralPath "$root\assets\PROVENANCE.json" -Raw | ConvertFrom-Json
if ($manifest.assets.Count -ne 10) { throw '10개 휘장의 출처 기록이 필요합니다.' }
foreach ($asset in $manifest.assets) {
    foreach ($pair in @(@($asset.path,$asset.sha256),@($asset.sourcePath,$asset.sourceSha256))) {
        $path = Join-Path (Split-Path $root -Parent) $pair[0]
        if ((Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash.ToLowerInvariant() -ne $pair[1]) { throw "휘장 출처 해시 불일치: $path" }
    }
}
$compiler = Join-Path $env:WINDIR 'Microsoft.NET\Framework64\v4.0.30319\csc.exe'
if (!(Test-Path $compiler)) { throw '.NET Framework 4.8 컴파일러가 필요합니다.' }
$arguments = @('/nologo','/codepage:65001','/optimize+','/target:winexe',"/out:$output\CodexMeterWidget.exe", "/win32manifest:$root\widget.manifest", "/win32icon:$root\assets\widget.ico")
$references = @('System','System.Core','System.Drawing','System.Windows.Forms','System.Web.Extensions','System.Security','System.Net.Http')
foreach ($reference in $references) { $arguments += "/reference:$reference.dll" }
for ($i=0; $i -lt 10; $i++) { $arguments += "/resource:$root\assets\prestige_$i.png,prestige_$i" }
$sources = @(Get-ChildItem -LiteralPath "$root\src" -Filter '*.cs' | ForEach-Object { $_.FullName })
& $compiler @arguments @sources
if ($LASTEXITCODE -ne 0) { throw '위젯 빌드 실패' }
if ($Test) {
    $testArguments = @('/nologo','/codepage:65001','/optimize+','/target:exe','/main:CodexMeterWidget.Tests',"/out:$output\WidgetTests.exe")
    foreach ($reference in $references) { $testArguments += "/reference:$reference.dll" }
    for ($i=0; $i -lt 10; $i++) { $testArguments += "/resource:$root\assets\prestige_$i.png,prestige_$i" }
    & $compiler @testArguments @sources "$root\tests\Tests.cs"
    if ($LASTEXITCODE -ne 0) { throw '테스트 빌드 실패' }
    & "$output\WidgetTests.exe" "$output\previews"
    if ($LASTEXITCODE -ne 0) { throw '테스트 실패' }
}
