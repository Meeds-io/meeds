#!/bin/bash
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


os400=false
darwin=false
case "`uname`" in
CYGWIN*) cygwin=true;;
OS400*) os400=true;;
Darwin*) darwin=true;;
esac

# resolve links - $0 may be a softlink
PRG="$0"

while [ -h "$PRG" ] ; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`/"$link"
  fi
done
 
PRGDIR=`dirname "$PRG"`
EXECUTABLE=bin/catalina.sh

# Check that target executable exists
if $os400; then
  # -x will Only work on the os400 if the files are: 
  # 1. owned by the user
  # 2. owned by the PRIMARY group of the user
  # this will not work if the user belongs in secondary groups
  eval
else
  if [ ! -x "$PRGDIR"/"$EXECUTABLE" ]; then
    echo "Cannot find $PRGDIR/$EXECUTABLE"
    echo "This file is needed to run this program"
    exit 1
  fi
fi

usage(){
  echo "Usage: "`basename "$PRG"`" [options]"
  echo ""
  echo "    Starts eXo Platform"
  echo ""
  echo "options:"
  echo ""
  echo "  --cluster                           Enables cluster mode of eXo Platform"
  echo "  --cluster-hosts <host1> <host2>     Defines list of cluster hosts that is member of the cluster."
  echo "                                      Each cluster host definition must contains exactly the following elemens: "
  echo "                                      - Unique host name in the cluster (<name>). Shouldn't be renamed once started with it. No default, this option is mandatory and has to be unique for each host."
  echo "                                      - HTTP protocol (<http_protocol>). Default: http"
  echo "                                      - IP V6 address or DNS name of host (<address>). Default: localhost"
  echo "                                        To use IPv4 add to startup script file (setenv-customize.sh) -Djava.net.preferIPv4Stack=true."
  echo "                                      - HTTP Port (<http_port>). Default: 8080"
  echo "                                      - A first TCP Port that will be configured for caches synchronization using jGroups (<tcp1_port>). Default: 7800"
  echo "                                      - A second TCP Port that will be used for caches synchronization using jGroups (<tcp2_port>). Default: 7900"
  echo "                                      Example: name=<name>,http_protocol=<http_protocol>,address=<address>,http_port=<http_port>,tcp1_port=<tcp1_port>,tcp2_port=<tcp2_port>"
  echo "  --cluster-current-host <name>       Current host <name> as defined in the list o hosts properties, see --cluster-hosts"
  echo "  --es-url <url>                      Elasticsearch URL. Default: http://127.0.0.1:9200"
  echo "  --blob-fs                           Whether use Filesystem to store blob files or not. Default: for cluster mode, it's stored in RDBMS else it will be stored in Filesystem."
  echo "  --blob-rdbms                        Whether use RDBMS to store blob files. Default: For cluster mode, it's stored in RDBMS else it will be stored in Filesystem."
  echo "  --debug                             Starts with JVM Debugger (Use \${EXO_DEBUG_PORT} to change the port. 8000 by default)"
  echo "  --dev                               Starts with Platform developer mode"
  echo "  --data <path>                       Defines a specific directory where to store data (can also be done by setting the environment variable EXO_DATA_DIR)"
  echo "  -c, --color                         Enforce to use colorized logs in console. (By default colors are activated on non-windows systems)"
  echo "  -nc, --nocolor                      Enforce to not use colorized logs in console. (By default colors are activated on non-windows systems)"
  echo "  -b, --background                    Starts as a background process. Use stop_eXo.sh to stop it. Console logs are deactivated."
  echo "  -h, --help                          This help message"
  exit 1
}

COMMAND="run"

# Process command line parameters
while [ "$1" != "" ]; do
  case $1 in
    --cluster )
      export EXO_CLUSTER=true
    ;;
    --cluster-current-host )
      if [ $# -lt 2 ]
      then
        echo "Missing value for option --cluster-current-host"
        echo ""
        usage
        exit
      fi
      shift
      export EXO_CLUSTER_NODE_NAME="$1"
    ;;
    --cluster-host )
      if [ $# -lt 2 ]
      then
        echo "Missing value for option --cluster-hosts"
        echo ""
        usage
        exit
      fi
      shift
      [ -n "$EXO_CLUSTER_HOSTS" ] && export EXO_CLUSTER_HOSTS="${EXO_CLUSTER_HOSTS} $1"
      [ -z "$EXO_CLUSTER_HOSTS" ] && export EXO_CLUSTER_HOSTS="$1"
    ;;
    --es-url )
      if [ $# -lt 2 ]
      then
        echo "Missing value for option --es-url"
        echo ""
        usage
        exit
      fi
      shift
      export EXO_ES_URL=$1
    ;;
    --blob-fs )
      export EXO_BLOB_FS=true
    ;;
    --blob-rdbms )
      export EXO_BLOB_FS=false
    ;;
    --dev )
      export EXO_DEV=true
    ;;
    --debug )
      export EXO_DEBUG=true
    ;;
    --data )
      # Error if no path provided
      if [ $# -lt 2 ]
      then
        echo "Missing value for option --data"
        echo ""
        usage
        exit
      fi
      shift
      export EXO_DATA_DIR=$1
    ;;
    -b | --background )
      COMMAND="start"
      # Don't activate console logs if launched as background task
      export EXO_LOGS_DISPLAY_CONSOLE=${EXO_LOGS_DISPLAY_CONSOLE:-false}
      # Define a PID file is launched in background
      export CATALINA_PID=${CATALINA_PID:-"$PRGDIR/temp/catalina.pid"}
    ;;
    -c | --color )
      # Enforce colors in console
      export EXO_LOGS_COLORIZED_CONSOLE=true
    ;;
    -nc | --nocolor )
      # Enforce no colors in console
      export EXO_LOGS_COLORIZED_CONSOLE=false
    ;;
    -h | --help )
      usage
      exit
      ;;
    * )
      echo "Invalid option !"
      echo ""
      usage
      exit
      ;;
    esac
    shift
done

# Activate console logs if we aren't in background
export EXO_LOGS_DISPLAY_CONSOLE=${EXO_LOGS_DISPLAY_CONSOLE:-true}

exec "$PRGDIR"/"$EXECUTABLE" "$COMMAND"
