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

@ECHO off

REM # ---------------------------------------------------------------------------
REM #            /!\     DON'T MODIFY THIS FILE      /!\
REM # ---------------------------------------------------------------------------
REM
REM # You mustn't do settings customizations here. Have a look at setenv-customize.sample.bat
REM
REM # Refer to eXo Platform Administrators Guide for more details.
REM # http://docs.exoplatform.com
REM
REM # ---------------------------------------------------------------------------
REM # You have 2 ways to customize your installation settings :
REM # 1- Rename the file setenv-customize.sample.bat to setenv-customize.bat and uncomment/change values
REM # 2- Use system environment variables of your system or local shell (Get the list in setenv-customize.sample.bat)
REM # ---------------------------------------------------------------------------
REM #            /!\     DON'T MODIFY THIS FILE      /!\
REM # ---------------------------------------------------------------------------

REM # We validate that Command extensions are available
VERIFY other 2>nul
SETLOCAL enableextensions
IF ERRORLEVEL 1 (
  ECHO Unable to enable extensions
  PAUSE
  EXIT 1
)
ENDLOCAL

REM # Load custom settings
IF NOT EXIST "%CATALINA_BASE%\bin\setenv-customize.bat" GOTO setEnvCustomize
call "%CATALINA_BASE%\bin\setenv-customize.bat"
:setEnvCustomize

REM # ---------------------------------------------------------------------------
REM # Default JVM configuration
REM # ---------------------------------------------------------------------------

IF NOT DEFINED EXO_JVM_SIZE_MAX SET EXO_JVM_SIZE_MAX=2g
IF NOT DEFINED EXO_JVM_SIZE_MIN SET EXO_JVM_SIZE_MIN=512m
IF NOT DEFINED EXO_JVM_PERMSIZE_MAX SET EXO_JVM_PERMSIZE_MAX=256m
IF NOT DEFINED EXO_JVM_USER_LANGUAGE SET EXO_JVM_USER_LANGUAGE=en
IF NOT DEFINED EXO_JVM_USER_REGION SET EXO_JVM_USER_REGION=US
IF NOT DEFINED EXO_DEBUG SET EXO_DEBUG=false
IF NOT DEFINED EXO_DEBUG_PORT SET EXO_DEBUG_PORT=8000

REM # ---------------------------------------------------------------------------
REM # Default EXO PLATFORM configuration
REM # ---------------------------------------------------------------------------

IF NOT DEFINED EXO_PROFILES SET EXO_PROFILES=all
IF NOT DEFINED EXO_DEV SET EXO_DEV=false
IF NOT DEFINED EXO_ASSETS_VERSION SET EXO_ASSETS_VERSION=${project.version}
IF NOT DEFINED EXO_JCR_SESSION_TRACKING SET EXO_JCR_SESSION_TRACKING=%EXO_DEV%
IF NOT DEFINED EXO_DATA_DIR SET EXO_DATA_DIR=%CATALINA_BASE%\gatein\data

REM # ---------------------------------------------------------------------------
REM # Default Logs configuration
REM # ---------------------------------------------------------------------------

REM # Default configuration for logs (using logback framework - http://logback.qos.ch/manual/configuration.html )
IF NOT DEFINED EXO_LOGS_LOGBACK_CONFIG_FILE SET EXO_LOGS_LOGBACK_CONFIG_FILE=%CATALINA_BASE%\conf\logback.xml
IF NOT DEFINED EXO_LOGS_DISPLAY_CONSOLE SET EXO_LOGS_DISPLAY_CONSOLE=false
IF NOT DEFINED EXO_LOGS_COLORIZED_CONSOLE SET EXO_LOGS_COLORIZED_CONSOLE=

REM # ---------------------------------------------------------------------------
REM # Default Tomcat configuration
REM # ---------------------------------------------------------------------------

REM # Global Tomcat settings
IF NOT DEFINED EXO_TOMCAT_UNPACK_WARS SET EXO_TOMCAT_UNPACK_WARS=%EXO_DEV%

REM # ---------------------------------------------------------------------------
REM # Export the needed system properties for server.xml
REM # ---------------------------------------------------------------------------

SET JAVA_OPTS=%JAVA_OPTS% -DEXO_TOMCAT_UNPACK_WARS=%EXO_TOMCAT_UNPACK_WARS% -DEXO_DEV=%EXO_DEV%

REM # ---------------------------------------------------------------------------
REM # Logs customization (Managed by slf4J\logback instead of tomcat-juli & co)
REM # ---------------------------------------------------------------------------

REM # Deactivate j.u.l
SET LOGGING_MANAGER=-Dnop
REM # Add additional bootstrap entries for logging purpose using SLF4J+Logback
REM # SLF4J deps
SET CLASSPATH=%CLASSPATH%;%CATALINA_HOME%\lib\slf4j-api-${org.slf4j.version}.jar
SET CLASSPATH=%CLASSPATH%;%CATALINA_HOME%\lib\jul-to-slf4j-${org.slf4j.version}.jar
REM # LogBack deps
SET CLASSPATH=%CLASSPATH%;%CATALINA_HOME%\lib\logback-core-${ch.qas.logback.version}.jar
SET CLASSPATH=%CLASSPATH%;%CATALINA_HOME%\lib\logback-classic-${ch.qas.logback.version}.jar
REM # Janino deps (used by logback for conditional processing in the config file)
SET CLASSPATH=%CLASSPATH%;%CATALINA_HOME%\lib\janino-${org.codehaus.janino.version}.jar
SET CLASSPATH=%CLASSPATH%;%CATALINA_HOME%\lib\commons-compiler-${org.codehaus.janino.version}.jar
REM # Jansi deps for colorized output on windows
SET CLASSPATH=%CLASSPATH%;%CATALINA_HOME%\lib\jansi-${org.fusesource.jansi.version}.jar

REM # ---------------------------------------------------------------------------
REM # Compute the CATALINA_OPTS
REM # ---------------------------------------------------------------------------

IF /I %EXO_DEBUG% EQU true (
  SET CATALINA_OPTS=%CATALINA_OPTS% -agentlib:jdwp=transport=dt_socket,address=%EXO_DEBUG_PORT%,server=y,suspend=n
)

IF /I %EXO_DEV% EQU true (
  SET CATALINA_OPTS=%CATALINA_OPTS% -Dorg.exoplatform.container.configuration.debug
  SET CATALINA_OPTS=%CATALINA_OPTS% -Dexo.product.developing=true
  SET CATALINA_OPTS=%CATALINA_OPTS% -Dignore.unregistered.webapp=false
)

REM # JCR session leak detector
SET CATALINA_OPTS=%CATALINA_OPTS% -Dexo.jcr.session.tracking.active=%EXO_JCR_SESSION_TRACKING%

REM # JVM memory allocation pool parameters
SET CATALINA_OPTS=%CATALINA_OPTS% -Xms%EXO_JVM_SIZE_MIN% -Xmx%EXO_JVM_SIZE_MAX% -XX:MaxPermSize=%EXO_JVM_PERMSIZE_MAX%

REM # Reduce the RMI GCs to once per hour for Sun JVMs.
SET CATALINA_OPTS=%CATALINA_OPTS% -Dsun.rmi.dgc.client.gcInterval=3600000 -Dsun.rmi.dgc.server.gcInterval=3600000

REM # Default user locale defined at JVM level
SET CATALINA_OPTS=%CATALINA_OPTS% -Duser.language=%EXO_JVM_USER_LANGUAGE% -Duser.region=%EXO_JVM_USER_REGION%

REM # Network settings
SET CATALINA_OPTS=%CATALINA_OPTS% -Djava.net.preferIPv4Stack=true

REM # Headless
SET CATALINA_OPTS=%CATALINA_OPTS% -Djava.awt.headless=true

REM # Platform profiles
SET CATALINA_OPTS=%CATALINA_OPTS% -Dexo.profiles=%EXO_PROFILES%

REM # Platform paths
SET CATALINA_OPTS=%CATALINA_OPTS% -Dexo.conf.dir.name=gatein\conf
SET CATALINA_OPTS=%CATALINA_OPTS% -Dexo.conf.dir="%CATALINA_BASE%\gatein\conf"
SET CATALINA_OPTS=%CATALINA_OPTS% -Dgatein.conf.dir="%CATALINA_BASE%\gatein\conf"
SET CATALINA_OPTS=%CATALINA_OPTS% -Djava.security.auth.login.config="%CATALINA_BASE%\conf\jaas.conf"
SET CATALINA_OPTS=%CATALINA_OPTS% -Dgatein.data.dir="%EXO_DATA_DIR%"
REM # JCR Data directory
SET CATALINA_OPTS=%CATALINA_OPTS% -Dgatein.jcr.data.dir="%EXO_DATA_DIR%\jcr"
REM # JCR values
SET CATALINA_OPTS=%CATALINA_OPTS% -Dgatein.jcr.storage.data.dir="%EXO_DATA_DIR%\jcr\values"
REM # JCR indexes
SET CATALINA_OPTS=%CATALINA_OPTS% -Dgatein.jcr.index.data.dir="%EXO_DATA_DIR%\jcr\index"


REM # Assets version
SET CATALINA_OPTS=%CATALINA_OPTS% -Dgatein.assets.version=%EXO_ASSETS_VERSION%

REM # Logback configuration file
SET CATALINA_OPTS=%CATALINA_OPTS% -Dlogback.configurationFile="%EXO_LOGS_LOGBACK_CONFIG_FILE%"

REM # Define the XML Parser
SET CATALINA_OPTS=%CATALINA_OPTS% -Djavax.xml.stream.XMLOutputFactory=com.sun.xml.internal.stream.XMLOutputFactoryImpl -Djavax.xml.stream.XMLInputFactory=com.sun.xml.internal.stream.XMLInputFactoryImpl -Djavax.xml.stream.XMLEventFactory=com.sun.xml.internal.stream.events.XMLEventsFactoryImpl

REM # PLF-4968/JCR-2164 : Avoid Exception when starting with Java 7 (http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6804124)
SET CATALINA_OPTS=%CATALINA_OPTS% -Djava.util.Arrays.useLegacyMergeSort=true

REM # Jod Converter activation
IF DEFINED EXO_JODCONVERTER_ENABLE SET CATALINA_OPTS=%CATALINA_OPTS% -Djodconverter.enable=%EXO_JODCONVERTER_ENABLE%
REM # Comma separated list of ports numbers to use for open office servers used to convert documents.
IF DEFINED EXO_JODCONVERTER_PORTS SET CATALINA_OPTS=%CATALINA_OPTS% -Djodconverter.portnumbers=%EXO_JODCONVERTER_PORTS%
REM # The absolute path to the office home on the server.
IF DEFINED EXO_JODCONVERTER_OFFICEHOME SET CATALINA_OPTS=%CATALINA_OPTS% -Djodconverter.officehome="%EXO_JODCONVERTER_OFFICEHOME%"

REM # Domain name used to produce absolute URLs in email notifications.
IF DEFINED EXO_DEPLOYMENT_URL SET CATALINA_OPTS=%CATALINA_OPTS% -Ddomain.url=%EXO_DEPLOYMENT_URL%
REM # Email display in from field of email notification.
IF DEFINED EXO_EMAIL_FROM SET CATALINA_OPTS=%CATALINA_OPTS% -Dsmtp.from=%EXO_EMAIL_FROM%
REM # SMTP Server hostname.
IF DEFINED EXO_EMAIL_SMTP_HOST SET CATALINA_OPTS=%CATALINA_OPTS% -Dsmtp.host=%EXO_EMAIL_SMTP_HOST%
REM # SMTP Server port.
IF DEFINED EXO_EMAIL_SMTP_PORT SET CATALINA_OPTS=%CATALINA_OPTS% -Dsmtp.port=%EXO_EMAIL_SMTP_PORT%
REM # True to enable the secure (TLS) SMTP. See RFC 3207.
IF DEFINED EXO_EMAIL_SMTP_STARTTLS_ENABLE SET CATALINA_OPTS=%CATALINA_OPTS% -Dsmtp.starttls.enable=%EXO_EMAIL_SMTP_STARTTLS_ENABLE%
REM # True to enable the SMTP authentication.
IF DEFINED EXO_EMAIL_SMTP_AUTH SET CATALINA_OPTS=%CATALINA_OPTS% -Dsmtp.auth=%EXO_EMAIL_SMTP_AUTH%
REM # Username to send for authentication.
IF DEFINED EXO_EMAIL_SMTP_USERNAME SET CATALINA_OPTS=%CATALINA_OPTS% -Dsmtp.username="%EXO_EMAIL_SMTP_USERNAME%"
REM # Password to send for authentication.
IF DEFINED EXO_EMAIL_SMTP_PASSWORD SET CATALINA_OPTS=%CATALINA_OPTS% -Dsmtp.password="%EXO_EMAIL_SMTP_PASSWORD%"
REM # Specify the port to connect to when using the specified socket factory.
IF DEFINED EXO_EMAIL_SMTP_SOCKET_FACTORY_PORT SET CATALINA_OPTS=%CATALINA_OPTS% -Dsmtp.socketFactory.port=%EXO_EMAIL_SMTP_SOCKET_FACTORY_PORT%
REM # This class will be used to create SMTP sockets.
IF DEFINED EXO_EMAIL_SMTP_SOCKET_FACTORY_CLASS SET CATALINA_OPTS=%CATALINA_OPTS% -Dsmtp.socketFactory.class=%EXO_EMAIL_SMTP_SOCKET_FACTORY_CLASS%

REM # Set the window name
SET TITLE=eXo Platform ${project.version}

:end
