@echo off

REM ----------------------------------------------------------------------------
REM  program : Arthas
REM   author : Core Engine @ Taobao.com
REM     date : 2015-11-11
REM  version : 3.0
REM ----------------------------------------------------------------------------



set ERROR_CODE=0

:init
REM Decide how to startup depending on the version of windows

REM -- Win98ME
if NOT "%OS%"=="Windows_NT" goto Win9xArg

REM set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal
goto WinNTGetScriptDir

:Win9xArg
REM Slurp the command line arguments.  This loop allows for an unlimited number
REM of arguments (up to the command line limit, anyway).
set BASEDIR=%CD%
goto repoSetup

:WinNTGetScriptDir
set BASEDIR=%~dp0

:repoSetup
set AGENT_JAR=%BASEDIR%\arthas-agent.jar
set CORE_JAR=%BASEDIR%\arthas-core.jar

set PID=%1

REM Setup JAVA_HOME
if "%JAVA_HOME%" == "" goto noJavaHome
if not exist "%JAVA_HOME%\bin\java.exe" goto noJavaHome
if not exist "%JAVA_HOME%\lib\tools.jar" goto noJavaHome
set JAVACMD="%JAVA_HOME%\bin\java"
set BOOT_CLASSPATH="-Xbootclasspath/a:%JAVA_HOME%\lib\tools.jar"
goto okJava

:noJavaHome
echo The JAVA_HOME environment variable is not defined correctly.
echo It is needed to run this program.
echo NB: JAVA_HOME should point to a JDK not a JRE.
goto exit

:okJava
set JAVACMD="%JAVA_HOME%"\bin\java

REM Reaching here means variables are defined and arguments have been captured
:endInit

%JAVACMD% -Dfile.encoding=UTF-8 %BOOT_CLASSPATH% -jar "%CORE_JAR%" -pid "%PID%"  -target-ip 127.0.0.1 -telnet-port 3658 -http-port 8563 -core "%CORE_JAR%" -agent "%AGENT_JAR%"
if %ERRORLEVEL% NEQ 0 goto error
goto attachSuccess

:error
if "%OS%"=="Windows_NT" endlocal
set ERROR_CODE=%ERRORLEVEL%
goto endNT

:attachSuccess
REM %JAVACMD% -Dfile.encoding=UTF-8 -Djava.awt.headless=true -cp "%CORE_JAR%" com.taobao.arthas.core.ArthasConsole 127.0.0.1 3658
telnet 127.0.0.1 3658

REM set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" goto endNT

REM For old DOS remove the set variables from ENV - we assume they were not set
REM before we started - at least we don't leave any baggage around
goto postExec

:endNT
REM If error code is set to 1 then the endlocal was done already in :error.
if %ERROR_CODE% EQU 0 endlocal


:postExec

if "%FORCE_EXIT_ON_ERROR%" == "on" (
  if %ERROR_CODE% NEQ 0 exit %ERROR_CODE%
)

exit /B %ERROR_CODE%
