@echo off
echo Building all ReactiveMusic targets...
if exist build\libs rd /s /q build\libs
call gradlew chiseledBuild %*
echo.
echo Built jars:
for /r build\libs %%f in (*.jar) do (
    echo %%f | findstr /v "sources" >nul && echo %%f
)
