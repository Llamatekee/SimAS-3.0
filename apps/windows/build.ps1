Param(
  [switch]$SkipMSI
)

$ErrorActionPreference = 'Stop'

$RootDir    = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$WindowsDir = Join-Path $RootDir 'apps/windows'
$OutDir     = Join-Path $RootDir 'out'
$InputDir   = Join-Path $WindowsDir 'input'
$LibsDir    = Join-Path $WindowsDir 'libs'

$AppName    = 'SimAS'
$AppVersion = '3.0.0'
$AppVendor  = 'SimAS Project'

$FxVersion  = '17.0.12'
$FxZipName  = "openjfx-$FxVersion`_windows-x64_bin-sdk.zip"
$FxUrl      = "https://download2.gluonhq.com/openjfx/$FxVersion/$FxZipName"
$FxSdkDir   = Join-Path $WindowsDir "javafx-sdk-$FxVersion"
$FxZip      = Join-Path $WindowsDir $FxZipName

$ITEXT_VER  = '5.5.13.3'
$ITEXT_JAR  = "itextpdf-$ITEXT_VER.jar"
$ITEXT_URL  = "https://repo1.maven.org/maven2/com/itextpdf/itextpdf/$ITEXT_VER/$ITEXT_JAR"

Write-Host "=== Building $AppName for Windows ==="

# 0) Guard: project structure
if (-not (Test-Path (Join-Path $RootDir 'src/bienvenida/Bienvenida.java'))) {
  throw "Error: Estructura del proyecto no encontrada en $RootDir"
}

# 1) Tooling checks
Write-Host 'Checking Java 17...'
$javaVersion = & java -version 2>&1
if ($javaVersion -notmatch '17\.') { throw 'Error: se requiere Java 17' }

Write-Host 'Checking jpackage...'
try { & jpackage --version | Out-Null } catch { throw 'Error: jpackage no está disponible' }

Write-Host 'Checking WiX Toolset (para MSI)...'
$wixAvailable = $false
if (Get-Command light.exe -ErrorAction SilentlyContinue) {
  $wixAvailable = $true
} elseif (Test-Path 'C:\\Program Files (x86)\\WiX Toolset v3.11\\bin\\light.exe') {
  $env:Path += ';C:\\Program Files (x86)\\WiX Toolset v3.11\\bin'
  $wixAvailable = $true
}
if (-not $wixAvailable) {
  Write-Warning 'WiX Toolset no detectado. Se generará solo app-image (sin MSI). Instala WiX 3.11+ para crear MSI.'
}

# 2) Clean previous build
Write-Host 'Cleaning previous build...'
Remove-Item -Recurse -Force $InputDir -ErrorAction SilentlyContinue
Remove-Item -Recurse -Force (Join-Path $WindowsDir $AppName) -ErrorAction SilentlyContinue
Get-ChildItem -Path $WindowsDir -Filter *.msi -ErrorAction SilentlyContinue | Remove-Item -Force -ErrorAction SilentlyContinue
Remove-Item -Force (Join-Path $WindowsDir "$AppName.jar") -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path $InputDir | Out-Null
New-Item -ItemType Directory -Force -Path $LibsDir | Out-Null
New-Item -ItemType Directory -Force -Path $OutDir | Out-Null

# 3) Ensure OpenJFX Windows SDK
if (-not (Test-Path $FxSdkDir)) {
  Write-Host "Fetching OpenJFX $FxVersion for Windows..."
  if (-not (Test-Path $FxZip)) {
    Invoke-WebRequest -Uri $FxUrl -OutFile $FxZip
  }
  Expand-Archive -Path $FxZip -DestinationPath $WindowsDir -Force
}

# 4) Third-party libraries
Write-Host 'Preparing third-party libraries (iText)...'
if (-not (Test-Path (Join-Path $LibsDir $ITEXT_JAR))) {
  Invoke-WebRequest -Uri $ITEXT_URL -OutFile (Join-Path $LibsDir $ITEXT_JAR)
}

# 5) Compile sources
Write-Host 'Compiling Java sources...'
$sources = Get-ChildItem -Path (Join-Path $RootDir 'src') -Filter *.java -Recurse | ForEach-Object { $_.FullName }
if (-not $sources -or $sources.Count -eq 0) { throw 'No se encontraron fuentes .java en src/' }
$sourcesFile = Join-Path $WindowsDir 'sources.txt'
$sources | Set-Content -Encoding ascii $sourcesFile
$cp = (Join-Path $FxSdkDir 'lib\*') + ';' + (Join-Path $LibsDir '*')
& javac -d $OutDir -cp $cp "@$sourcesFile"

# 6) Create executable JAR (incluye recursos de src)
Write-Host 'Creating JAR...'
& jar --create --file (Join-Path $WindowsDir "$AppName.jar") -C $OutDir . -C (Join-Path $RootDir 'src') .

# 7) Prepare inputs for jpackage
Write-Host 'Preparing inputs...'
Copy-Item -Recurse -Force $FxSdkDir (Join-Path $InputDir 'javafx')
Copy-Item -Force (Join-Path $WindowsDir "$AppName.jar") $InputDir
Copy-Item -Force (Join-Path $LibsDir $ITEXT_JAR) $InputDir

if (Test-Path (Join-Path $RootDir 'Manual_de_Usuario.pdf')) {
  Copy-Item -Force (Join-Path $RootDir 'Manual_de_Usuario.pdf') $InputDir
} else {
  Write-Warning 'Manual_de_Usuario.pdf no encontrado en la raíz del proyecto'
}

if (Test-Path (Join-Path $RootDir 'src/centroayuda')) {
  New-Item -ItemType Directory -Force -Path (Join-Path $InputDir 'src') | Out-Null
  Copy-Item -Recurse -Force (Join-Path $RootDir 'src/centroayuda') (Join-Path $InputDir 'src')
} else {
  Write-Warning 'src/centroayuda no encontrado; faltarán tutoriales HTML/imagenes'
}

# 8) Prepare icon from src/resources/logo2.png
$iconSrc = Join-Path $RootDir 'src/resources/logo2.png'
$iconIco = Join-Path $WindowsDir 'icon.ico'
$iconPng = Join-Path $WindowsDir 'icon.png'
$iconArg = @()

# 8.1) Si ya existe apps/windows/icon.ico, usarlo directamente
if (Test-Path $iconIco) {
  Write-Host "Using existing icon: $iconIco"
  $iconArg = @('--icon', $iconIco)
} elseif (Test-Path $iconSrc) {
  # 8.2) Intentar generar icon.ico desde logo2.png
  Write-Host "Preparing icon from $iconSrc"
  Copy-Item -Force $iconSrc $iconPng
  if (Get-Command magick.exe -ErrorAction SilentlyContinue) {
    & magick convert $iconSrc -resize 256x256 -background none -gravity center -extent 256x256 $iconIco
  } elseif (Get-Command convert.exe -ErrorAction SilentlyContinue) {
    & convert $iconSrc -resize 256x256 -background none -gravity center -extent 256x256 $iconIco
  } else {
    Write-Warning 'ImageMagick no está disponible; no se puede generar icon.ico automáticamente'
  }
  if (Test-Path $iconIco) { $iconArg = @('--icon', $iconIco) }
} else {
  Write-Warning 'Icono src/resources/logo2.png no encontrado; se usará el icono por defecto del sistema'
}

# 9) Build app-image
Write-Host 'Creating app-image...'
$appImageArgs = @(
  '--type','app-image',
  '--name',$AppName
) + $iconArg + @(
  '--input',$InputDir,
  '--main-jar',"$AppName.jar",
  '--main-class','bienvenida.Bienvenida',
  '--java-options','--module-path %APPDIR%\javafx\lib',
  '--java-options','--add-modules=javafx.controls,javafx.fxml,javafx.graphics,javafx.media,javafx.web,javafx.swing',
  '--dest',$WindowsDir
)
& jpackage @appImageArgs

# 10) Build MSI (si WiX disponible)
$finalMsi = Join-Path $WindowsDir 'SimAS-3.0.msi'
if ($wixAvailable -and -not $SkipMSI) {
  Write-Host 'Creating MSI...'
  $msiArgs = @(
    '--type','msi',
    '--name',$AppName,
    '--app-version',$AppVersion,
    '--vendor',$AppVendor,
    '--app-image', (Join-Path $WindowsDir $AppName)
  ) + $iconArg + @(
    '--win-menu',
    '--win-shortcut',
    '--dest',$WindowsDir
  )
  & jpackage @msiArgs

  $latestMsi = Get-ChildItem -Path $WindowsDir -Filter *.msi | Sort-Object LastWriteTime | Select-Object -Last 1
  if ($latestMsi) {
    if (Test-Path $finalMsi) { Remove-Item -Force $finalMsi }
    Rename-Item -Path $latestMsi.FullName -NewName (Split-Path -Leaf $finalMsi)
  }
} else {
  Write-Warning 'MSI omitido: instale WiX Toolset 3.11+ y reintente o ejecute con -SkipMSI para ocultar este aviso.'
}

Write-Host '=== Done ==='
Write-Host ("- App image: {0}" -f (Join-Path $WindowsDir $AppName))
Write-Host ("- Run locally: {0}" -f (Join-Path (Join-Path $WindowsDir $AppName) ("{0}.exe" -f $AppName)))
if (Test-Path $finalMsi) { Write-Host ("- Installer:  {0}" -f $finalMsi) }


