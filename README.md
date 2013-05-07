eXo Platform Public Distributions
=================================

How to build ?
--------------

**Pre-requisite** : Apache Maven >= 3.0.4

To build the Platform Community Tomcat Standalone package, from the root directory just launch :

    mvn install -s settings.xml

Take a coffee, a tea, whatever you like the time that Maven downloads the earth and then you'll have the result in `plf-community-tomcat-standalone/target/plf-community-tomcat-standalone-<<CURRENT_VERSION>>/plf-community-tomcat-standalone-<<CURRENT_VERSION>>`

The `settings.xml` file at the root of the project gives you :

*   the good setup for remote repositories (You need to declare <https://repository.exoplatform.org/public/> as remote repository if you want to use your own settings)
*   a custom local repository (`${user.home}/.m2/exo-repository/`) to no pollute your own one.

Build options
-------------

Add `-Dskip-archive` in your build command line to not generate the final zip archive (and thus gain few seconds of build).

*TIP* : Your build will be faster (~40%) using a JDK7

How to launch ?
---------------

From the top level directory of the project oou can launch the server you just generated with :

    plf-community-tomcat-standalone/target/platform-community-<<CURRENT_VERSION>>/platform-community-<<CURRENT_VERSION>>/start_eXo.(sh|bat)

Use `-h` or `--help` to list all options.

Known issues
============

Windows users
-------------

Scripts have to be launched from the directory where they are otherwise you have to explicitly set the environment variable `CATALINA_HOME` to the server home directory path.

Windows 64b users
-----------------

You can activate the colorized console by editing the configuration file

    plf-community-tomcat-standalone/target/platform-community-<<CURRENT_VERSION>>/platform-community-<<CURRENT_VERSION>>/gatein/configuration.properties

You need to had this line which is commented by default

    exo.logs.console.colorized=true

Depending of your system you may need to install the package [Microsoft Visual C++ 2008 Redistributable Package (x64)](http://www.microsoft.com/en-us/download/confirmation.aspx?id=15336)
