#!/bin/bash -ue
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

echo " # ============================ "
echo " # eXo Platform v. ${project.version} "
echo " # Extensions Manager "
echo " # ============================ "

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

usage(){
  echo "Usage: "`basename "$PRG"`" [action] [options]"
  echo ""
  echo "    Manages eXo Platform extensions"
  echo ""
  echo "actions:"
  echo ""
  echo "  list               Lists all available extensions"
  echo "  install all        Installs all available extensions"
  echo "  install <ext>      Installs the extension named <ext>"
  echo "  uninstall all      Uninstalls all available extensions"
  echo "  uninstall <ext>    Uninstalls the extension named <ext>"
  echo ""
  echo "options:"
  echo ""
  echo "  -h, --help         This help message"
  exit 1
}

list_extensions(){
	echo "Available extensions:"
  for _dir in `ls -1 ${PRGDIR}/extensions`; do
    echo "  $_dir"
  done
  echo ""
	echo "To install an extension use: "
	echo "  "`basename "$PRG"`" install <ext>"
  echo ""
	echo "To install all avalaible extensions use: "
	echo "  "`basename "$PRG"`" install all"
}

copy_extension(){
	local _extension=$1	
	echo "Installing extension \"$_extension\" ..."
	if [ -d ${PRGDIR}/extensions/$_extension/lib/ ]; then
    for _lib in `ls -1 ${PRGDIR}/extensions/$_extension/lib/*.jar`; do
		  echo -n "* Installing library "`basename $_lib`" ... "
  		if [ ! -f "${PRGDIR}/lib/"`basename $_lib` ]; then
	      cp $_lib ${PRGDIR}/lib
		  	echo "OK."
  		else
	  	  echo "SKIPPED. "`basename $_lib`" already installed."
  		fi
    done
	fi
	if [ -d ${PRGDIR}/extensions/$_extension/webapps/ ]; then
    for _war in `ls -1 ${PRGDIR}/extensions/$_extension/webapps/*.war`; do
		  echo -n "* Installing webapp "`basename $_war`" ... "
  		if [ ! -f "${PRGDIR}/webapps/"`basename $_war` ]; then
	      cp $_war ${PRGDIR}/webapps
		  	echo "OK."
  		else
	  	  echo "SKIPPED. "`basename $_war`" already installed."
  		fi
    done
	fi
	echo "Done"
}

install_extension(){
	local _extension=$1
  if [ "$_extension" == "all" ]; then
    for _dir in `ls -1 ${PRGDIR}/extensions`; do
      copy_extension $_dir
    done
  elif [ -d ${PRGDIR}/extensions/$_extension ]; then
    copy_extension $_extension
  else
    echo "[ERROR] Extension \"$_extension\" doesn't exist !"
    echo ""
    list_extensions
    exit 1;
  fi
}

delete_extension(){
	local _extension=$1	
	echo "Uninstalling extension \"$_extension\" ..."
  for _war in `ls -1 ${PRGDIR}/extensions/$_extension/webapps/*.war`; do
	  local _file="${PRGDIR}/webapps/"`basename $_war`
		echo -n "* Deleting webapp "`basename $_war`" ... "
		if [ -f $_file ]; then
	    rm $_file
			echo "OK."
		else
		  echo "SKIPPED. "`basename $_war`" not installed."
		fi
  done				
	echo "Done"
}


uninstall_extension(){
	local _extension=$1
  if [ "$_extension" == "all" ]; then
    for _dir in `ls -1 ${PRGDIR}/extensions`; do
      delete_extension $_dir
    done
  elif [ -d ${PRGDIR}/extensions/$_extension ]; then
    delete_extension $_extension
  else
    echo "[ERROR] Extension \"$_extension\" doesn't exist !"
    echo ""
    list_extensions
    exit 1;
  fi
}

# no action ? provide help
if [ $# -lt 1 ]; then
  echo "[ERROR] No action defined !"
  echo ""
  usage
  exit 1;
fi

# Action to do
ACTION=$1
shift

case "${ACTION}" in
  list)
    list_extensions
		exit 0
  ;;
  install)
    if [ $# -lt 1 ]; then
      echo "[ERROR] Missing extension name argument !"
      echo ""
      usage
      exit 1;
    fi
    install_extension $1
  ;;
  uninstall)
    if [ $# -lt 1 ]; then
      echo "[ERROR] Missing extension name argument !"
      echo ""
      usage
      exit 1;
    fi
    uninstall_extension $1
  ;;
  *)
    echo "[ERROR] Invalid action \"${ACTION}\""
    echo ""
    usage
    exit 1
  ;;
esac

exit 0