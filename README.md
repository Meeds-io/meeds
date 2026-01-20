# Meeds Public Distribution

## How to Build

**Prerequisite:** Apache Maven ≥ 3.5.0

To build the Meeds Community package, run the following command from the project root directory:

```bash
mvn install
```

Grab a coffee or tea while Maven downloads its dependencies. Once completed, the build will be available in:

```
plf-community-tomcat-standalone/target/meeds-community-<<CURRENT_VERSION>>/meeds-community-<<CURRENT_VERSION>>
```

> **Note:**
>
> * If you are using a Maven Repository Manager, proxy our repository: [https://repository.exoplatform.org/public/](https://repository.exoplatform.org/public/) (for snapshots and releases).
> * If you are using a mirror in your Maven settings, exclude the repository identifier `repository.exoplatform.org` if your mirror doesn’t proxy it.

### Build Options

Add the `-Dskip-archive` option to your build command to skip generating the final ZIP archive, saving a few seconds:

```bash
mvn install -Dskip-archive
```

## How to Launch

From the project root, start the generated server with:

```bash
plf-community-tomcat-standalone/target/meeds-community-<<CURRENT_VERSION>>/meeds-community-<<CURRENT_VERSION>>/start_eXo.(sh|bat)
```

Use `-h` or `--help` to list all available options.

## Known Issues

### Windows Users

* Scripts must be launched from their directory.
* Alternatively, set the `CATALINA_HOME` environment variable to the server home directory path.
* To enable the colorized console, use:

```bash
start_eXo.bat --color
```

## \:octocat: Docker Image Signatures

Starting with **Meeds `1.4.0-M04`**, Docker images from GitHub Container Registry (GHCR) are signed using [Cosign](https://github.com/sigstore/cosign).

### Verify GHCR Images

1. Save the following public key to a file named `cosign.pub`:

```text
-----BEGIN PUBLIC KEY-----
MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE2XMvg1dA7ZgfLB3c2nc8E/D+IsS+
DjvPlcu00HT94eL4R8QpcQO7YoBiiqFkxBnuHYl7nEclZ/fyQ1srG+UMjw==
-----END PUBLIC KEY-----
```

2. Verify the image:

```bash
cosign verify --key cosign.pub ghcr.io/meeds-io/meeds/meeds-io:<tag>
```

*Example:*

```bash
cosign verify --key cosign.pub ghcr.io/meeds-io/meeds/meeds-io:develop
```

Expected output:

```json
[{"critical":{"identity":{"docker-reference":"ghcr.io/meeds-io/meeds/meeds-io"},"image":{"docker-manifest-digest":"sha256:da29f98a3000ae5232ceb2502ce2ae10903969c762b1d3d4e43a8b7104b87888"},"type":"cosign container image signature"},"optional":null}]
```

### Verify DockerHub Images

Starting with **Meeds `1.5.0-M11`**, DockerHub images (`meedsio/meeds`) are also signed with Cosign. To verify:

```bash
cosign verify --key cosign.pub meedsio/meeds:<tag>
```

*Example:*

```bash
cosign verify --key cosign.pub meedsio/meeds:1.5.0-M11_0
```

## Thanks to All Contributors ❤️

<a href="https://github.com/Meeds-io/meeds/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=Meeds-io/meeds"/>
</a>
