Meeds Public Distribution
=========================

How to build ?
--------------

**Pre-requisite** : Apache Maven >= 3.5.0

To build the Meeds Community package, from the root directory just launch :

    mvn install

Take a coffee, a tea, whatever you like the time that Maven downloads the earth and then you'll have the result in `plf-community-tomcat-standalone/target/meeds-community-<<CURRENT_VERSION>>/meeds-community-<<CURRENT_VERSION>>`

If you are using a Maven Repository Manager you need to proxify our repository <https://repository.exoplatform.org/public/> (snapshots and releases).

If you are using a mirror in your maven settings you need to exclude our repository identifier `repository.exoplatform.org` if your mirror doesn't proxify it.

Build options
-------------

Add `-Dskip-archive` in your build command line to not generate the final zip archive (and thus gain few seconds of build).

How to launch ?
---------------

From the top level directory of the project you can launch the server you just generated with :

    plf-community-tomcat-standalone/target/meeds-community-<<CURRENT_VERSION>>/meeds-community-<<CURRENT_VERSION>>/start_eXo.(sh|bat)

Use `-h` or `--help` to list all options.

Known issues
============

Windows users
-------------

Scripts have to be launched from the directory where they are otherwise you have to explicitly set the environment variable `CATALINA_HOME` to the server home directory path.

You can force to activate the colorized console with the option 

    plf-community-tomcat-standalone/target/meeds-community-<<CURRENT_VERSION>>/meeds-community-<<CURRENT_VERSION>>/start_eXo.bat --color
    
:octocat: ghcr.io Docker image signature
========================================

Starting with Meeds `1.4.0-M04` from the github container registry, Meeds docker images will be signed with [cosign] (https://github.com/sigstore/cosign) tool.

In order to verify the signature of the Meeds docker image, please install the "cosign" command line tool. Then please follow these instructions:

- Save the following public key to `cosign.pub` file:
```gpg
-----BEGIN PUBLIC KEY-----
MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE2XMvg1dA7ZgfLB3c2nc8E/D+IsS+
DjvPlcu00HT94eL4R8QpcQO7YoBiiqFkxBnuHYl7nEclZ/fyQ1srG+UMjw==
-----END PUBLIC KEY-----
```
- Execute the following command:
```bash
cosign verify --key cosign.pub ghcr.io/meeds-io/meeds/meeds-io:<tag>
```
*Example:*
```bash
cosign verify --key cosign.pub ghcr.io/meeds-io/meeds/meeds-io:develop
```
  Output:
```json
[{"critical":{"identity":{"docker-reference":"ghcr.io/meeds-io/meeds/meeds-io"},"image":{"docker-manifest-digest":"sha256:da29f98a3000ae5232ceb2502ce2ae10903969c762b1d3d4e43a8b7104b87888"},"type":"cosign container image signature"},"optional":null}]
```
Also starting with Meeds `1.5.0-M11` from DockerHub, Meeds docker [meedsio/meeds](https://hub.docker.com/r/meedsio/meeds) image will be signed with `cosign` tool. In order to verify its signature:

Execute the following command:
```bash
cosign verify --key cosign.pub meedsio/meeds:<tag>
```
*Example:*
```bash
cosign verify --key cosign.pub meedsio/meeds:1.5.0-M11_0
```
Our Meeds image is also signed with Dockerhub [DCT](https://docs.docker.com/engine/security/trust/).
```bash
docker trust inspect --pretty meedsio/meeds
```

## Thanks to all the contributors ❤️
<a href = "https://github.com/Meeds-io/meeds/graphs/contributors">
  <img src = "https://contrib.rocks/image?repo=Meeds-io/meeds"/>
</a>
