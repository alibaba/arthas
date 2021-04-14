@echo off

REM ----------------------------------------------------------------------------
REM  program : Arthas
REM   author : Core Engine @ Taobao.com
REM     date : 2015-11-11
REM  version : 3.0
REM ----------------------------------------------------------------------------



set ERROR_CODE=0
set TELNET_PORT=3658
set HTTP_PORT=8563

set BASEDIR=%~dp0

if ["%~1"]==[""] (
  echo Example:
  echo   %~nx0 452
  echo   %~nx0 452 --ignore-tools # for jdk 9/10/11
  echo(
  echo Need the pid argument, you can run jps to list all java process ids.
  goto exit_bat
)

set JAVA_TOOL_OPTIONS
set AGENT_JAR=%BASEDIR%\arthas-agent.jar
set CORE_JAR=%BASEDIR%\arthas-core.jar

set PID=%1

echo %PID%| findstr /r "^[1-9][0-9]*$">nul

if %errorlevel% neq 0 (
  echo PID is not valid number!
  echo Example:
  echo   %~nx0 452
  echo   %~nx0 452 --ignore-tools # for jdk 9/10/11
  echo(
  echo Need the pid argument, you can run jps to list all java process ids.
  goto exit_bat
)

REM parse extend args
set ignoreTools=0
set exitProcess=0
for %%a in (%*) do (
  if "%%a"=="--no-interact" set exitProcess=1
  if "%%a"=="--ignore-tools" set ignoreTools=1
)

REM from https://stackoverflow.com/a/35445653 
:read_params
if not %1/==/ (
    if not "%__var%"=="" (
        if not "%__var:~0,1%"=="-" (
            endlocal
            goto read_params
        )
        endlocal & set %__var:~1%=%~1
    ) else (
        setlocal & set __var=%~1
    )
    shift
    goto read_params
)

if not "%telnet-port%"=="" set TELNET_PORT=%telnet-port%
if not "%http-port%"=="" set HTTP_PORT=%http-port%

echo JAVA_HOME: %JAVA_HOME%
echo telnet port: %TELNET_PORT%
echo http port: %HTTP_PORT%

REM Setup JAVA_HOME
if "%JAVA_HOME%" == "" goto noJavaHome
if not exist "%JAVA_HOME%\bin\java.exe" goto noJavaHome
if %ignoreTools% == 1 (
  echo Ignore tools.jar, make sure the java version ^>^= 9
) else (
  if not exist "%JAVA_HOME%\lib\tools.jar" (
    echo Can not find lib\tools.jar under %JAVA_HOME%!
    echo If java version ^<^= 1.8, please make sure JAVA_HOME point to a JDK not a JRE.
    echo If java version ^>^= 9, try to run as.bat ^<pid^> --ignore-tools
    goto exit_bat
  )
  set BOOT_CLASSPATH="-Xbootclasspath/a:%JAVA_HOME%\lib\tools.jar"
)

set JAVACMD="%JAVA_HOME%\bin\java"
goto okJava

:noJavaHome
echo The JAVA_HOME environment variable is not defined correctly.
echo It is needed to run this program.
echo NB: JAVA_HOME should point to a JDK not a JRE.
goto exit_bat

:okJava
%JAVACMD% -Dfile.encoding=UTF-8 %BOOT_CLASSPATH% -jar "%CORE_JAR%" -pid "%PID%"  -target-ip 127.0.0.1 -telnet-port %TELNET_PORT% -http-port %HTTP_PORT% -core "%CORE_JAR%" -agent "%AGENT_JAR%"
if %ERRORLEVEL% NEQ 0 goto exit_bat
if %exitProcess%==1 goto exit_bat
goto attachSuccess


:attachSuccess
WHERE telnet
IF %ERRORLEVEL% NEQ 0 (
  ECHO telnet wasn't found, please google how to install telnet under windows.
  ECHO Try to visit http://127.0.0.1:%HTTP_PORT% to connecto arthas server.
  start http://127.0.0.1:%HTTP_PORT%
) else (
  telnet 127.0.0.1 %TELNET_PORT%
)

:exit_bat
if %exitProcess%==1 exit %ERROR_CODE%
exit /B %ERROR_CODE%
