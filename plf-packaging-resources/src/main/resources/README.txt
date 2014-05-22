====
    Copyright (C) 2003-2014 eXo Platform SAS.

    This is free software; you can redistribute it and/or modify it
    under the terms of the GNU Lesser General Public License as
    published by the Free Software Foundation; either version 3 of
    the License, or (at your option) any later version.

    This software is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
    Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public
    License along with this software; if not, write to the Free
    Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
    02110-1301 USA, or see the FSF site: http://www.fsf.org.
====

Thank you for downloading eXo Platform ${org.exoplatform.platform.version}.

Follow the installation procedure and start eXo Platform 4.1 now!

------------------------------
System requirements
------------------------------

    * CPU:     3 GHz Multi-core recommended
    * Memory:  4GB of RAM (8GB recommended)
    * Disk:    2GB (depending of the amount of data)
    * OS:      Windows or Linux
    * JDK:     Java 6 or 7 (Set the JAVA_HOME environment variable)
    * Browser: Google Chrome 25+, Firefox 19+ or Internet Explorer 8+
    * The eXo server will run on port 8080, make sure this port is not currently in use

-------------------------------------
How to start the Platform Tomcat
-------------------------------------

    * PLF_HOME is the location of the unzipped eXo Platform server.
    * On Windows: Open a DOS prompt command, go to PLF_HOME directory and type the command: "start_eXo.bat"
    * On Linux: Open a terminal, go to PLF_HOME directory and type the command: "./start_eXo.sh"

------------------------------------
How to start the Platform Jboss (*)
------------------------------------

    * PLF_JBOSS_HOME is the location of JBoss EAP 6.2.0
    * Extract eXo Platform Jboss package
    * Copy all the extracted folders and files into PLF_JBOSS_HOME.
    * On Windows: Open a DOS prompt command, go to PLF_JBOSS_HOME directory and type the command: "bin\standalone.bat"
    * On Linux: Open a terminal, go to PLF_JBOSS_HOME directory and type the command: "./bin/standalone.sh"

(*) only available for Enterprise edition.

For more configurations, please check http://docs.exoplatform.com/PLF41/PLFAdministratorGuide.html

----------------------------------------------------------
How to access the Platform homepage
----------------------------------------------------------

    * Wait for the server to start. You should see something like this on the console

      INFO  | Server startup in XXXX ms

    * Enter the following URL into your browser's address bar: http://localhost:8080/portal

-------------------------------------
How to install extensions
-------------------------------------

Several extensions are not installed by default in the Express and Enterprise version of eXo Platform 4: 
    * crash	: Common Reusable SHell to interact with the JVM
    * acme (*)	: A demo website built with eXo Platform 4 (cf. next section for its usage)
    * cmis (*)	: Content Management Interoperability Services 
    * ide (*)	: Integrated online environment to develop applications (cf. next section for its usage)
    * wai (*)	: A demo website following Accessibility standards 

On Windows, Open a DOS prompt command, go to PLF_HOME directory and type the command:
    * To install an extension use: extension.bat --install <extension>
    * To install all available extensions use: extension.bat --install all
    * List all available extensions use: extension.bat --list

On Linux: Open a terminal, go to PLF_HOME directory and type the command :
    * To install an extension use: extension.sh --install <extension>
    * To install all available extensions use: extension.sh --install all
    * List all available extensions use: extension.sh --list

(*) only on Express and Enterprise editions

-----------------------------
Deployment of acme website
-----------------------------

When eXo Platform server is already started and you would like to install acme extension, you need to follow the steps below:
* Stop eXo Platform server
* Install acme extension with the extension script
* Set the variable "acme.portalConfig.metadata.override" as true. This can be done by one of the two ways:
** Use configuration.properties:
  In this file, you uncomment the line of this parameter
** Use customization configuration script:
  When you use the sample configuration script provided inside eXo Platform 4.1, you can uncomment this variable in the script.
  Please refer to "Customizing environment variables in Tomcat" or "Installing JBoss EAP" in Platform 4.1 Administration guide for more details of these scripts.
* Restart eXo Platform server.
* Stop eXo Platform server.
* Set the variable "acme.portalConfig.metadata.override" as false or comment again the corresponding line of this variable.
* Start eXo Platform server again to continue using it.

----------------------------------------------------------------
Exclusion of the IDE item from Administration drop-down menu
----------------------------------------------------------------

When eXo Platform server integrates the ide extension and you would like to exclude the IDE item from Administration drop-down menu, follow the steps below:
* Stop eXo Platform server
* Set the variable "ide.portalConfig.metadata.override" as false . This can be done by one of the two ways:
** Use configuration.properties:
  In this file, you uncomment the line of this parameter
** Use customization configuration script:
  When you use the sample configuration script provided inside eXo Platform 4.1, you can uncomment this variable in the script.
  Please refer to "Customizing environment variables in Tomcat" or "Installing JBoss EAP" in Platform 4.1 Administration guide for more details of these scripts.
* Restart eXo Platform server.

------------------
eXo Resources
------------------

Community         http://community.exoplatform.org
Forum             http://forum.exoplatform.org
Documentation     http://docs.exoplatform.com
Blog              http://blog.exoplatform.org
Support           http://support.exoplatform.com
eXo               http://www.exoplatform.com
Training          http://www.exoplatform.com/company/public/website/services/development/development-training
Consulting        http://www.exoplatform.com/company/public/website/services/development/development-consulting



