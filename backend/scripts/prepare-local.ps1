#Requires -Version 7.0
[CmdletBinding()]
param([switch]$UseDemoCredentials)
$ErrorActionPreference='Stop'
$backendRoot=Split-Path $PSScriptRoot -Parent
$localDirectory=Join-Path $backendRoot '.local/windows'
if(-not (Test-Path -LiteralPath $localDirectory)) {
    New-Item -ItemType Directory -Path $localDirectory -Force | Out-Null
    # Las claves, credenciales y bases quedan accesibles solo al usuario, SYSTEM y administradores.
    $acl=Get-Acl -LiteralPath $localDirectory
    $acl.SetAccessRuleProtection($true,$false)
    $identities=@([Security.Principal.WindowsIdentity]::GetCurrent().User,
        [Security.Principal.SecurityIdentifier]::new('S-1-5-18'),
        [Security.Principal.SecurityIdentifier]::new('S-1-5-32-544'))
    foreach($identity in $identities) {
        $acl.AddAccessRule([Security.AccessControl.FileSystemAccessRule]::new($identity,'FullControl',
            'ContainerInherit,ObjectInherit','None','Allow'))
    }
    Set-Acl -LiteralPath $localDirectory -AclObject $acl
}
$privatePath=Join-Path $localDirectory 'private.pem'
$publicPath=Join-Path $localDirectory 'public.pem'
$privateExists=Test-Path -LiteralPath $privatePath
$publicExists=Test-Path -LiteralPath $publicPath
$rsa=[Security.Cryptography.RSA]::Create(2048)
try {
    if(-not $privateExists -and -not $publicExists) {
        [IO.File]::WriteAllText($privatePath,$rsa.ExportPkcs8PrivateKeyPem())
        [IO.File]::WriteAllText($publicPath,$rsa.ExportSubjectPublicKeyInfoPem())
    } elseif(-not $privateExists -or -not $publicExists) {
        throw 'Par de claves incompleto. Revisar .local/windows; no se reemplazan archivos existentes.'
    } else {
        $rsa.ImportFromPem([IO.File]::ReadAllText($privatePath))
        $public=[Security.Cryptography.RSA]::Create()
        try {
            $public.ImportFromPem([IO.File]::ReadAllText($publicPath))
            if([Convert]::ToBase64String($rsa.ExportSubjectPublicKeyInfo()) -ne [Convert]::ToBase64String($public.ExportSubjectPublicKeyInfo())) {
                throw 'Las claves locales no pertenecen al mismo par.'
            }
        } finally {$public.Dispose()}
    }
} finally {$rsa.Dispose()}
function Write-NewProperties([string]$Name,[string[]]$Lines) {
    $path=Join-Path $localDirectory $Name
    if(-not (Test-Path -LiteralPath $path)) {
        $stream=[IO.File]::Open($path,[IO.FileMode]::CreateNew,[IO.FileAccess]::Write)
        try {
            $writer=[IO.StreamWriter]::new($stream,[Text.UTF8Encoding]::new($false))
            try {$writer.Write(($Lines -join "`n")+"`n")} finally {$writer.Dispose()}
        } finally {$stream.Dispose()}
    }
}
$common=@('spring.profiles.active=local','server.address=127.0.0.1',
    "security.jwt.public-key=$(([Uri]$publicPath).AbsoluteUri)")
$dbDirectory=$localDirectory.Replace('\','/')
if($UseDemoCredentials) {
    $adminPassword='test-admin-password-only';$captorPassword='test-captor-password-only'
} else {
    $adminPassword=[Convert]::ToHexString([Security.Cryptography.RandomNumberGenerator]::GetBytes(24))
    $captorPassword=[Convert]::ToHexString([Security.Cryptography.RandomNumberGenerator]::GetBytes(24))
}
Write-NewProperties 'users.properties' ($common+@('server.port=8081',
    "security.jwt.private-key=$(([Uri]$privatePath).AbsoluteUri)",
    "LOCAL_ADMIN_PASSWORD=$adminPassword","LOCAL_CAPTOR_PASSWORD=$captorPassword",
    "spring.datasource.url=jdbc:h2:file:$dbDirectory/identity;MODE=PostgreSQL",
    'spring.datasource.username=sa','spring.datasource.password=','spring.jpa.hibernate.ddl-auto=update'))
Write-NewProperties 'prospects.properties' ($common+@('server.port=8082',
    "spring.datasource.url=jdbc:h2:file:$dbDirectory/prospects;MODE=PostgreSQL",
    'spring.datasource.username=sa','spring.datasource.password=','spring.jpa.hibernate.ddl-auto=update'))
Write-NewProperties 'gateway.properties' ($common+@('server.port=8080',
    'services.users.url=http://127.0.0.1:8081','services.prospects.url=http://127.0.0.1:8082',
    'documentation.enabled=true'))
Write-Output 'Configuración Windows preparada en backend/.local/windows. Archivos existentes conservados.'
Write-Output 'El seed local solo crea cuentas ausentes: este comando no restablece contraseñas ni borra bases.'
