#!/bin/bash
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
  echo "  --debug            Starts with JVM Debugger (Use \${EXO_DEBUG_PORT} to change the port. 8000 by default)"
  echo "  --dev              Starts with Platform developer mode"
  echo "  --data <path>      Defines a specific directory where to store data (can also be done by setting the environment variable EXO_DATA_DIR)"
  echo "  -c, --color        Enforce to use colorized logs in console. (By default colors are activated on non-windows systems)"
  echo "  -nc, --nocolor     Enforce to not use colorized logs in console. (By default colors are activated on non-windows systems)"
  echo "  -b, --background   Starts as a background process. Use stop_eXo.sh to stop it. Console logs are deactivated."
  echo "  -h, --help         This help message"
  exit 1
}

COMMAND="run"

# Process command line parameters
while [ "$1" != "" ]; do
  case $1 in
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
