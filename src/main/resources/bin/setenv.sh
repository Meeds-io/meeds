#!/bin/sh

#
# Copyright (C) 2012 eXo Platform SAS.
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

# Only set CATALINA_HOME if not already set
[ -z "$CATALINA_HOME" ] && CATALINA_HOME=`cd "$PRGDIR/.." >/dev/null; pwd`

# Sets some variables
LOG_OPTS="-Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.SimpleLog"
SECURITY_OPTS="-Djava.security.auth.login.config=${CATALINA_HOME}/conf/jaas.conf"
EXO_OPTS="-Dexo.conf.dir.name=gatein/conf -Dexo.conf.dir=${CATALINA_HOME}/gatein/conf"
IDE_OPTS="-Djavasrc=$JAVA_HOME/src.zip -Djre.lib=$JAVA_HOME/jre/lib"
if [ "$EXO_PROFILES" = "" -o "$EXO_PROFILES" = "-Dexo.profiles=default" ] ; then
	EXO_PROFILES="-Dexo.profiles=default"
fi

##### For XML Parser ####
# Define the preferred XML Parser
# If you run eXo Platform on IBM Java, you must use the first one
#EXO_XML="-Djavax.xml.stream.XMLOutputFactory=com.sun.xml.stream.ZephyrWriterFactory -Djavax.xml.stream.XMLInputFactory=com.sun.xml.stream.ZephyrParserFactory -Djavax.xml.stream.XMLEventFactory=com.sun.xml.stream.events.ZephyrEventFactory"
EXO_XML="-Djavax.xml.stream.XMLOutputFactory=com.sun.xml.internal.stream.XMLOutputFactoryImpl -Djavax.xml.stream.XMLInputFactory=com.sun.xml.internal.stream.XMLInputFactoryImpl -Djavax.xml.stream.XMLEventFactory=com.sun.xml.internal.stream.events.XMLEventsFactoryImpl"
#########################
CATALINA_OPTS="-Xms256m -Xmx1024m -XX:MaxPermSize=256m $CATALINA_OPTS $LOG_OPTS $SECURITY_OPTS $EXO_OPTS $IDE_OPTS $EXO_PROFILES $EXO_XML"
export CATALINA_OPTS