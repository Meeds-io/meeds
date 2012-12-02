@echo off

@rem
@rem Copyright (C) 2012 eXo Platform SAS.
@rem
@rem This is free software; you can redistribute it and/or modify it
@rem under the terms of the GNU Lesser General Public License as
@rem published by the Free Software Foundation; either version 2.1 of
@rem the License, or (at your option) any later version.
@rem
@rem This software is distributed in the hope that it will be useful,
@rem but WITHOUT ANY WARRANTY; without even the implied warranty of
@rem MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
@rem Lesser General Public License for more details.
@rem
@rem You should have received a copy of the GNU Lesser General Public
@rem License along with this software; if not, write to the Free
@rem Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
@rem 02110-1301 USA, or see the FSF site: http://www.fsf.org.
@rem
 
setlocal ENABLEDELAYEDEXPANSION
rem Computes the absolute path of eXo
for %%i in ( !%~f0! ) do set SERVER_DIR=%%~dpi

cd %SERVER_DIR%bin

rem Launches the server
call catalina.bat start