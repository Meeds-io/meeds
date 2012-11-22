platform-tomcat-standalone
==========================

eXo Platform Standalone Tomcat Distribution

Prerequisites
-----------------

Apache Maven 3.0.4

Required settings
-----------------

For now to build the eXo Platform Standalone Tomcat Distribution you need to access to our private repositories either by adding in the command line :

```
 -Pexo-private
```

Or by activating it permanently in your Maven settings

```
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
                      http://maven.apache.org/xsd/settings-1.0.0.xsd">
  ...
  <activeProfiles>
    <activeProfile>exo-private</activeProfile>
    ...
  </activeProfiles>
  ...
</settings>
```

How to build
------------

By default you just need to call

```
mvn install
```

You can speedup the process and not generate the archive to have only the application server directory with

```
mvn install -P!create-archive
```

or with

```
mvn install -Dskip-archive
```

You can permanently deactivate the archive creation by adding in your maven settings

```
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
                      http://maven.apache.org/xsd/settings-1.0.0.xsd">
...
  <profiles>
    <profile>
      <id>local-properties</id>
      <properties>
        <skip-archive>true</skip-archive>
        ...
      </properties>
    </profile>
    ...
  </profiles>
  <activeProfiles>
    <activeProfile>local-properties</activeProfile>
    ...
  </activeProfiles>
  ...
</settings>
```

By default Maven updates its snapshots dependencies once a day. You may enforce it to update by adding the -U flag.

How to use
----------

The application server is created under target/platform-packaging-tomcat-VERSION.

You can start the application with

```
target/platform-packaging-tomcat-VERSION/start_eXo.sh
```

This is a non-blocking run. Logs will be under target/platform-packaging-tomcat-VERSION/logs/ and target/platform-packaging-tomcat-VERSION/gatein/logs

You can stop the application with

```
target/platform-packaging-tomcat-VERSION/stop_eXo.sh
```

While developing it may be better to use the blocking command line that will display logs in your current console

```
target/platform-packaging-tomcat-VERSION/run_eXo.sh
```

You'll stop the server with Ctrl+C

Notes : Behind the scene, these scripts are just calling all classical Apache Tomcat scripts. You can directly use these ones especially to setup you server as a service in your operating system or to manage the application server from your IDE (using WTP for example in eclipse).
