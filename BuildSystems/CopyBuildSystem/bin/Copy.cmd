@ECHO OFF
IF NOT EXIST .\build mkdir .\build
robocopy . .\build /E /NFL /NJH /NJS /NDL /NC /NS /NP /XF "%CD%\ISLAND" /XF "%CD%\BuildScript" /XF "%CD%\BuildScript.cmd" /XD "%CD%\build" /XD "%CD%\.git"
exit 0