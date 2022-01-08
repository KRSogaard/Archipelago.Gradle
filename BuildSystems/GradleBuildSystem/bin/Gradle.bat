@ECHO off

echo Trying to find GradleBuildSystem
for /f "delims=" %%A in ('maui path [GradleBuildSystem]pkg.dir') do set "GRADLE_PATH=%%A"
echo Using gradle at %GRADLE_PATH%


if "%~1"=="" (
  echo Executing %GRADLE_PATH%/archipelagoGradleW build
  %GRADLE_PATH%/archipelagoGradleW build
) else ( 
  echo Executing %GRADLE_PATH%/archipelagoGradleW %*
  %GRADLE_PATH%/archipelagoGradleW %*
)
