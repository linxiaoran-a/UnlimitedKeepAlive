# Pack module zip from repo files (repo-relative, requires JDK jar on PATH or JAVA_HOME)
$ErrorActionPreference='Stop'
$here=Split-Path -Parent $MyInvocation.MyCommand.Path
$out="$here\build\unlimited_keepalive"
if(Test-Path "$here\build"){Remove-Item "$here\build" -Recurse -Force}
New-Item -ItemType Directory -Force -Path "$out\tools","$out\webroot" | Out-Null
foreach($f in 'module.prop','config.sh','keepalive.sh','service.sh','uninstall.sh','webui_ctl.sh'){
  Copy-Item "$here\$f" "$out\$f" -Force
}
Copy-Item "$here\tools\icedump.dex" "$out\tools\icedump.dex" -Force
Copy-Item "$here\webroot\index.html" "$out\webroot\index.html" -Force
$jar = if($env:JAVA_HOME){"$env:JAVA_HOME\bin\jar.exe"}else{'jar.exe'}
$ver = (Select-String -Path "$here\module.prop" -Pattern '^version=(.+)$').Matches[0].Groups[1].Value
$zip = "$here\UnlimitedKeepAlive-KSU-$ver.zip"
if(Test-Path $zip){Remove-Item $zip -Force}
& $jar cfM $zip -C $out .
Write-Host "packed -> $zip"
