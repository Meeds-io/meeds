@REM
@REM Copyright (C) 2003-2013 eXo Platform SAS.
@REM
@REM This is free software; you can redistribute it and/or modify it
@REM under the terms of the GNU Lesser General Public License as
@REM published by the Free Software Foundation; either version 3 of
@REM the License, or (at your option) any later version.
@REM
@REM This software is distributed in the hope that it will be useful,
@REM but WITHOUT ANY WARRANTY; without even the implied warranty of
@REM MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
@REM Lesser General Public License for more details.
@REM
@REM You should have received a copy of the GNU Lesser General Public
@REM License along with this software; if not, write to the Free
@REM Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
@REM 02110-1301 USA, or see the FSF site: http://www.fsf.org.
@REM

@echo off

if "%OS%" == "Windows_NT" setlocal

rem Guess CATALINA_HOME if not defined
set "CURRENT_DIR=%cd%"
if not "%CATALINA_HOME%" == "" goto gotHome
set "CATALINA_HOME=%CURRENT_DIR%"
if exist "%CATALINA_HOME%\bin\catalina.bat" goto okHome
set "CATALINA_HOME=%cd%"
cd "%CURRENT_DIR%"
:gotHome
if exist "%CATALINA_HOME%\bin\catalina.bat" goto okHome
echo The CATALINA_HOME environment variable is not defined correctly
echo This environment variable is needed to run this program
goto end
:okHome

set "PRG=%~nx0"

set "EXECUTABLE=%CATALINA_HOME%\bin\catalina.bat"

rem Check that target executable exists
if exist "%EXECUTABLE%" goto okExec
echo Cannot find "%EXECUTABLE%"
echo This file is needed to run this program
goto end
:okExec

set COMMAND=run

rem Process command line parameters
:setArgs
if ""%1""=="""" (
  goto doneSetArgs
) else (
if /I "%1" EQU "--dev" (
  SET EXO_DEV=true
) else (
if /I "%1" EQU "--debug" (
  SET EXO_DEBUG=true
) else (
if /I "%1" EQU "--data" (
  if ""%2""=="""" (
    echo Missing value for option --data
    echo(
    goto usage
  )
  SET EXO_DATA_DIR=%2
  shift
) else (
if /I "%1" EQU "--background" (
  SET COMMAND=start
  rem Don't activate console logs if launched as background task
  IF NOT DEFINED EXO_LOGS_DISPLAY_CONSOLE SET EXO_LOGS_DISPLAY_CONSOLE=false
) else (
if /I "%1" EQU "-b" (
  SET COMMAND=start
  rem Don't activate console logs if launched as background task
  IF NOT DEFINED EXO_LOGS_DISPLAY_CONSOLE SET EXO_LOGS_DISPLAY_CONSOLE=false
) else (
if /I "%1" EQU "--color" (
  rem Enforce colors in console
  SET EXO_LOGS_COLORIZED_CONSOLE=true
) else (
if /I "%1" EQU "-c" (
  rem Enforce colors in console
  SET EXO_LOGS_COLORIZED_CONSOLE=true
) else (
if /I "%1" EQU "--nocolor" (
  rem Enforce no colors in console
  SET EXO_LOGS_COLORIZED_CONSOLE=false
) else (
if /I "%1" EQU "-nc" (
  rem Enforce no colors in console
  SET EXO_LOGS_COLORIZED_CONSOLE=false
) else (
if /I "%1" EQU "--help" (
  goto usage
) else (
if /I "%1" EQU "-h" (
  goto usage
) else (
  echo Invalid option !
  echo(
  goto usage
))))))))))))
shift
goto setArgs
:doneSetArgs
rem Activate console logs if we aren't in background
IF NOT DEFINED EXO_LOGS_DISPLAY_CONSOLE SET EXO_LOGS_DISPLAY_CONSOLE=true
goto start

:usage
  echo Usage: %PRG% [options]
  echo(
  echo     Starts eXo Platform
  echo(
  echo options:
  echo(
  echo   --debug            Starts with JVM Debugger (Use %%EXO_DEBUG_PORT%% to change the port. 8000 by default)
  echo   --dev              Starts with Platform developer mode
  echo   --data <path>      Defines a specific directory where to store data (can also be done by setting the environment variable EXO_DATA_DIR)
  echo   -c, --color        Enforce to use colorized logs in console. (By default colors are activated on non-windows systems)
  echo   -nc, --nocolor     Enforce to not use colorized logs in console. (By default colors are activated on non-windows systems)
  echo   -b, --background   Starts as a background process. Use stop_eXo.sh to stop it. Console logs are deactivated.
  echo   -h, --help         This help message
  goto end
:doneUsage

:start
call "%EXECUTABLE%" %COMMAND%
:doneStart

:end
