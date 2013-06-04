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

rem Guess PRODUCT_HOME if not defined
set "CURRENT_DIR=%cd%"
if not "%PRODUCT_HOME%" == "" goto gotHome
set "PRODUCT_HOME=%CURRENT_DIR%"
if exist "%PRODUCT_HOME%\extensions" goto okHome
set "PRODUCT_HOME=%cd%"
cd "%CURRENT_DIR%"
:gotHome

if exist "%PRODUCT_HOME%\extensions" goto okHome
echo The PRODUCT_HOME environment variable is not defined correctly
echo This environment variable is needed to run this program
goto end
:okHome

rem ---------------------------------------------------------------------------
rem Set JAVA_HOME or JRE_HOME if not already set, ensure any provided settings
rem are valid.
rem
rem From Apache Tomcat
rem ---------------------------------------------------------------------------

rem Make sure prerequisite environment variables are set

rem Otherwise either JRE or JDK are fine
if not "%JRE_HOME%" == "" goto gotJreHome
if not "%JAVA_HOME%" == "" goto gotJavaHome
echo Neither the JAVA_HOME nor the JRE_HOME environment variable is defined
echo At least one of these environment variable is needed to run this program
goto exit

:needJavaHome
rem Check if we have a usable JDK
if "%JAVA_HOME%" == "" goto noJavaHome
if not exist "%JAVA_HOME%\bin\java.exe" goto noJavaHome
if not exist "%JAVA_HOME%\bin\javaw.exe" goto noJavaHome
if not exist "%JAVA_HOME%\bin\jdb.exe" goto noJavaHome
if not exist "%JAVA_HOME%\bin\javac.exe" goto noJavaHome
set "JRE_HOME=%JAVA_HOME%"
goto okJava

:noJavaHome
echo The JAVA_HOME environment variable is not defined correctly.
echo It is needed to run this program in debug mode.
echo NB: JAVA_HOME should point to a JDK not a JRE.
goto exit

:gotJavaHome
rem No JRE given, use JAVA_HOME as JRE_HOME
set "JRE_HOME=%JAVA_HOME%"

:gotJreHome
rem Check if we have a usable JRE
if not exist "%JRE_HOME%\bin\java.exe" goto noJreHome
if not exist "%JRE_HOME%\bin\javaw.exe" goto noJreHome
goto okJava

:noJreHome
rem Needed at least a JRE
echo The JRE_HOME environment variable is not defined correctly
echo This environment variable is needed to run this program
goto exit

:okJava
rem Set standard command for invoking Java.
rem Note that NT requires a window name argument when using start.
rem Also note the quoting as JAVA_HOME may contain spaces.
set _RUNJAVA="%JRE_HOME%\bin\java"
set _RUNJDB="%JAVA_HOME%\bin\jdb"

rem Get remaining unshifted command line arguments and save them in the
set CMD_LINE_ARGS=
:setArgs
if ""%1""=="""" goto doneSetArgs
set CMD_LINE_ARGS=%CMD_LINE_ARGS% %1
shift
goto setArgs
:doneSetArgs

set _PLF_LIBRARIES_PATH="${platform.libraries.path}"
set _PLF_WEBAPPS_PATH="${platform.webapps.path}"

:execCmd
%_RUNJAVA% -Dproduct.home="%PRODUCT_HOME%" -Dplatform.libraries.path="%_PLF_LIBRARIES_PATH%" -Dplatform.webapps.path="%_PLF_WEBAPPS_PATH%" -jar "%PRODUCT_HOME%\extensions\plf-extensions-manager.jar" %CMD_LINE_ARGS%
goto end

:exit
exit /b 1

:end
exit /b 0