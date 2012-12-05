#!/bin/sh

#
# Copyright (C) 2012 eXo Platform SAS.
# 
# This is free software; you can redistribute it and/or modify it
# under the terms of the GNU Lesser General Public License as
# published by the Free Software Foundation; either version 2.1 of
# the License, or (at your option) any later version.
# 
# This software is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
# Lesser General Public License for more details.
# 
# You should have received a copy of the GNU Lesser General Public
# License along with this software; if not, write to the Free
# Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
# 02110-1301 USA, or see the FSF site: http://www.fsf.org.
#

########################################
# Settings customisation
########################################
# You have 2 ways to customize your installation settings :
# 1- uncomment and change value to override settings in the above section
# 2- use environment properties of the system to override the value
########################################
#EXO_JVM_VENDOR="IBM"
#EXO_JVM_SIZE_MAX=2g
#EXO_JVM_SIZE_MIN=1g
#EXO_PROFILES="default"
#EXO_HTTP_COMPRESSION="off"


#=============================================================================#
#           /!\     DON'T MODIFY BESIDE THIS LINE      /!\                    #
#=============================================================================#

########################################
# Default EXO PLATFORM configuration
########################################
EXO_PROFILES=${EXO_PROFILES:-"default"}
EXO_CONF_DIR_NAME=${EXO_CONF_DIR_NAME:-"gatein/conf"}
EXO_CONF_DIR=${EXO_CONF_DIR:-"${CATALINA_HOME}/${EXO_CONF_DIR_NAME}"}


########################################
# Default Logs configuration
########################################
# Default configuration for logs (using logback framework - http://logback.qos.ch/manual/configuration.html )
EXO_LOGS_LOGBACK_CONFIG_FILE=${EXO_LOGS_LOGBACK_CONFIG_FILE:-${CATALINA_HOME}/conf/logback.xml}

########################################
# Default JVM configuration
########################################
EXO_JVM_VENDOR=${EXO_JVM_VENDOR:-"ORACLE"}
EXO_JVM_SIZE_MAX=${EXO_JVM_SIZE_MAX:-1g}
EXO_JVM_SIZE_MIN=${EXO_JVM_SIZE_MIN:-512m}
EXO_JVM_PERMSIZE_MAX=${EXO_JVM_PERMSIZE_MAX:-256m}
EXO_JVM_PERMSIZE_MIN=${EXO_JVM_PERMSIZE_MIN:-128m}

########################################
# Default Tomcat configuration
########################################
# Global Tomcat settings
EXO_TOMCAT_SHUTDOWN_PORT=${EXO_TOMCAT_SHUTDOWN_PORT:-8005}
EXO_TOMCAT_SHUTDOWN_KEY=${EXO_TOMCAT_SHUTDOWN_KEY:-"SHUTDOWN"}
EXO_TOMCAT_REDIRECT_PORT=${EXO_TOMCAT_REDIRECT_PORT:-8443}
EXO_TOMCAT_URI_ENCODING=${EXO_TOMCAT_URI_ENCODING:-"UTF-8"}
EXO_TOMCAT_RMI_REGISTRY_PORT=${EXO_TOMCAT_RMI_REGISTRY_PORT:-10001}
EXO_TOMCAT_RMI_SERVER_PORT=${EXO_TOMCAT_RMI_SERVER_PORT:-10002}
EXO_TOMCAT_RMI_LOCAL_PORT=${EXO_TOMCAT_RMI_LOCAL_PORT:-"false"}
EXO_TOMCAT_JVMROUTE_NAME=${EXO_TOMCAT_JVMROUTE_NAME:-""}

# HTTP configuration
EXO_HTTP_PORT=${EXO_HTTP_PORT:-8080}
EXO_HTTP_ADDRESS=${EXO_HTTP_ADDRESS:-0.0.0.0}
EXO_HTTP_PROTOCOL=${EXO_HTTP_PROTOCOL:-"org.apache.coyote.http11.Http11NioProtocol"}
EXO_HTTP_CX_TIMEOUT=${EXO_HTTP_CX_TIMEOUT:-20000}
EXO_HTTP_COMPRESSION=${EXO_HTTP_COMPRESSION:-"off"}
EXO_HTTP_COMPRESSION_MIN_SIZE=${EXO_HTTP_COMPRESSION_MIN_SIZE:-2048}

# AJP configuration
EXO_AJP_PORT=${EXO_AJP_PORT:-8009}
EXO_AJP_ADDRESS=${EXO_AJP_ADDRESS:-0.0.0.0}
EXO_AJP_PROTOCOL=${EXO_AJP_PROTOCOL:-"AJP/1.3"}
EXO_AJP_CX_TIMEOUT=${EXO_AJP_CX_TIMEOUT:-20000}

########################################
# Default Datasources configuration
########################################
# Datasource IDM
EXO_DS_IDM_DRIVER=${EXO_DS_IDM_DRIVER:-"org.hsqldb.jdbcDriver"}
EXO_DS_IDM_USERNAME=${EXO_DS_IDM_USERNAME:-"sa"}
EXO_DS_IDM_PASSWORD=${EXO_DS_IDM_PASSWORD:-""}
EXO_DS_IDM_MAX_ACTIVE=${EXO_DS_IDM_MAX_ACTIVE:-20}
EXO_DS_IDM_MAX_IDLE=${EXO_DS_IDM_MAX_IDLE:-10}
EXO_DS_IDM_MAX_WAIT=${EXO_DS_IDM_MAX_WAIT:-10000}
EXO_DS_IDM_URL=${EXO_DS_IDM_URL:-"jdbc:hsqldb:file:${CATALINA_HOME}/gatein/data/hsql/exo-idm_portal"}

# Datasource PORTAL
EXO_DS_PORTAL_DRIVER=${EXO_DS_PORTAL_DRIVER:-"org.hsqldb.jdbcDriver"}
EXO_DS_PORTAL_USERNAME=${EXO_DS_PORTAL_USERNAME:-"sa"}
EXO_DS_PORTAL_PASSWORD=${EXO_DS_PORTAL_PASSWORD:-""}
EXO_DS_PORTAL_MAX_ACTIVE=${EXO_DS_PORTAL_MAX_ACTIVE:-20}
EXO_DS_PORTAL_MAX_IDLE=${EXO_DS_PORTAL_MAX_IDLE:-10}
EXO_DS_PORTAL_MAX_WAIT=${EXO_DS_PORTAL_MAX_WAIT:-10000}
EXO_DS_PORTAL_URL=${EXO_DS_PORTAL_URL:-"jdbc:hsqldb:file:${CATALINA_HOME}/gatein/data/hsql/exo-jcr_portal"}

########################################
# Export the needed system properties for server.xml
########################################
EXO_SERVER_XML_OPTS="${EXO_SERVER_XML_OPTS} -DEXO_TOMCAT_SHUTDOWN_PORT=${EXO_TOMCAT_SHUTDOWN_PORT}"
EXO_SERVER_XML_OPTS="${EXO_SERVER_XML_OPTS} -DEXO_TOMCAT_SHUTDOWN_KEY=${EXO_TOMCAT_SHUTDOWN_KEY}"
EXO_SERVER_XML_OPTS="${EXO_SERVER_XML_OPTS} -DEXO_TOMCAT_REDIRECT_PORT=${EXO_TOMCAT_REDIRECT_PORT}"
EXO_SERVER_XML_OPTS="${EXO_SERVER_XML_OPTS} -DEXO_TOMCAT_URI_ENCODING=${EXO_TOMCAT_URI_ENCODING}"
EXO_SERVER_XML_OPTS="${EXO_SERVER_XML_OPTS} -DEXO_TOMCAT_RMI_REGISTRY_PORT=${EXO_TOMCAT_RMI_REGISTRY_PORT}"
EXO_SERVER_XML_OPTS="${EXO_SERVER_XML_OPTS} -DEXO_TOMCAT_RMI_SERVER_PORT=${EXO_TOMCAT_RMI_SERVER_PORT}"
EXO_SERVER_XML_OPTS="${EXO_SERVER_XML_OPTS} -DEXO_TOMCAT_RMI_LOCAL_PORT=${EXO_TOMCAT_RMI_LOCAL_PORT}"
EXO_SERVER_XML_OPTS="${EXO_SERVER_XML_OPTS} -DEXO_TOMCAT_JVMROUTE_NAME=${EXO_TOMCAT_JVMROUTE_NAME}"

EXO_SERVER_XML_OPTS="${EXO_SERVER_XML_OPTS} -DEXO_HTTP_PORT=${EXO_HTTP_PORT}"
EXO_SERVER_XML_OPTS="${EXO_SERVER_XML_OPTS} -DEXO_HTTP_ADDRESS=${EXO_HTTP_ADDRESS}"
EXO_SERVER_XML_OPTS="${EXO_SERVER_XML_OPTS} -DEXO_HTTP_PROTOCOL=${EXO_HTTP_PROTOCOL}"
EXO_SERVER_XML_OPTS="${EXO_SERVER_XML_OPTS} -DEXO_HTTP_CX_TIMEOUT=${EXO_HTTP_CX_TIMEOUT}"
EXO_SERVER_XML_OPTS="${EXO_SERVER_XML_OPTS} -DEXO_HTTP_COMPRESSION=${EXO_HTTP_COMPRESSION}"
EXO_SERVER_XML_OPTS="${EXO_SERVER_XML_OPTS} -DEXO_HTTP_COMPRESSION_MIN_SIZE=${EXO_HTTP_COMPRESSION_MIN_SIZE}"

EXO_SERVER_XML_OPTS="${EXO_SERVER_XML_OPTS} -DEXO_AJP_PORT=${EXO_AJP_PORT}"
EXO_SERVER_XML_OPTS="${EXO_SERVER_XML_OPTS} -DEXO_AJP_ADDRESS=${EXO_AJP_ADDRESS}"
EXO_SERVER_XML_OPTS="${EXO_SERVER_XML_OPTS} -DEXO_AJP_PROTOCOL=${EXO_AJP_PROTOCOL}"
EXO_SERVER_XML_OPTS="${EXO_SERVER_XML_OPTS} -DEXO_AJP_CX_TIMEOUT=${EXO_AJP_CX_TIMEOUT}"

EXO_SERVER_XML_OPTS="${EXO_SERVER_XML_OPTS} -DEXO_DS_IDM_DRIVER=${EXO_DS_IDM_DRIVER}"
EXO_SERVER_XML_OPTS="${EXO_SERVER_XML_OPTS} -DEXO_DS_IDM_USERNAME=${EXO_DS_IDM_USERNAME}"
EXO_SERVER_XML_OPTS="${EXO_SERVER_XML_OPTS} -DEXO_DS_IDM_PASSWORD=${EXO_DS_IDM_PASSWORD}"
EXO_SERVER_XML_OPTS="${EXO_SERVER_XML_OPTS} -DEXO_DS_IDM_MAX_ACTIVE=${EXO_DS_IDM_MAX_ACTIVE}"
EXO_SERVER_XML_OPTS="${EXO_SERVER_XML_OPTS} -DEXO_DS_IDM_MAX_IDLE=${EXO_DS_IDM_MAX_IDLE}"
EXO_SERVER_XML_OPTS="${EXO_SERVER_XML_OPTS} -DEXO_DS_IDM_MAX_WAIT=${EXO_DS_IDM_MAX_WAIT}"
EXO_SERVER_XML_OPTS="${EXO_SERVER_XML_OPTS} -DEXO_DS_IDM_URL=${EXO_DS_IDM_URL}"

EXO_SERVER_XML_OPTS="${EXO_SERVER_XML_OPTS} -DEXO_DS_PORTAL_DRIVER=${EXO_DS_PORTAL_DRIVER}"
EXO_SERVER_XML_OPTS="${EXO_SERVER_XML_OPTS} -DEXO_DS_PORTAL_USERNAME=${EXO_DS_PORTAL_USERNAME}"
EXO_SERVER_XML_OPTS="${EXO_SERVER_XML_OPTS} -DEXO_DS_PORTAL_PASSWORD=${EXO_DS_PORTAL_PASSWORD}"
EXO_SERVER_XML_OPTS="${EXO_SERVER_XML_OPTS} -DEXO_DS_PORTAL_MAX_ACTIVE=${EXO_DS_PORTAL_MAX_ACTIVE}"
EXO_SERVER_XML_OPTS="${EXO_SERVER_XML_OPTS} -DEXO_DS_PORTAL_MAX_IDLE=${EXO_DS_PORTAL_MAX_IDLE}"
EXO_SERVER_XML_OPTS="${EXO_SERVER_XML_OPTS} -DEXO_DS_PORTAL_MAX_WAIT=${EXO_DS_PORTAL_MAX_WAIT}"
EXO_SERVER_XML_OPTS="${EXO_SERVER_XML_OPTS} -DEXO_DS_PORTAL_URL=${EXO_DS_PORTAL_URL}"

#EXO_SERVER_XML_OPTS="${EXO_SERVER_XML_OPTS} -D=${}"

########################################
# Logs customization (Managed by slf4J/logback instead of tomcat-juli & co)
########################################
# Deactivate j.u.l
LOGGING_MANAGER=-Dnop
# Add additional bootstrap entries for logging purpose using SLF4J+Logback
# SLF4J deps
CLASSPATH="$CLASSPATH":"$CATALINA_HOME"/lib/slf4j-api-${org.slf4j.version}.jar
CLASSPATH="$CLASSPATH":"$CATALINA_HOME"/lib/jul-to-slf4j-${org.slf4j.version}.jar
# LogBack deps
CLASSPATH="$CLASSPATH":"$CATALINA_HOME"/lib/logback-core-${ch.qas.logback.version}.jar
CLASSPATH="$CLASSPATH":"$CATALINA_HOME"/lib/logback-classic-${ch.qas.logback.version}.jar
# Janino deps (used by logback for conditional processing in the config file)
CLASSPATH="$CLASSPATH":"$CATALINA_HOME"/lib/janino-${org.codehaus.janino.version}.jar
CLASSPATH="$CLASSPATH":"$CATALINA_HOME"/lib/commons-compiler-${org.codehaus.janino.version}.jar

########################################
# Compute the CATALINA_OPTS
########################################
CATALINA_OPTS="${CATALINA_OPTS} -Xms${EXO_JVM_SIZE_MIN} -Xmx${EXO_JVM_SIZE_MAX} -XX:MaxPermSize=${EXO_JVM_PERMSIZE_MAX}"
CATALINA_OPTS="${CATALINA_OPTS} -Dexo.profiles=${EXO_PROFILES}"
CATALINA_OPTS="${CATALINA_OPTS} -Djava.security.auth.login.config=${CATALINA_HOME}/conf/jaas.conf"
CATALINA_OPTS="${CATALINA_OPTS} -Dexo.conf.dir.name=${EXO_CONF_DIR_NAME} -Dexo.conf.dir=${EXO_CONF_DIR}"
CATALINA_OPTS="${CATALINA_OPTS} -Djavasrc=${JAVA_HOME}/src.zip -Djre.lib=${JAVA_HOME}/jre/lib"
# Logback configuration file
CATALINA_OPTS="${CATALINA_OPTS} -Dlogback.configurationFile=${EXO_LOGS_LOGBACK_CONFIG_FILE}"
# Define the XML Parser depending on the JVM vendor
if [ "${EXO_JVM_VENDOR}" = "IBM" ]; then
  CATALINA_OPTS="${CATALINA_OPTS} -Djavax.xml.stream.XMLOutputFactory=com.sun.xml.stream.ZephyrWriterFactory -Djavax.xml.stream.XMLInputFactory=com.sun.xml.stream.ZephyrParserFactory -Djavax.xml.stream.XMLEventFactory=com.sun.xml.stream.events.ZephyrEventFactory"
else
  CATALINA_OPTS="${CATALINA_OPTS} -Djavax.xml.stream.XMLOutputFactory=com.sun.xml.internal.stream.XMLOutputFactoryImpl -Djavax.xml.stream.XMLInputFactory=com.sun.xml.internal.stream.XMLInputFactoryImpl -Djavax.xml.stream.XMLEventFactory=com.sun.xml.internal.stream.events.XMLEventsFactoryImpl"
fi
CATALINA_OPTS="${CATALINA_OPTS} -Djava.net.preferIPv4Stack=true"
# Disable EHCache update checker
CATALINA_OPTS="${CATALINA_OPTS} -Dnet.sf.ehcache.skipUpdateCheck=true"
CATALINA_OPTS="${CATALINA_OPTS} ${EXO_SERVER_XML_OPTS}"
