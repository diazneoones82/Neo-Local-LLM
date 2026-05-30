param(
    [string]$Configuration = "Release"
)

$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $PSScriptRoot
$DesktopRoot = $PSScriptRoot
$BuildDir = Join-Path $DesktopRoot "build"
$ClassesDir = Join-Path $BuildDir "classes"
$LibDir = Join-Path $BuildDir "lib"
$JarPath = Join-Path $LibDir "neo-local-lm-desktop.jar"
$MainClass = "com.neo.locallm.desktop.NEOLocalLMDesktop"
$AppName = "Neo Local LLM"
$AppVersion = "1.0.22"
$PackageDir = Join-Path $BuildDir "package-$AppVersion"
$IconPath = Join-Path $BuildDir "cherry.ico"

Remove-Item -LiteralPath $BuildDir -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path $ClassesDir, $LibDir, $PackageDir | Out-Null

$Sources = Get-ChildItem -Path (Join-Path $DesktopRoot "src") -Recurse -Filter "*.java" |
    ForEach-Object { $_.FullName }

if (-not $Sources) {
    throw "No Java desktop sources found."
}

Add-Type -AssemblyName System.Drawing
$bitmap = New-Object System.Drawing.Bitmap 256, 256
$graphics = [System.Drawing.Graphics]::FromImage($bitmap)
$graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$graphics.Clear([System.Drawing.Color]::Transparent)
$bg = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 255, 240, 244))
$graphics.FillRectangle($bg, 0, 0, 256, 256)
$leaf = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 22, 139, 91))
$petal = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 255, 156, 176))
$cherry = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 195, 22, 61))
$highlight = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(230, 253, 232, 238))
$stemPen = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(255, 122, 16, 42)), 12
$stemPen.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
$stemPen.EndCap = [System.Drawing.Drawing2D.LineCap]::Round
$graphics.FillPolygon($petal, [System.Drawing.Point[]]@(
    [System.Drawing.Point]::new(82, 74),
    [System.Drawing.Point]::new(126, 30),
    [System.Drawing.Point]::new(112, 88)
))
$graphics.FillPolygon($leaf, [System.Drawing.Point[]]@(
    [System.Drawing.Point]::new(128, 58),
    [System.Drawing.Point]::new(198, 38),
    [System.Drawing.Point]::new(164, 78)
))
$graphics.DrawLine($stemPen, 124, 62, 98, 124)
$graphics.FillEllipse($cherry, 38, 102, 130, 124)
$graphics.FillEllipse($cherry, 132, 102, 102, 124)
$bite = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 255, 240, 244))
$graphics.FillEllipse($bite, 212, 138, 58, 58)
$graphics.FillEllipse($highlight, 60, 120, 58, 30)
$graphics.Dispose()
$pngStream = New-Object System.IO.MemoryStream
$bitmap.Save($pngStream, [System.Drawing.Imaging.ImageFormat]::Png)
$bitmap.Dispose()
$png = $pngStream.ToArray()
$pngStream.Dispose()
$ico = New-Object System.IO.MemoryStream
$writer = New-Object System.IO.BinaryWriter $ico
$writer.Write([UInt16]0)
$writer.Write([UInt16]1)
$writer.Write([UInt16]1)
$writer.Write([byte]0)
$writer.Write([byte]0)
$writer.Write([byte]0)
$writer.Write([byte]0)
$writer.Write([UInt16]1)
$writer.Write([UInt16]32)
$writer.Write([UInt32]$png.Length)
$writer.Write([UInt32]22)
$writer.Write($png)
$writer.Flush()
[System.IO.File]::WriteAllBytes($IconPath, $ico.ToArray())
$writer.Dispose()
$ico.Dispose()

$SourcesFile = Join-Path $BuildDir "sources.txt"
$Sources | ForEach-Object { '"' + ($_ -replace '\\', '/') + '"' } | Set-Content -Path $SourcesFile -Encoding ASCII

& javac.exe --release 21 -encoding UTF-8 -d $ClassesDir "@$SourcesFile"
if ($LASTEXITCODE -ne 0) { throw "javac failed." }

Push-Location $ClassesDir
try {
    & jar.exe --create --file $JarPath --main-class $MainClass .
    if ($LASTEXITCODE -ne 0) { throw "jar failed." }
} finally {
    Pop-Location
}

& jpackage.exe `
    --type app-image `
    --name $AppName `
    --app-version $AppVersion `
    --vendor "NEO" `
    --input $LibDir `
    --main-jar (Split-Path -Leaf $JarPath) `
    --main-class $MainClass `
    --icon $IconPath `
    --dest $PackageDir `
    --java-options "-Dfile.encoding=UTF-8"
if ($LASTEXITCODE -ne 0) { throw "jpackage app-image failed." }

$AppImage = Join-Path $PackageDir $AppName
$ZipPath = Join-Path $BuildDir "Neo-Local-LLM-Windows-Portable.zip"
Compress-Archive -Path (Join-Path $AppImage "*") -DestinationPath $ZipPath -Force

$Csc = Join-Path $env:WINDIR "Microsoft.NET\Framework64\v4.0.30319\csc.exe"
$StandaloneExe = Join-Path $BuildDir "Neo-Local-LLM-Windows-Standalone.exe"
if (Test-Path $Csc) {
    $CscArgs = @(
        "/nologo",
        "/target:winexe",
        "/out:$StandaloneExe",
        "/win32icon:$IconPath",
        "/resource:$ZipPath,app.zip",
        "/reference:System.Windows.Forms.dll",
        "/reference:System.IO.Compression.dll",
        "/reference:System.IO.Compression.FileSystem.dll",
        (Join-Path $DesktopRoot "installer\StandaloneLauncher.cs")
    )
    & $Csc @CscArgs
    if ($LASTEXITCODE -ne 0) { throw "standalone EXE wrapper build failed." }
} else {
    Write-Warning "Could not find .NET Framework csc.exe; standalone wrapper was not created."
}

$InstallerDir = Join-Path $BuildDir "installer"
New-Item -ItemType Directory -Force -Path $InstallerDir | Out-Null
$InstallerExe = Join-Path $InstallerDir "Neo-Local-LLM-Windows-Setup.exe"

try {
    & jpackage.exe `
        --type exe `
        --name $AppName `
        --app-version $AppVersion `
        --vendor "NEO" `
        --input $LibDir `
        --main-jar (Split-Path -Leaf $JarPath) `
        --main-class $MainClass `
        --icon $IconPath `
        --dest $InstallerDir `
        --java-options "-Dfile.encoding=UTF-8" `
        --win-dir-chooser `
        --win-menu `
        --win-shortcut
} catch {
    Write-Warning "jpackage EXE installer failed: $($_.Exception.Message)"
}

Write-Host ""
Write-Host "Built Windows desktop package:"
Write-Host "  App image: $AppImage"
Write-Host "  Launcher:  $(Join-Path $AppImage "$AppName.exe")"
Write-Host "  Portable:  $ZipPath"
if (Test-Path $StandaloneExe) {
    Write-Host "  Single EXE package: $StandaloneExe"
}
if (Test-Path $InstallerExe) {
    Write-Host "  Installer: $InstallerExe"
} else {
    Write-Host "  Installer: not created on this machine; portable zip is ready."
}
