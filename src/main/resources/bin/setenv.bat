@echo off

rem Copyright (C) 2012 eXo Platform SAS.
rem 
rem This is free software; you can redistribute it and/or modify it
rem under the terms of the GNU Lesser General Public License as
rem published by the Free Software Foundation; either version 2.1 of
rem the License, or (at your option) any later version.
rem 
rem This software is distributed in the hope that it will be useful,
rem but WITHOUT ANY WARRANTY; without even the implied warranty of
rem MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
rem Lesser General Public License for more details.
rem 
rem You should have received a copy of the GNU Lesser General Public
rem License along with this software; if not, write to the Free
rem Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
rem 02110-1301 USA, or see the FSF site: http://www.fsf.org.

rem ########################################
rem Default EXO PLATFORM configuration
rem ########################################
set EXO_PROFILES=default
set EXO_CONF_DIR_NAME=gatein\conf
set EXO_CONF_DIR=%CATALINA_HOME%\%EXO_CONF_DIR_NAME%

rem ########################################
rem Default Logs configuration
rem ########################################
rem Default configuration for logs (using logback framework - http://logback.qos.ch/manual/configuration.html )
set EXO_LOGS_LOGBACK_CONFIG_FILE=%CATALINA_HOME%/conf/logback.xml

rem ########################################
rem Default JVM configuration
rem ########################################
set EXO_JVM_VENDOR=ORACLE
set EXO_JVM_SIZE_MAX=1g
set EXO_JVM_SIZE_MIN=512m
set EXO_JVM_PERMSIZE_MAX=256m
set EXO_JVM_PERMSIZE_MIN=128m

rem ########################################
rem Default Tomcat configuration
rem ########################################
rem Global Tomcat settings
set EXO_TOMCAT_SHUTDOWN_PORT=8005
set EXO_TOMCAT_SHUTDOWN_KEY=SHUTDOWN
set EXO_TOMCAT_REDIRECT_PORT=8443
set EXO_TOMCAT_URI_ENCODING=UTF-8
set EXO_TOMCAT_RMI_REGISTRY_PORT=10001
set EXO_TOMCAT_RMI_SERVER_PORT=10002
set EXO_TOMCAT_RMI_LOCAL_PORT=false
set EXO_TOMCAT_JVMROUTE_NAME=""

rem HTTP configuration
set EXO_HTTP_PORT=8080
set EXO_HTTP_ADDRESS=0.0.0.0
set EXO_HTTP_PROTOCOL=org.apache.coyote.http11.Http11NioProtocol
set EXO_HTTP_CX_TIMEOUT=20000
set EXO_HTTP_COMPRESSION=off
set EXO_HTTP_COMPRESSION_MIN_SIZE=2048

rem AJP configuration
set EXO_AJP_PORT=8009
set EXO_AJP_ADDRESS=0.0.0.0
set EXO_AJP_PROTOCOL=AJP/1.3
set EXO_AJP_CX_TIMEOUT=20000

rem ########################################
rem Default Datasources configuration
rem ########################################
rem Datasource IDM
set EXO_DS_IDM_DRIVER=org.hsqldb.jdbcDriver
set EXO_DS_IDM_USERNAME=sa
set EXO_DS_IDM_PASSWORD=""
set EXO_DS_IDM_MAX_ACTIVE=20
set EXO_DS_IDM_MAX_IDLE=10
set EXO_DS_IDM_MAX_WAIT=10000
set EXO_DS_IDM_URL="jdbc:hsqldb:file:%CATALINA_HOME%\gatein\data\hsql\exo-idm_portal"

rem Datasource PORTAL
set EXO_DS_PORTAL_DRIVER=org.hsqldb.jdbcDriver
set EXO_DS_PORTAL_USERNAME=sa
set EXO_DS_PORTAL_PASSWORD=""
set EXO_DS_PORTAL_MAX_ACTIVE=20
set EXO_DS_PORTAL_MAX_IDLE=10
set EXO_DS_PORTAL_MAX_WAIT=10000
set EXO_DS_PORTAL_URL="jdbc:hsqldb:file:%CATALINA_HOME%\gatein\data\hsql\exo-jcr_portal"

rem ########################################
rem Export the needed system properties for server.xml
rem ########################################
set EXO_SERVER_XML_OPTS=%EXO_SERVER_XML_OPTS% -DEXO_TOMCAT_SHUTDOWN_PORT=%EXO_TOMCAT_SHUTDOWN_PORT%
set EXO_SERVER_XML_OPTS=%EXO_SERVER_XML_OPTS% -DEXO_TOMCAT_SHUTDOWN_KEY=%EXO_TOMCAT_SHUTDOWN_KEY%
set EXO_SERVER_XML_OPTS=%EXO_SERVER_XML_OPTS% -DEXO_TOMCAT_REDIRECT_PORT=%EXO_TOMCAT_REDIRECT_PORT%
set EXO_SERVER_XML_OPTS=%EXO_SERVER_XML_OPTS% -DEXO_TOMCAT_URI_ENCODING=%EXO_TOMCAT_URI_ENCODING%
set EXO_SERVER_XML_OPTS=%EXO_SERVER_XML_OPTS% -DEXO_TOMCAT_RMI_REGISTRY_PORT=%EXO_TOMCAT_RMI_REGISTRY_PORT%
set EXO_SERVER_XML_OPTS=%EXO_SERVER_XML_OPTS% -DEXO_TOMCAT_RMI_SERVER_PORT=%EXO_TOMCAT_RMI_SERVER_PORT%
set EXO_SERVER_XML_OPTS=%EXO_SERVER_XML_OPTS% -DEXO_TOMCAT_RMI_LOCAL_PORT=%EXO_TOMCAT_RMI_LOCAL_PORT%
set EXO_SERVER_XML_OPTS=%EXO_SERVER_XML_OPTS% -DEXO_TOMCAT_JVMROUTE_NAME=%EXO_TOMCAT_JVMROUTE_NAME%

set EXO_SERVER_XML_OPTS=%EXO_SERVER_XML_OPTS% -DEXO_HTTP_PORT=%EXO_HTTP_PORT%
set EXO_SERVER_XML_OPTS=%EXO_SERVER_XML_OPTS% -DEXO_HTTP_ADDRESS=%EXO_HTTP_ADDRESS%
set EXO_SERVER_XML_OPTS=%EXO_SERVER_XML_OPTS% -DEXO_HTTP_PROTOCOL=%EXO_HTTP_PROTOCOL%
set EXO_SERVER_XML_OPTS=%EXO_SERVER_XML_OPTS% -DEXO_HTTP_CX_TIMEOUT=%EXO_HTTP_CX_TIMEOUT%
set EXO_SERVER_XML_OPTS=%EXO_SERVER_XML_OPTS% -DEXO_HTTP_COMPRESSION=%EXO_HTTP_COMPRESSION%
set EXO_SERVER_XML_OPTS=%EXO_SERVER_XML_OPTS% -DEXO_HTTP_COMPRESSION_MIN_SIZE=%EXO_HTTP_COMPRESSION_MIN_SIZE%

set EXO_SERVER_XML_OPTS=%EXO_SERVER_XML_OPTS% -DEXO_AJP_PORT=%EXO_AJP_PORT%
set EXO_SERVER_XML_OPTS=%EXO_SERVER_XML_OPTS% -DEXO_AJP_ADDRESS=%EXO_AJP_ADDRESS%
set EXO_SERVER_XML_OPTS=%EXO_SERVER_XML_OPTS% -DEXO_AJP_PROTOCOL=%EXO_AJP_PROTOCOL%
set EXO_SERVER_XML_OPTS=%EXO_SERVER_XML_OPTS% -DEXO_AJP_CX_TIMEOUT=%EXO_AJP_CX_TIMEOUT%

set EXO_SERVER_XML_OPTS=%EXO_SERVER_XML_OPTS% -DEXO_DS_IDM_DRIVER=%EXO_DS_IDM_DRIVER%
set EXO_SERVER_XML_OPTS=%EXO_SERVER_XML_OPTS% -DEXO_DS_IDM_USERNAME=%EXO_DS_IDM_USERNAME%
set EXO_SERVER_XML_OPTS=%EXO_SERVER_XML_OPTS% -DEXO_DS_IDM_PASSWORD=%EXO_DS_IDM_PASSWORD%
set EXO_SERVER_XML_OPTS=%EXO_SERVER_XML_OPTS% -DEXO_DS_IDM_MAX_ACTIVE=%EXO_DS_IDM_MAX_ACTIVE%
set EXO_SERVER_XML_OPTS=%EXO_SERVER_XML_OPTS% -DEXO_DS_IDM_MAX_IDLE=%EXO_DS_IDM_MAX_IDLE%
set EXO_SERVER_XML_OPTS=%EXO_SERVER_XML_OPTS% -DEXO_DS_IDM_MAX_WAIT=%EXO_DS_IDM_MAX_WAIT%
set EXO_SERVER_XML_OPTS=%EXO_SERVER_XML_OPTS% -DEXO_DS_IDM_URL=%EXO_DS_IDM_URL%

set EXO_SERVER_XML_OPTS=%EXO_SERVER_XML_OPTS% -DEXO_DS_PORTAL_DRIVER=%EXO_DS_PORTAL_DRIVER%
set EXO_SERVER_XML_OPTS=%EXO_SERVER_XML_OPTS% -DEXO_DS_PORTAL_USERNAME=%EXO_DS_PORTAL_USERNAME%
set EXO_SERVER_XML_OPTS=%EXO_SERVER_XML_OPTS% -DEXO_DS_PORTAL_PASSWORD=%EXO_DS_PORTAL_PASSWORD%
set EXO_SERVER_XML_OPTS=%EXO_SERVER_XML_OPTS% -DEXO_DS_PORTAL_MAX_ACTIVE=%EXO_DS_PORTAL_MAX_ACTIVE%
set EXO_SERVER_XML_OPTS=%EXO_SERVER_XML_OPTS% -DEXO_DS_PORTAL_MAX_IDLE=%EXO_DS_PORTAL_MAX_IDLE%
set EXO_SERVER_XML_OPTS=%EXO_SERVER_XML_OPTS% -DEXO_DS_PORTAL_MAX_WAIT=%EXO_DS_PORTAL_MAX_WAIT%
set EXO_SERVER_XML_OPTS=%EXO_SERVER_XML_OPTS% -DEXO_DS_PORTAL_URL=%EXO_DS_PORTAL_URL%

rem set EXO_SERVER_XML_OPTS=%EXO_SERVER_XML_OPTS% -D=%%

rem ########################################
rem Logs customization (Managed by slf4J\logback instead of tomcat-juli & co)
rem ########################################
rem Deactivate j.u.l
set LOGGING_MANAGER=-Dnop
rem Add additional bootstrap entries for logging purpose using SLF4J+Logback
rem SLF4J deps
set CLASSPATH=%CLASSPATH%;%CATALINA_HOME%\lib\slf4j-api-${org.slf4j.version}.jar
set CLASSPATH=%CLASSPATH%;%CATALINA_HOME%\lib\jul-to-slf4j-${org.slf4j.version}.jar
rem LogBack deps
set CLASSPATH=%CLASSPATH%;%CATALINA_HOME%\lib\logback-core-${ch.qas.logback.version}.jar
set CLASSPATH=%CLASSPATH%;%CATALINA_HOME%\lib\logback-classic-${ch.qas.logback.version}.jar
rem Janino deps (used by logback for conditional processing in the config file)
set CLASSPATH=%CLASSPATH%;%CATALINA_HOME%\lib\janino-${org.codehaus.janino.version}.jar
set CLASSPATH=%CLASSPATH%;%CATALINA_HOME%\lib\commons-compiler-${org.codehaus.janino.version}.jar
rem Jansi deps for colorized output on windows
set CLASSPATH=%CLASSPATH%;%CATALINA_HOME%\lib\jansi-${org.fusesource.jansi.version}.jar

rem ########################################
rem Compute the CATALINA_OPTS
rem ########################################
set CATALINA_OPTS=%CATALINA_OPTS% -Xms%EXO_JVM_SIZE_MIN% -Xmx%EXO_JVM_SIZE_MAX% -XX:MaxPermSize=%EXO_JVM_PERMSIZE_MAX%
set CATALINA_OPTS=%CATALINA_OPTS% -Dexo.profiles=%EXO_PROFILES%
set CATALINA_OPTS=%CATALINA_OPTS% -Djava.security.auth.login.config="%CATALINA_HOME%\conf\jaas.conf"
set CATALINA_OPTS=%CATALINA_OPTS% -Dexo.conf.dir.name="%EXO_CONF_DIR_NAME%" -Dexo.conf.dir="%EXO_CONF_DIR%"
set CATALINA_OPTS=%CATALINA_OPTS% -Djavasrc="%JAVA_HOME%\src.zip" -Djre.lib="%JAVA_HOME%\jre\lib"
rem Logback configuration file
set CATALINA_OPTS=%CATALINA_OPTS% -Dlogback.configurationFile="%EXO_LOGS_LOGBACK_CONFIG_FILE%"
rem Define the XML Parser depending on the JVM vendor
if %EXO_JVM_VENDOR%==IBM (
  set CATALINA_OPTS=%CATALINA_OPTS% -Djavax.xml.stream.XMLOutputFactory=com.sun.xml.stream.ZephyrWriterFactory -Djavax.xml.stream.XMLInputFactory=com.sun.xml.stream.ZephyrParserFactory -Djavax.xml.stream.XMLEventFactory=com.sun.xml.stream.events.ZephyrEventFactory
) else (
  set CATALINA_OPTS=%CATALINA_OPTS% -Djavax.xml.stream.XMLOutputFactory=com.sun.xml.internal.stream.XMLOutputFactoryImpl -Djavax.xml.stream.XMLInputFactory=com.sun.xml.internal.stream.XMLInputFactoryImpl -Djavax.xml.stream.XMLEventFactory=com.sun.xml.internal.stream.events.XMLEventsFactoryImpl
)
set CATALINA_OPTS=%CATALINA_OPTS% -Djava.net.preferIPv4Stack=true
rem Disable EHCache update checker
set CATALINA_OPTS=%CATALINA_OPTS% -Dnet.sf.ehcache.skipUpdateCheck=true
set CATALINA_OPTS=%CATALINA_OPTS% %EXO_SERVER_XML_OPTS%