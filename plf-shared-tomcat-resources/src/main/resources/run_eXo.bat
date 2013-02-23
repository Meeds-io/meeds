@echo off

@rem
@rem Copyright (C) 2012 eXo Platform SAS.
@rem
@rem This is free software; you can redistribute it and/or modify it
@rem under the terms of the GNU Lesser General Public License as
@rem published by the Free Software Foundation; either version 2.1 of
@rem the License, or (at your option) any later version.
@rem
@rem This software is distributed in the hope that it will be useful,
@rem but WITHOUT ANY WARRANTY; without even the implied warranty of
@rem MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
@rem Lesser General Public License for more details.
@rem
@rem You should have received a copy of the GNU Lesser General Public
@rem License along with this software; if not, write to the Free
@rem Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
@rem 02110-1301 USA, or see the FSF site: http://www.fsf.org.
@rem
 
if "%OS%" == "Windows_NT" setlocal

rem Guess CATALINA_HOME if not defined
set "CURRENT_DIR=%cd%"
if not "%CATALINA_HOME%" == "" goto gotHome
set "CATALINA_HOME=%CURRENT_DIR%"
if exist "%CATALINA_HOME%\bin\catalina.bat" goto okHome
cd ..
set "CATALINA_HOME=%cd%"
cd "%CURRENT_DIR%"
:gotHome
if exist "%CATALINA_HOME%\bin\catalina.bat" goto okHome
echo The CATALINA_HOME environment variable is not defined correctly
echo This environment variable is needed to run this program
goto end
:okHome

set "EXECUTABLE=%CATALINA_HOME%\bin\catalina.bat"

rem Check that target executable exists
if exist "%EXECUTABLE%" goto okExec
echo Cannot find "%EXECUTABLE%"
echo This file is needed to run this program
goto end
:okExec

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
if /I "%1" EQU "--help" (
  goto usage
) else (
if /I "%1" EQU "-h" (
  goto usage
) else (
  echo "Invalid option !"
  echo ""
  goto usage
)))))
shift
goto setArgs
:doneSetArgs
goto run

:usage
  echo "Usage: %~f0 [options]"
  echo ""
  echo "    Start Platform as foreground job"
  echo ""
  echo "options:"
  echo ""
  echo "  --debug      Start as foreground job with JVM Debugger (Use %%EXO_DEBUG_PORT%% to change the port. 8000 by default)"
  echo "  --dev        Start as foreground job with Platform developer mode"
  echo "  -h, --help   This help message"
  goto end
:doneUsage

:run
call "%EXECUTABLE%" run
:doneRun

:end
