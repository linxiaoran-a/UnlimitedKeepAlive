$ErrorActionPreference = 'Stop'
$WS   = 'C:\Users\ASUS\.zcode\workspace\default'
$PROJ = "$WS\IconDump"
$JDK  = "$WS\jdk-17.0.20.1+1"
$BT   = "$WS\sdk\build-tools\34.0.0"
$FW   = "$WS\sdk\platforms\android-34\android.jar"
$OUT  = "$PROJ\build"

$env:JAVA_HOME = $JDK
$env:Path = "$JDK\bin;$BT;$env:Path"

if (Test-Path $OUT) { Remove-Item $OUT -Recurse -Force }
New-Item -ItemType Directory -Force -Path "$OUT\classes" | Out-Null

Write-Host '== [1/3] javac (against real framework.jar for hidden APIs) =='
& javac.exe -source 8 -target 8 -Xlint:-options -cp $FW -d "$OUT\classes" "$PROJ\Main.java"
if ($LASTEXITCODE -ne 0) { throw 'javac failed' }

Write-Host '== [2/3] d8 -> dex =='
& d8.bat --release --lib $FW --output $OUT "$OUT\classes\Main.class"
if ($LASTEXITCODE -ne 0) { throw 'd8 failed' }

Write-Host '== [3/3] done =='
Copy-Item "$OUT\classes.dex" "$PROJ\icedump.dex" -Force
Get-Item "$PROJ\icedump.dex" | ForEach-Object { 'icedump.dex: ' + [math]::Round($_.Length/1KB,1) + ' KB' }
