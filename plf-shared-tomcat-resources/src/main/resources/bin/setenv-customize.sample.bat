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

REM ---------------------------------------------------------------------------
REM
REM Settings customisation
REM
REM Refer to eXo Platform Administrators Guide for more details.
REM
REM ---------------------------------------------------------------------------
REM You have 2 ways to customize your installation settings :
REM 1- Rename the file setenv-customize.sample.bat to setenv-customize.bat and uncomment/change values below
REM 2- Use system environment variables of your system or local shell
REM ---------------------------------------------------------------------------

REM ---------------------------------------------------------------------------
REM JVM configuration
REM ---------------------------------------------------------------------------

REM Home directory of the JVM to use (Default : try to auto-compute it from existing java executable in path by default)
REM Prefer to use the shortname syntax without spaces for the PATH
REM SET JAVA_HOME=C:\Progra~1\Java\jdk6

REM Maximum Heap Size to use (Default : 2g)
REM SET EXO_JVM_SIZE_MAX=1g

REM Minimum Heap Size to use (Default : 512m)
REM SET EXO_JVM_SIZE_MIN=512m

REM Size of the Permanent Generation. (Default : 256m)
REM SET EXO_JVM_PERMSIZE_MAX=128m

REM Default locale language
REM EXO_JVM_USER_LANGUAGE="fr"

REM Default locale region
REM EXO_JVM_USER_REGION="FR"

REM Loads in-process debugging libraries to attach a debugger (also available with --debug option on start_eXo.bat script)
REM SET EXO_DEBUG=true

REM Listening port for the debugger
REM SET EXO_DEBUG_PORT=8000

REM ---------------------------------------------------------------------------
REM PLATFORM configuration
REM ---------------------------------------------------------------------------

REM eXo Platform comes with different runtime profiles, enabling you to customize which modules you want to enable/disable
REM SET EXO_PROFILES=default,cluster,cluster-index-local

REM Assets versions used in static resources URLs. Useful to manage caches. (Default: The product version)
REM EXO_ASSETS_VERSION=42

REM Main directory where are stored all data (Default: %CATALINA_BASE%\gatein\data) (also available with --data <path> option on start_eXo.bat script)
REM SET EXO_DATA_DIR=%HOMEPATH%\eXo-platform\data

REM Activates the development mode of eXo platform (also available with --dev option on start_eXo.bat script)
REM SET EXO_DEV=true

REM Activates the JCR sessions leaks detector (Default: $EXO_DEV. true with --dev option on start_eXo.sh script)
REM EXO_JCR_SESSION_TRACKING=true

REM ---------------------------------------------------------------------------
REM Logs configuration
REM ---------------------------------------------------------------------------

REM Logback configuration file (http://logback.qos.ch/manual/configuration.html ) - For an advanced customization of logs
REM SET EXO_LOGS_LOGBACK_CONFIG_FILE=%CATALINA_BASE%/conf/logback.xml

REM SET EXO_LOGS_DISPLAY_CONSOLE=true

REM Enforce to display colorized logs in the console (Default: false for windows, true otherwise) (You can use --color and --nocolor options on start_eXo.bat to enforce the configuration)
REM SET EXO_LOGS_COLORIZED_CONSOLE=true

REM ---------------------------------------------------------------------------
REM Tomcat configuration
REM ---------------------------------------------------------------------------

REM Explodes all wars in the webapps directory (Default: $EXO_DEV. true with --dev option on start_eXo.bat script)
REM SET EXO_TOMCAT_UNPACK_WARS=%EXO_DEV%