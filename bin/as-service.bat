
REM specify JAVA_HOME here
REM set PRE_JAVA_HOME=C:\Program Files\Java\jdk1.8.0_131


@echo off

set basedir=%~dp0
set filename=%~nx0
set srv_name=arthas_srv
set srv_interact=false
set telnet_port=3658
set http_port=8563


REM parse extend args
set arg1=%1
set arg2=%2
set AS_ARGS=
set AS_WAIT=/wait
for %%a in (%*) do (
  if "%%a"=="--ignore-tools" set AS_ARGS=%AS_ARGS% --ignore-tools
  if "%%a"=="--interact" ( 
    set AS_WAIT=
	set AS_ARGS=%AS_ARGS% --interact 
	set srv_interact=true
  )
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

if not "%telnet-port%"=="" set telnet_port=%telnet-port%
if not "%http-port%"=="" set http_port=%http-port%
REM decode path: '@' -> ' '
if not "%my_java_home%"=="" set JAVA_HOME=%my_java_home:@= %


REM Setup JAVA_HOME
set JAVA_HOME=%JAVA_HOME:"=%
if "%JAVA_HOME%" == "" goto noJavaHome
if not exist "%JAVA_HOME%\bin\java.exe" goto noJavaHome
set JAVACMD="%JAVA_HOME%\bin\java"

REM Runas Service, do not call 'echo' before this line
if ["%arg1%"] == ["-service"] (
    set AS_ARGS=%AS_ARGS% -telnet-port %telnet_port% -http-port %http_port%
    start %AS_WAIT% %basedir%\as.bat %arg2% %AS_ARGS%
    exit 0
)

if ["%arg1%"] == ["-port"] (
    set port=%arg2%
    if "%port%" == "" goto :usage
    goto :find_port
)
if ["%arg1%"] == ["-pid"] (
    set pid=%arg2%
    if "%pid%" == "" goto :usage 
    goto :prepare_srv
)
if ["%arg1%"] == ["-remove"] (
    goto :remove_srv
)
goto :usage

:usage
echo Example:
echo   %filename% -port java_port
echo   %filename% -pid java_pid
echo   %filename% -port 8080
echo   %filename% -pid 2351
echo   %filename% -remove  ;remove arthas service
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

REM encode path: ' ' -> '@'
set srv_java_home=-my_java_home %JAVA_HOME: =@%
set srv_port=-telnet-port %telnet_port% -http-port %http_port%
set srv_type=type= own
set srv_binpath=binPath= "%basedir%\%filename% -service %pid% %srv_port% %srv_java_home% --no-interact"
if "%srv_interact%" == "true" (
	set srv_type=type= interact type= own
	set srv_binpath=binPath= "%basedir%\%filename% -service %pid% %srv_port% %srv_java_home%"
)
echo arthas srv binPath: %srv_binpath%
sc start UI0Detect
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
  ECHO Try to visit http://127.0.0.1:%http_port% to connecto arthas server.
  start http://127.0.0.1:%http_port%
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
