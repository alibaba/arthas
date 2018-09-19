rem arthas extend bat file
@echo off
setlocal EnableDelayedExpansion
cd /d %~dp0
@echo off > javapid.tmp

for /f "skip=1 tokens=2,10,11,12" %%i in ('tasklist /nh /v  /fi "imagename eq java.exe"' ) do (
	set /a n+=1 
	echo %%i %%j %%k %%l >> javapid.tmp
)

for /f "skip=1 tokens=2,10,11,12" %%i in ('tasklist /nh /v  /fi "imagename eq javaw.exe"' ) do (
	set /a n+=1 
	echo %%i %%j %%k %%l >> javapid.tmp
)

set n=0
for /f "tokens=*" %%i in (javapid.tmp) do (
	set /a n+=1 
	echo [!n!] %%i
)

:choose
set choice=
set /p choice=  Found existing java process, please choose one and hit RETURN:


if "%Choice%"=="" goto choose
if "%Choice%"=="quit" goto end
if "%Choice%"=="exit" goto end
if %Choice% gtr 999999 goto choose

set n=0
for /f "tokens=1" %%i in (javapid.tmp) do (
	set /a n+=1
	if !n!==%choice% set pid=%%i
)

del javapid.tmp

as.bat %pid%

:end
del javapid.tmp