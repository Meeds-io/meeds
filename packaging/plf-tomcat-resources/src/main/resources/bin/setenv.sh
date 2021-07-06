#!/bin/sh
#
# This file is part of the Meeds project (https://meeds.io/).
# Copyright (C) 2020 Meeds Association
# contact@meeds.io
# This program is free software; you can redistribute it and/or
# modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation; either
# version 3 of the License, or (at your option) any later version.
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# Lesser General Public License for more details.
# You should have received a copy of the GNU Lesser General Public License
# along with this program; if not, write to the Free Software Foundation,
# Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
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
# EXO PLATFORM Blob storage configuration
# -----------------------------------------------------------------------------

if [ -n  "$EXO_BLOB_FS" ]; then
  if [ "${EXO_BLOB_FS}" = "true" ]; then
    CATALINA_OPTS="${CATALINA_OPTS} -Dexo.files.binaries.storage.type=fs -Dexo.jcr.storage.enabled=true"
  else
    CATALINA_OPTS="${CATALINA_OPTS} -Dexo.files.binaries.storage.type=rdbms -Dexo.jcr.storage.enabled=false"
  fi
else
  [ "${EXO_CLUSTER}" = "true" ] && CATALINA_OPTS="${CATALINA_OPTS} -Dexo.files.binaries.storage.type=rdbms -Dexo.jcr.storage.enabled=false"
fi

# -----------------------------------------------------------------------------
# EXO PLATFORM ES configuration
# -----------------------------------------------------------------------------

[ -n "$EXO_ES_URL" ] && CATALINA_OPTS="${CATALINA_OPTS} -Dexo.es.index.server.url=${EXO_ES_URL} -Dexo.es.search.server.url=${EXO_ES_URL}"

# -----------------------------------------------------------------------------
# EXO PLATFORM Cluster configuration
# -----------------------------------------------------------------------------

if [ "${EXO_CLUSTER}" = "true" ]; then
  CATALINA_OPTS="${CATALINA_OPTS} -Dexo.es.embedded.enabled=false -Dexo.cometd.oort.configType=static"
  EXO_PROFILES="$EXO_PROFILES,cluster"

  [ -n "$EXO_CLUSTER_NODE_NAME" ] && CATALINA_OPTS="${CATALINA_OPTS} -Dexo.cluster.node.name=${EXO_CLUSTER_NODE_NAME}"

  if [ -n "$EXO_CLUSTER_HOSTS" ]; then
    CLUSTER_HOSTS=$(echo $EXO_CLUSTER_HOSTS | tr '\r' ' ' | tr '\n' ' ')
    for CLUSTER_HOST in $CLUSTER_HOSTS
    do
      CLUSTER_HOST_PARTS=$(echo $CLUSTER_HOST | sed "s/,/;exo_cluster_/g")
      eval "exo_cluster_${CLUSTER_HOST_PARTS}"
      for CLUSTER_HOST_PART in $CLUSTER_HOST_PARTS
      do
        [ -z "${exo_cluster_http_protocol}" ] && exo_cluster_http_protocol=http
        [ -z "${exo_cluster_address}" ] && exo_cluster_address=localhost
        [ -z "${exo_cluster_http_port}" ] && exo_cluster_http_port=8080
        [ -z "${exo_cluster_tcp1_port}" ] && exo_cluster_tcp1_port=7800
        [ -z "${exo_cluster_tcp2_port}" ] && exo_cluster_tcp2_port=7900

        if [ "${exo_cluster_name}" = "${EXO_CLUSTER_NODE_NAME}" ]; then
          CATALINA_OPTS="${CATALINA_OPTS} -Dexo.cometd.oort.url=${exo_cluster_http_protocol}://${exo_cluster_address}:${exo_cluster_http_port}/cometd/cometd"
          CATALINA_OPTS="${CATALINA_OPTS} -Dexo.jcr.cluster.jgroups.tcp.bind_addr=${exo_cluster_address}"
          CATALINA_OPTS="${CATALINA_OPTS} -Dexo.jcr.cluster.jgroups.tcp.bind_port=${exo_cluster_tcp1_port}"
          CATALINA_OPTS="${CATALINA_OPTS} -Dexo.service.cluster.jgroups.tcp.bind_addr=${exo_cluster_address}"
          CATALINA_OPTS="${CATALINA_OPTS} -Dexo.service.cluster.jgroups.tcp.bind_port=${exo_cluster_tcp2_port}"
        fi

        if [ -z $EXO_CLUSTER_HOSTS_TCP_1 ]; then
          EXO_CLUSTER_HOSTS_TCP_1="${exo_cluster_address}[${exo_cluster_tcp1_port}]"
        else
          EXO_CLUSTER_HOSTS_TCP_1="${EXO_CLUSTER_HOSTS_TCP_1},${exo_cluster_address}[${exo_cluster_tcp1_port}]"
        fi

        if [ "${exo_cluster_name}"  !=  "${EXO_CLUSTER_NODE_NAME}" ]; then
          if [ -z $EXO_CLUSTER_OORT_CLOUD ]; then
            EXO_CLUSTER_OORT_CLOUD="${exo_cluster_http_protocol}://${exo_cluster_address}:${exo_cluster_http_port}/cometd/cometd"
          else
            EXO_CLUSTER_OORT_CLOUD="${EXO_CLUSTER_OORT_CLOUD},${exo_cluster_http_protocol}://${exo_cluster_address}:${exo_cluster_http_port}/cometd/cometd"
          fi
        fi

        if [ -z $EXO_CLUSTER_HOSTS_TCP_2 ]; then
          EXO_CLUSTER_HOSTS_TCP_2="${exo_cluster_address}[${exo_cluster_tcp2_port}]"
        else
          EXO_CLUSTER_HOSTS_TCP_2="${EXO_CLUSTER_HOSTS_TCP_2},${exo_cluster_address}[${exo_cluster_tcp2_port}]"
        fi
      done
    done

    if [ -z $EXO_CLUSTER_OORT_CLOUD ]; then
      EXO_CLUSTER_OORT_CLOUD=""
    fi

    CATALINA_OPTS="${CATALINA_OPTS} -Dexo.jcr.cluster.jgroups.tcpping.initial_hosts=${EXO_CLUSTER_HOSTS_TCP_1}"
    CATALINA_OPTS="${CATALINA_OPTS} -Dexo.service.cluster.jgroups.tcpping.initial_hosts=${EXO_CLUSTER_HOSTS_TCP_2}"
    CATALINA_OPTS="${CATALINA_OPTS} -Dexo.cometd.oort.cloud=${EXO_CLUSTER_OORT_CLOUD}"
  fi
else
  CATALINA_OPTS="${CATALINA_OPTS} -Dexo.cluster.node.name="
fi

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
  CLASSPATH="$CLASSPATH":"$CATALINA_HOME/lib/slf4j-api.jar"
else
  CLASSPATH="$CATALINA_HOME/lib/slf4j-api.jar"
fi
CLASSPATH="$CLASSPATH":"$CATALINA_HOME/lib/jul-to-slf4j.jar"
# LogBack deps
CLASSPATH="$CLASSPATH":"$CATALINA_HOME/lib/logback-core.jar"
CLASSPATH="$CLASSPATH":"$CATALINA_HOME/lib/logback-classic.jar"
# Servlet API deps (used by logback)
CLASSPATH="$CLASSPATH":"$CATALINA_HOME/lib/servlet-api.jar"
# Janino deps (used by logback for conditional processing in the config file)
CLASSPATH="$CLASSPATH":"$CATALINA_HOME/lib/janino.jar"
CLASSPATH="$CLASSPATH":"$CATALINA_HOME/lib/commons-compiler.jar"

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

javaExec=java
if [ ! -z "$JAVA_HOME" ]; then
  javaExec=$JAVA_HOME/bin/java
fi

# Used JDK_JAVA_OPTIONS for JDK 9+ options since this variable is only recognized by JDK 9+
cmd=$($javaExec -jar "$CATALINA_HOME"/bin/exo-tools.jar isJava11OrSuperior)
if [ $? != 0 ]; then
  JDK_JAVA_OPTIONS="--add-modules java.activation --add-modules java.xml.bind"
fi
# Open all required modules for reflective access operations
JDK_JAVA_OPTIONS="$JDK_JAVA_OPTIONS --add-opens java.base/java.io=ALL-UNNAMED"
JDK_JAVA_OPTIONS="$JDK_JAVA_OPTIONS --add-opens java.base/java.lang=ALL-UNNAMED"
JDK_JAVA_OPTIONS="$JDK_JAVA_OPTIONS --add-opens java.base/java.lang.invoke=ALL-UNNAMED"
JDK_JAVA_OPTIONS="$JDK_JAVA_OPTIONS --add-opens java.base/java.lang.reflect=ALL-UNNAMED"
JDK_JAVA_OPTIONS="$JDK_JAVA_OPTIONS --add-opens=java.base/java.lang.module=ALL-UNNAMED"
JDK_JAVA_OPTIONS="$JDK_JAVA_OPTIONS --add-opens=java.base/jdk.internal.module=ALL-UNNAMED"
JDK_JAVA_OPTIONS="$JDK_JAVA_OPTIONS --add-opens java.base/java.math=ALL-UNNAMED"
JDK_JAVA_OPTIONS="$JDK_JAVA_OPTIONS --add-opens java.base/java.net=ALL-UNNAMED"
JDK_JAVA_OPTIONS="$JDK_JAVA_OPTIONS --add-opens java.base/java.nio=ALL-UNNAMED"
JDK_JAVA_OPTIONS="$JDK_JAVA_OPTIONS --add-opens java.base/java.text=ALL-UNNAMED"
JDK_JAVA_OPTIONS="$JDK_JAVA_OPTIONS --add-opens java.base/java.util=ALL-UNNAMED"
JDK_JAVA_OPTIONS="$JDK_JAVA_OPTIONS --add-opens java.base/java.util.concurrent=ALL-UNNAMED"
JDK_JAVA_OPTIONS="$JDK_JAVA_OPTIONS --add-opens java.base/sun.nio.ch=ALL-UNNAMED"
JDK_JAVA_OPTIONS="$JDK_JAVA_OPTIONS --add-opens java.management/java.lang.management=ALL-UNNAMED"
JDK_JAVA_OPTIONS="$JDK_JAVA_OPTIONS --add-opens java.desktop/java.awt.font=ALL-UNNAMED"
JDK_JAVA_OPTIONS="$JDK_JAVA_OPTIONS --add-opens java.rmi/sun.rmi.transport=ALL-UNNAMED"
JDK_JAVA_OPTIONS="$JDK_JAVA_OPTIONS --add-opens java.xml/com.sun.org.apache.xerces.internal.util=ALL-UNNAMED"
JDK_JAVA_OPTIONS="$JDK_JAVA_OPTIONS --add-opens java.xml/com.sun.org.apache.xerces.internal.parsers=ALL-UNNAMED"
JDK_JAVA_OPTIONS="$JDK_JAVA_OPTIONS  -noverify"

export JDK_JAVA_OPTIONS
