@echo off

set basedir=%~dp0
set srv_name=arthas
set srv_interact=false
set telnet_port=3658
set JAVA_HOME=C:\Program Files\Java\jdk1.8.0_131

REM parse extend args
set SRV_ARGS=
for %%a in (%*) do (
  if "%%a"=="--ignore-tools" set SRV_ARGS=%SRV_ARGS% --ignore-tools
  if "%%a"=="--interact" ( 
	set SRV_ARGS=%SRV_ARGS% --interact 
	set srv_interact=true
  )
)

REM Setup JAVA_HOME
if "%JAVA_HOME%" == "" goto noJavaHome
if not exist "%JAVA_HOME%\bin\java.exe" goto noJavaHome
set JAVACMD="%JAVA_HOME%\bin\java"

REM Runas Service, do not call 'echo' before this line
if ["%~1"] == ["-service"] (
    start /wait %basedir%\as.bat %2 %SRV_ARGS%
    exit 0
)

if ["%~1"] == ["-port"] (
    set port=%2
    if "%port%" == "" goto :usage
    goto :find_port
)
if ["%~1"] == ["-pid"] (
    set pid=%2
    if "%pid%" == "" goto :usage 
    goto :prepare_srv
)
if ["%~1"] == ["-remove"] (
    goto :remove_srv
)
goto :usage

:usage
echo Example:
echo   %~nx0 -port java_port
echo   %~nx0 -pid java_pid
echo   %~nx0 -port 8080
echo   %~nx0 -pid 2351
echo   %~nx0 -remove  ;remove arthas service
echo Need the port or pid argument.
exit /b -1

:noJavaHome
echo JAVA_HOME: %JAVA_HOME%
echo The JAVA_HOME environment variable is not defined correctly.
echo It is needed to run this program.
echo NB: JAVA_HOME should point to a JDK not a JRE.
exit /b -1

:remove_srv
echo Removing service: %srv_name% ...
sc stop %srv_name%
sc delete %srv_name%
exit /b 0


:find_port
@rem find pid by port
echo %port%| findstr /r "^[1-9][0-9]*$">nul
if %errorlevel% neq 0 (
    echo port is not valid number!
    goto :usage
)

echo Finding process of listening on port: %port%
set query_pid_command='netstat -ano ^^^| findstr ":%port%" ^^^| findstr "LISTENING"'
set pid=
for /f "tokens=5" %%i in (%query_pid_command%) do (
    set pid=%%i
)
if "%pid%" == "" (
    echo None process listening on port: %port%
    goto :end
)
echo Target process pid is %pid%


:prepare_srv
echo %pid%| findstr /r "^[1-9][0-9]*$">nul
if %errorlevel% neq 0 (
    echo PID is not valid number!
    goto :usage
)
echo Preparing arthas service and injecting arthas agent to process: %pid% ...

set srv_type=type= own
set srv_binpath=binPath= "%basedir%\%~nx0 -service %pid% --no-interact"
if "%srv_interact%" == "true" (
	set srv_type=type= interact type= own
	set srv_binpath=binPath= "%basedir%\%~nx0 -service %pid%"
)
sc create %srv_name% start= demand %srv_type% %srv_binpath%
sc config %srv_name% start= demand %srv_type% %srv_binpath%
sc stop %srv_name%
sc start %srv_name%

echo Waitting for arthas agent ...
set count=0

:waitfor_loop
echo checking
netstat -nao |findstr LIST |findstr :%telnet_port%
IF %ERRORLEVEL% NEQ 0 (
    set /a count+=1
    if %count% geq 4 (
        echo Arthas agent telnet port is not ready, maybe inject failed.
        goto :end
    )
    ping -w 1 -n 2 0.0.0.0 > nul
    goto :waitfor_loop
)
echo Arthas agent telnet port is ready.


:attachSuccess
WHERE telnet
IF %ERRORLEVEL% NEQ 0 (
  ECHO telnet wasn't found, please google how to install telnet under windows.
  ECHO Try to visit http://127.0.0.1:8563 to connecto arthas server.
  start http://127.0.0.1:8563
) else (
  telnet 127.0.0.1 %telnet_port%
)

echo 
echo Checking arthas telnet port [:%telnet_port%] ...
netstat -nao |findstr LIST |findstr :%telnet_port%
IF %ERRORLEVEL% EQU 0 (
	echo Arthas agent is still running!
	goto :choice
) else (
	echo Arthas agent is shutdown.
	goto :end
)

:choice
set /P c=Are you going to shutdown arthas agent [Y/N]?
echo input: %c%
if /I "%c%" EQU "Y" goto :shutdown_agent
if /I "%c%" EQU "N" goto :end
goto :choice

:shutdown_agent
echo Shutting down arthas ...
%JAVACMD% -jar arthas-client.jar -c shutdown 127.0.0.1 %telnet_port%


@rem check telnet port agian
echo Checking arthas telnet port [:%telnet_port%] ...
netstat -nao |findstr LIST |findstr :%telnet_port%
IF %ERRORLEVEL% EQU 0 (
	echo Arthas shutdown failed! 
) else (
	echo Arthas shutdown successfully.
)

:end
