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

REM Copyright (C) 2013 eXo Platform SAS.
REM 
REM This is free software; you can redistribute it and/or modify it
REM under the terms of the GNU Lesser General Public License as
REM published by the Free Software Foundation; either version 2.1 of
REM the License, or (at your option) any later version.
REM 
REM This software is distributed in the hope that it will be useful,
REM but WITHOUT ANY WARRANTY; without even the implied warranty of
REM MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
REM Lesser General Public License for more details.
REM 
REM You should have received a copy of the GNU Lesser General Public
REM License along with this software; if not, write to the Free
REM Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
REM 02110-1301 USA, or see the FSF site: http://www.fsf.org.

REM ---------------------------------------------------------------------------
REM            /!\     DON'T MODIFY THIS FILE      /!\
REM ---------------------------------------------------------------------------

REM ---------------------------------------------------------------------------
REM Settings customisation
REM ---------------------------------------------------------------------------
REM You have 2 ways to customize your installation settings :
REM 1- Rename the file setenv-customize.sample.bat to setenv-customize.bat and uncomment/change values
REM 2- Use system environment variables of your system or local shell
REM ---------------------------------------------------------------------------

rem Get standard environment variables
if not exist "%CATALINA_BASE%\bin\setenv-customize.bat" goto setEnvCustomize
call "%CATALINA_BASE%\bin\setenv-customize.bat"

:setEnvCustomize
REM We validate that Command extensions are available
VERIFY other 2>nul
SETLOCAL enableextensions
IF ERRORLEVEL 1 (
  ECHO Unable to enable extensions
  exit 1
)
ENDLOCAL

REM ---------------------------------------------------------------------------
REM Default EXO PLATFORM configuration
REM ---------------------------------------------------------------------------
IF NOT DEFINED EXO_PROFILES SET EXO_PROFILES=default
IF NOT DEFINED EXO_CONF_DIR_NAME SET EXO_CONF_DIR_NAME=gatein\conf
IF NOT DEFINED EXO_CONF_DIR SET EXO_CONF_DIR=%CATALINA_HOME%\%EXO_CONF_DIR_NAME%
IF NOT DEFINED EXO_DEBUG SET EXO_DEBUG=false
IF NOT DEFINED EXO_DEBUG_PORT SET EXO_DEBUG_PORT=8000
IF NOT DEFINED EXO_DEV SET EXO_DEV=false
IF NOT DEFINED EXO_ASSETS_VERSION SET EXO_ASSETS_VERSION=${project.version}

REM ---------------------------------------------------------------------------
REM Default Logs configuration
REM ---------------------------------------------------------------------------
REM Default configuration for logs (using logback framework - http://logback.qos.ch/manual/configuration.html )
IF NOT DEFINED EXO_LOGS_LOGBACK_CONFIG_FILE SET EXO_LOGS_LOGBACK_CONFIG_FILE=%CATALINA_HOME%/conf/logback.xml
IF NOT DEFINED EXO_LOGS_DISPLAY_CONSOLE SET EXO_LOGS_DISPLAY_CONSOLE=false
IF NOT DEFINED EXO_LOGS_CONSOLE_COLORIZED SET EXO_LOGS_CONSOLE_COLORIZED=

REM ---------------------------------------------------------------------------
REM Default JVM configuration
REM ---------------------------------------------------------------------------
IF NOT DEFINED EXO_JVM_SIZE_MAX SET EXO_JVM_SIZE_MAX=1g
IF NOT DEFINED EXO_JVM_SIZE_MIN SET EXO_JVM_SIZE_MIN=512m
IF NOT DEFINED EXO_JVM_PERMSIZE_MAX SET EXO_JVM_PERMSIZE_MAX=256m
IF NOT DEFINED EXO_JVM_PERMSIZE_MIN SET EXO_JVM_PERMSIZE_MIN=128m

REM ---------------------------------------------------------------------------
REM Default Tomcat configuration
REM ---------------------------------------------------------------------------
REM Global Tomcat settings
IF NOT DEFINED EXO_TOMCAT_UNPACK_WARS SET EXO_TOMCAT_UNPACK_WARS=%EXO_DEV%

REM ---------------------------------------------------------------------------
REM Export the needed system properties for server.xml
REM ---------------------------------------------------------------------------
SET JAVA_OPTS=%JAVA_OPTS% -DEXO_TOMCAT_UNPACK_WARS=%EXO_TOMCAT_UNPACK_WARS% -DEXO_DEV=%EXO_DEV%

REM ---------------------------------------------------------------------------
REM Logs customization (Managed by slf4J\logback instead of tomcat-juli & co)
REM ---------------------------------------------------------------------------
REM Deactivate j.u.l
SET LOGGING_MANAGER=-Dnop
REM Add additional bootstrap entries for logging purpose using SLF4J+Logback
REM SLF4J deps
SET CLASSPATH=%CLASSPATH%;%CATALINA_HOME%\lib\slf4j-api-${org.slf4j.version}.jar
SET CLASSPATH=%CLASSPATH%;%CATALINA_HOME%\lib\jul-to-slf4j-${org.slf4j.version}.jar
REM LogBack deps
SET CLASSPATH=%CLASSPATH%;%CATALINA_HOME%\lib\logback-core-${ch.qas.logback.version}.jar
SET CLASSPATH=%CLASSPATH%;%CATALINA_HOME%\lib\logback-classic-${ch.qas.logback.version}.jar
REM Janino deps (used by logback for conditional processing in the config file)
SET CLASSPATH=%CLASSPATH%;%CATALINA_HOME%\lib\janino-${org.codehaus.janino.version}.jar
SET CLASSPATH=%CLASSPATH%;%CATALINA_HOME%\lib\commons-compiler-${org.codehaus.janino.version}.jar
REM Jansi deps for colorized output on windows
SET CLASSPATH=%CLASSPATH%;%CATALINA_HOME%\lib\jansi-${org.fusesource.jansi.version}.jar

REM ---------------------------------------------------------------------------
REM Compute the CATALINA_OPTS
REM ---------------------------------------------------------------------------
IF /I %EXO_DEBUG% EQU true (
  SET CATALINA_OPTS=%CATALINA_OPTS% -Xrunjdwp:transport=dt_socket,address=%EXO_DEBUG_PORT%,server=y,suspend=n
)
IF /I %EXO_DEV% EQU true (
  SET CATALINA_OPTS=%CATALINA_OPTS% -Dorg.exoplatform.container.configuration.debug
  SET CATALINA_OPTS=%CATALINA_OPTS% -Dexo.product.developing=true
)
SET CATALINA_OPTS=%CATALINA_OPTS% -Xms%EXO_JVM_SIZE_MIN% -Xmx%EXO_JVM_SIZE_MAX% -XX:MaxPermSize=%EXO_JVM_PERMSIZE_MAX%
SET CATALINA_OPTS=%CATALINA_OPTS% -Dexo.profiles=%EXO_PROFILES%
SET CATALINA_OPTS=%CATALINA_OPTS% -Djava.security.auth.login.config="%CATALINA_HOME%\conf\jaas.conf"
SET CATALINA_OPTS=%CATALINA_OPTS% -Dexo.conf.dir.name="%EXO_CONF_DIR_NAME%" -Dexo.conf.dir="%EXO_CONF_DIR%"
SET CATALINA_OPTS=%CATALINA_OPTS% -Djavasrc="%JAVA_HOME%\src.zip" -Djre.lib="%JAVA_HOME%\jre\lib"
SET CATALINA_OPTS=%CATALINA_OPTS% -Dgatein.assets.version=%EXO_ASSETS_VERSION%
REM Logback configuration file
SET CATALINA_OPTS=%CATALINA_OPTS% -Dlogback.configurationFile="%EXO_LOGS_LOGBACK_CONFIG_FILE%"
REM Define the XML Parser
SET CATALINA_OPTS=%CATALINA_OPTS% -Djavax.xml.stream.XMLOutputFactory=com.sun.xml.internal.stream.XMLOutputFactoryImpl -Djavax.xml.stream.XMLInputFactory=com.sun.xml.internal.stream.XMLInputFactoryImpl -Djavax.xml.stream.XMLEventFactory=com.sun.xml.internal.stream.events.XMLEventsFactoryImpl
SET CATALINA_OPTS=%CATALINA_OPTS% -Djava.net.preferIPv4Stack=true
REM Disable EHCache update checker
SET CATALINA_OPTS=%CATALINA_OPTS% -Dnet.sf.ehcache.skipUpdateCheck=true
REM Disable Quartz update checker
SET CATALINA_OPTS=%CATALINA_OPTS% -Dorg.terracotta.quartz.skipUpdateCheck=true

REM Set the window name
SET TITLE=eXo Platform ${project.version}