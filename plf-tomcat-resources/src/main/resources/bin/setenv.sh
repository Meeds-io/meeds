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
[ -z $EXO_JVM_SIZE_MAX ] && EXO_JVM_SIZE_MAX="3g"
[ -z $EXO_JVM_SIZE_MIN ] && EXO_JVM_SIZE_MIN="512m"
[ -z $EXO_JVM_PERMSIZE_MAX ] && EXO_JVM_PERMSIZE_MAX="256m"
[ -z $EXO_JVM_METASPACE_SIZE_MAX ] && EXO_JVM_METASPACE_SIZE_MAX="512m"
[ -z $EXO_JVM_USER_LANGUAGE ] && EXO_JVM_USER_LANGUAGE="en"
[ -z $EXO_JVM_USER_REGION ] && EXO_JVM_USER_REGION="US"
[ -z $EXO_DEBUG ] && EXO_DEBUG=false
[ -z $EXO_DEBUG_PORT ] && EXO_DEBUG_PORT="8000"

# -----------------------------------------------------------------------------
# Default EXO PLATFORM configuration
# -----------------------------------------------------------------------------

[ -z $EXO_PROFILES ] && EXO_PROFILES="all"
[ -z $EXO_DEV ] && EXO_DEV=false
[ -z $EXO_JCR_SESSION_TRACKING ] && EXO_JCR_SESSION_TRACKING=$EXO_DEV
[ -z $EXO_CONF_DIR ] && EXO_CONF_DIR="$CATALINA_BASE/gatein/conf"
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
[ -z $EXO_TOMCAT_UNPACK_WARS ] && EXO_TOMCAT_UNPACK_WARS=true

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
if [ ! -z $CLASSPATH ]; then
  CLASSPATH="$CLASSPATH":"$CATALINA_HOME/lib/slf4j-api-${org.slf4j.version}.jar"
else
  CLASSPATH="$CATALINA_HOME/lib/slf4j-api-${org.slf4j.version}.jar"
fi
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
CATALINA_OPTS="$CATALINA_OPTS -Xms${EXO_JVM_SIZE_MIN} -Xmx${EXO_JVM_SIZE_MAX}"

CATALINA_OPTS="$CATALINA_OPTS -XX:MaxMetaspaceSize=${EXO_JVM_METASPACE_SIZE_MAX}"

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
CATALINA_OPTS="$CATALINA_OPTS -Dexo.conf.dir=\"${EXO_CONF_DIR}\""
CATALINA_OPTS="$CATALINA_OPTS -Dgatein.conf.dir=\"${EXO_CONF_DIR}\""
CATALINA_OPTS="$CATALINA_OPTS -Djava.security.auth.login.config=\"$CATALINA_BASE/conf/jaas.conf\""
CATALINA_OPTS="$CATALINA_OPTS -Dexo.data.dir=\"${EXO_DATA_DIR}\""
# JCR Data directory
CATALINA_OPTS="$CATALINA_OPTS -Dexo.jcr.data.dir=\"${EXO_DATA_DIR}/jcr\""
# JCR values
CATALINA_OPTS="$CATALINA_OPTS -Dexo.jcr.storage.data.dir=\"${EXO_DATA_DIR}/jcr/values\""
# JCR indexes
CATALINA_OPTS="$CATALINA_OPTS -Dexo.jcr.index.data.dir=\"${EXO_DATA_DIR}/jcr/index\""
# Files storage
CATALINA_OPTS="$CATALINA_OPTS -Dexo.files.storage.dir=\"${EXO_DATA_DIR}/files\""

# Logback configuration file
CATALINA_OPTS="$CATALINA_OPTS -Dlogback.configurationFile=\"${EXO_LOGS_LOGBACK_CONFIG_FILE}\""

# Define the XML Parser depending on the JVM vendor
if [ "${EXO_JVM_VENDOR}" = "IBM" ]; then
  CATALINA_OPTS="$CATALINA_OPTS -Djavax.xml.stream.XMLOutputFactory=com.sun.xml.stream.ZephyrWriterFactory -Djavax.xml.stream.XMLInputFactory=com.sun.xml.stream.ZephyrParserFactory -Djavax.xml.stream.XMLEventFactory=com.sun.xml.stream.events.ZephyrEventFactory"
else
  CATALINA_OPTS="$CATALINA_OPTS -Djavax.xml.stream.XMLOutputFactory=com.sun.xml.internal.stream.XMLOutputFactoryImpl -Djavax.xml.stream.XMLInputFactory=com.sun.xml.internal.stream.XMLInputFactoryImpl -Djavax.xml.stream.XMLEventFactory=com.sun.xml.internal.stream.events.XMLEventFactoryImpl -Djavax.xml.parsers.SAXParserFactory=com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl"
fi

# PLF-4968/JCR-2164 : Avoid Exception when starting with Java 7 (http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6804124)
CATALINA_OPTS="$CATALINA_OPTS -Djava.util.Arrays.useLegacyMergeSort=true"

# PLF-6550: Fix Startup problem when JVM hangs because of lack of entropy
CATALINA_OPTS="$CATALINA_OPTS -Djava.security.egd=file:/dev/./urandom"

# PLF-6965 set default file encoding to UTF-8 Independently from OS default charset
CATALINA_OPTS="$CATALINA_OPTS -Dfile.encoding=UTF-8"

cmd=$(java -jar $CATALINA_HOME/bin/exo-tools.jar isJava9OrSuperior)
if [ $? = 0 ]; then
  CATALINA_OPTS="$CATALINA_OPTS --add-modules java.activation --add-modules java.xml.bind"
fi