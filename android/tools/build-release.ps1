#Requires -Version 7.0
[CmdletBinding()]
param(
    [Parameter(Mandatory)][string]$ApiBaseUrl,
    [Parameter(Mandatory)][string]$SdkDirectory,
    [Parameter(Mandatory)][string]$SigningDirectory,
    [Parameter(Mandatory)][string]$OutputDirectory,
    [string]$BuildToolsVersion='36.0.0',
    [switch]$InitializeSigningKey
)
$ErrorActionPreference='Stop'
$androidRoot=Split-Path $PSScriptRoot -Parent
$repositoryRoot=Split-Path $androidRoot -Parent
$signingRoot=[IO.Path]::GetFullPath($SigningDirectory)
$outputRoot=[IO.Path]::GetFullPath($OutputDirectory)
if($signingRoot.StartsWith($repositoryRoot+[IO.Path]::DirectorySeparatorChar,[StringComparison]::OrdinalIgnoreCase) -or $signingRoot -eq $repositoryRoot) {
    throw 'La clave de firma debe guardarse fuera del repositorio.'
}
if($outputRoot -eq $signingRoot -or $outputRoot.StartsWith($signingRoot+[IO.Path]::DirectorySeparatorChar,[StringComparison]::OrdinalIgnoreCase) -or $signingRoot.StartsWith($outputRoot+[IO.Path]::DirectorySeparatorChar,[StringComparison]::OrdinalIgnoreCase)) {
    throw 'La carpeta de entrega debe estar separada de los secretos de firma.'
}
$uri=[Uri]$ApiBaseUrl
if($uri.Scheme -ne 'https' -or -not $uri.Host -or $uri.UserInfo -or $uri.Query -or $uri.Fragment -or $uri.AbsolutePath -ne '/') {
    throw 'La release requiere una URL HTTPS raíz, sin credenciales ni parámetros.'
}
if(-not $env:JAVA_HOME) {throw 'Configura JAVA_HOME con el JDK de Android Studio.'}
$keytool=Join-Path $env:JAVA_HOME 'bin/keytool.exe'
$tools=Join-Path $SdkDirectory "build-tools/$BuildToolsVersion"
$align=Join-Path $tools 'zipalign.exe'
$signer=Join-Path $tools 'apksigner.bat'
$aapt=Join-Path $tools 'aapt2.exe'
foreach($tool in @($keytool,$align,$signer,$aapt)) {if(-not(Test-Path -LiteralPath $tool)) {throw "Herramienta ausente: $tool"}}
[IO.Directory]::CreateDirectory($signingRoot) | Out-Null
[IO.Directory]::CreateDirectory($outputRoot) | Out-Null
$acl=[Security.AccessControl.DirectorySecurity]::new()
$acl.SetAccessRuleProtection($true,$false)
$owner=[Security.Principal.WindowsIdentity]::GetCurrent().User
foreach($identity in @($owner,[Security.Principal.SecurityIdentifier]::new('S-1-5-18'))) {
    $rule=[Security.AccessControl.FileSystemAccessRule]::new($identity,'FullControl','ContainerInherit,ObjectInherit','None','Allow')
    $acl.AddAccessRule($rule)
}
$currentAcl=Get-Acl -LiteralPath $signingRoot
$allowedSids=@($owner.Value,'S-1-5-18')
$unexpected=@($currentAcl.Access | Where-Object {
    $_.AccessControlType -eq 'Allow' -and $_.IdentityReference.Translate([Security.Principal.SecurityIdentifier]).Value -notin $allowedSids
})
# No reescribir una DACL que ya es privada: algunas sesiones de Windows
# requieren SeSecurityPrivilege al volver a aplicar un descriptor nuevo.
if(-not $currentAcl.AreAccessRulesProtected -or $unexpected.Count -gt 0) {
    Set-Acl -LiteralPath $signingRoot -AclObject $acl
}
$keystore=Join-Path $signingRoot 'g6-release.p12'
$passwordFile=Join-Path $signingRoot 'keystore-password.txt'
if(-not(Test-Path -LiteralPath $keystore)) {
    if(-not $InitializeSigningKey) {throw 'No existe clave de firma. Usa -InitializeSigningKey únicamente para su creación inicial.'}
    if(-not(Test-Path -LiteralPath $passwordFile)) {
        $password=[Convert]::ToHexString([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
        [IO.File]::WriteAllText($passwordFile,$password+[Environment]::NewLine,[Text.UTF8Encoding]::new($false))
        $password=$null
    }
    & $keytool -genkeypair -noprompt -alias g6-release -keyalg RSA -keysize 3072 -validity 10000 -storetype PKCS12 -keystore $keystore -storepass:file $passwordFile -keypass:file $passwordFile -dname 'CN=Captura Prospectos Grupo 6, OU=Proyecto Academico, O=Grupo 6, C=CL'
    if($LASTEXITCODE -ne 0) {throw 'No se pudo crear la clave; no se compila ni firma.'}
}
if(-not(Test-Path -LiteralPath $passwordFile)) {throw 'Falta la contraseña de la clave existente. No regenerar la clave.'}
Push-Location $androidRoot
try {
    & ./gradlew.bat :app:assembleDemoRelease :app:lintDemoRelease "-Pg6ApiBaseUrl=$ApiBaseUrl" --console=plain
    if($LASTEXITCODE -ne 0) {throw 'Falló la compilación release.'}
} finally {Pop-Location}
$unsigned=Join-Path $androidRoot 'app/build/outputs/apk/demo/release/app-demo-release-unsigned.apk'
$metadata=Get-Content (Join-Path (Split-Path $unsigned) 'output-metadata.json') -Raw | ConvertFrom-Json
$version=$metadata.elements[0].versionName
$apk=Join-Path $outputRoot "Captura-Prospectos-G6-$version-release.apk"
$aligned=Join-Path $outputRoot 'aligned-unsigned.apk'
& $align -P 16 -f 4 $unsigned $aligned
if($LASTEXITCODE -ne 0) {throw 'Falló zipalign.'}
# PKCS#12 usa la misma contraseña para almacén y clave; apksigner la reutiliza.
& $signer sign --ks $keystore --ks-key-alias g6-release --ks-pass "file:$passwordFile" --v4-signing-enabled false --out $apk $aligned
if($LASTEXITCODE -ne 0) {throw 'Falló la firma de la APK.'}
$verification=@(& $signer verify --verbose --print-certs $apk)
if($LASTEXITCODE -ne 0) {throw 'La firma no supera la verificación.'}
$verification | Write-Output
$verification | Set-Content (Join-Path $outputRoot 'signature-verification.txt') -Encoding utf8
& $align -c -P 16 4 $apk
if($LASTEXITCODE -ne 0) {throw 'La APK firmada no supera la verificación de alineación.'}
$badging=@(& $aapt dump badging $apk)
if($LASTEXITCODE -ne 0) {throw 'No se pudo leer el manifiesto final.'}
if($badging -match '^application-debuggable' -or -not($badging[0].Contains("name='$($metadata.applicationId)'")) -or -not($badging[0].Contains("versionCode='$($metadata.elements[0].versionCode)'"))) {
    throw 'Identidad, versión o flag debuggable inesperado en la APK final.'
}
$badging | Set-Content (Join-Path $outputRoot 'apk-badging.txt') -Encoding utf8
$certificate=Join-Path $outputRoot 'g6-release-certificate.cer'
& $keytool -exportcert -alias g6-release -keystore $keystore -storepass:file $passwordFile -file $certificate
if($LASTEXITCODE -ne 0) {throw 'No se pudo exportar el certificado público.'}
$hash=(Get-FileHash -LiteralPath $apk -Algorithm SHA256).Hash.ToLowerInvariant()
[IO.File]::WriteAllText((Join-Path $outputRoot 'SHA256SUMS.txt'),"$hash  $([IO.Path]::GetFileName($apk))`n",[Text.UTF8Encoding]::new($false))
@{versionName=$version;versionCode=$metadata.elements[0].versionCode;applicationId=$metadata.applicationId;apiBaseUrl=$ApiBaseUrl;sha256=$hash;signingAlias='g6-release';debuggable=$false} |
    ConvertTo-Json | Set-Content (Join-Path $outputRoot 'release-metadata.json') -Encoding utf8
Remove-Item -LiteralPath $aligned
Write-Output "APK release verificada: $apk"
Write-Output "Clave y contraseña conservadas fuera del repositorio: $signingRoot"
