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
if [ ! -x "$PRGDIR"/"$EXECUTABLE" ]; then
  echo "Cannot find $PRGDIR/$EXECUTABLE"
  echo "This file is needed to run this program"
  exit 1
fi

usage(){
  echo "Usage: "`basename "$PRG"`" [options]"
  echo ""
  echo "    Stop Platform, waiting up to 5 seconds for the process to end only if it was started as a background job"
  echo ""
  echo "options:"
  echo ""
  echo "  n            Stop Platform, waiting up to n seconds for the process to end"
  echo "  -force       Stop Platform, wait up to 5 seconds and then use kill -KILL if still running"
  echo "  n -force     Stop Platform, wait up to n seconds and then use kill -KILL if still running"
  echo "  -h, --help   This help message"
  exit 1
}

while [ "$1" != "" ]; do
  case $1 in
    -h | --help )
      usage
      exit
      ;;
    esac
    shift
done

export CATALINA_PID=${CATALINA_PID:-"$PRGDIR/temp/catalina.pid"}

exec "$PRGDIR"/"$EXECUTABLE" stop "$@"
