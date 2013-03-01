#!/bin/sh

#
# Copyright (C) 2013 eXo Platform SAS.
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

# -----------------------------------------------------------------------------
# Settings customisation
# -----------------------------------------------------------------------------
# You have 2 ways to customize your installation settings :
# 1- Rename the file setenv-customize.sample.sh to setenv-customize.sh and uncomment/change values
# 2- Use system environment variables of your system or local shell
# -----------------------------------------------------------------------------

# -----------------------------------------------------------------------------
# Default EXO PLATFORM configuration
# -----------------------------------------------------------------------------
#EXO_PROFILES="all"
#EXO_CONF_DIR_NAME="gatein/conf"
#EXO_CONF_DIR="$CATALINA_HOME/${EXO_CONF_DIR_NAME}"
#EXO_DEBUG=true
#EXO_DEBUG_PORT=8000
#EXO_DEV=true

# -----------------------------------------------------------------------------
# Default Logs configuration
# -----------------------------------------------------------------------------
# Default configuration for logs (using logback framework - http://logback.qos.ch/manual/configuration.html )
#EXO_LOGS_LOGBACK_CONFIG_FILE=$CATALINA_HOME/conf/logback.xml
#EXO_LOGS_DISPLAY_CONSOLE=true
#EXO_LOGS_CONSOLE_COLORIZED=true

# -----------------------------------------------------------------------------
# Default JVM configuration
# -----------------------------------------------------------------------------
#EXO_JVM_VENDOR="ORACLE"
#EXO_JVM_SIZE_MAX=1g
#EXO_JVM_SIZE_MIN=512m
#EXO_JVM_PERMSIZE_MAX=256m
#EXO_JVM_PERMSIZE_MIN=128m

# -----------------------------------------------------------------------------
# Default Tomcat configuration
# -----------------------------------------------------------------------------
# Global Tomcat settings
#CATALINA_PID=$CATALINA_HOME/temp/catalina.pid
#EXO_TOMCAT_UNPACK_WARS="$EXO_DEV"
