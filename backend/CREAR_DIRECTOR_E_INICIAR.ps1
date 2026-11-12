# Ejecutar desde PowerShell: powershell -ExecutionPolicy Bypass -File .\CREAR_DIRECTOR_E_INICIAR.ps1
# Requiere MySQL activo, Java 21 y Maven (o mvnw.cmd con wrapper disponible).
$ErrorActionPreference = "Stop"
Set-Location -LiteralPath $PSScriptRoot
$env:EDUCORE_DIRECTOR_EMAIL = Read-Host "Correo del Director para esta institucion"
$secret = Read-Host "Crea una contrasena de 12 o mas caracteres" -AsSecureString
$ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secret)
try {
    $env:EDUCORE_DIRECTOR_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr)
    if ($env:EDUCORE_DIRECTOR_PASSWORD.Length -lt 12) { throw "La contrasena debe contener 12 caracteres como minimo" }
    # Si la base esta vacia, este nombre crea la primera institucion de nivel Jardin.
    # Si ya existe una, no la modifica.
    $env:EDUCORE_BOOTSTRAP_SCHOOL_NAME = Read-Host "Nombre de la escuela (solo se usa si no hay instituciones)"
    $env:EDUCORE_DIRECTOR_TENANT_ID = Read-Host "ID de la institucion (dejar vacio si solo hay una)"
    Write-Host "Iniciando Spring Boot: se creara la cuenta solamente si no existe para la institucion elegida."
    if (Test-Path '.\mvnw.cmd') { & .\mvnw.cmd clean spring-boot:run }
    else { & mvn clean spring-boot:run }
} finally {
    if ($ptr -ne [IntPtr]::Zero) { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr) }
    Remove-Item Env:EDUCORE_DIRECTOR_PASSWORD -ErrorAction SilentlyContinue
    Remove-Item Env:EDUCORE_DIRECTOR_EMAIL -ErrorAction SilentlyContinue
    Remove-Item Env:EDUCORE_BOOTSTRAP_SCHOOL_NAME -ErrorAction SilentlyContinue
    Remove-Item Env:EDUCORE_DIRECTOR_TENANT_ID -ErrorAction SilentlyContinue
}
