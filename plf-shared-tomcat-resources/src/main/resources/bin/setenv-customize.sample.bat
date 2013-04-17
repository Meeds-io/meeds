@REM
@REM Copyright (C) 2003-2013 eXo Platform SAS.
@REM
@REM This is free software; you can redistribute it and/or modify it
@REM under the terms of the GNU Lesser General Public License as
@REM published by the Free Software Foundation; either version 3 of
@REM the License, or (at your option) any later version.
@REM
@REM This software is distributed in the hope that it will be useful,
@REM but WITHOUT ANY WARRANTY; without even the implied warranty of
@REM MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
@REM Lesser General Public License for more details.
@REM
@REM You should have received a copy of the GNU Lesser General Public
@REM License along with this software; if not, write to the Free
@REM Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
@REM 02110-1301 USA, or see the FSF site: http://www.fsf.org.
@REM

@echo off

REM Copyright (C) 2012 eXo Platform SAS.
REM 
REM This is free software; you can redistribute it and/or modify it
REM under the terms of the GNU Lesser General Public License as
REM published by the Free Software Foundation; either version 2.1 of
REM the License, or (at your option) any later version.
REM 
REM This software is distributed in the hope that it will be useful,
REM but WITHOUT ANY WARRANTY; without even the implied warranty of
REM MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
REM Lesser General Public License for more details.
REM 
REM You should have received a copy of the GNU Lesser General Public
REM License along with this software; if not, write to the Free
REM Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
REM 02110-1301 USA, or see the FSF site: http://www.fsf.org.

REM ---------------------------------------------------------------------------
REM Settings customisation
REM ---------------------------------------------------------------------------
REM You have 2 ways to customize your installation settings :
REM 1- Rename the file setenv-customize.sample.bat to setenv-customize.bat and uncomment/change values
REM 2- Use system environment variables of your system or local shell
REM ---------------------------------------------------------------------------

REM ---------------------------------------------------------------------------
REM Default EXO PLATFORM configuration
REM ---------------------------------------------------------------------------
REM SET EXO_PROFILES=all
REM SET EXO_DEBUG=false
REM SET EXO_DEBUG_PORT=8000
REM SET EXO_DEV=false

REM ---------------------------------------------------------------------------
REM Default Logs configuration
REM ---------------------------------------------------------------------------
REM Default configuration for logs (using logback framework - http://logback.qos.ch/manual/configuration.html )
REM SET EXO_LOGS_LOGBACK_CONFIG_FILE=%CATALINA_BASE%/conf/logback.xml
REM SET EXO_LOGS_DISPLAY_CONSOLE=true
REM SET EXO_LOGS_COLORIZED_CONSOLE=true

REM ---------------------------------------------------------------------------
REM Default JVM configuration
REM ---------------------------------------------------------------------------
REM SET EXO_JVM_SIZE_MAX=1g
REM SET EXO_JVM_SIZE_MIN=512m
REM SET EXO_JVM_PERMSIZE_MAX=256m
REM SET EXO_JVM_PERMSIZE_MIN=128m

REM ---------------------------------------------------------------------------
REM Default Tomcat configuration
REM ---------------------------------------------------------------------------
REM Global Tomcat settings
REM SET EXO_TOMCAT_UNPACK_WARS=%EXO_DEV%