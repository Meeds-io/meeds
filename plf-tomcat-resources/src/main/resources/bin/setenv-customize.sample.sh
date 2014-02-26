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
#
# Settings customization
#
# Refer to eXo Platform Administrators Guide for more details.
# http://docs.exoplatform.com
#
# -----------------------------------------------------------------------------
# You have 2 ways to customize your installation settings :
# 1- Rename the file setenv-customize.sample.sh to setenv-customize.sh and uncomment/change values below
# 2- Use system environment variables of your system or local shell
# -----------------------------------------------------------------------------

# -----------------------------------------------------------------------------
# JVM configuration
# -----------------------------------------------------------------------------

# Home directory of the JVM to use (Default : try to auto-compute it from existing java executable in path by default)
#JAVA_HOME="/opt/java/jdk6"

# Use EXO_JVM_VENDOR="IBM" with J9 JVMs (Default : "ORACLE")
#EXO_JVM_VENDOR="IBM"

# Maximum Heap Size to use (Default : 2g)
#EXO_JVM_SIZE_MAX="1g"

# Minimum Heap Size to use (Default : 512m)
#EXO_JVM_SIZE_MIN="1g"

# Size of the Permanent Generation. (Default : 256m)
#EXO_JVM_PERMSIZE_MAX="128m"

# Default locale language
#EXO_JVM_USER_LANGUAGE="fr"

# Default locale region
#EXO_JVM_USER_REGION="FR"

# Loads in-process debugging libraries to attach a debugger (true with --debug option on start_eXo.sh script)
#EXO_DEBUG=true

# Listening port for the debugger
#EXO_DEBUG_PORT="8000"

# -----------------------------------------------------------------------------
# PLATFORM configuration
# -----------------------------------------------------------------------------

# eXo Platform comes with different runtime profiles, enabling you to customize which modules you want to enable/disable. (Default: all)
#EXO_PROFILES="all,myOwnProfile"

# Assets versions used in static resources URLs. Useful to manage caches. (Default: The product version)
#EXO_ASSETS_VERSION="42"

# Main directory where are stored all data (Default: ${CATALINA_BASE}/gatein/data) (also available with --data <path> option on start_eXo.sh script)
#EXO_DATA_DIR="${HOME}/.eXo-platform/data"

# Activates the development mode of eXo platform (true with --dev option on start_eXo.sh script)
#EXO_DEV=true

# Activates the JCR sessions leaks detector (Default: $EXO_DEV. true with --dev option on start_eXo.sh script)
#EXO_JCR_SESSION_TRACKING=true

# -----------------------------------------------------------------------------
# SMTP/Emails configuration
# -----------------------------------------------------------------------------

# Domain name used to produce absolute URLs in email notifications. (Default: http://localhost:8080)
#EXO_DEPLOYMENT_URL="https://intranet.mycompany.com"

# Email display in "from" field of email notification. (Default: noreply@exoplatform.com)
#EXO_EMAIL_FROM="intranet@mycompany.com"

# SMTP Server hostname. (Default: localhost)
#EXO_EMAIL_SMTP_HOST="smtp.gmail.com"

# SMTP Server port. (Default: 25)
#EXO_EMAIL_SMTP_PORT="465"

# True to enable the secure (TLS) SMTP. See RFC 3207. (Default: false)
#EXO_EMAIL_SMTP_STARTTLS_ENABLE=true

# True to enable the SMTP authentication. (Default: false)
#EXO_EMAIL_SMTP_AUTH=true

# Username to send for authentication. (Default: <NONE>)
#EXO_EMAIL_SMTP_USERNAME="account@gmail.com"

# Password to send for authentication. (Default: <NONE>)
#EXO_EMAIL_SMTP_PASSWORD="password"

# Specify the port to connect to when using the specified socket factory. (Default: <NONE>)
#EXO_EMAIL_SMTP_SOCKET_FACTORY_PORT="465"

# This class will be used to create SMTP sockets. (Default: <NONE>)
#EXO_EMAIL_SMTP_SOCKET_FACTORY_CLASS="javax.net.ssl.SSLSocketFactory"

# -----------------------------------------------------------------------------
# JOD Converter configuration
# -----------------------------------------------------------------------------
# Used to preview documents
#
# Requires to have openoffice/libreoffice server installed

# Jod Converter activation (Default : true)
#EXO_JODCONVERTER_ENABLE=false

# Comma separated list of ports numbers to use for open office servers used to convert documents.
# One office server instance will be created for each port. (Default : 2002)
#EXO_JODCONVERTER_PORTS="2002,2003,2004,2005"

# The absolute path to the office home on the server. (Default : Path automatically discovered based on the OS default locations)
#EXO_JODCONVERTER_OFFICEHOME="/usr/lib/libreoffice"

# -----------------------------------------------------------------------------
# Logs configuration
# -----------------------------------------------------------------------------

# Logback configuration file (http://logback.qos.ch/manual/configuration.html ) - For an advanced customization of logs
#EXO_LOGS_LOGBACK_CONFIG_FILE="$CATALINA_BASE/conf/logback.xml"

# Enforce to display logs in the console (Default: true if started with start_eXo.sh without --background option, false otherwise)
#EXO_LOGS_DISPLAY_CONSOLE=true

# Enforce to display colorized logs in the console (Default: false for windows, true otherwise) (You can use --color and --nocolor options on start_eXo.sh to enforce the configuration)
#EXO_LOGS_COLORIZED_CONSOLE=true

# -----------------------------------------------------------------------------
# Tomcat configuration
# -----------------------------------------------------------------------------

# File used to store the PID of the process. (Default: ${CATALINA_BASE}/temp/catalina.pid if start_eXo.sh is launched with --background option. Empty otherwise)
#CATALINA_PID="$CATALINA_BASE/temp/catalina.pid"

# Explodes all wars in the webapps directory (Default: $EXO_DEV. true with --dev option on start_eXo.sh script)
#EXO_TOMCAT_UNPACK_WARS=true

# -----------------------------------------------------------------------------
# Advanced settings (We directly append some settings in CATALINA_OPTS)
# -----------------------------------------------------------------------------

# JVM HeapDumpOnOutOfMemoryError (Useful to debug but the dump is long to produce)
#CATALINA_OPTS="${CATALINA_OPTS} -XX:+HeapDumpOnOutOfMemoryError"
#CATALINA_OPTS="${CATALINA_OPTS} -XX:HeapDumpPath=\"${CATALINA_HOME}/logs/\""

# JVM GC Details
#CATALINA_OPTS="${CATALINA_OPTS} -XX:+PrintGCDetails"
#CATALINA_OPTS="${CATALINA_OPTS} -Xloggc:\"${CATALINA_HOME}/logs/gc.log\""

# JMX (Sample of configuration to activate it without SSL and with access/password credentials)
#CATALINA_OPTS="${CATALINA_OPTS} -Dcom.sun.management.jmxremote=true"
#CATALINA_OPTS="${CATALINA_OPTS} -Dcom.sun.management.jmxremote.ssl=false"
#CATALINA_OPTS="${CATALINA_OPTS} -Dcom.sun.management.jmxremote.password.file=\"${CATALINA_HOME}/conf/jmxremote.password\""
#CATALINA_OPTS="${CATALINA_OPTS} -Dcom.sun.management.jmxremote.access.file=\"${CATALINA_HOME}/conf/jmxremote.access\""
# If you access to JMX though an SSH tunnel
#CATALINA_OPTS="${CATALINA_OPTS} -Djava.rmi.server.hostname=localhost"
# If you are behind a proxy you may have to activate the JMX Remote Lifecycle Listener fixes the ports used by the JMX/RMI Server
# For more details see the configuration file conf/server.xml

# JCR Statistics
#CATALINA_OPTS="${CATALINA_OPTS} -DJDBCWorkspaceDataContainer.statistics.enabled=true -DJCRStatisticsManager.persistence.timeout=30000"

# CRaSH Extension (change telnet and SSH ports)
#CATALINA_OPTS="${CATALINA_OPTS} -Dcrash.telnet.port=12345"
#CATALINA_OPTS="${CATALINA_OPTS} -Dcrash.ssh.port=54321"

# Hibernate dialect for supporting Postgresql Plus
#CATALINA_OPTS="${CATALINA_OPTS} -Dhibernate.dialect=org.hibernate.dialect.PostgresPlusDialect"

# ACME extension (override default acme site data)
#CATALINA_OPTS="${CATALINA_OPTS} -Dacme.portalConfig.metadata.override=true"

# IDE extension (customize default ide extension)
#CATALINA_OPTS="${CATALINA_OPTS} -Dide.portalConfig.metadata.override=false"
