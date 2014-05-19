#!/bin/sh
#
# Copyright (C) 2003-2013 eXo Platform SAS.
#
# This is free software; you can redistribute it and/or modify it
# under the terms of the GNU Lesser General Public License as
# published by the Free Software Foundation; either version 3 of
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

# -----------------------------------------------------------------------------
#                  /!\     DON'T MODIFY THIS FILE     /!\
# -----------------------------------------------------------------------------
#
# You mustn't do settings customizations here. Have a look at setenv-customize.sample.sh
#
# Refer to eXo Platform Administrators Guide for more details.
# http://docs.exoplatform.com
#
# -----------------------------------------------------------------------------
# You have 2 ways to customize your installation settings :
# 1- Rename the file setenv-customize.sample.sh to setenv-customize.sh and uncomment/change values
# 2- Use system environment variables of your system or local shell (Get the list in setenv-customize.sample.sh)
# -----------------------------------------------------------------------------
#                  /!\     DON'T MODIFY THIS FILE     /!\
# -----------------------------------------------------------------------------

case "`uname`" in
  CYGWIN*)
    echo "=========================================================="
    echo "Cygwin isn't supported. Please use .bat scripts on Windows"
    echo "=========================================================="
    exit -1;
  ;;
esac

# Load custom settings
if [ -r "$CATALINA_BASE/bin/setenv-customize.sh" ]; then
  . "$CATALINA_BASE/bin/setenv-customize.sh"
fi

# -----------------------------------------------------------------------------
# Default JVM configuration
# -----------------------------------------------------------------------------

[ -z $EXO_JVM_VENDOR ] && EXO_JVM_VENDOR="ORACLE"
[ -z $EXO_JVM_SIZE_MAX ] && EXO_JVM_SIZE_MAX="2g"
[ -z $EXO_JVM_SIZE_MIN ] && EXO_JVM_SIZE_MIN="512m"
[ -z $EXO_JVM_PERMSIZE_MAX ] && EXO_JVM_PERMSIZE_MAX="256m"
[ -z $EXO_JVM_USER_LANGUAGE ] && EXO_JVM_USER_LANGUAGE="en"
[ -z $EXO_JVM_USER_REGION ] && EXO_JVM_USER_REGION="US"
[ -z $EXO_DEBUG ] && EXO_DEBUG=false
[ -z $EXO_DEBUG_PORT ] && EXO_DEBUG_PORT="8000"

# -----------------------------------------------------------------------------
# Default EXO PLATFORM configuration
# -----------------------------------------------------------------------------

[ -z $EXO_PROFILES ] && EXO_PROFILES="all"
[ -z $EXO_DEV ] && EXO_DEV=false
[ -z $EXO_ASSETS_VERSION ] && EXO_ASSETS_VERSION="${project.version}"
[ -z $EXO_JCR_SESSION_TRACKING ] && EXO_JCR_SESSION_TRACKING=$EXO_DEV
[ -z $EXO_DATA_DIR ] && EXO_DATA_DIR="$CATALINA_BASE/gatein/data"

# -----------------------------------------------------------------------------
# Default Logs configuration
# -----------------------------------------------------------------------------

# Default configuration for logs (using logback framework - http://logback.qos.ch/manual/configuration.html )
[ -z $EXO_LOGS_LOGBACK_CONFIG_FILE ] && EXO_LOGS_LOGBACK_CONFIG_FILE="$CATALINA_BASE/conf/logback.xml"
[ -z $EXO_LOGS_DISPLAY_CONSOLE ] && EXO_LOGS_DISPLAY_CONSOLE=false
[ -z $EXO_LOGS_COLORIZED_CONSOLE ] && EXO_LOGS_COLORIZED_CONSOLE=""

# -----------------------------------------------------------------------------
# Default Tomcat configuration
# -----------------------------------------------------------------------------

# Global Tomcat settings
[ -z $EXO_TOMCAT_UNPACK_WARS ] && EXO_TOMCAT_UNPACK_WARS=$EXO_DEV

# -----------------------------------------------------------------------------
# Export the needed system properties for server.xml
# -----------------------------------------------------------------------------

JAVA_OPTS="$JAVA_OPTS -DEXO_TOMCAT_UNPACK_WARS=${EXO_TOMCAT_UNPACK_WARS} -DEXO_DEV=${EXO_DEV}"

# -----------------------------------------------------------------------------
# Logs customization (Managed by slf4J/logback instead of tomcat-juli & co)
# -----------------------------------------------------------------------------

# Deactivate j.u.l
LOGGING_MANAGER="-Dnop"
# Add additional bootstrap entries for logging purpose using SLF4J+Logback
# SLF4J deps
CLASSPATH="$CLASSPATH":"$CATALINA_HOME/lib/slf4j-api-${org.slf4j.version}.jar"
CLASSPATH="$CLASSPATH":"$CATALINA_HOME/lib/jul-to-slf4j-${org.slf4j.version}.jar"
# LogBack deps
CLASSPATH="$CLASSPATH":"$CATALINA_HOME/lib/logback-core-${ch.qas.logback.version}.jar"
CLASSPATH="$CLASSPATH":"$CATALINA_HOME/lib/logback-classic-${ch.qas.logback.version}.jar"
# Janino deps (used by logback for conditional processing in the config file)
CLASSPATH="$CLASSPATH":"$CATALINA_HOME/lib/janino-${org.codehaus.janino.version}.jar"
CLASSPATH="$CLASSPATH":"$CATALINA_HOME/lib/commons-compiler-${org.codehaus.janino.version}.jar"

# -----------------------------------------------------------------------------
# Compute the CATALINA_OPTS
# -----------------------------------------------------------------------------

if $EXO_DEBUG ; then
  CATALINA_OPTS="$CATALINA_OPTS -agentlib:jdwp=transport=dt_socket,address=${EXO_DEBUG_PORT},server=y,suspend=n"
fi

if $EXO_DEV ; then
  CATALINA_OPTS="$CATALINA_OPTS -Dorg.exoplatform.container.configuration.debug"
  CATALINA_OPTS="$CATALINA_OPTS -Dexo.product.developing=true"
  CATALINA_OPTS="$CATALINA_OPTS -Dignore.unregistered.webapp=false"
fi

# JCR session leak detector
CATALINA_OPTS="$CATALINA_OPTS -Dexo.jcr.session.tracking.active=${EXO_JCR_SESSION_TRACKING}"

# JVM memory allocation pool parameters
CATALINA_OPTS="$CATALINA_OPTS -Xms${EXO_JVM_SIZE_MIN} -Xmx${EXO_JVM_SIZE_MAX} -XX:MaxPermSize=${EXO_JVM_PERMSIZE_MAX}"

# Reduce the RMI GCs to once per hour for Sun JVMs.
CATALINA_OPTS="$CATALINA_OPTS -Dsun.rmi.dgc.client.gcInterval=3600000 -Dsun.rmi.dgc.server.gcInterval=3600000"

# Default user locale defined at JVM level
CATALINA_OPTS="$CATALINA_OPTS -Duser.language=${EXO_JVM_USER_LANGUAGE} -Duser.region=${EXO_JVM_USER_REGION}"

# Network settings
CATALINA_OPTS="$CATALINA_OPTS -Djava.net.preferIPv4Stack=true"

# Headless
CATALINA_OPTS="$CATALINA_OPTS -Djava.awt.headless=true"

# Platform profiles
CATALINA_OPTS="$CATALINA_OPTS -Dexo.profiles=${EXO_PROFILES}"

# Platform paths
CATALINA_OPTS="$CATALINA_OPTS -Dexo.conf.dir.name=gatein/conf"
CATALINA_OPTS="$CATALINA_OPTS -Dexo.conf.dir=\"$CATALINA_BASE/gatein/conf\""
CATALINA_OPTS="$CATALINA_OPTS -Dgatein.conf.dir=\"$CATALINA_BASE/gatein/conf\""
CATALINA_OPTS="$CATALINA_OPTS -Djava.security.auth.login.config=\"$CATALINA_BASE/conf/jaas.conf\""
CATALINA_OPTS="$CATALINA_OPTS -Dgatein.data.dir=\"${EXO_DATA_DIR}\""
# JCR Data directory
CATALINA_OPTS="$CATALINA_OPTS -Dgatein.jcr.data.dir=\"${EXO_DATA_DIR}/jcr\""
# JCR values
CATALINA_OPTS="$CATALINA_OPTS -Dgatein.jcr.storage.data.dir=\"${EXO_DATA_DIR}/jcr/values\""
# JCR indexes
CATALINA_OPTS="$CATALINA_OPTS -Dgatein.jcr.index.data.dir=\"${EXO_DATA_DIR}/jcr/index\""

# Assets version
CATALINA_OPTS="$CATALINA_OPTS -Dgatein.assets.version=${EXO_ASSETS_VERSION}"

# Logback configuration file
CATALINA_OPTS="$CATALINA_OPTS -Dlogback.configurationFile=\"${EXO_LOGS_LOGBACK_CONFIG_FILE}\""

# Define the XML Parser depending on the JVM vendor
if [ "${EXO_JVM_VENDOR}" = "IBM" ]; then
  CATALINA_OPTS="$CATALINA_OPTS -Djavax.xml.stream.XMLOutputFactory=com.sun.xml.stream.ZephyrWriterFactory -Djavax.xml.stream.XMLInputFactory=com.sun.xml.stream.ZephyrParserFactory -Djavax.xml.stream.XMLEventFactory=com.sun.xml.stream.events.ZephyrEventFactory"
else
  CATALINA_OPTS="$CATALINA_OPTS -Djavax.xml.stream.XMLOutputFactory=com.sun.xml.internal.stream.XMLOutputFactoryImpl -Djavax.xml.stream.XMLInputFactory=com.sun.xml.internal.stream.XMLInputFactoryImpl -Djavax.xml.stream.XMLEventFactory=com.sun.xml.internal.stream.events.XMLEventsFactoryImpl"
fi

# PLF-4968/JCR-2164 : Avoid Exception when starting with Java 7 (http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6804124)
CATALINA_OPTS="$CATALINA_OPTS -Djava.util.Arrays.useLegacyMergeSort=true"

# Jod Converter activation
[ ! -z $EXO_JODCONVERTER_ENABLE ] && CATALINA_OPTS="$CATALINA_OPTS -Djodconverter.enable=${EXO_JODCONVERTER_ENABLE}"
# Comma separated list of ports numbers to use for open office servers used to convert documents.
[ ! -z $EXO_JODCONVERTER_PORTS ] && CATALINA_OPTS="$CATALINA_OPTS -Djodconverter.portnumbers=${EXO_JODCONVERTER_PORTS}"
# The absolute path to the office home on the server.
[ ! -z $EXO_JODCONVERTER_OFFICEHOME ] && CATALINA_OPTS="$CATALINA_OPTS -Djodconverter.officehome=\"${EXO_JODCONVERTER_OFFICEHOME}\""

# Domain name used to produce absolute URLs in email notifications.
[ ! -z $EXO_DEPLOYMENT_URL ] && CATALINA_OPTS="$CATALINA_OPTS -Ddomain.url=${EXO_DEPLOYMENT_URL}"
# Email display in "from" field of email notification.
[ ! -z $EXO_EMAIL_FROM ] && CATALINA_OPTS="$CATALINA_OPTS -Dsmtp.from=${EXO_EMAIL_FROM}"
# SMTP Server hostname.
[ ! -z $EXO_EMAIL_SMTP_HOST ] && CATALINA_OPTS="$CATALINA_OPTS -Dsmtp.host=${EXO_EMAIL_SMTP_HOST}"
# SMTP Server port.
[ ! -z $EXO_EMAIL_SMTP_PORT ] && CATALINA_OPTS="$CATALINA_OPTS -Dsmtp.port=${EXO_EMAIL_SMTP_PORT}"
# True to enable the secure (TLS) SMTP. See RFC 3207.
[ ! -z $EXO_EMAIL_SMTP_STARTTLS_ENABLE ] && CATALINA_OPTS="$CATALINA_OPTS -Dsmtp.starttls.enable=${EXO_EMAIL_SMTP_STARTTLS_ENABLE}"
# True to enable the SMTP authentication.
[ ! -z $EXO_EMAIL_SMTP_AUTH ] && CATALINA_OPTS="$CATALINA_OPTS -Dsmtp.auth=${EXO_EMAIL_SMTP_AUTH}"
# Username to send for authentication.
[ ! -z $EXO_EMAIL_SMTP_USERNAME ] && CATALINA_OPTS="$CATALINA_OPTS -Dsmtp.username=${EXO_EMAIL_SMTP_USERNAME}"
# Password to send for authentication.
[ ! -z $EXO_EMAIL_SMTP_PASSWORD ] && CATALINA_OPTS="$CATALINA_OPTS -Dsmtp.password=${EXO_EMAIL_SMTP_PASSWORD}"
# Specify the port to connect to when using the specified socket factory.
[ ! -z $EXO_EMAIL_SMTP_SOCKET_FACTORY_PORT ] && CATALINA_OPTS="$CATALINA_OPTS -Dsmtp.socketFactory.port=${EXO_EMAIL_SMTP_SOCKET_FACTORY_PORT}"
# This class will be used to create SMTP sockets.
[ ! -z $EXO_EMAIL_SMTP_SOCKET_FACTORY_CLASS ] && CATALINA_OPTS="$CATALINA_OPTS -Dsmtp.socketFactory.class=${EXO_EMAIL_SMTP_SOCKET_FACTORY_CLASS}"
