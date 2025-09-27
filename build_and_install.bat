@echo off
echo Building Smart AutoCorrect App...
echo.

echo [1/4] Getting Flutter dependencies...
flutter pub get
if %errorlevel% neq 0 (
    echo Error: Failed to get dependencies
    pause
    exit /b 1
)

echo.
echo [2/4] Building APK...
flutter build apk --release
if %errorlevel% neq 0 (
    echo Error: Failed to build APK
    pause
    exit /b 1
)

echo.
echo [3/4] Installing APK...
flutter install
if %errorlevel% neq 0 (
    echo Error: Failed to install APK
    echo Make sure your Android device is connected and USB debugging is enabled
    pause
    exit /b 1
)

echo.
echo [4/4] Build complete!
echo.
echo The Smart AutoCorrect app has been installed on your device.
echo.
echo Next steps:
echo 1. Open the app
echo 2. Enable Accessibility Service
echo 3. Allow Overlay Permission
echo 4. Add your correction rules
echo 5. Start typing in any app to see corrections!
echo.
pause
