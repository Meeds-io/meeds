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

# -----------------------------------------------------------------------------
# Settings customisation
# -----------------------------------------------------------------------------
# You have 2 ways to customize your installation settings :
# 1- Rename the file setenv-customize.sample.sh to setenv-customize.sh and uncomment/change values
# 2- Use system environment variables of your system or local shell
# -----------------------------------------------------------------------------

if [ -r "$CATALINA_BASE/bin/setenv-customize.sh" ]; then
  . "$CATALINA_BASE/bin/setenv-customize.sh"
fi

case "`uname`" in
  CYGWIN*)
    echo "=========================================================="
    echo "Cygwin isn't supported. Please use .bat scripts on Windows"
    echo "=========================================================="
    exit -1;
  ;;
esac

# -----------------------------------------------------------------------------
# Default EXO PLATFORM configuration
# -----------------------------------------------------------------------------
[ -z $EXO_PROFILES ] && EXO_PROFILES="default"
[ -z $EXO_CONF_DIR_NAME ] && EXO_CONF_DIR_NAME="gatein/conf"
[ -z $EXO_CONF_DIR ] && EXO_CONF_DIR="$CATALINA_HOME/${EXO_CONF_DIR_NAME}"
[ -z $EXO_DEBUG ] && EXO_DEBUG=false
[ -z $EXO_DEBUG_PORT ] && EXO_DEBUG_PORT=8000
[ -z $EXO_DEV ] && EXO_DEV=false
[ -z $EXO_ASSETS_VERSION ] && EXO_ASSETS_VERSION=${project.version}

# -----------------------------------------------------------------------------
# Default Logs configuration
# -----------------------------------------------------------------------------
# Default configuration for logs (using logback framework - http://logback.qos.ch/manual/configuration.html )
[ -z $EXO_LOGS_LOGBACK_CONFIG_FILE ] && EXO_LOGS_LOGBACK_CONFIG_FILE=$CATALINA_HOME/conf/logback.xml
[ -z $EXO_LOGS_DISPLAY_CONSOLE ] && EXO_LOGS_DISPLAY_CONSOLE=false
[ -z $EXO_LOGS_CONSOLE_COLORIZED ] && EXO_LOGS_CONSOLE_COLORIZED=

# -----------------------------------------------------------------------------
# Default JVM configuration
# -----------------------------------------------------------------------------
[ -z $EXO_JVM_VENDOR ] && EXO_JVM_VENDOR="ORACLE"
[ -z $EXO_JVM_SIZE_MAX ] && EXO_JVM_SIZE_MAX=1g
[ -z $EXO_JVM_SIZE_MIN ] && EXO_JVM_SIZE_MIN=512m
[ -z $EXO_JVM_PERMSIZE_MAX ] && EXO_JVM_PERMSIZE_MAX=256m
[ -z $EXO_JVM_PERMSIZE_MIN ] && EXO_JVM_PERMSIZE_MIN=128m

# -----------------------------------------------------------------------------
# Default Tomcat configuration
# -----------------------------------------------------------------------------
# Global Tomcat settings
[ -z $EXO_TOMCAT_UNPACK_WARS ] && EXO_TOMCAT_UNPACK_WARS="$EXO_DEV"

# -----------------------------------------------------------------------------
# Export the needed system properties for server.xml
# -----------------------------------------------------------------------------
JAVA_OPTS="$JAVA_OPTS -DEXO_TOMCAT_UNPACK_WARS=${EXO_TOMCAT_UNPACK_WARS} -DEXO_DEV=${EXO_DEV}"

# -----------------------------------------------------------------------------
# Logs customization (Managed by slf4J/logback instead of tomcat-juli & co)
# -----------------------------------------------------------------------------
# Deactivate j.u.l
LOGGING_MANAGER=-Dnop
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
  CATALINA_OPTS="$CATALINA_OPTS -Xrunjdwp:transport=dt_socket,address=${EXO_DEBUG_PORT},server=y,suspend=n"
fi
if $EXO_DEV ; then
  CATALINA_OPTS="$CATALINA_OPTS -Dorg.exoplatform.container.configuration.debug"
  CATALINA_OPTS="$CATALINA_OPTS -Dexo.product.developing=true"
fi
CATALINA_OPTS="$CATALINA_OPTS -Xms${EXO_JVM_SIZE_MIN} -Xmx${EXO_JVM_SIZE_MAX} -XX:MaxPermSize=${EXO_JVM_PERMSIZE_MAX}"
CATALINA_OPTS="$CATALINA_OPTS -Dexo.profiles=${EXO_PROFILES}"
CATALINA_OPTS="$CATALINA_OPTS -Djava.security.auth.login.config=$CATALINA_HOME/conf/jaas.conf"
CATALINA_OPTS="$CATALINA_OPTS -Dexo.conf.dir.name=${EXO_CONF_DIR_NAME} -Dexo.conf.dir=${EXO_CONF_DIR}"
CATALINA_OPTS="$CATALINA_OPTS -Djavasrc=${JAVA_HOME}/src.zip -Djre.lib=${JAVA_HOME}/jre/lib"
CATALINA_OPTS="$CATALINA_OPTS -Dgatein.assets.version=${EXO_ASSETS_VERSION}"
# Logback configuration file
CATALINA_OPTS="$CATALINA_OPTS -Dlogback.configurationFile=${EXO_LOGS_LOGBACK_CONFIG_FILE}"
# Define the XML Parser depending on the JVM vendor
if [ "${EXO_JVM_VENDOR}" = "IBM" ]; then
  CATALINA_OPTS="$CATALINA_OPTS -Djavax.xml.stream.XMLOutputFactory=com.sun.xml.stream.ZephyrWriterFactory -Djavax.xml.stream.XMLInputFactory=com.sun.xml.stream.ZephyrParserFactory -Djavax.xml.stream.XMLEventFactory=com.sun.xml.stream.events.ZephyrEventFactory"
else
  CATALINA_OPTS="$CATALINA_OPTS -Djavax.xml.stream.XMLOutputFactory=com.sun.xml.internal.stream.XMLOutputFactoryImpl -Djavax.xml.stream.XMLInputFactory=com.sun.xml.internal.stream.XMLInputFactoryImpl -Djavax.xml.stream.XMLEventFactory=com.sun.xml.internal.stream.events.XMLEventsFactoryImpl"
fi
CATALINA_OPTS="$CATALINA_OPTS -Djava.net.preferIPv4Stack=true"
# Disable EHCache update checker
CATALINA_OPTS="$CATALINA_OPTS -Dnet.sf.ehcache.skipUpdateCheck=true"
# Disable Quartz update checker
CATALINA_OPTS="$CATALINA_OPTS -Dorg.terracotta.quartz.skipUpdateCheck=true"

