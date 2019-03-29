@echo off

REM DON'T CHANGE THE FIRST LINE OF THE FILE, WINDOWS SERVICE RUN BAT NEED IT! (@echo off) 
REM don't call 'echo' before 'start as.bat'
REM You can specify Java Home via AS_JAVA_HOME here or Windows System Environment, but not in cmd.exe
REM set AS_JAVA_HOME=C:\Program Files\Java\jdk1.8.0_131

set basedir=%~dp0
set filename=%~nx0
set srv_name=arthas
set telnet_port=3658
set http_port=8563


REM parse extend args
set arg1=%1
set pid=
set port=
set ignoreTools=0
set as_remove_srv=0
set as_service=0
set srv_interact=0
for %%a in (%*) do (
  if "%%a"=="--remove" set as_remove_srv=1
  if "%%a"=="--service" set as_service=1
  if "%%a"=="--interact"  set srv_interact=1
  if "%%a"=="--ignore-tools" set ignoreTools=1
)


REM Parse command line args (https://stackoverflow.com/a/35445653)
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


REM Setup JAVA_HOME
REM Decode -java-home: '@' -> ' '
if not "%java-home%"=="" set JAVA_HOME=%java-home:@= %
REM If has AS_JAVA_HOME, overriding JAVA_HOME
if not "%AS_JAVA_HOME%" == "" set JAVA_HOME=%AS_JAVA_HOME%
REM use defined is better then "%var%" == "", avoid trouble of ""
if not defined JAVA_HOME goto noJavaHome
REM Remove "" in path
set JAVA_HOME=%JAVA_HOME:"=%
if not exist "%JAVA_HOME%\bin\java.exe" goto noJavaHome
if %ignoreTools% == 1 (
  echo Ignore tools.jar, make sure the java version ^>^= 9
) else (
  if not exist "%JAVA_HOME%\lib\tools.jar" (
    echo Can not find lib\tools.jar under %JAVA_HOME%!
    echo If java version ^<^= 1.8, please make sure JAVA_HOME point to a JDK not a JRE.
    echo If java version ^>^= 9, try to run %filename% ^<pid^> --ignore-tools
    goto :end
  )
)
set JAVACMD="%JAVA_HOME%\bin\java"

REM Runas Service, don't call 'echo' before 'start as.bat'
set as_args=-telnet-port %telnet_port% -http-port %http_port%
if %srv_interact%==0  set as_args=%as_args% --no-interact
if %ignoreTools%==1 set as_args=%as_args% --ignore-tools
if %as_service%==1 (
	REM run as.bat
	start /wait %basedir%\as.bat %pid% %as_args%
	exit 0
	
	REM DEBUG run args
	REM echo as_args: %as_args%
	REM echo start /wait %basedir%\as.bat %pid% %as_args%
	REM exit /b 0
)

REM If the first arg is a number, then set it as pid
echo %arg1%| findstr /r "^[1-9][0-9]*$">nul
if %errorlevel% equ 0  set pid=%arg1%

echo pid: %pid%
echo port: %port%

if not ["%pid%"] == [""] (
    goto :prepare_srv
)
if not ["%port%"] == [""] (
    goto :find_port
)
if %as_remove_srv%==1 (
    goto :remove_srv
)
goto :usage


:remove_srv
echo Removing service: %srv_name% ...
sc stop %srv_name%
sc delete %srv_name%
exit /b 0


:find_port
netstat -nao |findstr LIST |findstr :%telnet_port%
IF %ERRORLEVEL% EQU 0 (
	echo Arthas agent already running, just connect to it!
	goto :attachSuccess
)
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
REM check telnet port
netstat -nao |findstr LIST |findstr :%telnet_port%
IF %ERRORLEVEL% EQU 0 (
	echo Arthas agent already running, just connect to it!
	goto :attachSuccess
)

REM validate pid
echo %pid%| findstr /r "^[1-9][0-9]*$">nul
if %errorlevel% neq 0 (
    echo PID is not valid number!
    goto :usage
)
echo Preparing arthas service and injecting arthas agent to process: %pid% ...

REM encode java path, avoid space in service args: ' ' -> '@'
set srv_java_home=-java-home %JAVA_HOME: =@%
set srv_args=-pid %pid% -telnet-port %telnet_port% -http-port %http_port% %srv_java_home% 
if %srv_interact%==1 (
	sc start UI0Detect
	set srv_type=type= interact type= own
	set srv_binpath=binPath= "%basedir%\%filename% %srv_args% --service --interact"
) else (
	set srv_type=type= own
	set srv_binpath=binPath= "%basedir%\%filename% %srv_args% --service"
)
echo arthas srv type: %srv_type%
echo arthas srv binPath: %srv_binpath%

sc create %srv_name% start= demand %srv_type% %srv_binpath%
sc config %srv_name% start= demand %srv_type% %srv_binpath%
if %errorlevel% NEQ 0 (
	echo Config Arthas service failed
	exit /b -1
)

sc stop %srv_name%
REM fork start Arthas service, avoid blocking
if %srv_interact%==1 (
	start /B sc start %srv_name%
)else (
	start /B sc start %srv_name% > nul 2>&1
)

REM check and connect arthas ..
echo Waitting for arthas agent ...
set count=0

:waitfor_loop
echo checking
netstat -nao |findstr LIST |findstr :%telnet_port%
IF %ERRORLEVEL% NEQ 0 (
    set /a count+=1
    if %count% geq 8 (
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

echo(
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
goto :end



:usage
echo Arthas for Windows Service.
echo Usage:
echo   %filename% java_pid [option] ..
echo   %filename% [-pid java_pid] [option] ..
echo   %filename% [-port java_port] [option] ..
echo(
echo Options:
echo   -pid java_pid      : Attach by java process pid
echo   -port java_port    : Attach by java process listen port
echo   -telnet-port port  : Change arthas telnet port 
echo   -http-port port    : Change arthas http/websocket port 
echo   --interact         : Enable windows service interactive UI, useful for debug
echo   --remove           : Remove Arthas windows service
echo   --ignore-tools     : Ignore checking JAVA_HOME\lib\tools.jar for jdk 9/10/11
echo(
echo Example:
echo   %filename% 2351 
echo   %filename% 2351 -telnet-port 2000 -http-port 2001
echo   %filename% -pid 2351 
echo   %filename% -port 8080 --interact 
echo   %filename% --remove  #remove arthas service
exit /b -1


:noJavaHome
echo JAVA_HOME: %JAVA_HOME%
echo The JAVA_HOME environment variable is not defined correctly.
echo It is needed to run this program.
echo NB: JAVA_HOME should point to a JDK not a JRE.
exit /b -1

:end
