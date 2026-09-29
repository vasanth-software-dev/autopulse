@echo off
echo ========================================================
echo        AutoPulse - Push to GitHub & Build APK
echo ========================================================
echo.
set /p REPO_URL="Enter your GitHub Repository URL (e.g. https://github.com/vasanth-software-dev/autopulse.git): "
if "%REPO_URL%"=="" goto error

git remote remove origin >nul 2>&1
git remote add origin %REPO_URL%
git branch -M main
echo.
echo Pushing code to GitHub...
git push -u origin main
if %ERRORLEVEL% NEQ 0 goto failed

echo.
echo ========================================================
echo SUCCESS! Code pushed to GitHub.
echo.
echo GitHub Actions is now building your Android APK.
echo 1. Open your repository on GitHub.
echo 2. Click the 'Actions' tab.
echo 3. Click the running workflow: 'Build AutoPulse Android APK'.
echo 4. Under 'Artifacts', download 'AutoPulse-v1.0.0-APK'.
echo 5. Install the APK on Phone 1!
echo ========================================================
goto end

:error
echo ERROR: Repository URL cannot be empty.
goto end

:failed
echo.
echo ERROR: git push failed. Please verify your repository URL and GitHub credentials.

:end
pause
