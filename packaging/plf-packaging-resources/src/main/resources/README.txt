
Thank you for downloading eXo Platform ${org.exoplatform.platform.version}.

------------------------------
System requirements
------------------------------

    * CPU:     3 GHz Multi-core recommended
    * Memory:  4GB of RAM (8GB recommended)
    * Disk:    2GB (depending of the amount of data)
    * OS:      Windows or Linux
    * JDK:     Java 8 (Set the JAVA_HOME environment variable)
    * Browser (desktop): Google Chrome 68+, Firefox 60+, Edge 17+
    * Browser (mobile): Google Chrome 68+ (Android and iOS) or Safari 11.1+ (iOS)
    * The eXo Platform server will run on port 8080, make sure this port is not currently in use


More compatibility info is available at : 
https://www.exoplatform.com/legal/supported-environments.pdf

-------------------------------------
How to start the Platform Tomcat
-------------------------------------

    * PLF_HOME is the location of the unzipped eXo Platform server.
    * On Windows: Open a DOS prompt command, go to PLF_HOME directory and type the command: "start_eXo.bat"
    * On Linux: Open a terminal, go to PLF_HOME directory and type the command: "./start_eXo.sh"


----------------------------------------------------------
How to access the Platform homepage
----------------------------------------------------------

    * Wait for the server to start. You should see something like this on the console

      INFO  | Server startup in XXXX ms

    * Enter the following URL into your browser's address bar: http://localhost:8080/portal

-------------------------------------
How to install add-ons
-------------------------------------

Several add-ons are not installed by default. To find a list of compatible add-ons, use the add-ons manager.


On Windows, Open a DOS prompt command, go to PLF_HOME directory and type the command:
    * To install an add-on use: addon.bat install <add-on>
    * List all available add-ons use: addon.bat list

On Linux: Open a terminal, go to PLF_HOME directory and type the command :
    * To install an add-on use: addon --install <add-on>
    * List all available add-ons use: addon list


------------------
eXo Resources
------------------

Community         https://community.exoplatform.com
Documentation     https://www.exoplatform.com/docs
Blog              https://www.exoplatform.com/blog
eXo               https://www.exoplatform.com
