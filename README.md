Meeds Public Distribution
=========================

How to build ?
--------------

**Pre-requisite** : Apache Maven >= 3.5.0

To build the Meeds Community package, from the root directory just launch :

    mvn install

Take a coffee, a tea, whatever you like the time that Maven downloads the earth and then you'll have the result in `plf-community-tomcat-standalone/target/meeds-community-<<CURRENT_VERSION>>/meeds-community-<<CURRENT_VERSION>>`

If you are using a Maven Repository Manager you need to proxify our repository <https://repository.exoplatform.org/public/> (snapshots and releases).

If you are using a mirror in your maven settings you need to exlude our repository identifier `repository.exoplatform.org` if your mirror don't proxify it.

Build options
-------------

Add `-Dskip-archive` in your build command line to not generate the final zip archive (and thus gain few seconds of build).

How to launch ?
---------------

From the top level directory of the project oou can launch the server you just generated with :

    plf-community-tomcat-standalone/target/meeds-community-<<CURRENT_VERSION>>/meeds-community-<<CURRENT_VERSION>>/start_eXo.(sh|bat)

Use `-h` or `--help` to list all options.

Known issues
============

Windows users
-------------

Scripts have to be launched from the directory where they are otherwise you have to explicitly set the environment variable `CATALINA_HOME` to the server home directory path.

You can force to activate the colorized console with the option 

    plf-community-tomcat-standalone/target/meeds-community-<<CURRENT_VERSION>>/meeds-community-<<CURRENT_VERSION>>/start_eXo.bat --color
