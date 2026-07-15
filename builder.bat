@echo off

:: === Clean mode ===
if "%1"=="clean" (
    taskkill /f /im javaw.exe >nul 2>nul
    if exist build (
        echo === Removing build folder ===
        rmdir /s /q build
    )
    if exist out if "%2"=="-o" (
        echo === Removing out folder ===
        rmdir /s /q out
    )
    exit /b
)


if "%1"=="run" (

    taskkill /f /im javaw.exe >nul 2>nul
    :: De Morgan's Law OR logic: If NOT (missing build AND missing out), then at least one exists.
    :: If BOTH are missing, skip the clean step and go straight to Build.
    if not exist build if not exist out goto :Build
    
    echo === Found existing build/out files. Cleaning first... ===
    :: %0 calls this script file itself to execute the clean
    call %0 clean -o

    :Build
    call %0 build
    
    echo === Running minecraft-clone ===
    out\minecraft-clone
    exit /b
)

if "%1"=="upload" (
    setlocal

    REM === CONFIG ===
    set VERSION=v1.0.0
    set FILE1=out\minecraft-clone.exe
    set FILE2=out\installer.exe

    echo Removing old assets...
    gh release delete-asset %VERSION% minecraft-clone.exe
    gh release delete-asset %VERSION% installer.exe

    echo Uploading new assets...
    gh release upload %VERSION% %FILE1% %FILE2% --clobber

    echo Done!

)

:: === Build mode ===
if "%1"=="build" (

        
    if not exist out (
        echo === Making folder ===
        mkdir out
    )

    if not exist build\ (
        mkdir build
    )
    echo === Making icon ===
    magick helpers\icon.svg -define icon:auto-resize=256,128,64,48,32,16 helpers\icon.ico
    magick helpers\icon.svg -resize 256x256 helpers\icon.png

    echo === Making IconData.java ===
    certutil -encodehex helpers\icon.png helpers\icon.hex 0
    python -u helpers\make_icon_java.py

    echo === Compiling Java ===
    javac -d build src\game\*.java

    echo === Creating JAR ===
    jar cfe build\minecraft.jar Main -C build .

    echo === Building EXE with Launch4j ===
    java -jar "C:\Program Files (x86)\Launch4j\launch4j.jar" helpers\config.xml

    echo === Building installer ===
    g++ src\installer\installer.cpp -o out\installer.exe -lole32 -lshell32 -lshlwapi


    echo === Done! ===
) else (
    echo How to use:
    echo clean  Cleans the project.
    echo.
    echo How to use 'clean':
    echo If "-o" is specified like "builder clean -o", it removes the out folder as well.
    echo.
    echo build  Builds the project.
)
