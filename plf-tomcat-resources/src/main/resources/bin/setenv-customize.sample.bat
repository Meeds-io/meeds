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
REM
REM # Settings customization
REM
REM # Refer to eXo Platform Administrators Guide for more details.
REM # http://docs.exoplatform.com
REM
REM # ---------------------------------------------------------------------------
REM # You have 2 ways to customize your installation settings :
REM # 1- Rename the file setenv-customize.sample.bat to setenv-customize.bat and uncomment/change values below
REM # 2- Use system environment variables of your system or local shell
REM # ---------------------------------------------------------------------------

REM # ---------------------------------------------------------------------------
REM # JVM configuration
REM # ---------------------------------------------------------------------------

REM # Home directory of the JVM to use (Default : try to auto-compute it from existing java executable in path by default)
REM SET JAVA_HOME=C:\Program Files\Java\jdk6

REM # Maximum Heap Size to use (Default : 3g)
REM SET EXO_JVM_SIZE_MAX=4g

REM # Minimum Heap Size to use (Default : 512m)
REM SET EXO_JVM_SIZE_MIN=512m

REM # Size of the Permanent Generation. (Default : 256m)
REM SET EXO_JVM_PERMSIZE_MAX=128m

REM # Size of the Metaspace. (Default : 512m)
REM SET EXO_JVM_METASPACE_SIZE_MAX=256m

REM # Default locale language
REM SET EXO_JVM_USER_LANGUAGE=fr

REM # Default locale region
REM SET EXO_JVM_USER_REGION=FR

REM # Loads in-process debugging libraries to attach a debugger (also available with --debug option on start_eXo.bat script)
REM SET EXO_DEBUG=true

REM # Listening port for the debugger
REM SET EXO_DEBUG_PORT=8000

REM # ---------------------------------------------------------------------------
REM # PLATFORM configuration
REM # ---------------------------------------------------------------------------

REM # eXo Platform comes with different runtime profiles, enabling you to customize which modules you want to enable/disable. (Default: all)
REM SET EXO_PROFILES=all,myOwnProfile

REM # Main directory where are stored all configuration files (Default: %CATALINA_BASE%\gatein\conf)
REM SET EXO_CONF_DIR=%HOMEPATH%\eXo-platform\conf

REM # Main directory where are stored all data (Default: %CATALINA_BASE%\gatein\data) (also available with --data <path> option on start_eXo.bat script)
REM SET EXO_DATA_DIR=%HOMEPATH%\eXo-platform\data

REM # Activates the development mode of eXo platform (also available with --dev option on start_eXo.bat script)
REM SET EXO_DEV=true

REM # Activates the JCR sessions leaks detector (Default: $EXO_DEV. true with --dev option on start_eXo.sh script)
REM SET EXO_JCR_SESSION_TRACKING=true

REM # ---------------------------------------------------------------------------
REM # Logs configuration
REM # ---------------------------------------------------------------------------

REM # Logback configuration file (http://logback.qos.ch/manual/configuration.html ) - For an advanced customization of logs
REM SET EXO_LOGS_LOGBACK_CONFIG_FILE=%CATALINA_BASE%/conf/logback.xml

REM SET EXO_LOGS_DISPLAY_CONSOLE=true

REM # Enforce to display colorized logs in the console (Default: false for windows, true otherwise) (You can use --color and --nocolor options on start_eXo.bat to enforce the configuration)
REM SET EXO_LOGS_COLORIZED_CONSOLE=true

REM # ---------------------------------------------------------------------------
REM # Tomcat configuration
REM # ---------------------------------------------------------------------------

REM # Explodes all wars in the webapps directory (Default: $EXO_DEV. true with --dev option on start_eXo.bat script)
REM SET EXO_TOMCAT_UNPACK_WARS=%EXO_DEV%

REM # -----------------------------------------------------------------------------
REM # Advanced settings (We directly append some settings in CATALINA_OPTS)
REM # -----------------------------------------------------------------------------

REM # JVM HeapDumpOnOutOfMemoryError (Useful to debug but the dump is long to produce)
REM SET CATALINA_OPTS=%CATALINA_OPTS% -XX:+HeapDumpOnOutOfMemoryError
REM SET CATALINA_OPTS=%CATALINA_OPTS% -XX:HeapDumpPath="%CATALINA_HOME%/logs/"

REM # JVM GC Details
REM SET CATALINA_OPTS=%CATALINA_OPTS% -XX:+PrintGCDetails
REM SET CATALINA_OPTS=%CATALINA_OPTS% -Xloggc:"%CATALINA_HOME%/logs/gc.log"

REM # JMX (Sample of configuration to activate it without SSL and with access/password credentials)
REM SET CATALINA_OPTS=%CATALINA_OPTS% -Dcom.sun.management.jmxremote=true
REM SET CATALINA_OPTS=%CATALINA_OPTS% -Dcom.sun.management.jmxremote.ssl=false
REM SET CATALINA_OPTS=%CATALINA_OPTS% -Dcom.sun.management.jmxremote.password.file=%CATALINA_HOME%/conf/jmxremote.password
REM SET CATALINA_OPTS=%CATALINA_OPTS% -Dcom.sun.management.jmxremote.access.file=%CATALINA_HOME%/conf/jmxremote.access
REM # If you access to JMX though an SSH tunnel
REM SET CATALINA_OPTS=%CATALINA_OPTS% -Djava.rmi.server.hostname=localhost
REM # If you are behind a proxy you may have to activate the JMX Remote Lifecycle Listener fixes the ports used by the JMX/RMI Server
REM # For more details see the configuration file conf/server.xml

REM # JCR Statistics
REM SET CATALINA_OPTS=%CATALINA_OPTS% -DJDBCWorkspaceDataContainer.statistics.enabled=true -DJCRStatisticsManager.persistence.timeout=30000

REM # CRaSH add-on (change telnet and SSH ports)
REM SET "CATALINA_OPTS=%CATALINA_OPTS% -Dcrash.telnet.port=12345"
REM SET "CATALINA_OPTS=%CATALINA_OPTS% -Dcrash.ssh.port=54321"

REM # Hibernate dialect for supporting Postgresql Plus
REM SET "CATALINA_OPTS=%CATALINA_OPTS% -Dhibernate.dialect=org.hibernate.dialect.PostgresPlusDialect"

REM # ACME add-on (override default acme site data)
REM SET "CATALINA_OPTS=%CATALINA_OPTS% -Dexo.acme.portalConfig.metadata.override=true"

REM # IDE add-on (customize default ide add-on data)
REM SET "CATALINA_OPTS=%CATALINA_OPTS% -Dexo.ide.portalConfig.metadata.override=false"
