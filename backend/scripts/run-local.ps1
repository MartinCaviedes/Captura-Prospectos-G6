#Requires -Version 7.0
[CmdletBinding()]
param([switch]$Stop)
$ErrorActionPreference='Stop'
$backendRoot=Split-Path $PSScriptRoot -Parent
$localDirectory=Join-Path $backendRoot '.local/windows'
$statePath=Join-Path $localDirectory 'processes.json'
function Is-OwnedProcess($record) {
    $serviceProcess=Get-Process -Id $record.ProcessId -ErrorAction SilentlyContinue
    if(-not $serviceProcess) {return $false}
    # PowerShell 7.5+ puede convertir fechas ISO de JSON a DateTime automáticamente.
    if($serviceProcess.StartTime.ToUniversalTime().Ticks -ne ([DateTimeOffset]$record.StartedAt).UtcDateTime.Ticks) {return $false}
    $command=(Get-CimInstance Win32_Process -Filter "ProcessId = $($record.ProcessId)").CommandLine
    return $command -and $command.Contains($record.Jar) -and $command.Contains($record.Config)
}
if($Stop) {
    if(Test-Path -LiteralPath $statePath) {
        foreach($record in @(Get-Content -LiteralPath $statePath -Raw | ConvertFrom-Json)) {
            if(Is-OwnedProcess $record) {Stop-Process -Id $record.ProcessId;Write-Output "Detenido $($record.Service)."}
            elseif(Get-Process -Id $record.ProcessId -ErrorAction SilentlyContinue) {
                throw 'El PID registrado pertenece a otro proceso. No se detiene.'
            }
        }
        Remove-Item -LiteralPath $statePath
    }
    Write-Output 'Bases y configuración conservadas.'
    return
}
if(Test-Path -LiteralPath $statePath) {
    $records=@(Get-Content -LiteralPath $statePath -Raw | ConvertFrom-Json)
    if($records.Count -eq 3 -and @($records | Where-Object {-not (Is-OwnedProcess $_)}).Count -eq 0) {
        Write-Output 'Los tres servicios de este lanzador ya están activos en 8080/8081/8082.'
        return
    }
    throw 'Hay un estado anterior parcial. Ejecutar scripts/run-local.ps1 -Stop y volver a iniciar.'
}
& (Join-Path $PSScriptRoot 'prepare-local.ps1')
$definitions=@(
    @{Service='users-service';Config='users';Port=8081},
    @{Service='prospects-service';Config='prospects';Port=8082},
    @{Service='api-gateway';Config='gateway';Port=8080})
foreach($definition in $definitions) {
    $probe=[Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback,$definition.Port)
    try {$probe.Start()} catch {throw "Puerto $($definition.Port) ocupado. Detener ese servicio desde su propio lanzador antes de iniciar."}
    finally {$probe.Stop()}
    $jar=Join-Path $backendRoot "$($definition.Service)/target/$($definition.Service)-0.0.1-SNAPSHOT.jar"
    if(-not (Test-Path -LiteralPath $jar)) {throw 'Faltan JAR. Compilar primero con .\mvnw.cmd verify.'}
}
$started=@()
try {
    foreach($definition in $definitions) {
        $jar=Join-Path $backendRoot "$($definition.Service)/target/$($definition.Service)-0.0.1-SNAPSHOT.jar"
        $config=([Uri](Join-Path $localDirectory "$($definition.Config).properties")).AbsoluteUri
        $logPrefix=Join-Path $localDirectory "$($definition.Config)-$(Get-Date -Format 'yyyyMMdd-HHmmss')"
        $serviceProcess=Start-Process java -ArgumentList @('-jar',('"'+$jar+'"'),("--spring.config.additional-location=$config")) `
            -WorkingDirectory $backendRoot -WindowStyle Hidden -PassThru `
            -RedirectStandardOutput "$logPrefix.stdout.log" -RedirectStandardError "$logPrefix.stderr.log"
        $started+=@{Service=$definition.Service;ProcessId=$serviceProcess.Id;
            StartedAt=$serviceProcess.StartTime.ToUniversalTime().ToString('o');Jar=$jar;Config=$config}
        $started | ConvertTo-Json | Set-Content -LiteralPath $statePath
        $ready=$false
        for($attempt=0;$attempt -lt 90;$attempt++) {
            if($serviceProcess.HasExited) {throw "$($definition.Service) terminó al iniciar. Revisar $logPrefix.stdout.log y stderr.log."}
            try {
                $response=Invoke-RestMethod "http://127.0.0.1:$($definition.Port)/actuator/health/readiness" -TimeoutSec 1
                if($response.status -eq 'UP') {$ready=$true;break}
            } catch {}
            Start-Sleep -Milliseconds 500
        }
        if(-not $ready) {throw "Timeout de arranque de $($definition.Service). Revisar logs en .local/windows."}
        Write-Output "$($definition.Service) listo en $($definition.Port)."
    }
} catch {
    foreach($record in $started) {if(Is-OwnedProcess $record) {Stop-Process -Id $record.ProcessId}}
    if(Test-Path -LiteralPath $statePath) {Remove-Item -LiteralPath $statePath}
    throw
}
Write-Output 'Backend local activo. Swagger: http://localhost:8080/docs/index.html'
Write-Output 'Sin apagado automático a los 30 minutos. Para detenerlo: scripts/run-local.ps1 -Stop'
